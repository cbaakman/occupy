package net.cbaakman.occupy.util;

public abstract class StringHashable {
	
	@Override
	public final int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public final boolean equals(Object other) {
		if (other.getClass().equals(this.getClass())) {
			return other.hashCode() == hashCode();
		}
		else
			return false;
	}
}
