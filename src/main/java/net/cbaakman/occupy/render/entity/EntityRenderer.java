package net.cbaakman.occupy.render.entity;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.game.Entity;

public abstract class EntityRenderer<T extends Entity> {
	
	public void cleanUp(GL3 gl3) throws GL3Error {
	}

	public void renderOpaque(GL3 gl3, float[] projectionMatrix, float[] modelViewMatrix, T entity) throws GL3Error {		
	}
	
	public void renderTransparent(GL3 gl3, float[] projectionMatrix, float[] modelViewMatrix, T entity) throws GL3Error {
	}
}