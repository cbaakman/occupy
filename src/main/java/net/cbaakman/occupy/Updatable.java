package net.cbaakman.occupy;


import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.cbaakman.occupy.communicate.Identifier;

@Data
public abstract class Updatable {
	
	@Setter(AccessLevel.NONE)
	private Identifier ownerId;
	
	protected Updatable(Identifier ownerId) {
		this.ownerId = ownerId;
	}
	
	public void updateOnClient(final float dt) {
	}
	public void updateOnServer(final float dt) {
	}
}
