package net.cbaakman.occupy.resource;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;

public class GL3ResourceManager {
	
	private static Logger logger = Logger.getLogger(GL3ResourceManager.class);
	
	private Set<GL3Resource<?>> resources = new HashSet<GL3Resource<?>>();
			
	public void disposeAll(GL3 gl3) {
		for (GL3Resource<?> resource : resources)
			resource.dispose(gl3);
		resources.clear();
	}
	
	public <T> T submit(GL3 gl3, GL3Resource<T> resource) throws InitError,
																 NotLoadedError {
		resources.add(resource);
		
		return resource.init(gl3, this);
	}
}
