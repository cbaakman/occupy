package net.cbaakman.occupy.network;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import net.cbaakman.occupy.network.annotations.ServerToClient;

public class Server extends Updater {
	
	private List<Connection> clientConnections = new ArrayList<Connection>();
	
	public void addClientConnection(Connection connection) {
		clientConnections.add(connection);
	}

	@Override
	public void update(final float dt) {
		updateAllFromClients();
		updateAllLocal(dt);
		updateAllToClients();
	}

	private void updateAllToClients() {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			UUID id = entry.getKey();
			Updatable updatable = entry.getValue();
			
			for (Field field : updatable.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(ServerToClient.class)) {
					field.setAccessible(true);
					
					try {
						Object value = field.get(updatable);

						for (Connection clientConnection : clientConnections)
							clientConnection.send(new Update(id, field.getName(), value));
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

	private void updateAllFromClients() {
		for (Connection clientConnection : clientConnections) {
			
			for (Update update : clientConnection.poll()) {
				
				UUID updatableID = update.getObjectID();
				String fieldID = update.getFieldID();
				Object newValue = update.getValue();
				
				Updatable updatable = updatables.get(updatableID);
				if (updatable == null) {
					updatable = new Updatable(clientConnection.getOtherEndId());
					updatables.put(updatableID, updatable);
				}
				
				if (!updatable.getOwnerID().equals(clientConnection.getOtherEndId()))
					continue;
				
				System.out.println("get field " + fieldID + " on " + updatable.getClass().getName());
				
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
	}

	private void updateAllLocal(final float dt) {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			entry.getValue().updateOnClient(dt);
		}
	}
}
