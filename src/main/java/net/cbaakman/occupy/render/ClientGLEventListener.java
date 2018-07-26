package net.cbaakman.occupy.render;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.transcoder.TranscoderException;
import org.xml.sax.SAXException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.FloatUtil;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.error.GL3Error;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.ParseError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.SVGStyle;
import net.cbaakman.occupy.font.Font;

public class ClientGLEventListener implements GLEventListener {

	private Font font;
	private Client client;
	private GLU glu = new GLU();
	private GLTextRenderer glTextRenderer;
	
	public ClientGLEventListener(Client client, Font font) {
		this.client = client;
		this.font = font;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		float[] projectionMatrix = new float[16],
				modelviewMatrix = new float[16];

		FloatUtil.makeOrtho(projectionMatrix, 0, true, 0.0f, client.getGLCanvas().getWidth(),
        											   0.0f, client.getGLCanvas().getHeight(), -1.0f, 1.0f);
		FloatUtil.makeIdentity(modelviewMatrix);

		int error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        
        gl3.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
        
        error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);
        
        error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        
        gl3.glEnable(GL3.GL_BLEND);
        
        error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        
        gl3.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        
		error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        
        float[] resultMatrix = new  float[16];
        FloatUtil.multMatrix(projectionMatrix, modelviewMatrix, resultMatrix);
        try {
			glTextRenderer.renderTextLeftAlign(gl3, resultMatrix, "Hello!");
		} catch (MissingGlyphError e) {
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		glTextRenderer.cleanupGL(gl3);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();

		glTextRenderer = new GLTextRenderer(gl3, font);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
	}
}
