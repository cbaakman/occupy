package net.cbaakman.occupy.render;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;

import net.cbaakman.occupy.communicate.Client;

public class ClientGLEventListener implements GLEventListener {

	private Client client;
	private GLU glu = new GLU();
	
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
        
        gl2.glDisable(GL2.GL_DEPTH_TEST);
        gl2.glDisable(GL2.GL_CULL_FACE);
        
        gl2.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

		gl2.glBegin(GL2.GL_QUADS);

		gl2.glVertex2f(100, 100);

		gl2.glVertex2f(500, 100);

		gl2.glVertex2f(500, 500);

		gl2.glVertex2f(100, 500);

		gl2.glEnd();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// TODO Auto-generated method stub

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub

	}

}
