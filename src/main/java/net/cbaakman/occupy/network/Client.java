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
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

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
		for (Update update : getUpdatesWith(ClientToServer.class)) {
			serverConnection.send(new Message(MessageType.UPDATE, update));
		}
	}

	private void updateAllFromServer() {
		for (Message message : serverConnection.poll()) {
			if (message.getType().equals(MessageType.UPDATE))
				processUpdateWith((Update)message.getData(), ServerToClient.class);
		}
	}

	private void updateAllLocal(final float dt) {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			entry.getValue().updateOnClient(dt);
		}
	}
}
