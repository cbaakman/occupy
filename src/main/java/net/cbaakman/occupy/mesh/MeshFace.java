package net.cbaakman.occupy.mesh;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import net.cbaakman.occupy.math.Vector2f;

@Data
public class MeshFace {
	private List<MeshVertex> vertices = new ArrayList<MeshVertex>();
	private List<Vector2f> texCoords = new ArrayList<Vector2f>();
	private boolean smooth;
	
	/**
	 * triangle
	 */
	public MeshFace(MeshVertex v0, MeshVertex v1, MeshVertex v2,
					Vector2f tex0, Vector2f tex1, Vector2f tex2) {
		vertices.add(v0);
		vertices.add(v1);
		vertices.add(v2);
		texCoords.add(tex0);
		texCoords.add(tex1);
		texCoords.add(tex2);
	}
	
	/**
	 * quad
	 */
	public MeshFace(MeshVertex v0, MeshVertex v1, MeshVertex v2, MeshVertex v3,
			Vector2f tex0, Vector2f tex1, Vector2f tex2, Vector2f tex3) {
		vertices.add(v0);
		vertices.add(v1);
		vertices.add(v2);
		vertices.add(v3);
		texCoords.add(tex0);
		texCoords.add(tex1);
		texCoords.add(tex2);
		texCoords.add(tex3);
	}
}
