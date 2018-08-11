package net.cbaakman.occupy.render.entity;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.render.GLMeshRenderer;
import net.cbaakman.occupy.render.Shader;

public class InfantryRenderer extends EntityRenderer<Infantry> {
	
	static Logger logger = Logger.getLogger(InfantryRenderer.class);
	
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

	private int shaderProgram = 0;
	private GLMeshRenderer glMeshRenderer;

	public InfantryRenderer(Client client, GL3 gl3)
			throws GL3Error, SeriousError {
		BufferedImage meshImage;
		try {
			glMeshRenderer = new GLMeshRenderer(gl3, client.getResourceManager().getMesh("infantry"));
			meshImage = client.getResourceManager().getImage("infantry");
			
		} catch (KeyError | InterruptedException | ExecutionException e) {
			throw new SeriousError(e);
		}
		
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
		
		glMeshRenderer.setTexture("infantry", texture);
		
		try {
			shaderProgram = Shader.createProgram(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
		} catch (ShaderCompileError | ShaderLinkError e) {
			throw new SeriousError(e);
		}
        
        gl3.glBindAttribLocation(shaderProgram, GLMeshRenderer.POSITION_VERTEX_INDEX, "position");
		GL3Error.check(gl3);

        gl3.glBindAttribLocation(shaderProgram, GLMeshRenderer.TEXCOORD_VERTEX_INDEX, "texCoord");
		GL3Error.check(gl3);

        gl3.glBindAttribLocation(shaderProgram, GLMeshRenderer.NORMAL_VERTEX_INDEX, "normal");
		GL3Error.check(gl3);
	}
	
	public void cleanup(GL3 gl3) throws GL3Error {

		glMeshRenderer.cleanup(gl3);
		
		Shader.deleteProgram(gl3, shaderProgram);
	}

	public void renderOpaque(GL3 gl3, float[] projectionMatrix,
									  float[] modelViewMatrix, Infantry infantry)
			throws GL3Error {
		gl3.glDisable(GL3.GL_BLEND);
		GL3Error.check(gl3);
		
		gl3.glEnable(GL3.GL_CULL_FACE);
		GL3Error.check(gl3);

		gl3.glEnable(GL3.GL_DEPTH_TEST);
		GL3Error.check(gl3);
        
        gl3.glUseProgram(shaderProgram);
		GL3Error.check(gl3);
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "projectionMatrix");
		if (projectionMatrixLocation == -1)
			GL3Error.throwMe(gl3);	
		
		gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
		GL3Error.check(gl3);

		int modelviewMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "modelviewMatrix");
		if (modelviewMatrixLocation == -1)
			GL3Error.throwMe(gl3);	
		
		float[] resultMatrix = new float[16],
				translationMatrix = new float[16],
				rotationMatrix = new float[16];
		
		Vector3f position = infantry.getPosition();
		FloatUtil.makeTranslation(translationMatrix, true, position.getX(), position.getY(), position.getZ());
		infantry.getOrientation().toMatrix(rotationMatrix, 0);
		
		FloatUtil.multMatrix(modelViewMatrix, translationMatrix, resultMatrix);
		modelViewMatrix = resultMatrix.clone();
		FloatUtil.multMatrix(modelViewMatrix, rotationMatrix, resultMatrix);
		
		gl3.glUniformMatrix4fv(modelviewMatrixLocation, 1, false, FloatBuffer.wrap(resultMatrix));
		GL3Error.check(gl3);
		
        gl3.glActiveTexture(GL3.GL_TEXTURE0);
		GL3Error.check(gl3);
        
        int textureLocation = gl3.glGetUniformLocation(shaderProgram, "meshTexture");
        if (textureLocation == -1)
        	GL3Error.throwMe(gl3);
        
        gl3.glUniform1i(textureLocation, 0);
		GL3Error.check(gl3);

		glMeshRenderer.render(gl3, infantry.getAnimationState().getAnimationState());
        
        gl3.glUseProgram(0);
		GL3Error.check(gl3);
	}
}
