package net.cbaakman.occupy.security;

import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.util.Properties;

import org.junit.Test;

import junit.framework.TestCase;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;
import org.apache.log4j.PropertyConfigurator;

public class LoginTest extends TestCase {

	@Test
	public void test() throws InterruptedException, AuthenticationError, IOException {
		
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
