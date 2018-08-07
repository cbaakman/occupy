package net.cbaakman.occupy.math;

public class Triangle3f {
	private Vector3f[] points;
	
	public Triangle3f(Vector3f p0, Vector3f p1, Vector3f p2) {
		points = new Vector3f[] {p0, p1, p2};
	}

	public Triangle3f(Vector3f[] points) {
		this.points = new Vector3f[] {points[0], points[1], points[2]};
	}
}
