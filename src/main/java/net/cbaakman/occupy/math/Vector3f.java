package net.cbaakman.occupy.math;

import java.io.Serializable;

import com.jogamp.opengl.math.FloatUtil;

import lombok.Data;

@Data
public class Vector3f implements Comparable<Vector3f>, Serializable {
	
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
		this.x = 0.0f;
		this.y = 0.0f;
		this.z = 0.0f;
	}
	
	public void move(Vector3f v) {
		this.x += v.getX();
		this.y += v.getY();
		this.z += v.getZ();
	}

	public void move(float[] v) {
		this.x += v[0];
		this.y += v[1];
		this.z += v[2];
	}

	public Vector3f add(Vector3f o) {
		return new Vector3f(x + o.x, y + o.y, z + o.z);
	}

	public Vector3f subtract(Vector3f o) {
		return new Vector3f(x - o.x, y - o.y, z - o.z);
	}

	public Vector3f divideBy(float f) {
		return new Vector3f(x / f, y / f, z / f);
	}

	public Vector3f multiplyBy(float f) {
		return new Vector3f(x * f, y * f, z * f);
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
	
	@Override
	public String toString() {
		return String.format("(%f %f %f)", x, y, z);
	}

	public Vector3f unit() {
		return this.divideBy(this.length());
	}

	public float length() {
		return (float)Math.sqrt(x*x + y*y + z*z);
	}

	public float length2() {
		return x*x + y*y + z*z;
	}

	public float[] toList() {
		return new float[] {x, y, z};
	}
}
