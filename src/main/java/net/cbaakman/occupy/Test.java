package net.cbaakman.occupy;

import java.net.InetAddress;

import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class Test {

	public static void main(String[] args) throws InterruptedException, CommunicationError {
		
		final ErrorHandler errorHandler = new ErrorHandler() {

			@Override
			public void handle(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		};
		
		
		final Server server = new NetworkServer(errorHandler, 5000);
		final Client client = new NetworkClient(errorHandler, new Address(InetAddress.getLoopbackAddress(), 5000));
		
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
		
		Thread clientThread = new Thread("client") {
			public void run() {
				try {
					client.run();
				} catch (InitError e) {
					errorHandler.handle(e);
				}
			}
		};
		clientThread.start();
		
		System.out.println("started");
		
		Thread.sleep(1000);
		
		client.connectToServer();
		
		System.out.println("connected");
		
		server.stop();
		client.stop();
		
		System.out.println("stopped");
	}
}
