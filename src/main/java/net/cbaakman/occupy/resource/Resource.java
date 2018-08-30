package net.cbaakman.occupy.resource;

import java.util.Set;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.load.LoadRecord;

public interface Resource<T> {
	
	/**
	 * Tells the loader that it should wait for these to complete,
	 * before initializing this resource.
	 */
	Set<LoadRecord<?>> getDependencies(); 

	T init(GL3 gl3) throws InitError, NotReadyError;
	
	void dispose(GL3 gl3);
}
