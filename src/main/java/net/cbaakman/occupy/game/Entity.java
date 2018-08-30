package net.cbaakman.occupy.game;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.annotations.ServerToClient;

public class Entity extends Updatable {
	
	@ServerToClient
	long milliseconds0;

	public Entity(Client client) {
		super(client);
		
		milliseconds0 = System.currentTimeMillis();
	}

	public Entity(Server server) {
		super(server);

		milliseconds0 = System.currentTimeMillis();
	}
	
	public long getMillisecondsExists() {
		return System.currentTimeMillis() - milliseconds0;
	}
}
