package net.cbaakman.occupy.security;

import java.net.InetAddress;
import java.security.InvalidKeyException;

import org.junit.Test;

import junit.framework.TestCase;
import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class LoginTest extends TestCase {

	@Test
	public void test() throws InterruptedException, AuthenticationError {
	
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
		
		Thread.currentThread().sleep(1000);
		
		client.login(new Credentials("hello", "world"));
		
		client.stop();
		server.stop();
	}
}
