package net.cbaakman.occupy.errors;

import net.cbaakman.occupy.load.Loadable;

public class NotLoadedError extends Exception {

	public NotLoadedError(Loadable loadable) {
		super(String.format("%s resource is not ready yet", loadable.getClass().getName()));
	}
}
