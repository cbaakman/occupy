package net.cbaakman.occupy.load;

import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.resource.Resource;

public class Worker {
	
	Logger logger = Logger.getLogger(Worker.class);

	private Boolean running = false;

	public final boolean isRunningAJob() {
		synchronized(running) {
			return running;
		}
	}

	public final void loop(Loader loader, GL3 gl3) {
		while (loader.countWaiting() > 0) {
			
			LoadRecord<?> jobRecord;
			synchronized (running) {
				jobRecord = loader.takeJobFor(this);
				running = true;
			}
			
			if (jobRecord != null) {
				
				try {
					jobRecord.run(gl3);
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
