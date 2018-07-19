package net.cbaakman.occupy.font;

import java.awt.image.BufferedImage;

import lombok.Data;

@Data
public class Glyph {
	private String name = "";
	private char unicodeId;
	
	/**
	 * May be null if the glyph isn't supposed to have an image.
	 */
	private BufferedImage image = null;
	
	/**
	* Negative means: don't use, take font's default.
	*/
	private float horizOriginX = -1.0f,
				  horizOriginY = -1.0f,
				  horizAdvX = -1.0f;
}
