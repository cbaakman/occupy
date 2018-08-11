package net.cbaakman.occupy.game;

import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.communicate.Server;

public class Entity extends Updatable {

	public Entity(Client client) {
		super(client);
	}

	public Entity(Server server) {
		super(server);
	}
}
