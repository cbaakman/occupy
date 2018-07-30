package net.cbaakman.occupy.errors;

public class RenderError extends Exception {

	public RenderError(Throwable cause) {
		initCause(cause);
	}
	
	public RenderError(String message) {
		super(message);
	}
}
