package net.cbaakman.occupy.errors;

public class CommunicationError extends Exception {
	
	public CommunicationError(Throwable cause) {
		initCause(cause);
	}
	
	public CommunicationError(String message) {
		super(message);
	}
}
