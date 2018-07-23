package net.cbaakman.occupy.math;

import lombok.Data;

@Data
public class Quaternion4f implements Comparable<Quaternion4f> {
	
	float x, y, z, w;

	@Override
	public int compareTo(Quaternion4f o) {
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
				else {
					if (this.getW() < o.getW())
						return -1;
					else if (this.getW() > o.getW())
						return 1;
					else
						return 0;
				}
			}
		}
	}

	public Quaternion4f(float x, float y, float z, float w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}
	public Quaternion4f() {
	}
}
