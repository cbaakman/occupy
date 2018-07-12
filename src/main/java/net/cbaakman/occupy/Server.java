package net.cbaakman.occupy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.cbaakman.occupy.network.annotations.ClientToServer;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

public abstract class Server {
	
	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	private Set<UUID> clientIds = new HashSet<UUID>();

	public abstract void run();
	
	protected void onClientConnect(UUID clientId) {
		synchronized(clientIds) {
			clientIds.add(clientId);
		}
	}
	
	protected abstract void disconnectClient(UUID clientId);
	
	protected void onMessage(UUID clientId, Message message) {

		if (message.getType().equals(MessageType.UPDATE)) {
			processUpdate(clientId, (Update)message.getData());
		}
		else if (message.getType().equals(MessageType.DISCONNECT)) {	
			disconnectClient(clientId);
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
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
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
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
	}
	
	protected abstract void shutdown();
}
