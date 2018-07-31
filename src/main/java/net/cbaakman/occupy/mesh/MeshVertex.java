package net.cbaakman.occupy.mesh;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MeshVertex {
	private String id;
	private Vector3f position = new Vector3f(),
					 normal = new Vector3f();
	
	public MeshVertex copy() {
		MeshVertex n = new MeshVertex();
		n.setId(id);
		n.setPosition(position.copy());
		n.setNormal(normal.copy());
		return n;
	}
}
