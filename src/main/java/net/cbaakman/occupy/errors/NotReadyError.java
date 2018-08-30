package net.cbaakman.occupy.errors;

import net.cbaakman.occupy.resource.Resource;

public class NotReadyError extends Exception {

	public NotReadyError(Resource resource) {
		super(String.format("%s resource is not ready yet", resource.getClass().getName()));
	}
}
