package net.cbaakman.occupy.game;

import java.util.UUID;

import lombok.Data;

@Data
public class AttackOrder extends Order {

	UUID targetId;
	
	public AttackOrder(UUID targetId) {
		this.targetId = targetId;
	}
}
