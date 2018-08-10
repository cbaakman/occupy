package net.cbaakman.occupy.game;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MoveOrder extends Order {

	private Vector3f destination;
	
	public MoveOrder(Vector3f destination) {
		this.destination = destination;
	}
	
	@Override
	public String toString() {
		return String.format("move to %s", destination.toString());
	}
}
