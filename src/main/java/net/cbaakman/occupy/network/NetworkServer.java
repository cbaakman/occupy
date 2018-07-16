package net.cbaakman.occupy.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.Data;
import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.security.SSLChannel;

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
	TCPServer tcpServer;
	
	public NetworkServer(ErrorHandler errorHandler, int listenPort) {
		super(errorHandler);
		this.listenPort = listenPort;
	}
	
	private void initTCP() throws IOException {
		tcpServer = new TCPServer(listenPort) {

			@Override
			protected void onConnectionError(Exception e) {
				onCommunicationError(new CommunicationError(e));
			}

			@Override
			protected void onConnection(Address address, SocketChannel connectionChannel) {
				try{						
					ObjectInputStream ois = new ObjectInputStream(connectionChannel.socket().getInputStream());
					RequestType type = (RequestType)ois.readObject();
					if (type.equals(RequestType.LOGIN)) {
						NetworkServer.this.processLogin(address, connectionChannel.socket());
					}
					connectionChannel.close();
					
				} catch (IOException e) {
					onCommunicationError(new CommunicationError(e));
					
				} catch (ClassNotFoundException | InvalidKeyException | NoSuchPaddingException |
						IllegalBlockSizeException | BadPaddingException e) {
					// Not supposed to happen!
					SeriousErrorHandler.handle(e);
				}
			}
		};
	}
	
	protected void processLogin(Address address, Socket socket) throws InvalidKeyException,
																	   NoSuchPaddingException,
																	   IllegalBlockSizeException,
																	   BadPaddingException,
																	   IOException,
																	   ClassNotFoundException {

		SSLChannel sslChannel = new SSLChannel(socket.getInputStream(), socket.getOutputStream());
		byte[] data = sslChannel.receive();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Credentials credentials = (Credentials)ois.readObject();
		
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		synchronized(clientRecords) {						
			ClientRecord clientRecord = new ClientRecord(address);
			try {
				onClientLogin(clientRecord.getClientId(), credentials);
				
				clientRecords.put(address, clientRecord);
				
				oos.writeObject(ResponseType.OK);
			} catch (AuthenticationError e) {
				oos.writeObject(ResponseType.AUTHENTICATION_ERROR);
			}
		}
		oos.close();
	}

	private void closeTCP() throws IOException, InterruptedException {
		tcpServer.disconnect();
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger(listenPort) {

			@Override
			void onReceive(Address senderAddress, Message message) {
									
				synchronized(clientRecords) {
					if (clientRecords.containsKey(senderAddress)) {
					
						ClientRecord clientRecord = clientRecords.get(senderAddress);

						NetworkServer.this.onMessage(clientRecord.getClientId(), message);
					}
				}
			}

			@Override
			void onReceiveError(Exception e) {
				onCommunicationError(new CommunicationError(e));
			}
		};
	}
	private void closeUDP() throws IOException, InterruptedException {
		udpMessenger.disconnect();
	}

	public void run() throws InitError {

		try {
			initTCP();
			initUDP();
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
		
		try {
			closeTCP();
			closeUDP();
		} catch (IOException | InterruptedException e) {
			// Should not happen!
			SeriousErrorHandler.handle(e);
		}
	}
	
	@Override
	public void stop() {
		running = false;
	}

	@Override
	protected void logoutClient(UUID clientId) {		
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
				if (entry.getValue().getClientId().equals(clientId)) {
					try {
						udpMessenger.send(entry.getKey(), message);
					} catch (IOException e) {
						onCommunicationError(new CommunicationError(e));
					}
				}
			}
		}
	}
}
