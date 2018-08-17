package net.cbaakman.occupy.render;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.math.Vector2f;

public class GLSprite2DRenderer {
	
	private static class SpriteVertex extends Vertex {
		@VertexAttrib(index=0)
		Vector2f position;

		@VertexAttrib(index=1)
		Vector2f texCoord;
		
		public SpriteVertex(float px, float py, float tx, float ty) {
			this.position = new Vector2f(px, py);
			this.texCoord = new Vector2f(tx, ty);
		}
	}

	
	private static final String VERTEX_SHADER_SRC = "#version 150\n" +
										      		"uniform mat4 projectionMatrix;" + 
										      		"in vec2 position;" + 
										      		"in vec2 texCoord;" +
										      		"out vec2 texCoords;" +
										      		"void main() { " +
										      		"  texCoords = texCoord;" +
										      		"  gl_Position = projectionMatrix * vec4(position.x, position.y, 0.0, 1.0);" +
										      		"}",
						        FRAGMENT_SHADER_SRC = "#version 150\n" +
						        					  "uniform sampler2D spriteTexture;" +
						        					  "in vec2 texCoords;" + 
										      		  "out vec4 fragColor;" + 
										        	  "void main() { fragColor = texture(spriteTexture, texCoords); }";
	
	private VertexBuffer<SpriteVertex> vbo;
	private int shaderProgram;
	private Texture texture = null;
	
	public GLSprite2DRenderer(GL3 gl3) throws GL3Error, ShaderCompileError, ShaderLinkError {
		vbo = VertexBuffer.create(gl3, SpriteVertex.class, 4, GL3.GL_DYNAMIC_DRAW);
		
		shaderProgram = Shader.createProgram(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
	}
	
	public void cleanUp(GL3 gl3) throws GL3Error {
		vbo.cleanup(gl3);
		Shader.deleteProgram(gl3, shaderProgram);
	}
	
	/**
	 * @param position	center of the sprite
	 * @param scale		when 1.0, a -1.0,-1.0 to 1.0, 1.0 square is rendered around 'position'
	 * @param tx1	left horizontal coordinate, rendered at negative x
	 * @param ty1	top vertical coordinate, rendered at negative y
	 * @param tx2	right horizontal coordinate, rendered at positive x
	 * @param ty2	bottom vertical coordinate, rendered at positive y
	 */
	public void set(GL3 gl3, Vector2f position, float scale,
			        float tx1, float ty1, float tx2, float ty2)
			        throws GL3Error {
		try {
			vbo.update(gl3,
				new SpriteVertex[] {
					new SpriteVertex(position.getX() - scale, position.getY() + scale, tx1, ty2),
					new SpriteVertex(position.getX() + scale, position.getY() + scale, tx2, ty2),
					new SpriteVertex(position.getX() - scale, position.getY() - scale, tx1, ty1),
					new SpriteVertex(position.getX() + scale, position.getY() - scale, tx2, ty1)
				},
			0);
		} catch (IndexOutOfBoundsException e) {
			throw new SeriousError(e);
		}
	}
	
	public void setTexture(Texture texture) {
		this.texture = texture;
	}
	
	public void render(GL3 gl3, float[] projectionMatrix) throws GL3Error {

		gl3.glUseProgram(shaderProgram);
		GL3Error.check(gl3);
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "projectionMatrix");
		if (projectionMatrixLocation == -1)
			GL3Error.throwMe(gl3);

		gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
		GL3Error.check(gl3);
		
		if (texture != null) {
			texture.enable(gl3);
			texture.bind(gl3);
		}
		
        gl3.glActiveTexture(GL3.GL_TEXTURE0);
		GL3Error.check(gl3);
        
        int textureLocation = gl3.glGetUniformLocation(shaderProgram, "spriteTexture");
        if (textureLocation == -1)
        	GL3Error.throwMe(gl3);
        
        gl3.glUniform1i(textureLocation, 0);
		GL3Error.check(gl3);
		
		vbo.draw(gl3, GL3.GL_TRIANGLE_STRIP);
		
		if (texture != null) {
			texture.disable(gl3);
		}

		gl3.glUseProgram(0);
		GL3Error.check(gl3);
	}
}
