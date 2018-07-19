package net.cbaakman.occupy.font;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class Font {

	/**
	 * all the font's glyphs should fit inside this
	 */
	private BoundingBox boundingBox;
	
	/**
	 * Can be overridden by the glyph's value.
	 */
	float horizOriginX = 0.0f,
		  horizOriginY = 0.0f,
	      horizAdvX = 0.0f,
	      size;
	
	private Map<Character, Glyph> glyphs = new HashMap<Character, Glyph>();
	private Map<Character, Map<Character, Float>> hKernTable = new HashMap<Character, Map<Character, Float>>();
	
	public Glyph getGlyph(Character c) {
		return glyphs.get(c);
	}
}
