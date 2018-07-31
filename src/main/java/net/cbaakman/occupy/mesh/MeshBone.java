package net.cbaakman.occupy.mesh;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MeshBone {
	private String id;
	private MeshBone parent = null;
	
	Vector3f headPosition = new Vector3f(),
			 tailPosition = new Vector3f();
	float weight = 1.0f;
	
	List<MeshVertex> vertices = new ArrayList<MeshVertex>();
}
