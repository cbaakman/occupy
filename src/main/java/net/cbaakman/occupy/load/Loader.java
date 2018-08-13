package net.cbaakman.occupy.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import net.cbaakman.occupy.errors.ErrorQueue;

public class Loader {
	
	static Logger logger = Logger.getLogger(Loader.class);
	
	private class LoadJobEntry {
		public LoadJobEntry(LoadJob<? extends Object> job) {
			this.job = job;
		}
		private LoadJob<? extends Object> job;
		
		private Object result = null;
		private ExecutionException error = null;
		
		public LoadJob<? extends Object> getJob() {
			return job;
		}
		
		// Make sure the setters and getters are not used at the same time!
		
		public synchronized Object getResult() {
			return result;
		}
		
		public synchronized void setResult(Object r) {
			result = r;
		}
		
		public synchronized ExecutionException getError() {
			return error;
		}
		
		public synchronized void setError(ExecutionException e) {
			error = e;
		}
	}
	
	private Boolean started = false,
					finished = false;
	
	private List<LoadJobEntry> jobs = new ArrayList<LoadJobEntry>();
	private Integer nJobsDone = 0,
					nJobsError = 0;
	
	public LoadStats getStats() {
		synchronized(threads) {
			synchronized(jobs) {
				synchronized(nJobsDone) {
					synchronized(nJobsError) {
						LoadStats stats = new LoadStats();
						
						stats.setWaiting(jobs.size());
						stats.setDone(nJobsDone);
						stats.setError(nJobsError);
						
						int count = 0;
						for (LoaderThread thread : threads)
							if (thread.isRunningAJob())
								count++;
						stats.setRunning(count);

						return stats;
					}
				}
			}
		}
	}
	
	private void markJobless(LoaderThread thread) {
		boolean done = false;
		synchronized(threads) {
			threads.remove(thread);
			done = threads.size() <= 0;
		}
		if (done) {
			synchronized(finished) {
				finished = true;
			}
			
			if (runWhenDone != null) {
				Thread t = new Thread(runWhenDone);
				t.start();
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
		synchronized(started) {
			synchronized(threads) {
				for (LoaderThread t : threads)
					t.start();
			}
			started = true;
		}
	}
	
	public boolean isStarted() {
		synchronized(started) {
			return started;
		}
	}
	public boolean isFinished() {
		synchronized(finished) {
			return finished;
		}
	}
	
	public void interrupt() {
		synchronized(jobs) {
			jobs.clear();  // prevent the threads from picking up new jobs
		}
	}
	
	private int countWaiting() {
		synchronized(jobs) {
			return jobs.size();
		}
	}
	
	private class LoaderThread extends Thread {
		
		private Boolean runningAJob = false;
		
		@Override
		public void run() {
			// WARNING: This thread will hang if 'getStats' is called in it!
			while (countWaiting() > 0) {
				LoadJobEntry chosenEntry = null;
				synchronized(jobs) {
					synchronized(runningAJob) {
						for (LoadJobEntry entry : jobs) {
							if (entry.getJob().isReady() &&
									entry.getResult() == null && entry.getError() == null) {

								jobs.remove(entry);
								chosenEntry = entry;
								runningAJob = true;
								break;
							}
						}
					}
				}
				
				if (chosenEntry != null) {
					try {
						Object result = chosenEntry.getJob().call();
						
						synchronized(nJobsDone) {
							synchronized(runningAJob) {
								chosenEntry.setResult(result);
								
								nJobsDone++;
								runningAJob = false;
							}
						}
					} catch (Exception e) {
						synchronized(nJobsError) {
							synchronized(runningAJob) {
								
								if (errorQueue != null)
									errorQueue.pushError(e);
								
								chosenEntry.setError(new ExecutionException(e));
								
								nJobsError++;
								runningAJob = false;
							}
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
			
			markJobless(this);
		}

		public boolean isRunningAJob() {
			synchronized(runningAJob) {
				return runningAJob;
			}
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
						throw entry.getError();
					
					Thread.sleep(100);
				}
				return (T)entry.getResult();
			}

			@Override
			public T get(long n, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException {
				
				long t0 = System.currentTimeMillis();
				
				while (entry.getResult() == null) {
				
					if ((System.currentTimeMillis() - t0) > unit.toMillis(n))
						throw new TimeoutException();
					
					if (entry.getError() != null)
						throw entry.getError();
					
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
