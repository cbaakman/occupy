package net.cbaakman.occupy.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;

@Data
public abstract class Updater {
	
	@Setter(AccessLevel.NONE)
	private UUID id;
	
	public Updater() {
		this.id = UUID.randomUUID();
	}

	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	protected Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	
	public abstract void update(final float dt);
}
