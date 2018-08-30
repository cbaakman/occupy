package net.cbaakman.occupy.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;

public class ResourceManager {
	
	private static Logger logger = Logger.getLogger(ResourceManager.class);
	
	private List<Resource<?>> resources = new ArrayList<Resource<?>>();

	private final Loader loader;
	
	public ResourceManager(Client client) {
		loader = new Loader(client.getConfig().getLoadConcurrency());
	}
	
	public Loader getLoader() {
		return loader;
	}
	
	public void submitAll(Loader loader) {
		for (Resource<?> resource : resources)
			loader.submit(resource);
	}
	
	public void disposeAll(GL3 gl3) {
		for (Resource<?> resource : resources)
			resource.dispose(gl3);
		resources.clear();
	}
	
	public <T> LoadRecord<T> submit(Resource<T> resource) {
		resources.add(resource);
		
		return loader.submit(resource);
	}
}
