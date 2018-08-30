package net.cbaakman.occupy.resource;

import java.io.InputStream;

import net.cbaakman.occupy.mesh.MeshFactory;

public class MeshFactoryResource extends FileDependentResource<MeshFactory> {

	public MeshFactoryResource(String path) {
		super(path);
	}

	@Override
	protected MeshFactory read(InputStream is) throws Exception {
		return MeshFactory.parse(is);
	}
}
