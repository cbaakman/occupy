package net.cbaakman.occupy.render;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import net.cbaakman.occupy.load.Loader;

public class LoadGLEventListener implements GLEventListener {
	
	private static Logger logger = Logger.getLogger(LoadGLEventListener.class);

	private Loader loader;
	
	private static float LOAD_BAR_WIDTH = 200.0f;
	private static float LOAD_BAR_HEIGHT = 20.0f;
	private static float LOAD_BAR_EDGE = 3.0f;
	
	/*
	    rect order: x1, y1, x2, y2
	    rect 1 is most outside, rect 3 is most inside
	 */
	private float [][] rects;
	
	public LoadGLEventListener(Loader loader) {
		this.loader = loader;
		
	    float f;
	    rects = new float[3][4];
	    for (int i = 0; i < 3; i++)
	    {
	        f = 2 - i;
	
	        rects[i][0] = -(f * LOAD_BAR_EDGE + LOAD_BAR_WIDTH / 2);
	        rects[i][1] = -(f * LOAD_BAR_EDGE + LOAD_BAR_HEIGHT / 2);
	        rects[i][2] = f * LOAD_BAR_EDGE + LOAD_BAR_WIDTH / 2;
	        rects[i][3] = f * LOAD_BAR_EDGE + LOAD_BAR_HEIGHT / 2;
	    }
	}
	
	@Override
	public void init(GLAutoDrawable drawable) {
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		int loadDone = loader.getJobsDone(),
			loadTotal = loadDone + loader.countJobsLeft();
        float fractionLoaded = 0.0f;
        if (loadTotal > 0)
            fractionLoaded = ((float)loadDone) / loadTotal;
		
		GL2 gl2 = drawable.getGL().getGL2();
        
        int w = drawable.getSurfaceWidth(),
        	h = drawable.getSurfaceHeight();

        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glLoadIdentity();
        gl2.glOrtho(-(float)(w) / 2, (float)(w) / 2, -(float)(h) / 2, (float)(h) / 2, -1.0f, 1.0f);

        gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glLoadIdentity();

        gl2.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl2.glClear(GL2.GL_COLOR_BUFFER_BIT);

        gl2.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl2.glDisable(GL2.GL_CULL_FACE);
        gl2.glDisable(GL2.GL_LIGHTING);

        // Outer line:
        gl2.glBegin(GL2.GL_QUAD_STRIP);
        gl2.glVertex2f(rects[0][0], rects[0][1]);
        gl2.glVertex2f(rects[1][0], rects[1][1]);
        gl2.glVertex2f(rects[0][2], rects[0][1]);
        gl2.glVertex2f(rects[1][2], rects[1][1]);
        gl2.glVertex2f(rects[0][2], rects[0][3]);
        gl2.glVertex2f(rects[1][2], rects[1][3]);
        gl2.glVertex2f(rects[0][0], rects[0][3]);
        gl2.glVertex2f(rects[1][0], rects[1][3]);
        gl2.glVertex2f(rects[0][0], rects[0][1]);
        gl2.glVertex2f(rects[1][0], rects[1][1]);
        gl2.glEnd();

        // progress bar
	    float x1, x2;
        x1 = rects[2][0];
        x2 = x1 + fractionLoaded * (rects[2][2] - rects[2][0]);
        gl2.glBegin(GL2.GL_QUADS);
        gl2.glVertex2f(x1, rects[2][1]);
        gl2.glVertex2f(x2, rects[2][1]);
        gl2.glVertex2f(x2, rects[2][3]);
        gl2.glVertex2f(x1, rects[2][3]);
        gl2.glEnd();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}

}
