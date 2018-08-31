package net.cbaakman.occupy.resource;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;

public interface GL3Resource<T> {
	
	T init(GL3 gl3, GL3ResourceManager resourceManager) throws InitError, NotLoadedError;
	
	void dispose(GL3 gl3);
}
