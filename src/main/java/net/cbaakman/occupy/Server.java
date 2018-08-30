package net.cbaakman.occupy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Map.Entry;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.Data;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Authenticator;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Connection;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.communicate.ServerInfo;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.communicate.enums.RequestType;
import net.cbaakman.occupy.communicate.enums.ResponseType;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorQueue;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.RenderError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.game.PlayerRecord;
import net.cbaakman.occupy.security.SSLChannel;
import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class Server {
	
	final static Logger logger = Logger.getLogger(Server.class);
	
	private ErrorQueue errorQueue = new ErrorQueue();
	private volatile boolean running;
	
	@Data
	private class ClientRecord {
		private PlayerRecord playerRecord;
		
		private Date lastContact = new Date();
		
		public ClientRecord(String username) {
			// TODO: lookup player record on disk
			this.playerRecord = new PlayerRecord(username);
		}
	}
	
	private SynchronizedPool<UUID, Updatable> updatables = new SynchronizedPool<UUID, Updatable>();
	private SynchronizedPool<Object, ClientRecord> clientRecords = new SynchronizedPool<Object, ClientRecord>();
	
	protected ServerConfig config;
	private Object serverId = UUID.randomUUID();
	
	public Server(ServerConfig config) {
		this.config = config;
	}

	public static String getVersion() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model;
		try {
			model = reader.read(new FileReader("pom.xml"));
		} catch (IOException | XmlPullParserException e) {
			throw new SeriousError(e);
		}
		return model.getVersion();
	}
	
	public ServerInfo getInfo() {
		ServerInfo info = new ServerInfo();
		
		info.setServerId(serverId);
		
		info.setServerVersion(getVersion());
		
		return info;
	}

	public void run() throws InitError {
		
		running = true;

		initCommunication();
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		
		try {
			while (running) {
				ticks = System.currentTimeMillis();
				dt = (float)(ticks - ticks0) / 1000;
				ticks0 = ticks;
				
				try {
					update(dt);
				} catch (CommunicationError e) {
					logger.error(e.getMessage(), e);
				}
			}
		} finally {  // always try to shut down tidily
			shutdownCommunication();
		}
	}
	
	public void stop() {
		running = false;
	}
	
	protected abstract void shutdownCommunication();

	protected abstract void initCommunication() throws InitError;

	protected void onClientConnect(Object clientId, Connection connection)
			throws CommunicationError {
		
		if (clientRecords.hasKey(clientId)) {
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
			else if (request.equals(RequestType.GET_SERVER_INFO)) {
				writeResponse(ResponseType.OK, connection);
				sendInfo(connection);
			}
		} catch (ClassNotFoundException | InvalidKeyException | NoSuchPaddingException |
				 IllegalBlockSizeException | BadPaddingException e) {
			
			throw new SeriousError(e);
			
		} catch (IOException e) {
			throw new CommunicationError(e);
		}
	}
	
	private void sendInfo(Connection connection) throws IOException {
		ServerInfo info = getInfo();

		ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
		oos.writeObject(info);
		oos.flush();
	}
	
	private File getPasswordFile() {
		if (config.getDataDir() == null)
			throw new SeriousError("no data dir set in config");
		
		return new File(config.getDataDir().toFile(), "passwd");
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

	private void onClientLogin(Object clientId, Credentials credentials) throws AuthenticationError {
		
		Authenticator authenticator = new Authenticator(getPasswordFile());
		
		try {
			if (!authenticator.authenticate(credentials))
				throw new AuthenticationError("invalid credentials");
		} catch (IOException e) {
			throw new SeriousError(e);
		}
		
		synchronized(clientRecords) {
			clientRecords.put(clientId, new ClientRecord(credentials.getUsername()));
		}
	}
	
	protected void logoutClient(Object clientId) {

		clientRecords.remove(clientId);
	}
	
	protected void onPacket(Object clientId, Packet packet) throws SeriousError {
				
		if (clientRecords.hasKey(clientId)) {
			
			clientRecords.get(clientId).setLastContact(new Date());

			if (packet.getType().equals(PacketType.UPDATE)) {
				processUpdate(clientId, (Update)packet.getData());
			}
			else if (packet.getType().equals(PacketType.LOGOUT)) {	
				logoutClient(clientId);
			}
			else if (packet.getType().equals(PacketType.PING)) {
				int bounce = (Integer)packet.getData();
				if (bounce > 0) {
					bounce--;
					try {
						sendPacket(clientId, new Packet(PacketType.PING, bounce));
					} catch (CommunicationError e) {
						logger.error(e.getMessage(), e);
					}
				}
			}
		}
	}

	protected void processUpdate(Object clientId, Update update) throws SeriousError {
		
		Updatable updatable = updatables.get(update.getObjectID());
			
		if (updatable == null)
			return;
		
		for (Entry<String, Object> entry : update.getFieldValues().entrySet()) {
			String fieldId = entry.getKey();
			Object value = entry.getValue();
			
			try {
				Field field = update.getObjectClass().getField(fieldId);
				
				if (field.isAnnotationPresent(ClientToServer.class)) {
				
					field.setAccessible(true);
					field.set(updatable, value);
				}
				
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
					IllegalAccessException e) {
				throw new SeriousError(e);
			}
		}
	}
	
	protected abstract void sendPacket(Object clientId, Packet packet) throws CommunicationError;
	
	private void update(float dt) throws CommunicationError {
				
		// timeout clients
		Date now = new Date();
		for (Object id : clientRecords.getKeys()) {
			ClientRecord clientRecord = clientRecords.get(id);
			if ((now.getTime() - clientRecord.getLastContact().getTime()) > config.getContactTimeoutMS()) {
				clientRecords.remove(id);
			}
		}
		
		for (Updatable updatable : updatables.getAll()) {
			updatable.updateOnServer(dt);
		}
		
		for (Object clientId : clientRecords.getKeys()) {
			ClientRecord clientRecord = clientRecords.get(clientId);
			
			// Ping to maintain contact:
			sendPacket(clientId, new Packet(PacketType.PING, 1));
			
			for (UUID objectId : updatables.getKeys()) {
				Updatable updatable = updatables.get(objectId);
				
				Class<? extends Updatable> objectClass = updatable.getClass();
				
				Update update = new Update(objectClass, objectId);
				
				for (Field field : updatable.getDeclaredFieldsSinceUpdatable()) {
					
					if (field.isAnnotationPresent(ServerToClient.class) ||
						field.isAnnotationPresent(ClientToServer.class) && 
						!updatable.mayBeUpdatedBy(clientRecord.getPlayerRecord())) {

						field.setAccessible(true);
						
						try {
							update.setValue(field.getName(), field.get(updatable));
							
						} catch (IllegalArgumentException | IllegalAccessException e) {
							throw new SeriousError(e);
						}
					}
				}
				sendPacket(clientId, new Packet(PacketType.UPDATE, update));
			}
		}
		
		try {
			errorQueue.throwAnyFirstEncounteredError();
		} catch (RenderError | InitError e) {
			// Server doesn't render or have separate init threads!
			throw new SeriousError(e);
		}
	}
	
	public ErrorQueue getErrorQueue() {
		return errorQueue;
	}
	
	public <T extends Updatable> void addUpdatable(T updatable) {
		updatables.put(UUID.randomUUID(), updatable);
	}
}
