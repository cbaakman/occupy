package net.cbaakman.occupy;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

import net.cbaakman.occupy.network.Client;
import net.cbaakman.occupy.network.LocalConnection;
import net.cbaakman.occupy.network.Server;
import net.cbaakman.occupy.network.Updatable;
import net.cbaakman.occupy.network.Updater;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.annotations.ClientToServer;

public class TestLocalRunner {
	
	static class TestUpdatable extends Updatable {

		public TestUpdatable(UUID ownerID) {
			super(ownerID);
		}
		
		@ServerToClient
		private int testServerToClient = 0;

		@ClientToServer
		private int testClientToServer = 0;
		
		@Override
		public void updateOnClient(final float dt) {
			testClientToServer++;
			
			System.out.println(String.format("client: testServerToClient = %d", testServerToClient));
		}

		@Override
		public void updateOnServer(final float dt) {
			testServerToClient++;
			
			System.out.println(String.format("server: testClientToServer = %d", testClientToServer));
		}
	}
	
	public static void main(String[] args) throws NoSuchFieldException,
												  SecurityException,
												  IllegalArgumentException,
												  IllegalAccessException {

		final Server server = new Server();
		
		Thread serverThread = new Thread()
		{			
			@Override
			public void run() {
				
				long t0 = System.currentTimeMillis();
				
				while (!Thread.currentThread().isInterrupted()) {
					long t = System.currentTimeMillis();
					float dt = (float)(t - t0) / 1000;
					t0 = t;

					System.out.println(String.format("update server, dt = %.3f", dt));
					server.update(dt);
				}
			}
		};
		
		Client client = LocalConnection.Connect(server);
		
		Field clientUpdatablesField = Updater.class.getDeclaredField("updatables");
		clientUpdatablesField.setAccessible(true);
		
		((Map<UUID, Updatable>)clientUpdatablesField.get(client)).put(UUID.randomUUID(),
																	  new TestUpdatable(client.getId()));

	
		serverThread.start();
		
		int n;
		long t0 = System.currentTimeMillis();
		
		for (n = 0; n < 100; n++) {
			
			long t = System.currentTimeMillis();
			float dt = (float)(t - t0) / 1000;
			t0 = t;
			
			System.out.println(String.format("update client, n = %d, dt=%.3f", n, dt));
			client.update(dt);
		}

		serverThread.interrupt();
	}
}
