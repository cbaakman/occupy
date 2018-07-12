package net.cbaakman.occupy;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;

import net.cbaakman.occupy.network.annotations.ClientToServer;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

public abstract class Client {

	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();

	public abstract void run();
	
	public abstract void connectToServer();
	protected abstract void sendMessage(Message message);

	protected void onMessage(Message message) {
		
		if (message.getType().equals(MessageType.UPDATE)) {
			processUpdateFromServer((Update)message.getData());
		}
		else if (message.getType().equals(MessageType.DISCONNECT)) {
		}
	}
	
	private void processUpdateFromServer(Update update) {
		Updatable updatable;
		
		synchronized(updatables) {
			updatable = updatables.get(update.getObjectID());
		}
			
		if (updatable == null) {
			try {
				updatable = update.getObjectClass().newInstance();
				
				synchronized(updatables) {
					updatables.put(update.getObjectID(), updatable);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
		}
		
		try {
			Field field = update.getObjectClass().getField(update.getFieldID());
			
			if (field.isAnnotationPresent(ServerToClient.class)) {
			
				field.setAccessible(true);
				field.set(updatable, update.getValue());
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	protected void update(float dt) {
		synchronized (updatables) {
			for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
				entry.getValue().updateOnClient(dt);
			}
		}
		synchronized(updatables) {
			for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
				UUID objectId = entry.getKey();
				Updatable updatable = entry.getValue();
				
				Class<? extends Updatable> objectClass = updatable.getClass();
				
				for (Field field : objectClass.getDeclaredFields()) {
					
					if (field.isAnnotationPresent(ClientToServer.class)) {
						
						field.setAccessible(true);
				
						try {
							Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));

							sendMessage(new Message(MessageType.UPDATE, update));
							
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	protected abstract void shutdown();
}
