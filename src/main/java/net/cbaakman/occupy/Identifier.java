package net.cbaakman.occupy;

import java.io.Serializable;

public interface Identifier extends Serializable {

	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object other);
}
