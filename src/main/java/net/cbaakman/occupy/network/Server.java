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

	private void updateAllLocal(final float dt) {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			entry.getValue().updateOnServer(dt);
		}
	}
}
