package net.cbaakman.occupy.load;

import java.util.Set;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;

public interface Loadable<T> {

	T load() throws InitError, NotLoadedError;

	
	/**
	 * Tells the loader that it should wait for these to complete,
	 * before initializing this resource.
	 */
	Set<LoadRecord<?>> getDependencies(); 
}
