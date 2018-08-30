package net.cbaakman.occupy.render;

import java.awt.Point;
import java.util.concurrent.ExecutionException;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.resource.ResourceLinker;
import net.cbaakman.occupy.resource.ResourceLocator;
import net.cbaakman.occupy.resource.ResourceManager;
import net.cbaakman.occupy.resource.WaitResource;

public class GameDefaultCursor extends GL3Cursor {
	
	Texture texture;
	
	private GL3Sprite2DRenderer spriteRenderer;
	
	public GameDefaultCursor(GL3Sprite2DRenderer spriteRenderer) {
		this.spriteRenderer = spriteRenderer;
	}

	@Override
	public void orderFrom(ResourceManager resourceManager) {
		LoadRecord<Texture> asyncTexture = ResourceLinker.submitTextureJobs(
				resourceManager, ResourceLocator.getImagePath("cursor_default"));
		
		resourceManager.submit(new WaitResource(asyncTexture) {

			@Override
			protected void run(GL3 gl3) throws NotReadyError, InitError {
				texture = asyncTexture.get();
			}
		});
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
