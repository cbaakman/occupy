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

import lombok.Data;
import net.cbaakman.occupy.font.FontFactory;

public class Loader {
	
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

	public int countJobsLeft() {
		synchronized(jobs) {
			return jobs.size();
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
	
	public static void runJob(LoadJobEntry entry) {
		try {
			entry.result = entry.job.call();
		} catch (Exception e) {
			entry.error = e;
		}
	}
	
	private class LoaderThread extends Thread {
		@Override
		public void run() {			
			while (countJobsLeft() > 0) {
				LoadJobEntry entry = pickJobToRun();
				if (entry != null)
					runJob(entry);
			}
		}
	}
	
	private List<LoaderThread> threads = new ArrayList<LoaderThread>();
	
	public Loader(int nConcurrent) {
		
		int i;
		for (i = 0; i < nConcurrent; i++)
			threads.add(new LoaderThread());
	}
	
	public void start() {
		for (LoaderThread t : threads)
			t.start();
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
