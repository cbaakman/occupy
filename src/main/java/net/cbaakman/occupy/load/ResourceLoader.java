package net.cbaakman.occupy.load;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.cbaakman.occupy.font.FontFactory;

public class ResourceLoader {
	
	private List<Resource> resources = new ArrayList<Resource>();
	
	ExecutorService executor = Executors.newFixedThreadPool(10);
	
	public int countResources() {
		return resources.size();
	}
	public int countResourcesDone() {
		int count = 0;
		for (Resource resource : resources) {
			if (resource.isDone()) {
				count ++;
			}
		}
		return count;
	}

	public Future<FontFactory> addFont(final String resourcePath) {
		
		final Resource<FontFactory> resource = new Resource<FontFactory>() {

			@Override
			protected FontFactory load() throws Exception {
				return FontFactory.parse(ResourceLoader.class.getResourceAsStream(resourcePath));
			}
		};
		resources.add(resource);
		
		return executor.submit(new Callable<FontFactory>() {

			@Override
			public FontFactory call() throws Exception {
				return resource.load();
			}
		});
	}
}
