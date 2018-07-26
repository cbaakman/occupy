package net.cbaakman.occupy.math;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;

import lombok.Data;

@Data
public class Vector2f implements Comparable<Vector2f> {

	private float x, y;
	
	@Override
	public int compareTo(Vector2f o) {
		if (this.getX() < o.getX())
			return -1;
		else if(this.getX() > o.getX())
			return 1;
		else {
			if (this.getY() < o.getY())
				return -1;
			else if (this.getY() > o.getY())
				return 1;
			else
				return 0;
		}
	}

	public Vector2f(float x, float y) {
		this.x = x;
		this.y = y;
	}
	public Vector2f(){
	}
}
