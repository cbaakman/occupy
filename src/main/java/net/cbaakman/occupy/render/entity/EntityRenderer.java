package net.cbaakman.occupy.render.entity;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.game.Entity;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.resource.GL3ResourceManager;
import net.cbaakman.occupy.resource.GL3ResourceUser;

public abstract class EntityRenderer<T extends Entity> implements GL3ResourceUser {

	public void renderOpaque(GL3 gl3, float[] projectionMatrix, float[] modelViewMatrix, T entity) throws GL3Error {		
	}
	
	public void renderTransparent(GL3 gl3, float[] projectionMatrix, float[] modelViewMatrix, T entity) throws GL3Error {
	}
}
