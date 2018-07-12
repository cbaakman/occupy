package net.cbaakman.occupy;

import java.net.InetAddress;

import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		
		final Server server = new NetworkServer(5000);
		final Client client = new NetworkClient(new Address(InetAddress.getLoopbackAddress(), 5000));
		
		Thread serverThread = new Thread() {
			public void run() {
				server.run();
			}
		};
		serverThread.start();
		
		Thread clientThread = new Thread() {
			public void run() {
				client.run();
			}
		};
		clientThread.start();
		
		Thread.sleep(5000);
		
		client.connectToServer();
	}
}
