package net.cbaakman.occupy.render;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import lombok.Data;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.Glyph;

public class GLTextRenderer {
	
	static Logger logger = Logger.getLogger(GLTextRenderer.class);
	
	@Data
	public class GLGlyphEntry {
		int glTextureId;
	}
	private Map<Character, GLGlyphEntry> glyphEntries = new HashMap<Character, GLGlyphEntry>();
	
	private Font font;
	
	public GLTextRenderer(GL2 gl2, Font font) {
		this.font = font;
		
		for (Entry<Character, Glyph> entry : font.getGlyphs().entrySet()) {
			
			GLGlyphEntry glyphEntry = new GLGlyphEntry();
			glyphEntry.setGlTextureId(generateGlyphTexture(gl2, entry.getValue()));
			
			glyphEntries.put(entry.getKey(), glyphEntry);
		}
	}
	
	public void cleanupGL(GL2 gl2) {
		for (GLGlyphEntry glyphEntry : glyphEntries.values()) {
			destroyGlyphTexture(gl2, glyphEntry.getGlTextureId());
		}
	}
	
	private static int generateGlyphTexture(GL2 gl2, Glyph glyph) {
		
		// No image means no texture.
		if (glyph.getImage() == null)
			return 0;
		
		int[] glTextureIds = {0};
		
		gl2.glGenTextures(1, glTextureIds, 0);
		
		gl2.glBindTexture(GL2.GL_TEXTURE_2D, glTextureIds[0]);

	    // These settings make it look smooth:
		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);

	    // This automatically clamps texture coordinates to [0.0 -- 1.0]
		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl2.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);

		// Fill the texture
		BufferedImage image = glyph.getImage();
		TextureData textureData = AWTTextureIO.newTextureData(gl2.getGLProfile(), image, false);
		gl2.glTexImage2D(GL2.GL_TEXTURE_2D, 0, textureData.getInternalFormat(),
						 textureData.getWidth(), textureData.getHeight(),
						 0, textureData.getPixelFormat(), textureData.getPixelType(), textureData.getBuffer());

		// Unbinding the texture
		gl2.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		
		return glTextureIds[0];
	}
	private static void destroyGlyphTexture(GL2 gl2, int glTextureId) {
		gl2.glDeleteTextures(1, new int[] {glTextureId}, 0);
	}

	void renderGlyph(GL2 gl2, Character c) {
		
		Glyph glyph = font.getGlyph(c);
		if (glyph.getImage() == null)
			return;
		
		float x = 0.0f, y = 0.0f,
				
			  /* The glyph bounding box might be a little bit smaller than the texture.
			   * Use fw and fh to correct it. */
              tw = (float)(glyph.getImage().getWidth()) / font.getBoundingBox().getWidth(),
              th = (float)(glyph.getImage().getHeight()) / font.getBoundingBox().getHeight();
	
		if (glyph.getHorizOriginX() > 0.0f)
			x = glyph.getHorizOriginX();
		if (glyph.getHorizOriginY() > 0.0f)
			y = glyph.getHorizOriginY();
		
		gl2.glPushMatrix();
		gl2.glTranslatef(x, y, 0.0f);
		
		gl2.glBindTexture(GL2.GL_TEXTURE_2D, glyphEntries.get(c).getGlTextureId());

		gl2.glBegin(GL2.GL_QUADS);

		gl2.glTexCoord2f(0.0f, th);
		gl2.glVertex2f(x, y + glyph.getImage().getHeight());

		gl2.glTexCoord2f(tw, th);
		gl2.glVertex2f(x + glyph.getImage().getWidth(), y + glyph.getImage().getHeight());

		gl2.glTexCoord2f(tw, 0.0f);
		gl2.glVertex2f(x + glyph.getImage().getWidth(), y);

		gl2.glTexCoord2f(0.0f, 0.0f);
		gl2.glVertex2f(x, y);

		gl2.glEnd();
		
		gl2.glPopMatrix();
	}
}
