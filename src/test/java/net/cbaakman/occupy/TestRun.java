package net.cbaakman.occupy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import net.cbaakman.occupy.authenticate.Authenticator;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.game.MoveOrder;
import net.cbaakman.occupy.game.Unit;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class TestRun {
	
	private static Credentials loginCredentials = new Credentials("user", "pass");
	
	private static Path createDataDirectory(String prefix) throws IOException{
		return Files.createTempDirectory(prefix);
	}
	
	private static void runWith(ServerConfig serverConfig, ClientConfig clientConfig) {
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
		
		Thread clientThread = new Thread("client") {
			public void run() {
				try {
					client.run();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {  // When the client stops, the server stops.
					server.stop();
				}
			}
		};
		clientThread.start();
		
		// Log in after 1 second, allow the server to initialize.
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		try {
			client.login(loginCredentials);
		} catch (AuthenticationError | CommunicationError e) {
			
			client.stop();
			server.stop();
			
			e.printStackTrace();
		}
		
		// Place some random units
		int i;
		float minX = -100.0f, maxX = 100.0f,
			  minZ = -100.0f, maxZ = 100.0f,
			  x, z;
		Random random = new Random();
		for (i = 0; i < 100; i++) {
			Unit unit = new Infantry(server);
			x = minX + random.nextFloat() * (maxX - minX);
			z = minZ + random.nextFloat() * (maxZ - minZ);
			unit.setPosition(new Vector3f(x, 0.0f, z));
			x = minX + random.nextFloat() * (maxX - minX);
			z = minZ + random.nextFloat() * (maxZ - minZ);
			unit.setCurrentOrder(new MoveOrder(new Vector3f(x, 0.0f, z)));
			
			server.addUpdatable(unit);
		}
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

	public static void main(String[] args)
			throws IOException {

		ServerConfig serverConfig = new ServerConfig();
		ClientConfig clientConfig = new ClientConfig();
		
		try {
			int serverPort = 5000;
			serverConfig.setListenPort(serverPort);
			serverConfig.setDataDir(createDataDirectory("server"));
			File mapFile = new File(serverConfig.getDataDir().toFile(), "map.zip");
			Files.copy(TestRun.class.getResourceAsStream("/map/testmap.zip"), mapFile.toPath());
			File passwdFile = new File(serverConfig.getDataDir().toFile(), "passwd");
			(new Authenticator(passwdFile)).add(loginCredentials);
			
			clientConfig.setServerAddress(new Address(InetAddress.getLoopbackAddress(), serverPort));
			clientConfig.setDataDir(createDataDirectory("client"));
			
			runWith(serverConfig, clientConfig);
		}
		finally {
			removeTree(serverConfig.getDataDir());
			removeTree(clientConfig.getDataDir());
		}
	}
}
