package net.cbaakman.occupy.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.Data;
import net.cbaakman.occupy.math.Quaternion4f;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MeshBoneAnimation {
	@Data
	public class Key {
		Quaternion4f rotation = new Quaternion4f();
		Vector3f location = new Vector3f();
	}
	@Data
	public class Layer {
		private MeshBone bone = null;
		private SortedMap<Integer, Key> keys = new TreeMap<Integer, Key>();
	}
	
	private List<Layer> layers = new ArrayList<Layer>();
	private int length;
}