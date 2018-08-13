package net.cbaakman.occupy;

import java.util.ArrayList;
import java.util.List;

public class JobScheduler {

	private List<Runnable> jobs = new ArrayList<Runnable>();
	
	public void schedule(Runnable job) {
		synchronized(jobs) {
			jobs.add(job);
		}
	}
	
	public void executeAll() {
		synchronized(jobs) {
			for (Runnable job : jobs) {
				job.run();
			}
			jobs.clear();
		}
	}
}
