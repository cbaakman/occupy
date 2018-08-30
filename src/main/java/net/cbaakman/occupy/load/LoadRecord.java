package net.cbaakman.occupy.load;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.resource.Resource;

public class LoadRecord<T> {

	private final Resource<T> resource;
	
	private final Loader loader;

	public LoadRecord(Loader loader, Resource<T> resource) {
		this.loader = loader;
		this.resource = resource;
	}
	
	private T result = null;
	private InitError error = null;
	
	public Loader getLoader() {
		return loader;
	}
	
	private synchronized void setError(InitError error) {
		this.error = error;
	}
	
	private synchronized void setResult(T result) {
		this.result = result;
	}
	
	public void run(GL3 gl3) {
		try {
			T result = resource.init(gl3);
			
			setResult(result);
		}
		catch (InitError e) {
			setError(e);
		} catch (NotReadyError e) {
			throw new SeriousError(e);
		}
	}

	public synchronized T get() throws NotReadyError, InitError {
		
		if (getError() != null)
			throw getError();

		if (getResult() == null)
			throw new NotReadyError(resource);
		
		return getResult();
	}

	public synchronized InitError getError() {
		return error;
	}
	
	public synchronized T getResult() {
		return result;
	}
	
	/**
	 * Meaning: ready to run.
	 */
	public synchronized boolean isReady() {
		for (LoadRecord<?> dependency : resource.getDependencies()) {
			if (!dependency.isDone())
				return false;
		}
		return true;
	}

	public synchronized boolean isDone() {
		return result != null;
	}
	
	public synchronized boolean hasFailed() {
		return error != null;
	}
	
	public synchronized boolean hasRun() {
		return result != null || error != null;
	}
	
	@Override
	public String toString() {
		return String.format("loadrecord-for-%s", resource.toString());
	}	
	
	/**
	 * Hash code is important, since the loader stores the jobs in a hash set.
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof LoadRecord<?>) {
			return other.hashCode() == this.hashCode();
		}
		else
			return false;
	}
}
