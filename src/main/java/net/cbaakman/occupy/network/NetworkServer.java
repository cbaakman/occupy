package net.cbaakman.occupy.network;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
import net.cbaakman.occupy.WhileThread;
import net.cbaakman.occupy.config.Config;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
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
	ServerSocketChannel tcpChannel;
	
	public NetworkServer(ErrorHandler errorHandler, int listenPort) {
		super(errorHandler);
		this.listenPort = listenPort;
	}

	public void run() throws InitError {
		try {
			tcpChannel = ServerSocketChannel.open();
			tcpChannel.bind(new InetSocketAddress(listenPort));
			tcpChannel.configureBlocking(false);
		} catch (IOException e) {
			throw new InitError(e);
		}
		
		final boolean listenForConnect = true;
		
		WhileThread tcpThread = new WhileThread("tcp-server") {
			public void repeat() {
				try {
					SocketChannel connectionChannel = tcpChannel.accept();
					
					if (connectionChannel != null) {
						InetSocketAddress remoteAddres = (InetSocketAddress)connectionChannel.getRemoteAddress();
						
						ByteBuffer buf = ByteBuffer.allocate(8);
						connectionChannel.read(buf);
						
						System.out.println(buf.array());
						
						Address address = new Address(remoteAddres.getAddress(),
													  remoteAddres.getPort());
						
						// .. transfer login data over the inputstream .. //
						
						connectionChannel.close();
						
						UUID clientId;
						synchronized(clientRecords) {
							ClientRecord clientRecord = new ClientRecord(address);
							clientRecords.put(address, clientRecord);
							clientId = clientRecord.getClientId();
						}
						
						NetworkServer.this.onClientConnect(clientId);
					}
				} catch (IOException e) {
					onCommunicationError(new CommunicationError(e));
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

				@Override
				void onReceiveError(Exception e) {
					onCommunicationError(new CommunicationError(e));
				}
			};
		} catch (IOException e) {
			throw new InitError(e);
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
		
		tcpThread.stopRunning();
		try {
			tcpChannel.close();
		} catch (IOException e) {
			// Should not happen!
			e.printStackTrace();
			System.exit(1);
		}
		
		try {
			udpMessenger.disconnect();
		} catch (IOException e) {
			// Not supposed to happen!
			e.printStackTrace();
			System.exit(1);
		}
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
