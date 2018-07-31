package net.cbaakman.occupy.render;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import lombok.Data;
import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.MissingGlyphError;
import net.cbaakman.occupy.errors.RenderError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.Glyph;
import net.cbaakman.occupy.font.TextAlignment;
import net.cbaakman.occupy.font.TextLine;
import net.cbaakman.occupy.font.enums.HorizontalTextAlignment;
import net.cbaakman.occupy.font.enums.VerticalTextAlignment;
import net.cbaakman.occupy.math.Vector2f;

public class GLTextRenderer {
	
	static Logger logger = Logger.getLogger(GLTextRenderer.class);
	
	static private final String VERTEX_SHADER_SRC = "#version 150\n" +
												    "in vec2 position," +
												    "        texCoord;" +
												    "out vec2 texCoords;" +
												    "uniform mat4 projectionMatrix;" +
												    "void main() {" +
												    "  gl_Position = projectionMatrix * vec4(position.x, position.y, 0.0, 1.0);" +
												    "  texCoords = texCoord;" +
												    "}",
								FRAGMENT_SHADER_SRC = "#version 150\n" +
													  "uniform sampler2D glyphTexture;" +
													  "in vec2 texCoords;" +
													  "out vec4 fragColor;" +
													  "void main() {" +
													  "  fragColor = texture(glyphTexture, texCoords);" +
													  "}";
	private int shaderProgram = 0;
	
	@Data
	public class GLGlyphEntry {
		Texture glTexture;
	}
	private Map<Character, GLGlyphEntry> glyphEntries = new HashMap<Character, GLGlyphEntry>();
	
	private static final int POSITION_VERTEX_INDEX = 0,
						 	 TEXCOORD_VERTEX_INDEX = 1;
	
	class GlyphVertex extends Vertex {
		
		@VertexAttrib(index=POSITION_VERTEX_INDEX)
		Vector2f position = new Vector2f();

		@VertexAttrib(index=TEXCOORD_VERTEX_INDEX)
		Vector2f texCoord = new Vector2f();
		
		public GlyphVertex(float x, float y, float tx, float ty) {
			position = new Vector2f(x, y);
			texCoord = new Vector2f(tx, ty);
		}
	}
	
	VertexBuffer<GlyphVertex> vbo = null;
	
	private Font font;
	
	public GLTextRenderer(GL3 gl3, Font font) throws ShaderCompileError, ShaderLinkError, GL3Error {
		this.font = font;
		
		shaderProgram = Shader.createProgram(gl3, VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
        
        gl3.glBindAttribLocation(shaderProgram, POSITION_VERTEX_INDEX, "position");
		GL3Error.check(gl3);

        gl3.glBindAttribLocation(shaderProgram, TEXCOORD_VERTEX_INDEX, "texCoord");
		GL3Error.check(gl3);
		
		
		for (Entry<Character, Glyph> entry : font.getGlyphs().entrySet()) {
			
			GLGlyphEntry glyphEntry = new GLGlyphEntry();
			glyphEntry.setGlTexture(generateGlyphTexture(gl3, entry.getValue()));
			
			glyphEntries.put(entry.getKey(), glyphEntry);
		}
		
		vbo = VertexBuffer.create(gl3, GlyphVertex.class, 4, GL3.GL_DYNAMIC_DRAW);
	}
	
	public void cleanupGL(GL3 gl3) throws GL3Error {
		
		vbo.cleanup(gl3);

		Shader.deleteProgram(gl3, shaderProgram);
		
		for (GLGlyphEntry glyphEntry : glyphEntries.values()) {
			if (glyphEntry.getGlTexture() != null)
				glyphEntry.getGlTexture().destroy(gl3);
		}
	}
	
	private static Texture generateGlyphTexture(GL3 gl3, Glyph glyph) {
		
		// No image means no texture.
		if (glyph.getImage() == null)
			return null;

		// Fill the texture
		BufferedImage image = glyph.getImage();
		
		// In svg font the coordinates are upside down!
		TextureData textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), image, false);
		if (!textureData.getMustFlipVertically()) {
			ImageUtil.flipImageVertically(image);
			textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), image, false);
		}

		Texture texture = TextureIO.newTexture(gl3, textureData);

	    // These settings make it look smooth:
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
		
	    // This automatically clamps texture coordinates to [0.0 -- 1.0]
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_CLAMP_TO_EDGE);
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_CLAMP_TO_EDGE);
		
		return texture;
	}

	public void renderGlyph(GL3 gl3, float[] projectionMatrix, char c)
			throws MissingGlyphError, GL3Error {
		
		Glyph glyph = font.getGlyph(c);
		if (glyph == null)
			throw new MissingGlyphError(c);
		
		GLGlyphEntry glyphEntry = glyphEntries.get(c);
		
		if (glyph.getImage() == null || glyphEntry.getGlTexture() == null)
			return;
		
		float x = font.getHorizOriginX(c),
			  y = font.getHorizOriginY(c),
				
			  /* The glyph bounding box might be a little bit smaller than the texture.
			   * Use fw and fh to correct it. */
              tw = (float)(glyph.getImage().getWidth()) / font.getBoundingBox().getWidth(),
              th = (float)(glyph.getImage().getHeight()) / font.getBoundingBox().getHeight();
		
		
		float[] glyphMatrix = new float[16];
		FloatUtil.makeTranslation(glyphMatrix, true, -x, -y, 0.0f);
		
		gl3.glUseProgram(shaderProgram);
		GL3Error.check(gl3);
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram, "projectionMatrix");
		if (projectionMatrixLocation == -1)
			GL3Error.throwMe(gl3);	
		
		float[] resultMatrix = new float[16];
		FloatUtil.multMatrix(projectionMatrix, glyphMatrix, resultMatrix);
		gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(resultMatrix));
		GL3Error.check(gl3);
		
        gl3.glActiveTexture(GL3.GL_TEXTURE0);
		GL3Error.check(gl3);
		
        glyphEntry.getGlTexture().enable(gl3);
		GL3Error.check(gl3);
		
        glyphEntry.getGlTexture().bind(gl3);
		GL3Error.check(gl3);
        
        int textureLocation = gl3.glGetUniformLocation(shaderProgram, "glyphTexture");
        if (textureLocation == -1)
        	GL3Error.throwMe(gl3);
        
        gl3.glUniform1i(textureLocation, 0);
		GL3Error.check(gl3);
        
        GlyphVertex[] vertices = new GlyphVertex[] {
        	new GlyphVertex(x, y + glyph.getImage().getHeight(), 0.0f, th),
        	new GlyphVertex(x, y, 0.0f, 0.0f),
        	new GlyphVertex(x + glyph.getImage().getWidth(), y + glyph.getImage().getHeight(), tw, th),
        	new GlyphVertex(x + glyph.getImage().getWidth(), y, tw, 0.0f)
        };
        
		vbo.update(gl3, vertices, 0);
		vbo.draw(gl3, GL3.GL_TRIANGLE_STRIP);
        
        glyphEntry.getGlTexture().disable(gl3);
       
        gl3.glUseProgram(0);  
		GL3Error.check(gl3);
	}
	
	public void renderTextLeftAlign(GL3 gl3, float[] projectionMatrix, String text)
			throws MissingGlyphError, GL3Error {
		float x = 0.0f;
		int i = 0;
		for (i = 0; i < text.length(); i++) {

			if (i > 0) {
				x += font.getHKern(text.charAt(i - 1), text.charAt(i));
			}
			
			float[] translation = new float[16],
					resultMatrix = new float[16];
			FloatUtil.makeTranslation(translation, true, x, 0.0f, 0.0f);
			FloatUtil.multMatrix(projectionMatrix, translation, resultMatrix);
			
			renderGlyph(gl3, resultMatrix, text.charAt(i));
			
			x += font.getHorizAdvX(text.charAt(i));
		}
	}
	
	public void RenderAlignedText(GL3 gl3, float[] projectionMatrix, TextAlignment alignment,
								  HorizontalTextAlignment ha, VerticalTextAlignment va)
								throws MissingGlyphError, GL3Error {
		
		float[] vTtranslation = new float[16];
		if (va.equals(VerticalTextAlignment.TOP))
			FloatUtil.makeTranslation(vTtranslation, true, 0.0f, -alignment.getTextHeight(), 0.0f);
		else if (va.equals(VerticalTextAlignment.CENTER))
			FloatUtil.makeTranslation(vTtranslation, true, 0.0f, -alignment.getTextHeight() / 2, 0.0f);
		else
			FloatUtil.makeIdentity(vTtranslation);
		
		
		for (TextLine line : alignment.getLines()) {
			
			float[] hTranslation = new float[16];
			
			if (va.equals(HorizontalTextAlignment.RIGHT))
				FloatUtil.makeTranslation(hTranslation, true, (alignment.getMaxWidth() - line.getWidth()), 0.0f, 0.0f);
			else if (va.equals(HorizontalTextAlignment.CENTER))
				FloatUtil.makeTranslation(hTranslation, true, (alignment.getMaxWidth() - line.getWidth()) / 2, 0.0f, 0.0f);
			else
				FloatUtil.makeIdentity(hTranslation);
			
			float[] resultMatrix = FloatUtil.multMatrix(projectionMatrix, FloatUtil.multMatrix(hTranslation, vTtranslation));
			
			renderTextLeftAlign(gl3, resultMatrix, line.toString());
		}
	}
}
