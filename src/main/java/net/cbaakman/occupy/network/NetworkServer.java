package net.cbaakman.occupy.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import lombok.Data;
import net.cbaakman.occupy.communicate.Identifier;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;

public class NetworkServer extends Server {

	private boolean running;
	UDPMessenger udpMessenger;
	TCPServer tcpServer;
	
	public NetworkServer(ErrorHandler errorHandler, ServerConfig config) {
		super(errorHandler, config);
	}
	
	private void initTCP() throws IOException {
		tcpServer = new TCPServer(config.getListenPort()) {

			@Override
			protected void onConnectionError(Exception e) {
				onCommunicationError(new CommunicationError(e));
			}

			@Override
			protected void onConnection(Address address, SocketChannel connectionChannel) {
				try{			
					NetworkServer.this.onClientConnect(address, new SocketConnection(connectionChannel.socket()));
					
					connectionChannel.close();
					
				} catch (IOException e) {
					onCommunicationError(new CommunicationError(e));
				}
			}
		};
	}

	private void closeTCP() throws IOException, InterruptedException {
		tcpServer.disconnect();
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger(config.getListenPort()) {

			@Override
			void onReceive(Address senderAddress, Packet message) {
									
				NetworkServer.this.onPacket(senderAddress, message);
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
	protected void sendPacket(Identifier clientId, Packet packet) {
		if (clientId instanceof Address)
			try {
				udpMessenger.send((Address)clientId, packet);
			} catch (IOException e) {
				onCommunicationError(new CommunicationError(e));
			}
		else
			onCommunicationError(new CommunicationError(
					String.format("cannot send to client with ID type %s", clientId.getClass().getName())));
	}
}
