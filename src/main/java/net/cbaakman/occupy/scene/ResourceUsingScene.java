package net.cbaakman.occupy.scene;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.resource.ResourceManager;

public class ResourceUsingScene extends Scene {

	private final ResourceManager resourceManager;
	
	public ResourceUsingScene(Client client) {
		resourceManager = new ResourceManager(client);
	}
	
	public final ResourceManager getResourceManager() {
		return resourceManager;
	}
	
	@Override
	public final void dispose(GLAutoDrawable drawable) {
		resourceManager.disposeAll(drawable.getGL().getGL3());
	}
	
	@Override
	public final void init(GLAutoDrawable drawable) {
		// This method should not be used, since the loader handles resource initialization.
	}
}
