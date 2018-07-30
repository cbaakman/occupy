package net.cbaakman.occupy;

import java.net.InetAddress;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class TestRun {

	public static void main(String[] args) throws InterruptedException, InitError {
				
		int serverPort = 5000;
		ServerConfig serverConfig = new ServerConfig();
		serverConfig.setListenPort(serverPort);
		
		ClientConfig clientConfig = new ClientConfig();
		clientConfig.setServerAddress(new Address(InetAddress.getLoopbackAddress(), serverPort));
		
		final Server server = new NetworkServer(serverConfig);
		final Client client = new NetworkClient(clientConfig);
		
		Thread serverThread = new Thread("server") {
			public void run() {
				try {
					server.run();
				} catch (Exception e) {
					
					client.stop();
					server.stop();
					
					e.printStackTrace();
				}
			}
		};
		serverThread.start();
		
		try {
			client.run();
		} catch (Exception e) {

			client.stop();
			server.stop();
			
			e.printStackTrace();
		}
		
		server.stop();
	}
}
