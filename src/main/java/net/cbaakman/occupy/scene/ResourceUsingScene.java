package net.cbaakman.occupy.scene;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.resource.GL3ResourceInitializer;
import net.cbaakman.occupy.resource.GL3ResourceManager;
import net.cbaakman.occupy.resource.GL3ResourceUser;

public abstract class ResourceUsingScene extends Scene implements GL3ResourceUser {
	
	static Logger logger = Logger.getLogger(ResourceUsingScene.class);

	private final GL3ResourceManager resourceManager;
	private final Client client;
	private boolean initDone = false;
	
	public ResourceUsingScene(Client client) {
		this.client = client;
		resourceManager = new GL3ResourceManager();
	}
	
	public final GL3ResourceManager getResourceManager() {
		return resourceManager;
	}
	
	@Override
	public final void init(GLAutoDrawable drawable) {
		if (initializer != null) {
			try {
				initializer.run(drawable.getGL().getGL3(), resourceManager);
				initDone = true;
			} catch (GLException | NotLoadedError e) {
				throw new SeriousError(e);
			} catch (InitError e) {
				client.getErrorQueue().pushError(e);
			}
		}
	}
	
	@Override
	public final void display(GLAutoDrawable drawable) {
		if (initDone) {
			render(drawable);
		}
	}

	protected abstract void render(GLAutoDrawable drawable);

	@Override
	public final void dispose(GLAutoDrawable drawable) {
		resourceManager.disposeAll(drawable.getGL().getGL3());
	}
	
	private GL3ResourceInitializer initializer;

	public void orderFrom(Loader loader) {
		try {
			initializer = pipeResources(loader);
		} catch (InitError e) {
			client.getErrorQueue().pushError(e);
		} catch (NotLoadedError e) {
			throw new SeriousError(e);
		}
	}
}
