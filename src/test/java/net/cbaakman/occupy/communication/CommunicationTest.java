package net.cbaakman.occupy.communication;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;
import net.cbaakman.occupy.authenticate.Authenticator;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.communicate.ServerInfo;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class CommunicationTest extends TestCase {
	
	Logger logger = Logger.getLogger(CommunicationTest.class);
	
	private static Path createDataDirectory(String prefix) throws IOException{
		return Files.createTempDirectory(prefix);
	}
	
	private static void removeTree(Path path) {
		if (path == null)
			return;
		
		File file = path.toFile();
		
		if (file.isDirectory()) {
			for (File child : file.listFiles())
				removeTree(child.toPath());
		}
		if (file.exists())
			file.delete();
	}
	
	private Server server;
	private Client client;
	
	private ServerConfig serverConfig = new ServerConfig();
	private ClientConfig clientConfig = new ClientConfig();
	
	Thread serverThread, clientThread;
	
	private Credentials loginCredentials = new Credentials("hello", "world");
	
	@Override
	protected void setUp() throws InterruptedException, IOException {
		
		Random random = new Random();
		int serverPort = random.nextInt(1000) + 5000;
		serverConfig.setListenPort(serverPort);
		
		serverConfig.setDataDir(createDataDirectory("server"));
		
		File mapFile = new File(serverConfig.getDataDir().toFile(), "map.zip");
		Files.copy(CommunicationTest.class.getResourceAsStream("/map/testmap.zip"), mapFile.toPath());
		
		File passwdFile = new File(serverConfig.getDataDir().toFile(), "passwd");
		(new Authenticator(passwdFile)).add(loginCredentials);
		
		clientConfig.setServerAddress(new Address(InetAddress.getLoopbackAddress(), serverPort));
		
		clientConfig.setDataDir(createDataDirectory("client"));
		
		server = new NetworkServer(serverConfig);
		client = new NetworkClient(clientConfig);
		
		serverThread = new Thread("server") {
			public void run() {
				try {
					server.run();
				} catch (Exception e) {
					
					server.stop();
					client.stop();
					
					Assert.fail();
				}
			}
		};
		serverThread.start();
		
		clientThread = new Thread("client") {
			public void run() {
				try {
					client.run();
				} catch (Exception e) {
					
					server.stop();
					client.stop();
					
					Assert.fail();
				}
			}
		};
		clientThread.start();
		
		Thread.sleep(1000);
	}
	
	@Override
	protected void tearDown() throws InterruptedException {

		try {
			client.stop();
			server.stop();

			clientThread.join();
			serverThread.join();
		}
		finally {
			removeTree(serverConfig.getDataDir());
			removeTree(clientConfig.getDataDir());
		}
	}

	@Test
	public void testLogin() throws AuthenticationError, CommunicationError {
		
		client.login(loginCredentials);
	}
	
	@Test
	public void testServerInfo() throws CommunicationError {

		ServerInfo serverInfo = client.getServerInfo();
		assertEquals(serverInfo.getServerVersion(), client.getVersion());
	}
	
	@Test
	public void testDownloadMap() throws CommunicationError, IOException {
		client.downloadMap("test");
		
		File mapFile = client.getMapFile("test");
		assertTrue(mapFile.isFile());
	}
}
