package net.cbaakman.occupy.network;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Updatable {
	
	@Setter(AccessLevel.NONE)
	private UUID ownerId;
	
	protected Updatable(UUID ownerId) {
		this.ownerId = ownerId;
	}
	
	public void updateOnClient(final float dt) {
	}
	public void updateOnServer(final float dt) {
	}
}
