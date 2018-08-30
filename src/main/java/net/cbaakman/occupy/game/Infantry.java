package net.cbaakman.occupy.game;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Server;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshBoneAnimationState;
import net.cbaakman.occupy.mesh.MeshFactory;

public class Infantry extends Unit {
	
	Logger logger = Logger.getLogger(Infantry.class);
	
	private final static float WALK_SPEED = 1.0f;
	
	public Infantry(Server server) {
		super(server);
	}
	
	public Infantry(Client client) {
		super(client);
	}
	
	@Override
	public void updateOnClient(float dt) {
		
		if (currentOrder instanceof MoveOrder) {
			MoveOrder order = (MoveOrder)currentOrder;
			moveTo(order.getDestination(), dt);
		}
	}

	private void moveTo(Vector3f destination, float dt) {
		
		Vector3f distance = destination.subtract(position);
		
		Vector3f direction = distance.unit();
		
		float[] xv = new float[3],
			    yv = new float[3],
			    zv = new float[3],
			    up = new float[] {0.0f, 1.0f, 0.0f};
		
		Vector3f move = direction.multiplyBy(WALK_SPEED * dt);
		if (move.length2() > distance.length2())
			move = distance;
		
		if (distance.length() > 0) {
			orientation.setLookAt(direction.toList(), up, xv, yv, zv);
		
			position = position.add(move);
		}
	}

	@Override
	public void updateOnServer(float dt) {
		if (currentOrder instanceof MoveOrder) {
			MoveOrder order = (MoveOrder)currentOrder;
			moveTo(order.getDestination(), dt);
		}
	}
}
