package net.cbaakman.occupy.load;

public abstract class Resource<T> {
	
	private Throwable error = null;
	private T result = null;

	boolean isDone() {
		return error != null || result != null;
	}

	public boolean hasError() {
		return error != null;
	}

	public Throwable getError() {
		return error;
	}

	protected abstract T load() throws Exception;
	
	public void runLoad() {
		try {
			result = load();
		} catch (Throwable e) {
			error = e;
		}
	}

	public T get() {
		return result;
	}
}
