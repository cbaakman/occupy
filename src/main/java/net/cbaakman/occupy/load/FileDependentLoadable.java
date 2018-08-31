package net.cbaakman.occupy.load;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;

public abstract class FileDependentLoadable<T> implements Loadable<T> {
	
	Logger logger = Logger.getLogger(FileDependentLoadable.class);

	private String path;
	
	public FileDependentLoadable(String path) {
		this.path = path;
	}

	@Override
	public final T load() throws InitError {
		InputStream is = FileDependentLoadable.class.getResourceAsStream(path);
		if (is == null)
			throw new InitError(new FileNotFoundException(path));
		
		try {
			return read(is);
		} catch (Exception e) {
			throw new InitError(e);
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	protected abstract T read(InputStream is) throws Exception;

	@Override
	public final Set<LoadRecord<?>> getDependencies() {
		return new HashSet<LoadRecord<?>>();
	}
	
	@Override
	public String toString() {
		return String.format("%s:%s", this.getClass().getCanonicalName(), path);
	}
}
