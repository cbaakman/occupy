package net.cbaakman.occupy.resource;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.load.LoadRecord;

public abstract class FileDependentResource<T> implements Resource<T> {
	
	Logger logger = Logger.getLogger(FileDependentResource.class);

	private String path;
	
	public FileDependentResource(String path) {
		this.path = path;
	}

	@Override
	public final T init(GL3 gl3) throws InitError {
		InputStream is = FileDependentResource.class.getResourceAsStream(path);
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
	public final void dispose(GL3 gl3) {
	}
	
	@Override
	public String toString() {
		return String.format("%s:%s", this.getClass().getCanonicalName(), path);
	}
}
