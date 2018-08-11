package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.mesh.MeshFactory;

public class ResourceManager {

	private Client client;
	
	private Map<String, Future<BufferedImage>> images = new HashMap<String, Future<BufferedImage>>();
	private Map<String, Future<MeshFactory>> meshes = new HashMap<String, Future<MeshFactory>>();
	
	public ResourceManager(Client client) {
		this.client = client;
	}

	public MeshFactory getMesh(String name)
		throws KeyError, InterruptedException, ExecutionException {
		
		synchronized(meshes) {
			if (!meshes.containsKey(name))
				throw new KeyError(name);
			
			return meshes.get(name).get();
		}
	}

	public BufferedImage getImage(String name)
		throws KeyError, InterruptedException, ExecutionException {
		
		synchronized(images) {
			if (!images.containsKey(name))
				throw new KeyError(name);
			
			return images.get(name).get();
		}
	}

	public void addAllJobsTo(Loader loader) {
		
		addImageJobTo(loader, "infantry");
		addMeshJobTo(loader, "infantry");
	}

	private void addMeshJobTo(Loader loader, String name) {
		synchronized(meshes) {
			meshes.put(name, loader.add(Resource.getMeshJob(name)));
		}
	}

	private void addImageJobTo(Loader loader, String name) {
		synchronized(images) {
			images.put(name, loader.add(Resource.getImageJob(name)));
		}
	}
}
