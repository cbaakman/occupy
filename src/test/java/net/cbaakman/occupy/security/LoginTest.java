package net.cbaakman.occupy.security;

import java.io.IOException;
import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class LoginTest extends TestCase {

	@Test
	public void test() throws InterruptedException, AuthenticationError,
							  IOException, SeriousError, CommunicationError {

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
					
					server.stop();
					client.stop();
					
					e.printStackTrace();
					Assert.fail();
				}
			}
		};
		serverThread.start();
		
		Thread clientThread = new Thread("client") {
			public void run() {
				try {
					client.run();
				} catch (Exception e) {
					
					server.stop();
					client.stop();
					
					e.printStackTrace();
					Assert.fail();
				}
			}
		};
		clientThread.start();
		
		Thread.sleep(1000);
		
		client.login(new Credentials("hello", "world"));
		
		client.stop();
		server.stop();
	}
}
