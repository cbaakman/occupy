package net.cbaakman.occupy.font;

import java.awt.Color;

import lombok.Data;

@Data
public class SVGStyle {
	private Color fillColor = new Color(1.0f, 1.0f, 1.0f, 1.0f),
				  strokeColor = new Color(0.0f, 0.0f, 0.0f, 1.0f);
	private float strokeWidth = 1.0f;
	
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
