package net.cbaakman.occupy.font;

import java.util.ArrayList;
import java.util.List;

public class TextAlignment {

	private List<TextLine> lines = new ArrayList<TextLine>();
	private float maxWidth;
	
	public TextAlignment(float maxWidth) {
		this.maxWidth = maxWidth;
	}
	
	public float getTextHeight() {
		
		float h = 0.0f;
		for (TextLine line : lines) {
			h += line.getHeight();
		}
		return h;
	}

	public List<TextLine> getLines() {
		return lines;
	}
	
	public float getMaxWidth() {
		return maxWidth;
	}
}
