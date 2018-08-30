package net.cbaakman.occupy.load;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;

import net.cbaakman.occupy.errors.ErrorQueue;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.resource.Resource;

public class Loader {
	
	static Logger logger = Logger.getLogger(Loader.class);
	
	private Boolean concurrentStarted = false,
					finished = false;
	
	private Set<LoadRecord<?>> jobRecords = new HashSet<LoadRecord<?>>();
	
	private List<Worker> workers = new ArrayList<Worker>();
	
	private Integer nJobsDone = 0,
					nJobsError = 0;
	
	public LoadStats getStats() {
		synchronized(workers) {
			synchronized(jobRecords) {
				synchronized(nJobsDone) {
					synchronized(nJobsError) {
						LoadStats stats = new LoadStats();
						
						stats.setWaiting(jobRecords.size());
						stats.setDone(nJobsDone);
						stats.setError(nJobsError);
						
						int count = 0;
						for (Worker worker : workers)
							if (worker.isRunningAJob())
								count++;
						stats.setRunning(count);

						return stats;
					}
				}
			}
		}
	}
	
	private void onJobless(Worker worker) {
		boolean done = false;
		synchronized(workers) {
			workers.remove(worker);
			synchronized(jobRecords) {
				done = workers.isEmpty() && jobRecords.isEmpty();
			}
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
	
	private int nConcurrent;
	
	public Loader(int nConcurrent) {
		this.nConcurrent = nConcurrent;
	}
	
	public void startConcurrent(GLAutoDrawable drawable) {
		synchronized(workers) {
			synchronized(concurrentStarted) {
				int i;
				for (i = 0; i < nConcurrent; i++) {
					GLContext sharedContext;
					try {
						sharedContext = drawable.createContext(drawable.getContext());
					} catch (Exception e) {
						Loader.this.cancel();
						if (errorQueue != null)
							errorQueue.pushError(e);
						throw e;
					}
					
					final Worker worker = new Worker();
					
					Thread thread = new Thread(String.format("loader-%d", i)) {
						@Override
						public void run() {
							try {
								if (sharedContext.makeCurrent() == GLContext.CONTEXT_NOT_CURRENT)
									throw new SeriousError("cannot make context current");
							} catch (Exception e) {
								Loader.this.cancel();
								if (errorQueue != null)
									errorQueue.pushError(e);
								throw e;
							}
							
							worker.loop(Loader.this, sharedContext.getGL().getGL3());
							
							sharedContext.destroy();
							
							onJobless(worker);
						}
					};
					thread.start();
					
					workers.add(worker);
				}
				concurrentStarted = true;
			}
		}
	}
	
	public boolean isConcurrentStarted() {
		synchronized(concurrentStarted) {
			return concurrentStarted;
		}
	}
	public boolean isFinished() {
		synchronized(finished) {
			return finished;
		}
	}
	
	public void cancel() {		
		synchronized(jobRecords) {
			jobRecords.clear();  // prevent the workers from picking up new jobs
		}
	}
	
	public int countWaiting() {
		synchronized(jobRecords) {
			return jobRecords.size();
		}
	}


	public LoadRecord<?> takeJobFor(Worker worker) {
		synchronized(jobRecords) {
			for (LoadRecord<?> record : jobRecords) {
				if (record.isReady() && !record.hasRun()) {
					jobRecords.remove(record);
					return record;
				}
			}
		}
		
		return null;
	}

	public void reportResult(LoadRecord<?> jobRecord) {
		
		synchronized(nJobsDone) {
			if (jobRecord.isDone()) {
				nJobsDone++;
			}
		}
		synchronized(nJobsError) {
			if (jobRecord.hasFailed()) {
				nJobsError++;
				if (errorQueue != null)
					errorQueue.pushError(jobRecord.getError());
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
	
	public <T> LoadRecord<T> submit(Resource<T> resource) {
		
		final LoadRecord<T> record = new LoadRecord<T>(this, resource);
		
		synchronized(jobRecords) {
			jobRecords.add(record);
		}
		
		return record;
	}
}
