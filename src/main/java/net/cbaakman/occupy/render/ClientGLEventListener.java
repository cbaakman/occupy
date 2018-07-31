package net.cbaakman.occupy.render;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.mesh.MeshFactory;

public class ClientGLEventListener implements GLEventListener {
	
	Logger logger = Logger.getLogger(ClientGLEventListener.class);

	
	static private final String VERTEX_SHADER_SRC = "#version 150\n" +
												    "in vec3 position;" +
												    "in vec2 texCoord;" +
												    "in vec3 normal;" + 
												    "out VertexData {" +
												    "  vec2 texCoord;" +
												    "  vec3 normal;" +
												    "} vertexOut;" +
												    "uniform mat4 modelviewMatrix;" +
												    "uniform mat4 projectionMatrix;" +
												    "void main() {" +
												    "  gl_Position = projectionMatrix * modelviewMatrix * vec4(position, 1.0);" +
												    "  mat4 normalMatrix = transpose(inverse(modelviewMatrix));" +
												    "  vertexOut.texCoord = texCoord;" +
												    "  vertexOut.normal = (normalMatrix * vec4(normal, 1.0)).xyz;" +
												    "}",
								FRAGMENT_SHADER_SRC = "#version 150\n" +
													  "uniform sampler2D meshTexture;" +
													  "const vec3 lightDirection = vec3(0.5773, -0.5773, -0.5773);" + 
													  "in VertexData {" +
													  "  vec2 texCoord;" +
													  "  vec3 normal;" +
													  "} vertexIn;" +
													  "out vec4 fragColor;" +
													  "void main() {" +
													  "  vec3 n = normalize(vertexIn.normal);" +
													  "  float f = (1.0 - dot(lightDirection, n)) * 0.5;" +
													  "  fragColor = f * texture(meshTexture, vertexIn.texCoord);" +
													  "}";

	private Font font;
	private MeshFactory meshFactory;
	private Client client;
	private GLTextRenderer glTextRenderer;
	private GLMeshRenderer glMeshRenderer;
	private int meshShaderProgram;
	private BufferedImage meshImage;
	
	private long t0 = System.currentTimeMillis();;
	
	public ClientGLEventListener(Client client, Font font, MeshFactory meshFactory, BufferedImage meshImage) {
		this.client = client;
		this.font = font;
		this.meshFactory = meshFactory;
		this.meshImage = meshImage;
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		float rotation = ((float)(System.currentTimeMillis() - t0)) / 1000;
		
        try {
			float[] projectionMatrix = new float[16],
					modelviewMatrix = new float[16],
					rotationMatrix = new float[16],
					resultMatrix = new  float[16];
	        
	        gl3.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
			GL3Error.check(gl3);

			gl3.glClearDepth(1.0f);
			GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
			GL3Error.check(gl3);
			
	        gl3.glDisable(GL3.GL_BLEND);
			GL3Error.check(gl3);
			
			gl3.glEnable(GL3.GL_CULL_FACE);
			GL3Error.check(gl3);

			gl3.glEnable(GL3.GL_DEPTH_TEST);
			GL3Error.check(gl3);
	        
	        gl3.glUseProgram(meshShaderProgram);
			GL3Error.check(gl3);
			
			FloatUtil.makePerspective(projectionMatrix, 0, true,
									  (float)(Math.PI / 4),
									  ((float)drawable.getSurfaceWidth()) / drawable.getSurfaceHeight(),
									  0.1f, 1000.0f);
			
			FloatUtil.makeTranslation(modelviewMatrix, true, 0.0f, -3.0f, -10.0f);
			FloatUtil.makeRotationAxis(rotationMatrix, 0, rotation, 0.0f, 1.0f, 0.0f, new float[4]);
			FloatUtil.multMatrix(modelviewMatrix, rotationMatrix, resultMatrix);
			modelviewMatrix = resultMatrix.clone();
			
			int projectionMatrixLocation = gl3.glGetUniformLocation(meshShaderProgram, "projectionMatrix");
			if (projectionMatrixLocation == -1)
				GL3Error.throwMe(gl3);	
			
			gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
			GL3Error.check(gl3);

			int modelviewMatrixLocation = gl3.glGetUniformLocation(meshShaderProgram, "modelviewMatrix");
			if (modelviewMatrixLocation == -1)
				GL3Error.throwMe(gl3);	
			
			gl3.glUniformMatrix4fv(modelviewMatrixLocation, 1, false, FloatBuffer.wrap(modelviewMatrix));
			GL3Error.check(gl3);
			
	        gl3.glActiveTexture(GL3.GL_TEXTURE0);
			GL3Error.check(gl3);
	        
	        int textureLocation = gl3.glGetUniformLocation(meshShaderProgram, "meshTexture");
	        if (textureLocation == -1)
	        	GL3Error.throwMe(gl3);
	        
	        gl3.glUniform1i(textureLocation, 0);
			GL3Error.check(gl3);

	        glMeshRenderer.render(gl3);
	        
	        gl3.glUseProgram(0);
			GL3Error.check(gl3);
			
	        gl3.glEnable(GL3.GL_BLEND);
			GL3Error.check(gl3);
	        
	        gl3.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
			GL3Error.check(gl3);
			
			gl3.glDisable(GL3.GL_CULL_FACE);
			GL3Error.check(gl3);

			gl3.glDisable(GL3.GL_DEPTH_TEST);
			GL3Error.check(gl3);

			FloatUtil.makeOrtho(projectionMatrix, 0, true, 0.0f, drawable.getSurfaceWidth(),
	        											   0.0f, drawable.getSurfaceHeight(), -1.0f, 1.0f);
			FloatUtil.makeIdentity(modelviewMatrix);
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
			glMeshRenderer.cleanup(gl3);
			
			Shader.deleteProgram(gl3, meshShaderProgram);
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();

		try {
			glTextRenderer = new GLTextRenderer(gl3, font);
			
			glMeshRenderer = new GLMeshRenderer(gl3, meshFactory);
			
			TextureData textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), meshImage, true);
			if (textureData.getMustFlipVertically()) {
				ImageUtil.flipImageVertically(meshImage);
				textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), meshImage, true);
			}

			Texture texture = TextureIO.newTexture(gl3, textureData);

			texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
			texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
			texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
			texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
			
			glMeshRenderer.setTexture("Material", texture);
			
			meshShaderProgram = Shader.createProgram(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
	        
	        gl3.glBindAttribLocation(meshShaderProgram, GLMeshRenderer.POSITION_VERTEX_INDEX, "position");
			GL3Error.check(gl3);

	        gl3.glBindAttribLocation(meshShaderProgram, GLMeshRenderer.TEXCOORD_VERTEX_INDEX, "texCoord");
			GL3Error.check(gl3);

	        gl3.glBindAttribLocation(meshShaderProgram, GLMeshRenderer.NORMAL_VERTEX_INDEX, "normal");
			GL3Error.check(gl3);
			
		} catch (ShaderCompileError | ShaderLinkError | GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		// TODO Auto-generated method stub
	}
}
