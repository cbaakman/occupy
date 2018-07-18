package net.cbaakman.occupy.font;

import lombok.Data;

@Data
public class BoundingBox {
	private float left, bottom, right, top;
	
	public BoundingBox(float left, float bottom, float right, float top) {
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.top = top;
	}

	public float getWidth() {
		return Math.abs(left - right);
	}

	public float getHeight() {
		return Math.abs(top - bottom);
	}
}
