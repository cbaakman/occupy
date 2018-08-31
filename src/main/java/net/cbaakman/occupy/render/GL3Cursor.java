package net.cbaakman.occupy.render;

import java.awt.Point;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.resource.GL3ResourceManager;

public abstract class GL3Cursor {
	
	public void update(float dt) {
	}

	public abstract void render(GLAutoDrawable drawable, Point mousePosition) throws GL3Error;
}
