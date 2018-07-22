package net.cbaakman.occupy.load;

import java.util.concurrent.Callable;

public interface LoadJob<T> extends Callable<T> {
	
	/**
	 * Should only return true if the job is ready to run.
	 * That is: all other jobs that this one depends on have finished.
	 */
	public boolean isReady();
}
