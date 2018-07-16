package net.cbaakman.occupy.errors;

public class SeriousErrorHandler {

	/**
	 * Should be called when bugs occur.
	 */
	public static void handle(Exception e) {
		e.printStackTrace();
		System.exit(1);
	}
}
