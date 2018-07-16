package net.cbaakman.occupy;

import java.net.InetAddress;

import net.cbaakman.occupy.enums.MessageType;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

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
		final Server server = new NetworkServer(errorHandler, serverPort);
		final Client client = new NetworkClient(errorHandler, new Address(InetAddress.getLoopbackAddress(), serverPort));
		
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
