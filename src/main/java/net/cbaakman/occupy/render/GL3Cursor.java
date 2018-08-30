package net.cbaakman.occupy.render;

import java.awt.Point;

import com.jogamp.opengl.GLAutoDrawable;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.resource.ResourceManager;

public abstract class GL3Cursor {
	
	public void update(float dt) {
	}

	public abstract void render(GLAutoDrawable drawable, Point mousePosition) throws GL3Error;

	public abstract void orderFrom(ResourceManager resourceManager);
}
