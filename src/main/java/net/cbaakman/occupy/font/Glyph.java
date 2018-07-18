package net.cbaakman.occupy.font;

import java.awt.image.BufferedImage;

import lombok.Data;

@Data
public class Glyph {
	private String name = "";
	private char unicodeId;
	
	private BufferedImage image;
	
	/**
	* Negative means: don't use, take font's default.
	*/
	private float horizOriginX = -1.0f,
				  horizOriginY = -1.0f,
				  horizAdvX = -1.0f;
}
