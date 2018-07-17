package net.cbaakman.occupy;

import java.net.InetAddress;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class TestRun {

	public static void main(String[] args) throws InterruptedException, InitError {
		
		final ErrorHandler errorHandler = new ErrorHandler() {

			@Override
			public void handle(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		};
		
		int serverPort = 5000;
		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setListenPort(serverPort);
		
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.setServerAddress(new Address(InetAddress.getLoopbackAddress(), serverPort));
		
		final Server server = new NetworkServer(errorHandler, serverConfig);
		final Client client = new NetworkClient(errorHandler, clientConfig);
		
		Thread serverThread = new Thread("server") {
			public void run() {
				try {
					server.run();
				} catch (InitError e) {
					errorHandler.handle(e);
				}
			}
		};
		serverThread.start();
		
		client.run();
		
		server.stop();
	}
}
