package net.cbaakman.occupy.resource;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.render.Vertex;
import net.cbaakman.occupy.render.VertexBuffer;

public class VertexBufferResource<T extends Vertex> implements GL3Resource<VertexBuffer<T>> {
	
	static Logger logger = Logger.getLogger(VertexBufferResource.class);

	private Class<T> vertexClass;
	private int vertexCount;
	private int glUsage;
	
	private static int jobCount = 0;
	private int jobId;

	public VertexBufferResource(Class<T> vertexClass, int vertexCount, int glUsage) {
		this.vertexClass = vertexClass;
		this.vertexCount = vertexCount;
		this.glUsage = glUsage;

		jobId = jobCount;
		jobCount++;
	}
	
	private VertexBuffer<T> vbo;
	
	@Override
	public VertexBuffer<T> init(GL3 gl3, GL3ResourceManager resourceManager) throws InitError {
		try {
			vbo = VertexBuffer.create(gl3, vertexClass, vertexCount, glUsage);
			return vbo;
		} catch (GL3Error e) {
			throw new InitError(e);
		}
	}

	@Override
	public void dispose(GL3 gl3) {
		try {
			if (vbo != null)
				vbo.dispose(gl3);
		} catch (GL3Error e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	@Override
	public String toString() {
		return String.format("vbo_%d", jobId);
	}
}
