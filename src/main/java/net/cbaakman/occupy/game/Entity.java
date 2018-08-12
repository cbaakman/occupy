package net.cbaakman.occupy.game;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.Updatable;

public class Entity extends Updatable {

	public Entity(Client client) {
		super(client);
	}

	public Entity(Server server) {
		super(server);
	}
}
