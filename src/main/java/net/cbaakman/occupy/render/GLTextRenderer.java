package net.cbaakman.occupy.render;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.Glyph;

public class GLTextRenderer {

	static void renderGlyph(GL2 gl2, Glyph glyph, Font font) {
		
		float x = 0.0f, y = 0.0f,
				
			  // The glyph bouding box might be a little bit smaller than the texture. Use fw and fh to correct it.
              tw = (float)(glyph.getImage().getWidth()) / font.getBoundingBox().getWidth(),
              th = (float)(glyph.getImage().getHeight()) / font.getBoundingBox().getHeight();
	
		if (glyph.getHorizOriginX() > 0.0f)
			x = glyph.getHorizOriginX();
		if (glyph.getHorizOriginY() > 0.0f)
			y = glyph.getHorizOriginY();
		
		gl2.glPushMatrix();
		gl2.glTranslatef(x, y, 0.0f);
		
		//gl2.glBindTexture(GL2.GL_TEXTURE_2D, glyph);

		gl2.glBegin(GL2.GL_QUADS);

		gl2.glTexCoord2f (0, th);
		gl2.glVertex2f (x, y);

		gl2.glTexCoord2f (tw, th);
		gl2.glVertex2f (x + glyph.getImage().getWidth(), y);

		gl2.glTexCoord2f (tw, 0);
		gl2.glVertex2f (x + glyph.getImage().getWidth(), y + glyph.getImage().getHeight());

		gl2.glTexCoord2f (0, 0);
		gl2.glVertex2f (x, y + glyph.getImage().getHeight());

		gl2.glEnd();
		
		gl2.glPopMatrix();
	}
}
