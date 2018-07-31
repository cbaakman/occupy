package net.cbaakman.occupy.math;

import com.jogamp.opengl.math.FloatUtil;

import lombok.Data;

@Data
public class Vector3f implements Comparable<Vector3f> {
	
	private float x, y, z;
	
	@Override
	public int compareTo(Vector3f o) {
		if (this.getX() < o.getX())
			return -1;
		else if(this.getX() > o.getX())
			return 1;
		else {
			if (this.getY() < o.getY())
				return -1;
			else if (this.getY() > o.getY())
				return 1;
			else {
				if (this.getZ() < o.getZ())
					return -1;
				else if (this.getZ() > o.getZ())
					return 1;
				else
					return 0;
			}
		}
	}

	public Vector3f(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public Vector3f() {
	}

	public Vector3f add(Vector3f o) {
		return new Vector3f(x + o.x, y + o.y, z + o.z);
	}

	public Vector3f divide(float d) {
		return new Vector3f(x / d, y / d, z / d);
	}

	public Vector3f copy() {
		return new Vector3f(x, y, z);
	}

	public Vector3f getTransformedBy(float[] matrix) {
		
		float[] v = new float[] {x, y, z, 1.0f},
				r = new float[4];
		
		FloatUtil.multMatrixVec(matrix, v, r);
		
		return new Vector3f(r[0], r[1], r[2]);
	}
}
