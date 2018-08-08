package net.cbaakman.occupy.network;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import net.cbaakman.occupy.communicate.Identifier;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousError;

public class NetworkServer extends Server {
	
	Logger logger = Logger.getLogger(NetworkServer.class);

	UDPMessenger udpMessenger;
	TCPServer tcpServer;
	
	public NetworkServer(ServerConfig config) {
		super(config);
	}
	
	private void initTCP() throws IOException {
		tcpServer = new TCPServer(getErrorQueue(),config.getListenPort()) {

			@Override
			protected void onConnection(Address address, SocketChannel connectionChannel)
					throws CommunicationError {
				NetworkServer.this.onClientConnect(address, new SocketConnection(connectionChannel.socket()));
					
				try {
					connectionChannel.close();
				} catch (IOException e) {
					getErrorQueue().pushError(e);
				}
			}
		};
	}

	private void closeTCP() throws IOException, InterruptedException {
		tcpServer.disconnect();
	}
	
	private void initUDP() throws IOException {
		udpMessenger = new UDPMessenger(getErrorQueue(), config.getListenPort()) {

			@Override
			void onReceive(Address senderAddress, Packet message) {
									
				NetworkServer.this.onPacket(senderAddress, message);
			}
		};
	}
	private void closeUDP() throws IOException, InterruptedException {
		udpMessenger.disconnect();
	}

	@Override
	protected void initCommunication() throws InitError {
		try {
			initTCP();
			initUDP();
		} catch (IOException e) {
			throw new InitError(e);
		}
	}

	@Override
	protected void shutdownCommunication() {
		try {
			closeTCP();
			closeUDP();
		} catch (IOException | InterruptedException e) {
			throw new SeriousError(e);
		}
	}

	@Override
	protected void sendPacket(Identifier clientId, Packet packet) throws CommunicationError {
		if (clientId instanceof Address)
			try {
				udpMessenger.send((Address)clientId, packet);
			} catch (IOException e) {
				throw new CommunicationError(e);
			}
		else
			throw new CommunicationError(
					String.format("cannot send to client with ID type %s", clientId.getClass().getName()));
	}
}
