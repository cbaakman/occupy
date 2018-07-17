package net.cbaakman.occupy.communicate;

public interface Identifier {

	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object other);
}
