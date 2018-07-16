package net.cbaakman.occupy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.enums.MessageType;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;

public abstract class Server {
	
	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	private Set<UUID> clientIds = new HashSet<UUID>();
	
	private ErrorHandler errorHandler;
	
	public Server(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public abstract void run() throws InitError;
	
	protected void onClientLogin(UUID clientId, Credentials credentials) throws AuthenticationError {
		
		// For now, always accept the credentials.
		
		synchronized(clientIds) {
			clientIds.add(clientId);
		}
	}
	
	protected abstract void logoutClient(UUID clientId);
	
	protected void onMessage(UUID clientId, Message message) {

		if (message.getType().equals(MessageType.UPDATE)) {
			processUpdate(clientId, (Update)message.getData());
		}
		else if (message.getType().equals(MessageType.LOGOUT)) {	
			logoutClient(clientId);
			synchronized(clientIds) {
				clientIds.remove(clientId);
			}
		}
	}

	protected void processUpdate(UUID clientId, Update update) {
		
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
	
	protected abstract void sendMessage(UUID clientId, Message message);
	
	protected void update(float dt) {
		synchronized (updatables) {
			for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
				entry.getValue().updateOnServer(dt);
			}
		}
		
		synchronized(clientIds) {
			for (UUID clientId : clientIds) {
				synchronized(updatables) {
					for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
						UUID objectId = entry.getKey();
						Updatable updatable = entry.getValue();
						
						Class<? extends Updatable> objectClass = updatable.getClass();
						
						for (Field field : objectClass.getDeclaredFields()) {
							
							if (field.isAnnotationPresent(ServerToClient.class) ||
								field.isAnnotationPresent(ClientToServer.class) &&
								!clientId.equals(updatable.getOwnerId())) {
								
								field.setAccessible(true);
						
								try {
									Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));

									sendMessage(clientId, new Message(MessageType.UPDATE, update));
									
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
