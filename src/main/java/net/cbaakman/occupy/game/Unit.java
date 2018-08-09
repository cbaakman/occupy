package net.cbaakman.occupy.game;

import java.util.UUID;

import lombok.Data;
import net.cbaakman.occupy.Identifier;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.math.Vector3f;

@Data
public abstract class Unit extends Updatable {

	@ServerToClient
	protected Vector3f position;
	
	@ClientToServer
	protected Order currentOrder;
}
