package net.cbaakman.occupy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.config.ServerConfig;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.Address;
import net.cbaakman.occupy.network.NetworkClient;
import net.cbaakman.occupy.network.NetworkServer;

public class TestRun {
	
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
		
		try {
			client.run();
		} catch (Exception e) {

			client.stop();
			server.stop();
			
			e.printStackTrace();
		}
		
		server.stop();
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
