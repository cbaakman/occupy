package net.cbaakman.occupy.network;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.Update;
import net.cbaakman.occupy.config.Config;
import net.cbaakman.occupy.network.annotations.ClientToServer;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

public class NetworkServer extends Server {
	
	@Data
	private class ClientRecord {
		private Address clientAddress;
		private UUID clientId = UUID.randomUUID();
		
		public ClientRecord(Address clientAddress) {
			this.clientAddress = clientAddress;
		}
	}

	private Map<Address, ClientRecord> clientRecords = new HashMap<Address, ClientRecord>();
	private boolean running;
	private int listenPort;
	UDPMessenger udpMessenger;
	ServerSocket tcpSocket;
	
	public NetworkServer(int listenPort) {
		this.listenPort = listenPort;
	}

	public void run() {
		try {
			tcpSocket = new ServerSocket(listenPort);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		Thread tcpThread = new Thread() {
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Socket connectionSocket = tcpSocket.accept();

						Address address = new Address(connectionSocket.getInetAddress(),
													  connectionSocket.getPort());
						
						// .. transfer login data over the inputstream .. //
						
						connectionSocket.close();
						
						UUID clientId;
						synchronized(clientRecords) {
							ClientRecord clientRecord = new ClientRecord(address);
							clientRecords.put(address, clientRecord);
							clientId = clientRecord.getClientId();
						}
						
						NetworkServer.this.onClientConnect(clientId);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		};
		tcpThread.start();
		
		try {
			udpMessenger = new UDPMessenger(listenPort) {

				@Override
				void onReceive(Address senderAddress, Message message) {
					synchronized(clientRecords) {
						if (clientRecords.containsKey(senderAddress))
							return;
						
						ClientRecord clientRecord = clientRecords.get(senderAddress);

						NetworkServer.this.onMessage(clientRecord.getClientId(), message);
					}
				}
			};
		} catch (SocketException e) {
			e.printStackTrace();
			return;
		}
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		running = true;
		
		while (running) {
			ticks = System.currentTimeMillis();
			dt = (float)(ticks - ticks0) / 1000;
			
			update(dt);
		}
		
		tcpThread.interrupt();
	}
	
	protected void shutdown() {
		running = false;
	}

	@Override
	protected void disconnectClient(UUID clientId) {
		synchronized(clientRecords) {
			for (Entry<Address, ClientRecord> entry : clientRecords.entrySet()) {
				if (entry.getValue().getClientId().equals(clientId)) {
					clientRecords.remove(entry);
				}
			}
		}
	}

	@Override
	protected void sendMessage(UUID clientId, Message message) {
		synchronized(clientRecords) {
			for (Entry<Address, ClientRecord> entry : clientRecords.entrySet()) {
				if (entry.getValue().equals(clientId))
					udpMessenger.send(entry.getKey(), message);
			}
		}
	}
}
