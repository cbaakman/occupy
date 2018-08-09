package net.cbaakman.occupy;

public interface Identifier {

	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object other);
}
