package net.cbaakman.occupy.errors;

public class InitError extends Exception {

	public InitError(Throwable cause) {
		initCause(cause);
	}
	
	public InitError(String message) {
		super(message);
	}
}
