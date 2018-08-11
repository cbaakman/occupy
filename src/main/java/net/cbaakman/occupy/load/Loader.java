package net.cbaakman.occupy.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import lombok.Data;
import net.cbaakman.occupy.errors.ErrorQueue;

public class Loader {
	
	static Logger logger = Logger.getLogger(Loader.class);
	
	private class LoadJobEntry {
		public LoadJobEntry(LoadJob<? extends Object> job) {
			this.job = job;
		}
		private LoadJob<? extends Object> job;
		
		private Object result = null;
		private Exception error = null;
		
		public synchronized LoadJob<? extends Object> getJob() {
			return job;
		}
		
		public synchronized void setJob(LoadJob<? extends Object> j) {
			job = j;
		}
		
		public synchronized Object getResult() {
			return result;
		}
		
		public synchronized void setResult(Object r) {
			result = r;
		}
		
		public synchronized Exception getError() {
			return error;
		}
		
		public synchronized void setError(Exception e) {
			error = e;
		}
	}
	
	private List<LoadJobEntry> jobs = new ArrayList<LoadJobEntry>();
	private Integer nJobsDone = 0, nJobsError = 0;
	
	public LoadStats getStats() {
		
		LoadStats stats = new LoadStats();
		synchronized(nJobsDone) {
			synchronized(jobs) {
				synchronized(threads) {
					stats.setWaiting(jobs.size());
					stats.setDone(nJobsDone);
					stats.setError(nJobsError);
					
					int count = 0;
					for (LoaderThread t : threads)
						if (t.isAlive())
							count++;
					stats.setRunning(count);
				}
			}
		}
		return stats;
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
			entry.setResult(entry.getJob().call());

			synchronized(nJobsDone) {
				nJobsDone++;
			}
		} catch (Exception e) {
			if (errorQueue != null)
				errorQueue.pushError(e);
			entry.setError(e);

			synchronized(nJobsError) {
				nJobsError++;
			}
		}
	}
	
	private class LoaderThread extends Thread {
		@Override
		public void run() {				
			while (getStats().getWaiting() > 0) {
				LoadJobEntry entry = pickJobToRun();
				if (entry != null)
					runJob(entry);
				else
					try {
						Thread.sleep(1000);
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
	private ErrorQueue errorQueue = null;

	public void whenDone(Runnable runnable) {
		this.runWhenDone = runnable;
	}
	public void setErrorQueue(ErrorQueue errorQueue) {
		this.errorQueue = errorQueue;
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
	
	public void interrupt() {
		synchronized(jobs) {
			jobs.clear();  // prevent the threads from picking up new jobs
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
					
					Thread.sleep(100);
				}
				return (T)entry.getResult();
			}

			@Override
			public T get(long n, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				
				long t0 = System.currentTimeMillis();
				
				while (entry.getResult() == null &&
						(System.currentTimeMillis() - t0) < unit.toMillis(n)) {
					if (entry.getError() != null)
						throw new ExecutionException(entry.getError());
					
					Thread.sleep(unit.toMillis(1));
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
