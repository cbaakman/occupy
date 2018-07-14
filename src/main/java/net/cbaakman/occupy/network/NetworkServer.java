package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import lombok.Data;
import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.WhileThread;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;

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
	TCPServerThread tcpServerThread;
	
	public NetworkServer(ErrorHandler errorHandler, int listenPort) {
		super(errorHandler);
		this.listenPort = listenPort;
	}
	
	private void initNetwork() throws InitError {
		try {
			tcpServerThread = new TCPServerThread(listenPort) {

				@Override
				protected void onConnectionError(Exception e) {
					onCommunicationError(new CommunicationError(e));
				}

				@Override
				protected void onConnection(Address address, SocketChannel connectionChannel) {
					try{						
						// .. transfer login data .. //
						
						connectionChannel.close();
						
						UUID clientId;
						synchronized(clientRecords) {
							ClientRecord clientRecord = new ClientRecord(address);
							clientRecords.put(address, clientRecord);
							clientId = clientRecord.getClientId();
						}
						
						NetworkServer.this.onClientConnect(clientId);
						
					} catch (IOException e) {
						onCommunicationError(new CommunicationError(e));
					}
				}
			};
		} catch (IOException e) {
			throw new InitError(e);
		}
		tcpServerThread.start();
		
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

				@Override
				void onReceiveError(Exception e) {
					onCommunicationError(new CommunicationError(e));
				}
			};
		} catch (IOException e) {
			throw new InitError(e);
		}
	}
	
	private void closeNetwork() {
		tcpServerThread.stopRunning();
		
		try {
			udpMessenger.disconnect();
		} catch (IOException e) {
			// Not supposed to happen!
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run() throws InitError {
		
		initNetwork();
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		running = true;
		
		while (running) {
			ticks = System.currentTimeMillis();
			dt = (float)(ticks - ticks0) / 1000;
			
			update(dt);
		}
		
		closeNetwork();
	}
	
	@Override
	protected void stop() {
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
					try {
						udpMessenger.send(entry.getKey(), message);
					} catch (IOException e) {
						onCommunicationError(new CommunicationError(e));
					}
			}
		}
	}
}
