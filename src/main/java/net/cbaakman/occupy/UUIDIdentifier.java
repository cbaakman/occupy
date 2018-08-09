package net.cbaakman.occupy;

import java.io.Serializable;
import java.util.UUID;

public class UUIDIdentifier implements Identifier {

	private UUID id;
	
	public UUIDIdentifier(UUID id) {
		this.id = id;
	}
	
	public int hashCode() {
		return id.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other instanceof UUIDIdentifier)
		{
			UUIDIdentifier otherIdentifier = (UUIDIdentifier)other;
			return otherIdentifier.getID().equals(id);
		}
		else
			return false;
	}

	private UUID getID() {
		return id;
	}
}
