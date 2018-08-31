package net.cbaakman.occupy.load;

import java.io.InputStream;

import net.cbaakman.occupy.mesh.MeshFactory;

public class MeshFactoryLoadable extends FileDependentLoadable<MeshFactory> {

	public MeshFactoryLoadable(String path) {
		super(path);
	}

	@Override
	protected MeshFactory read(InputStream is) throws Exception {
		return MeshFactory.parse(is);
	}
}
