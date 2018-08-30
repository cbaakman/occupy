package net.cbaakman.occupy.render.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.game.Entity;
import net.cbaakman.occupy.load.Loader;

public class EntityRenderRegistry implements Iterable<EntityRenderer<?>> {
	
	private Map<Class<? extends Entity>, EntityRenderer<?>> entityRenderers =
			new HashMap<Class<? extends Entity>, EntityRenderer<?>>();
	
	public <T extends Entity> void registerForEntity(Class<T> entityClass, EntityRenderer<T> renderer) {
		entityRenderers.put(entityClass, renderer);
	}
	
	public <T extends Entity> EntityRenderer<T> getForEntity(Class<T> entityClass) 
		throws KeyError {
		if (entityRenderers.containsKey(entityClass))
			return (EntityRenderer<T>)entityRenderers.get(entityClass);
		else
			throw new KeyError(String.format("no renderer for entity class %s", entityClass.getName()));
	}

	@Override
	public Iterator<EntityRenderer<?>> iterator() {
		return entityRenderers.values().iterator();
	}
}
