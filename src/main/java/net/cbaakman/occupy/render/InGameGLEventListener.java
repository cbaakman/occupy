package net.cbaakman.occupy.render;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.game.Camera;
import net.cbaakman.occupy.game.Entity;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.render.entity.EntityRenderer;
import net.cbaakman.occupy.render.entity.InfantryRenderer;

public class InGameGLEventListener implements GLEventListener {
	
	Logger logger = Logger.getLogger(InGameGLEventListener.class);
	
	private Client client;

	private Map<Class<? extends Entity>, EntityRenderer<?>> entityRenderers =
			new HashMap<Class<? extends Entity>, EntityRenderer<?>>();
	
	public InGameGLEventListener(Client client) {
		this.client = client;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
        try {	        
	        gl3.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
			GL3Error.check(gl3);

			gl3.glClearDepth(1.0f);
			GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
			GL3Error.check(gl3);


			float[] projectionMatrix = new float[16];
			FloatUtil.makePerspective(projectionMatrix, 0, true,
									  (float)(Math.PI / 4),
									  ((float)drawable.getSurfaceWidth()) / drawable.getSurfaceHeight(),
									  0.1f, 1000.0f);

			float[] modelViewMatrix = client.getCamera().getMatrix();
			
			for (Updatable updatable : client.getUpdatables()) {
				if (updatable instanceof Entity) {
					Entity entity = (Entity)updatable;
					renderEntity(gl3, projectionMatrix, modelViewMatrix, entity.getClass().cast(entity));
				}
			}
			
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}
	
	private <T extends Entity> void renderEntity(GL3 gl3, float[] projectionMatrix,
														  float[] modelViewMatrix,
														  T entity)
							  throws GL3Error {
		Class<T> entityClass = (Class<T>)entity.getClass(); 
		EntityRenderer<T> renderer = (EntityRenderer<T>)entityRenderers.get(entityClass);
		if (renderer == null) {
			client.getErrorQueue().pushError(
					new KeyError(String.format("missing entity renderer for %s",
											   entityClass.getName())));
		}
		renderer.renderOpaque(gl3, projectionMatrix, modelViewMatrix, entity);
		renderer.renderTransparent(gl3, projectionMatrix, modelViewMatrix, entity);
	}
	
	private <T extends Entity> void registerRenderer(Class<T> entityClass, EntityRenderer<T> renderer) {
		entityRenderers.put(entityClass, renderer);
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			for (EntityRenderer<?> renderer : entityRenderers.values()) {
				renderer.cleanUp(gl3);
			}
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			registerRenderer(Infantry.class, new InfantryRenderer(client, gl3));
		} catch (GL3Error | SeriousError e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
	}
}
