package net.cbaakman.occupy.render;

import java.nio.FloatBuffer;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.math.Vector2f;

public class GL3Sprite2DRenderer {
	
	public static class SpriteVertex extends Vertex {
		@VertexAttrib(index=0)
		Vector2f position;

		@VertexAttrib(index=1)
		Vector2f texCoord;
		
		public SpriteVertex(float px, float py, float tx, float ty) {
			this.position = new Vector2f(px, py);
			this.texCoord = new Vector2f(tx, ty);
		}
	}

	
	private VertexBuffer<SpriteVertex> vbo;
	private ShaderProgram shaderProgram;
	private Texture texture = null;
	
	public GL3Sprite2DRenderer(ShaderProgram shaderProgram, VertexBuffer<SpriteVertex> vbo) {
		this.shaderProgram = shaderProgram;
		this.vbo = vbo;
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

		shaderProgram.useProgram(gl3, true);
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram.program(), "projectionMatrix");
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
        
        int textureLocation = gl3.glGetUniformLocation(shaderProgram.program(), "tex");
        if (textureLocation == -1)
        	GL3Error.throwMe(gl3);
        
        gl3.glUniform1i(textureLocation, 0);
		GL3Error.check(gl3);
		
		vbo.draw(gl3, GL3.GL_TRIANGLE_STRIP);
		
		if (texture != null) {
			texture.disable(gl3);
		}

		shaderProgram.useProgram(gl3, false);
	}
}
