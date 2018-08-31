package net.cbaakman.occupy.resource;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;

public interface GL3ResourceInitializer {

	void run(GL3 gl3, GL3ResourceManager resourceManager) throws InitError, NotLoadedError;
}
