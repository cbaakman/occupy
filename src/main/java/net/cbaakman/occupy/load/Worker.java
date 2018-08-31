package net.cbaakman.occupy.load;

import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.resource.GL3Resource;

public class Worker {
	
	Logger logger = Logger.getLogger(Worker.class);

	private Boolean running = false;

	public final boolean isRunningAJob() {
		synchronized(running) {
			return running;
		}
	}

	public final void loop(Loader loader) {
		while (loader.countWaiting() > 0) {
			
			LoadRecord<?> jobRecord;
			synchronized (running) {
				jobRecord = loader.takeJobFor(this);
				running = true;
			}
			
			if (jobRecord != null) {
				
				try {
					jobRecord.run();
				} finally {
					synchronized (running) {
						loader.reportResult(jobRecord);
						running = false;
					}
				}
			}
			else {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}
}
