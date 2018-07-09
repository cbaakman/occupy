package net.cbaakman.occupy.network;

import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.cbaakman.occupy.config.Config;
import net.cbaakman.occupy.network.annotations.ClientToServer;

public class Client extends Updater {
	
	private Connection serverConnection;
	
	public Client(Connection serverConnection) {
		this.serverConnection = serverConnection;
	}

	@Override
	public void update(final float dt) {
		updateAllFromServer();
		updateAllLocal(dt);
		updateAllToServer();
	}

	private void updateAllToServer() {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			UUID id = entry.getKey();
			Updatable updatable = entry.getValue();
			
			for (Field field : updatable.getClass().getDeclaredFields()) {
				
				if (field.isAnnotationPresent(ClientToServer.class)) {
					field.setAccessible(true);
					
					try {
						Object value = field.get(updatable);

						serverConnection.send(new Update(id, field.getName(), value));
					}
					catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
					catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void updateAllFromServer() {
		for (Update update : serverConnection.poll()) {
			
			UUID updatableID = update.getObjectID();
			String fieldID = update.getFieldID();
			Object newValue = update.getValue();
			
			Updatable updatable = updatables.get(updatableID);
			if (updatable == null) {
				updatable = new Updatable(serverConnection.getOtherEndId());
				updatables.put(updatableID, updatable);
			}
			
			try {
				Field field = updatable.getClass().getDeclaredField(fieldID);
				field.setAccessible(true);
				
				field.set(updatable, newValue);
			}
			catch (NoSuchFieldException e) {
				e.printStackTrace();
			}
			catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private void updateAllLocal(final float dt) {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			entry.getValue().updateOnClient(dt);
		}
	}
}
