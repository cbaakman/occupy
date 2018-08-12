package net.cbaakman.occupy.game;

import java.util.UUID;

import org.apache.log4j.Logger;

import com.jogamp.opengl.math.Quaternion;

import lombok.Data;
import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Identifier;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.math.Vector3f;

@Data
public abstract class Unit extends Entity {
	
	Logger logger = Logger.getLogger(Unit.class);

	@ServerToClient
	private String ownerName;
	
	@ServerToClient
	protected int health;

	@ServerToClient
	protected Vector3f position = new Vector3f();
	
	protected Quaternion orientation = new Quaternion();
	
	@ClientToServer
	protected Order currentOrder = null;
	
	@Override
	public boolean mayBeUpdatedBy(PlayerRecord player) {		
		return player.getName().equals(ownerName);
	}

	public Unit(Server server) {
		super(server);
	}

	public Unit(Client client) {
		super(client);
	}
}
