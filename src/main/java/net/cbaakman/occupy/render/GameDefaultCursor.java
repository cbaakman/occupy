package net.cbaakman.occupy.render;

import java.awt.Point;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.resource.ResourceLinker;
import net.cbaakman.occupy.resource.ResourceLocator;
import net.cbaakman.occupy.resource.GL3ResourceUser;
import net.cbaakman.occupy.resource.GL3Resource;
import net.cbaakman.occupy.resource.GL3ResourceInitializer;
import net.cbaakman.occupy.resource.GL3ResourceManager;

public class GameDefaultCursor extends GL3Cursor implements GL3ResourceUser {
	
	static Logger logger = Logger.getLogger(GameDefaultCursor.class);
	
	private Texture texture;
	private GL3Sprite2DRenderer spriteRenderer;

	@Override
	public GL3ResourceInitializer pipeResources(Loader loader) throws InitError, NotLoadedError {
		
		final GL3Resource<GL3Sprite2DRenderer> resourceSprite2D = ResourceLinker.forSprite2D(loader);

		final GL3Resource<Texture> textureResource = ResourceLinker.forTexture(loader, ResourceLocator.getImagePath("cursor_default"));
		
		return new GL3ResourceInitializer() {
			@Override
			public void run(GL3 gl3, GL3ResourceManager resourceManager)
					throws InitError, NotLoadedError {
				spriteRenderer = resourceManager.submit(gl3, resourceSprite2D);
				texture = resourceManager.submit(gl3, textureResource);
			}
		};
	}

	private float[] projectionMatrix = new float[16];
	
	@Override
	public void render(GLAutoDrawable drawable, Point mousePosition) throws GL3Error {

		FloatUtil.makeOrtho(projectionMatrix, 0, true,
							0.0f, (float)drawable.getSurfaceWidth(),
							(float)drawable.getSurfaceHeight(), 0.0f,
							-1.0f, 1.0f);
		
		GL3 gl3  = drawable.getGL().getGL3();

		gl3.glEnable(GL3.GL_BLEND);
		GL3Error.check(gl3);
		
		gl3.glDisable(GL3.GL_DEPTH_TEST);
		GL3Error.check(gl3);
		
		gl3.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
		GL3Error.check(gl3);
				
		spriteRenderer.setTexture(texture);
		spriteRenderer.set(gl3, new Vector2f(mousePosition.x, mousePosition.y), 16.0f,
							   0.0f, 1.0f, 1.0f, 0.0f);
		spriteRenderer.render(gl3, projectionMatrix);
	}
}
