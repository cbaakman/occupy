package net.cbaakman.occupy.math;

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
}
