package net.cbaakman.occupy.font;

import java.awt.Color;
import java.util.Objects;

import lombok.Data;

@Data
public class FontStyle {
	
	private float size = 12.0f;
	
	private Color fillColor = new Color(1.0f, 1.0f, 1.0f, 1.0f),
				  strokeColor = new Color(0.0f, 0.0f, 0.0f, 1.0f);
	private float strokeWidth = 1.0f;
	
	@Override
	public int hashCode() {
		return Objects.hash(size, fillColor, strokeColor, strokeWidth);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof FontStyle)
		{
			FontStyle o = (FontStyle)other;
			
			return o.getSize()== size &&
				   o.getFillColor().equals(fillColor) &&
				   o.getStrokeColor().equals(strokeColor) &&
				   o.getStrokeWidth() == strokeWidth;
		}
		else
			return false;
	}
	
	@Override
	public String toString() {
		return String.format("fill:rgb(%d,%d,%d);" +
							 "fill-opacity:%f;" +
							 "stroke:rgb(%d,%d,%d);" +
							 "stroke-opacity:%f;" +
							 "stroke-width:%f",
							 fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(),
							 (float)(fillColor.getAlpha()) / 255,
							 strokeColor.getRed(), strokeColor.getGreen(), strokeColor.getBlue(),
							 (float)(strokeColor.getAlpha()) / 255,
							 strokeWidth);
	}
}
