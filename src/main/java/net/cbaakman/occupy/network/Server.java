package net.cbaakman.occupy.network;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import net.cbaakman.occupy.network.annotations.ClientToServer;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

public class Server extends Updater {
	
	@Data
	private class ClientAddress {
		private InetAddress address;
		private int port;
		
		public ClientAddress(InetAddress address, int port) {
			this.address = address;
			this.port = port;
		}
	}
	
	@Data
	private class ClientRecord {
		private ClientAddress clientAddress;
		private UUID id = UUID.randomUUID();
		
		public ClientRecord(ClientAddress clientAddress) {
			this.clientAddress = clientAddress;
		}
	}
	
	private Map<ClientAddress, ClientRecord> clientRecords = new HashMap<ClientAddress, ClientRecord>();
	
	private UDPMessenger udpMessenger;
	
	public Server(int listenPort) throws SocketException {
		udpMessenger = new UDPMessenger(listenPort) {

			@Override
			void onReceive(InetAddress senderAddress, int senderPort, Message message) {
				ClientAddress clientAddress = new ClientAddress(senderAddress, senderPort);
				ClientRecord clientRecord;
				synchronized(clientRecords) {
					if (clientRecords.containsKey(clientAddress))
						clientRecords.put(clientAddress, new ClientRecord(clientAddress));
					
					clientRecord = clientRecords.get(clientAddress);
				}

				if (message.getType().equals(MessageType.UPDATE))
					processUpdateWith((Update)message.getData(), ClientToServer.class, clientRecord.getId());
			}
		};
	}

	@Override
	public void update(final float dt) {
		updateAllLocal(dt);
		updateAllToClients();
	}


	private void updateAllToClients() {
		synchronized(clientRecords) {
			for (Entry<ClientAddress, ClientRecord> entry : clientRecords.entrySet()) {
				
				ClientAddress address = entry.getKey();
				
				for (Update update : getUpdatesWith(ServerToClient.class)) {
					udpMessenger.send(address.getAddress(), address.getPort(),
									  new Message(MessageType.UPDATE, update));
				}
			}
		}
	}

	private void updateAllLocal(final float dt) {
		for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
			entry.getValue().updateOnServer(dt);
		}
	}
}
