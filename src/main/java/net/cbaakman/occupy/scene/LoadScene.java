package net.cbaakman.occupy.scene;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.load.LoadStats;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.render.Vertex;
import net.cbaakman.occupy.render.VertexBuffer;
import net.cbaakman.occupy.resource.ShaderProgramResource;

public class LoadScene extends Scene {
	
	private static Logger logger = Logger.getLogger(LoadScene.class);

	private final Loader loader;
	private final Client client;
	
	private static float LOAD_BAR_WIDTH = 200.0f;
	private static float LOAD_BAR_HEIGHT = 20.0f;
	private static float LOAD_BAR_EDGE = 3.0f;
	
	public LoadScene(Client client, ResourceUsingScene nextScene) {
		this.loader = nextScene.getResourceManager().getLoader();
		this.client = client;

		loader.whenDone(new Runnable() {
			@Override
			public void run() {				
				client.switchScene(nextScene);
			}
		});
		loader.setErrorQueue(client.getErrorQueue());
	}
	
	private static final String VERTEX_SHADER_SRC = "#version 150\n" +
										      		"uniform mat4 projectionMatrix;\n" + 
										      		"in vec2 position;\n" + 
										      		"void main() { gl_Position = projectionMatrix * vec4(position.x, position.y, 0.0, 1.0); }",
						      FRAGMENT_SHADER_SRC = "#version 150\n" +
										      		"out vec4 fragColor;\n" + 
										        	"void main() { fragColor = vec4(1.0, 1.0, 1.0, 1.0); }";
	
	private ShaderProgram shaderProgram;
	
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
		
		loader.startConcurrent(drawable);
		
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
			  
			shaderProgram = ShaderProgramResource.link(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
	        
	        gl3.glBindAttribLocation(shaderProgram.program(), VERTEX_INDEX, "position");
	        GL3Error.check(gl3);        
	        
		} catch (ShaderCompileError | GL3Error | ShaderLinkError e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		
		if (!loader.isFinished())
			loader.cancel();
		
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			shaderProgram.destroy(gl3);
			
			vboFrame.dispose(gl3);
			vboBar.dispose(gl3);
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
			
			shaderProgram.useProgram(gl3, true);
			
			int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram.program(), "projectionMatrix");
			if (projectionMatrixLocation == -1)
				GL3Error.throwMe(gl3);
			
			gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
			GL3Error.check(gl3);
			
	        gl3.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
	        GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT);
	        GL3Error.check(gl3);		
	        
	        LoadStats loadStats = loader.getStats();
			
	        float fractionLoaded = 0.0f;
	        if (loadStats.getTotal() > 0)
	            fractionLoaded = ((float)loadStats.getDone()) / loadStats.getTotal();
	        
	        gl3.glDisable(GL3.GL_CULL_FACE);
	        GL3Error.check(gl3);
	        
	        gl3.glDisable(GL3.GL_DEPTH_TEST);
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

			shaderProgram.useProgram(gl3, false);
        }
        catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
        }
	}
}
