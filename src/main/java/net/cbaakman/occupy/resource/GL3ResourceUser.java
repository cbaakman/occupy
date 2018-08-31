package net.cbaakman.occupy.resource;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.load.Loader;

public interface GL3ResourceUser {

	/**
	 * @param loader the loader that will execute the load jobs
	 * @return should tell what to do when the loading is done and GL3 is available.
	 */
	GL3ResourceInitializer pipeResources(Loader loader) throws InitError, NotLoadedError;
}
