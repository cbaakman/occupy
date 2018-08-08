package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Connection;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousError;

public class NetworkClient extends Client {
	
	private UDPMessenger udpMessenger;
	
	public NetworkClient(ClientConfig config) {
		super(config);
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger(getErrorQueue()) {
			@Override
			void onReceive(Address address, Packet message) {
				if (!address.getAddress().equals(config.getServerAddress()))
					return;

				NetworkClient.this.onPacket(message);
			}
		};
	}
	
	private void closeUDP() throws IOException, InterruptedException {
		udpMessenger.disconnect();
	}
	
	@Override
	protected void initCommunication() throws InitError {
		try {
			initUDP();
		} catch (IOException e) {
			throw new InitError(e);
		}
	}
	
	@Override
	protected void shutdownCommunication() {
		try {
			closeUDP();
		} catch (IOException | InterruptedException e) {
			throw new SeriousError(e);
		}
	}

	@Override
	public Connection connectToServer() throws CommunicationError, SeriousError {
		if (udpMessenger == null)
			throw new SeriousError("client udp has not been initialized");
		
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
	public void sendPacket(Packet packet) throws CommunicationError {
		try {
			udpMessenger.send(config.getServerAddress(), packet);
		} catch (IOException e) {
			throw new CommunicationError(e);
		}
	}
}
