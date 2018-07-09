package net.cbaakman.occupy.network;

import java.util.UUID;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@Data
public abstract class Updatable{
	
	@Setter(AccessLevel.NONE)
	private UUID ownerID;
	
	protected Updatable(UUID ownerID) {
		this.ownerID = ownerID;
	}
	
	public void updateOnClient(final float dt) {
	}
	public void updateOnServer(final float dt) {
	}
}
