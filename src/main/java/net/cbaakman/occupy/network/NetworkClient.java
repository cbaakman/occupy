package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Connection;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;

public class NetworkClient extends Client {
	
	private UDPMessenger udpMessenger;
	private boolean running;
	
	public NetworkClient(ErrorHandler errorHandler, ClientConfig config) {
		super(errorHandler, config);
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger() {
			@Override
			void onReceive(Address address, Packet message) {
				if (!address.getAddress().equals(config.getServerAddress()))
					return;

				NetworkClient.this.onPacket(message);
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
	public Connection connectToServer() throws CommunicationError {
		if (udpMessenger == null)
			SeriousErrorHandler.handle(new RuntimeException("client is not running"));
		
		Socket socket = new Socket();
		try {
			// Must use the same port as udp, for the server to know.
			socket.bind(new InetSocketAddress(udpMessenger.getPort()));
			
			socket.connect(new InetSocketAddress(config.getServerAddress().getAddress(),
												 config.getServerAddress().getPort()), 1000);
			if (socket.isConnected()) {
				
				return new SocketConnection(socket);
			}
			else {
				socket.close();
				
				throw new CommunicationError(
					String.format("not connected to server at %s %d",
								  config.getServerAddress().getAddress(),
								  config.getServerAddress().getPort()));
			}
			
		} catch (IOException e) {
			try {
				socket.close();
			} catch (IOException ex) {
				throw new CommunicationError(ex);
			}
			throw new CommunicationError(e);
		}
	}

	@Override
	public void sendPacket(Packet packet) {
		try {
			udpMessenger.send(config.getServerAddress(), packet);
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}

	@Override
	public void stop() {
		running = false;
	}
}
