package net.cbaakman.occupy.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.load.LoadRecord;

public abstract class WaitResource implements Resource<Void> {
	
	Set<LoadRecord<?>> dependencies = new HashSet<LoadRecord<?>>();
	
	public WaitResource(LoadRecord<?> ... dependencies) {
		for (LoadRecord<?> dependency : dependencies) {
			this.dependencies.add(dependency);
		}
	}

	public WaitResource(Collection<LoadRecord<?>> dependencies) {
		for (LoadRecord<?> dependency : dependencies) {
			this.dependencies.add(dependency);
		}
	}

	@Override
	public Set<LoadRecord<?>> getDependencies() {
		return dependencies;
	}
	
	protected abstract void run(GL3 gl3) throws NotReadyError, InitError;
	
	public Void init(GL3 gl3) throws NotReadyError, InitError {
		run(gl3);
		return null;
	}

	@Override
	public final void dispose(GL3 gl3) {
	}
}
