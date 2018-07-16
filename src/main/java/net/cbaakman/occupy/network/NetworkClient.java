package net.cbaakman.occupy.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.security.SSLChannel;

public class NetworkClient extends Client {
	
	private Address serverAddress;
	private UDPMessenger udpMessenger;
	private boolean running;
	
	public NetworkClient(ErrorHandler errorHandler, Address serverAddress) {
		super(errorHandler);
		
		this.serverAddress = serverAddress;
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger() {
			@Override
			void onReceive(Address address, Message message) {
				if (!address.getAddress().equals(NetworkClient.this.serverAddress))
					return;

				NetworkClient.this.onMessage(message);
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
			initUDP();
		} catch (IOException e) {
			throw new InitError(e);
		}
		
		onInit();
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		running = true;
		
		while (running) {
			ticks = System.currentTimeMillis();
			dt = (float)(ticks - ticks0) / 1000;

			update(dt);
		}
		onShutdown();
		
		try {
			closeUDP();
		} catch (IOException | InterruptedException e) {
			// Should not happen!
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void login(Credentials credentials) throws AuthenticationError {
		try {			
			Socket socket = new Socket();
			
			// Must use the same port as udp, for the server to know.
			socket.bind(new InetSocketAddress(udpMessenger.getPort()));
			
			socket.connect(new InetSocketAddress(serverAddress.getAddress(),
									   			 serverAddress.getPort()), 1000);
			if (socket.isConnected()) {
				// Tell the server that we want to log in:
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				oos.writeObject(RequestType.LOGIN);
				oos.flush();
				
				SSLChannel sslChannel = new SSLChannel(socket.getInputStream(), socket.getOutputStream());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				oos = new ObjectOutputStream(baos);
				oos.writeObject(credentials);
				baos.close();
				oos.close();
				try {
					sslChannel.send(baos.toByteArray());
				} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
						| NoSuchPaddingException e) {
					// Not supposed to happen!
					SeriousErrorHandler.handle(e);
				}
				
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				
				ResponseType response = null;
				try {
					response = (ResponseType)ois.readObject();
				} catch (ClassNotFoundException e) {
					// Not supposed to happen!
					SeriousErrorHandler.handle(e);
					return;
				} finally {
					socket.close();
				}
				
				if (response.equals(ResponseType.OK)) {
					// Authentication OK
					return;
				}
				else if (response.equals(ResponseType.AUTHENTICATION_ERROR))
					throw new AuthenticationError();
				else
					SeriousErrorHandler.handle(new RuntimeException(
							String.format("not applicable response: %s", response.name())));
			}
			else
				onCommunicationError(
					new CommunicationError(
						String.format("not connected to server at %s %d",
									  serverAddress.getAddress(),
									  serverAddress.getPort())));
			
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}

	@Override
	public void sendMessage(Message message) {
		try {
			udpMessenger.send(serverAddress, message);
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}

	@Override
	public void stop() {
		running = false;
	}
}
