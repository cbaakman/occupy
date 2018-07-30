package net.cbaakman.occupy.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.math.FloatUtil;

import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.ErrorQueue;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector2f;

public class LoadGLEventListener implements GLEventListener {
	
	private static Logger logger = Logger.getLogger(LoadGLEventListener.class);

	private Loader loader;
	private Client client;
	
	private static float LOAD_BAR_WIDTH = 200.0f;
	private static float LOAD_BAR_HEIGHT = 20.0f;
	private static float LOAD_BAR_EDGE = 3.0f;
	
	public LoadGLEventListener(Loader loader, Client client) {
		this.loader = loader;
		this.client = client;
	}
	
	private static final String VERTEX_SHADER_SRC = "#version 150\n" +
										      		"uniform mat4 projectionMatrix;\n" + 
										      		"in vec2 position;\n" + 
										      		"void main() { gl_Position = projectionMatrix * vec4(position.x, position.y, 0.0, 1.0); }",
						      FRAGMENT_SHADER_SRC = "#version 150\n" +
										      		"out vec4 fragColor;\n" + 
										        	"void main() { fragColor = vec4(1.0, 1.0, 1.0, 1.0); }";
	private int shaderProgram = 0;
	
	private static final int VERTEX_INDEX = 0;
	
	private class LoadVertex extends Vertex {
		
		@VertexAttrib(index=VERTEX_INDEX)
		private Vector2f position = new Vector2f();
		
		public LoadVertex(float x, float y) {
			this.position = new Vector2f(x, y);
		}
		
		public Vector2f getPosition() {
			return position;
		}
	}
	
	VertexBuffer<LoadVertex> vboFrame = null,
							 vboBar = null;
	
	static private float[][] rects = new float[3][4];
	static {
		/* 
	    	rect order: x1, y1, x2, y2
	    	rect 1 is most outside, rect 3 is most inside
		 */
	    float f;
	    int i;
	    for (i = 0; i < 3; i++)
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
		
		GL3 gl3 = drawable.getGL().getGL3();

		try {
			List<LoadVertex> verticesFrame = new ArrayList<LoadVertex>();
			verticesFrame.add(new LoadVertex(rects[0][0], rects[0][1]));
			verticesFrame.add(new LoadVertex(rects[1][0], rects[1][1]));
			verticesFrame.add(new LoadVertex(rects[0][2], rects[0][1]));
			verticesFrame.add(new LoadVertex(rects[1][2], rects[1][1]));
			verticesFrame.add(new LoadVertex(rects[0][2], rects[0][3]));
			verticesFrame.add(new LoadVertex(rects[1][2], rects[1][3]));
			verticesFrame.add(new LoadVertex(rects[0][0], rects[0][3]));
			verticesFrame.add(new LoadVertex(rects[1][0], rects[1][3]));
			verticesFrame.add(new LoadVertex(rects[0][0], rects[0][1]));
			verticesFrame.add(new LoadVertex(rects[1][0], rects[1][1]));

			vboFrame = VertexBuffer.create(gl3, LoadVertex.class, verticesFrame.size(), GL3.GL_STATIC_DRAW);
			vboFrame.update(gl3, verticesFrame, 0);
			
			vboBar = VertexBuffer.create(gl3, LoadVertex.class, 4, GL3.GL_DYNAMIC_DRAW); 
			
			shaderProgram = Shader.createProgram(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
		} catch (ShaderCompileError | GL3Error | ShaderLinkError e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			Shader.deleteProgram(gl3, shaderProgram);
			
			vboFrame.cleanup(gl3);
			vboBar.cleanup(gl3);
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		
		GL3 gl3 = drawable.getGL().getGL3();
		
        int w = drawable.getSurfaceWidth(),
            h = drawable.getSurfaceHeight();

        try {
		
	    	float[] projectionMatrix = new float[16];
			FloatUtil.makeOrtho(projectionMatrix, 0, true, -(float)(w) / 2, (float)(w) / 2, -(float)(h) / 2, (float)(h) / 2, -1.0f, 1.0f);
			
			gl3.glUseProgram(shaderProgram);
			GL3Error.check(gl3);
			
			int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "projectionMatrix");
			if (projectionMatrixLocation == -1)
				GL3Error.throwMe(gl3);
			
			gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
			GL3Error.check(gl3);
			
	        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	        GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);
	        GL3Error.check(gl3);		
			
			int loadDone = loader.getJobsDone(),
				loadTotal = loadDone + loader.countJobsLeft();
	        float fractionLoaded = 0.0f;
	        if (loadTotal > 0)
	            fractionLoaded = ((float)loadDone) / loadTotal;
	        
	        gl3.glDisable(GL3.GL_CULL_FACE);
	        GL3Error.check(gl3);
	        
	        gl3.glDisable(GL3.GL_DEPTH_TEST);
	        GL3Error.check(gl3);
	        
	        gl3.glBindAttribLocation(shaderProgram, VERTEX_INDEX, "position");
	        GL3Error.check(gl3);        
	
		    float x1, x2;
	        x1 = rects[2][0];
	        x2 = x1 + fractionLoaded * (rects[2][2] - rects[2][0]);
	        
	        LoadVertex[] barVertices = new LoadVertex[] {
	        	new LoadVertex(x1, rects[2][1]),
	        	new LoadVertex(x1, rects[2][3]),
	        	new LoadVertex(x2, rects[2][1]),
	        	new LoadVertex(x2, rects[2][3])
	        };        
        
		    vboFrame.draw(gl3, GL3.GL_TRIANGLE_STRIP);
		    
		    vboBar.update(gl3,  barVertices, 0);
		    vboBar.draw(gl3, GL3.GL_TRIANGLE_STRIP);
        }
        catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
        }
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}
}
