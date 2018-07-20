package net.cbaakman.occupy.font;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import lombok.Data;

@Data
public class Font {
	
	Logger logger = Logger.getLogger(Font.class);

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

	public float getHKern(char c1, char c2) {
		if (hKernTable.containsKey(c1)) {
			if (hKernTable.get(c1).containsKey(c2)) {
				return hKernTable.get(c1).get(c2);
			}
			else {
				return 0.0f;
			}
		}
		else {
			return 0.0f;
		}
	}

	public float getHorizAdvX(char c) {
		if (glyphs.containsKey(c)) {
			float advX = glyphs.get(c).getHorizAdvX();
			if (advX > 0.0f)
				return advX;
		}
		return horizAdvX;
	}

	public float getHorizOriginX(char c) {
		if (glyphs.containsKey(c)) {
			float originX = glyphs.get(c).getHorizOriginX();
			if (originX > 0.0f)
				return originX;
		}
		return horizOriginX;
	}

	public float getHorizOriginY(char c) {
		if (glyphs.containsKey(c)) {
			float originY = glyphs.get(c).getHorizOriginY();
			if (originY > 0.0f)
				return originY;
		}
		return horizOriginY;
	}

	public float getGlyphWidth(char c) {
		if (glyphs.containsKey(c))
			return glyphs.get(c).getHorizAdvX();
		
		return 0.0f;
	}
}
