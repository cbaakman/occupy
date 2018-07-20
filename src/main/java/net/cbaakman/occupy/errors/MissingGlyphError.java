package net.cbaakman.occupy.errors;

public class MissingGlyphError extends Exception {

	public MissingGlyphError(char c) {
		super(String.format("no glyph for \'%c\'", c));
	}
}
