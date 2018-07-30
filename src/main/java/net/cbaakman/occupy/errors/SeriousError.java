package net.cbaakman.occupy.errors;


/**
 * When this it thrown, it's surely a bug.
 */
public class SeriousError extends RuntimeException {

	public SeriousError(Throwable cause) {
		initCause(cause);
	}
	
	public SeriousError(String message) {
		super(message);
	}
}
