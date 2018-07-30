package net.cbaakman.occupy.render;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.math.FloatUtil;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.font.Font;

public class ClientGLEventListener implements GLEventListener {

	private Font font;
	private Client client;
	private GLTextRenderer glTextRenderer;
	
	public ClientGLEventListener(Client client, Font font) {
		this.client = client;
		this.font = font;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
        try {
			float[] projectionMatrix = new float[16],
					modelviewMatrix = new float[16];
	
			FloatUtil.makeOrtho(projectionMatrix, 0, true, 0.0f, client.getGLCanvas().getWidth(),
	        											   0.0f, client.getGLCanvas().getHeight(), -1.0f, 1.0f);
			FloatUtil.makeIdentity(modelviewMatrix);
	        
	        gl3.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
			GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);
			GL3Error.check(gl3);
	        
	        gl3.glEnable(GL3.GL_BLEND);
			GL3Error.check(gl3);
	        
	        gl3.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
			GL3Error.check(gl3);
	        
	        float[] resultMatrix = new  float[16];
	        FloatUtil.multMatrix(projectionMatrix, modelviewMatrix, resultMatrix);

			glTextRenderer.renderTextLeftAlign(gl3, resultMatrix, "Hello!");
		} catch (MissingGlyphError | GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			glTextRenderer.cleanupGL(gl3);
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();

		try {
			glTextRenderer = new GLTextRenderer(gl3, font);
		} catch (ShaderCompileError | ShaderLinkError | GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
	}
}
