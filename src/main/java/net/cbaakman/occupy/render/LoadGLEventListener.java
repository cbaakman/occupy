package net.cbaakman.occupy.render;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.math.FloatUtil;

import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.error.GL3Error;
import net.cbaakman.occupy.error.ShaderCompileError;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector2f;

public class LoadGLEventListener implements GLEventListener {
	
	private static Logger logger = Logger.getLogger(LoadGLEventListener.class);

	private Loader loader;
	
	private static float LOAD_BAR_WIDTH = 200.0f;
	private static float LOAD_BAR_HEIGHT = 20.0f;
	private static float LOAD_BAR_EDGE = 3.0f;
	
	private Vector2f[] frameVectors;
	
	public LoadGLEventListener(Loader loader) {
		this.loader = loader;
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
		
		public LoadVertex(Vector2f position) {
			this.position = position;
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
		
		List<LoadVertex> verticesFrame = new ArrayList<LoadVertex>();
		verticesFrame.add(new LoadVertex(new Vector2f(rects[0][0], rects[0][1])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[1][0], rects[1][1])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[0][2], rects[0][1])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[1][2], rects[1][1])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[0][2], rects[0][3])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[1][2], rects[1][3])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[0][0], rects[0][3])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[1][0], rects[1][3])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[0][0], rects[0][1])));
		verticesFrame.add(new LoadVertex(new Vector2f(rects[1][0], rects[1][1])));

		try {
			vboFrame = VertexBuffer.create(gl3, LoadVertex.class, verticesFrame.size(), GL3.GL_STATIC_DRAW);
			vboFrame.update(gl3, verticesFrame, 0);
			
			vboBar = VertexBuffer.create(gl3, LoadVertex.class, 4, GL3.GL_DYNAMIC_DRAW);
		} catch (GL3Error e) {
			SeriousErrorHandler.handle(e);
		}
		
		int vertexShader = 0,
			fragmentShader = 0;
		try {			
			vertexShader = Shader.compile(gl3, GL3.GL_VERTEX_SHADER, VERTEX_SHADER_SRC);
			fragmentShader = Shader.compile(gl3, GL3.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SRC);
			shaderProgram = Shader.createProgram(gl3, new int[] {vertexShader, fragmentShader});
			
		} catch (ShaderCompileError | InitError e) {
			
			gl3.glDeleteShader(vertexShader);
			gl3.glDeleteShader(fragmentShader);
			
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		
		GL3 gl3 = drawable.getGL().getGL3();
		
		gl3.glDeleteProgram(shaderProgram);
	
		int error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
		
		try {
			vboFrame.cleanup(gl3);
			vboBar.cleanup(gl3);
		} catch (GL3Error e) {
			SeriousErrorHandler.handle(e);
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		
		GL3 gl3 = drawable.getGL().getGL3();
		
        int w = drawable.getSurfaceWidth(),
            h = drawable.getSurfaceHeight();
		
    	float[] projectionMatrix = new float[16];
		FloatUtil.makeOrtho(projectionMatrix, 0, true, -(float)(w) / 2, (float)(w) / 2, -(float)(h) / 2, (float)(h) / 2, -1.0f, 1.0f);
		
		gl3.glUseProgram(shaderProgram);
		int error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "projectionMatrix");
		if (projectionMatrixLocation == -1)
			SeriousErrorHandler.handle(new GL3Error(gl3.glGetError()));
		
		gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
		error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
		
        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);
		
		
		int loadDone = loader.getJobsDone(),
			loadTotal = loadDone + loader.countJobsLeft();
        float fractionLoaded = 0.0f;
        if (loadTotal > 0)
            fractionLoaded = ((float)loadDone) / loadTotal;
        
        gl3.glDisable(GL3.GL_CULL_FACE);
        gl3.glDisable(GL3.GL_DEPTH_TEST);
        
        gl3.glBindAttribLocation(shaderProgram, VERTEX_INDEX, "position");
        error = gl3.glGetError();
        if (error != GL3.GL_NO_ERROR)
			SeriousErrorHandler.handle(new GL3Error(error));
        

	    float x1, x2;
        x1 = rects[2][0];
        x2 = x1 + fractionLoaded * (rects[2][2] - rects[2][0]);
        
        LoadVertex[] barVertices = new LoadVertex[] {
        	new LoadVertex(new Vector2f(x1, rects[2][1])),
        	new LoadVertex(new Vector2f(x1, rects[2][3])),
        	new LoadVertex(new Vector2f(x2, rects[2][1])),
        	new LoadVertex(new Vector2f(x2, rects[2][3]))
        };        
        
        try {
    	    vboFrame.draw(gl3, GL3.GL_TRIANGLE_STRIP);
    	    
    	    vboBar.update(gl3,  barVertices, 0);
    	    vboBar.draw(gl3, GL3.GL_TRIANGLE_STRIP);
	    } catch (GL3Error e) {
			SeriousErrorHandler.handle(new GL3Error(error));
	    }
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}

}
