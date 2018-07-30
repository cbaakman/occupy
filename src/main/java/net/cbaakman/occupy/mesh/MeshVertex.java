package net.cbaakman.occupy.mesh;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MeshVertex {
	private Vector3f position = new Vector3f(),
					 normal = new Vector3f();
}
