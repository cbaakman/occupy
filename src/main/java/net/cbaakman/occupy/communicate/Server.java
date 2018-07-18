package net.cbaakman.occupy.communicate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.Data;
import lombok.Setter;
import lombok.AccessLevel;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.Update;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.communicate.enums.RequestType;
import net.cbaakman.occupy.communicate.enums.ResponseType;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.security.SSLChannel;
import org.apache.log4j.Logger;

public abstract class Server {
	
	final static Logger logger = Logger.getLogger(Server.class);
	
	@Data
	private class ClientRecord {
		
		@Setter(AccessLevel.NONE)
		private Identifier clientId;
		
		private Date lastContact = new Date();
		
		public ClientRecord(Identifier id) {
			this.clientId = id;
		}
	}
	
	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	private Map<Identifier, ClientRecord> clientRecords = new HashMap<Identifier, ClientRecord>();
	
	protected ErrorHandler errorHandler;
	protected ServerConfig config;
	
	public Server(ErrorHandler errorHandler, ServerConfig config) {
		this.errorHandler = errorHandler;
		this.config = config;
	}

	public abstract void run() throws InitError;
	
	protected void onClientConnect(Identifier clientId, Connection connection) {
		
		logger.debug("received a client connection");
		
		if (clientRecords.containsKey(clientId)) {
			clientRecords.get(clientId).setLastContact(new Date());
		}
		
		try {
			RequestType request = readRequest(connection);
			
			if (request.equals(RequestType.LOGIN)) {
				Credentials credentials = receiveCredentials(connection);
				
				try {
					onClientLogin(clientId, credentials);
					
					writeResponse(ResponseType.OK, connection);
				} catch (AuthenticationError e) {
					writeResponse(ResponseType.AUTHENTICATION_ERROR, connection);
				}
			}
		} catch (ClassNotFoundException | InvalidKeyException | NoSuchPaddingException |
				 IllegalBlockSizeException | BadPaddingException e) {
			
			// Not supposed to happen!
			SeriousErrorHandler.handle(e);
			
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}
	
	private static Credentials receiveCredentials(Connection connection) throws IOException,
																		 InvalidKeyException,
																		 NoSuchPaddingException,
																		 IllegalBlockSizeException,
																		 BadPaddingException,
																		 ClassNotFoundException {

		SSLChannel sslChannel = new SSLChannel(connection.getInputStream(), connection.getOutputStream());
		byte[] data = sslChannel.receive();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		return (Credentials)ois.readObject();
	}
	
	private static RequestType readRequest(Connection connection) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
		return (RequestType)ois.readObject();
	}
	
	private static void writeResponse(ResponseType response, Connection connection) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
		oos.writeObject(response);
		oos.flush();
	}

	private void onClientLogin(Identifier clientId, Credentials credentials) throws AuthenticationError {
		
		// For now, always accept the credentials.
		
		synchronized(clientRecords) {
			clientRecords.put(clientId, new ClientRecord(clientId));
		}
	}
	
	protected void logoutClient(Identifier clientId) {

		synchronized(clientRecords) {
			clientRecords.remove(clientId);
		}
	}
	
	protected void onPacket(Identifier clientId, Packet packet) {
		
		logger.debug("received a client packet");
		
		if (clientRecords.containsKey(clientId)) {
			
			clientRecords.get(clientId).setLastContact(new Date());

			if (packet.getType().equals(PacketType.UPDATE)) {
				processUpdate(clientId, (Update)packet.getData());
			}
			else if (packet.getType().equals(PacketType.LOGOUT)) {	
				logoutClient(clientId);
			}
		}
	}

	protected void processUpdate(Identifier clientId, Update update) {
		
		Updatable updatable;
		
		synchronized(updatables) {
			updatable = updatables.get(update.getObjectID());
		}
			
		if (updatable == null)
			return;
		
		if (!updatable.getOwnerId().equals(clientId))
			return;
		
		try {
			Field field = update.getObjectClass().getField(update.getFieldID());
			
			if (field.isAnnotationPresent(ClientToServer.class)) {
			
				field.setAccessible(true);
				field.set(updatable, update.getValue());
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
				IllegalAccessException e) {
			// Must not happen!
			SeriousErrorHandler.handle(e);
		}
	}
	
	protected abstract void sendPacket(Identifier clientId, Packet packet);
	
	protected void update(float dt) {
		
		// timeout clients
		synchronized (clientRecords) {
			Date now = new Date();
			for (Entry<Identifier, ClientRecord> entry : clientRecords.entrySet()) {
				ClientRecord clientRecord = entry.getValue();
				if ((now.getTime() - clientRecord.getLastContact().getTime()) > config.getContactTimeoutMS()) {
					clientRecords.remove(clientRecord.getClientId());
				}
			}
		}
		
		synchronized (updatables) {
			for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
				entry.getValue().updateOnServer(dt);
			}
		}
		
		synchronized(clientRecords) {
			for (ClientRecord clientRecord : clientRecords.values()) {
				synchronized(updatables) {
					for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
						UUID objectId = entry.getKey();
						Updatable updatable = entry.getValue();
						
						Class<? extends Updatable> objectClass = updatable.getClass();
						
						for (Field field : objectClass.getDeclaredFields()) {
							
							if (field.isAnnotationPresent(ServerToClient.class) ||
								field.isAnnotationPresent(ClientToServer.class) &&
								!clientRecord.getClientId().equals(updatable.getOwnerId())) {
								
								field.setAccessible(true);
						
								try {
									Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));

									logger.debug(String.format("sending update packet to %s", clientRecord.getClientId()));
									
									sendPacket(clientRecord.getClientId(), new Packet(PacketType.UPDATE, update));
									
								} catch (IllegalArgumentException | IllegalAccessException e) {
									// Must not happen!
									SeriousErrorHandler.handle(e);
								}
							}
						}
					}
				}
			}
		}
	}
	
	public abstract void stop();
	
	protected void onCommunicationError(CommunicationError e) {
		synchronized(e) {
			errorHandler.handle(e);
		}
	}
}