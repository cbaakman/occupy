package net.cbaakman.occupy.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import lombok.Data;
import net.cbaakman.occupy.font.FontFactory;

public class Loader {
	
	static Logger logger = Logger.getLogger(Loader.class);
	
	@Data
	private class LoadJobEntry {
		public LoadJobEntry(LoadJob job) {
			this.job = job;
		}
		private LoadJob job;
		private Object result = null;
		private Exception error = null;
	}
	
	private List<LoadJobEntry> jobs = new ArrayList<LoadJobEntry>();
	private Integer nJobsDone = 0;

	public int countJobsLeft() {
		synchronized(jobs) {
			return jobs.size();
		}
	}
	public int getJobsDone() {
		synchronized(nJobsDone) {
			return nJobsDone;
		}
	}

	private LoadJobEntry pickJobToRun() {
		synchronized(jobs) {
			for (LoadJobEntry entry : jobs) {
				if (entry.getJob().isReady() &&
						entry.getResult() == null && entry.getError() == null) {
					
					jobs.remove(entry);
					return entry;
				}
			}
		}
		
		return null;
	}
	
	public void runJob(LoadJobEntry entry) {
		try {
			entry.result = entry.job.call();
		} catch (Exception e) {
			entry.error = e;
		}
		synchronized(nJobsDone) {
			nJobsDone++;
		}
	}
	
	private class LoaderThread extends Thread {
		@Override
		public void run() {			
			while (countJobsLeft() > 0) {
				LoadJobEntry entry = pickJobToRun();
				if (entry != null)
					runJob(entry);
				else
					try {
						Thread.currentThread().sleep(1000);
					} catch (InterruptedException e) {
						logger.error(e.getMessage(), e);
					}
			}
			
			if (countThreadsAlive() <= 1)  // am I the last worker thread?
				if (runWhenDone != null)
					runWhenDone.run();
		}
	}
	
	private Runnable runWhenDone = null;

	public void whenDone(Runnable runnable) {
		this.runWhenDone = runnable;
	}
	
	private List<LoaderThread> threads = new ArrayList<LoaderThread>();
	
	public Loader(int nConcurrent) {
		
		int i;
		synchronized(threads) {
			for (i = 0; i < nConcurrent; i++)
				threads.add(new LoaderThread());
		}
	}
	
	public void start() {
		synchronized(threads) {
			for (LoaderThread t : threads)
				t.start();
		}
	}
	
	public int countThreadsAlive() {

		int count = 0;
		synchronized(threads) {
			for (LoaderThread t : threads) {
				if (t.isAlive())
					count++;
			}
		}
		return count;
	}
	
	public <T> Future<T> add(LoadJob<T> job) {
		
		final LoadJobEntry entry = new LoadJobEntry(job);
		
		synchronized(jobs) {
			jobs.add(entry);
		}
		
		return new Future<T>() {

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				return false;
			}

			@Override
			public T get() throws InterruptedException, ExecutionException {
				
				while (entry.getResult() == null) {
					if (entry.getError() != null)
						throw new ExecutionException(entry.getError());
					
					Thread.currentThread().sleep(100);
				}
				return (T)entry.getResult();
			}

			@Override
			public T get(long n, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
				
				long t0 = System.currentTimeMillis();
				
				while (entry.getResult() == null &&
						(System.currentTimeMillis() - t0) < unit.toMillis(n)) {
					if (entry.getError() != null)
						throw new ExecutionException(entry.getError());
					
					Thread.currentThread().sleep(unit.toMillis(1));
				}
				return (T)entry.getResult();
			}

			@Override
			public boolean isCancelled() {
				return false;
			}

			@Override
			public boolean isDone() {
				return entry.getResult() != null;
			}
		};
	}
}
