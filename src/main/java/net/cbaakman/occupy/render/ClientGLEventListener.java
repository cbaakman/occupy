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

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.ParseError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.SVGStyle;
import net.cbaakman.occupy.font.Font;

public class ClientGLEventListener implements GLEventListener {

	private Client client;
	private GLU glu = new GLU();
	private GLTextRenderer glTextRenderer;
	
	public ClientGLEventListener(Client client) {
		this.client = client;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl2 = drawable.getGL().getGL2();

        // Projection.
        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glLoadIdentity();
        gl2.glOrtho(0.0f, client.getGLCanvas().getWidth(),
        			0.0f, client.getGLCanvas().getHeight(), -1.0f, 1.0f);

		// Change to model view matrix.
        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glLoadIdentity();
        
        gl2.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl2.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl2.glEnable(GL2.GL_TEXTURE_2D);
        gl2.glEnable(GL2.GL_BLEND);
        gl2.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        
        try {
			glTextRenderer.renderTextLeftAlign(gl2, "Hello!");
		} catch (MissingGlyphError e) {
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL2 gl2 = drawable.getGL().getGL2();
		glTextRenderer.cleanupGL(gl2);
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl2 = drawable.getGL().getGL2();
		
		FontFactory fontFactory;
		try {
			fontFactory = FontFactory.parse(ClientGLEventListener.class.getResourceAsStream("/font/Lumean.svg"));
			Font font = fontFactory.generateFont(36, new SVGStyle());
			
			glTextRenderer = new GLTextRenderer(gl2, font);
			
		} catch (NumberFormatException | IOException |
				 ParserConfigurationException | SAXException |
				 ParseError | TranscoderException | NullPointerException e) {
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
	}
}
