package net.cbaakman.occupy.render;

import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.SeriousError;

@Data
public class VertexBuffer<T extends Vertex> {
	
	static Logger logger = Logger.getLogger(VertexBuffer.class);
	
	private static VertexBuffer currentMapped = null;

	@Setter(AccessLevel.NONE)
	private int glHandle = 0;

	@Setter(AccessLevel.NONE)
	private int vertexCount;

	@Setter(AccessLevel.NONE)
	private Class<T> vertexClass;
	
	private VertexBuffer(int vertexCount, Class<T> vertexClass) {
		this.vertexCount = vertexCount;
		this.vertexClass = vertexClass;
	}
	
	public static <T extends Vertex> VertexBuffer<T> create(GL3 gl3, Class<T> vertexClass,
															int vertexCount, int glUsage) throws GL3Error {
		
		VertexBuffer<T> vbo = new VertexBuffer<T>(vertexCount, vertexClass);
		
		int [] handles = new int[1]; 
		gl3.glGenBuffers(1, handles, 0);
		vbo.glHandle = handles[0];

		GL3Error.check(gl3);
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, vbo.glHandle);

		GL3Error.check(gl3);
		
		gl3.glBufferData(GL3.GL_ARRAY_BUFFER,
						 Vertex.getFloatCount(vertexClass) * vertexCount * Buffers.SIZEOF_FLOAT,
						 null, glUsage);

		GL3Error.check(gl3);
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

		GL3Error.check(gl3);
		
		return vbo;
	}
	
	public void cleanup(GL3 gl3) throws GL3Error {
		gl3.glDeleteBuffers(1, new int[]{glHandle}, 0);

		GL3Error.check(gl3);
	}
	
	public void update(GL3 gl3, T[] vertices, long vertexOffset)
			throws GL3Error, IndexOutOfBoundsException {
		
		if (currentMapped != null)
			throw new SeriousError("a buffer is still mapped");
		
		if ((vertices.length + vertexOffset) > vertexCount)
			throw new IndexOutOfBoundsException();
				
		FloatBuffer floatBuffer = Vertex.wrapInBuffer(vertices, getVertexClass());
		
		update(gl3, floatBuffer, vertexOffset);
	}
	
	public void update(GL3 gl3, List<T> vertices, long vertexOffset)
			throws GL3Error, IndexOutOfBoundsException {
		
		if (currentMapped != null)
			throw new SeriousError("a buffer is still mapped");
		
		if ((vertices.size() + vertexOffset) > vertexCount)
			throw new IndexOutOfBoundsException();
				
		FloatBuffer floatBuffer = Vertex.wrapInBuffer(vertices, getVertexClass());
		
		update(gl3, floatBuffer, vertexOffset);
	}
	
	private void update(GL3 gl3, FloatBuffer floatBuffer, long vertexOffset) throws GL3Error {
		
		if (currentMapped != null)
			throw new SeriousError("a buffer is still mapped");
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, glHandle);
		GL3Error.check(gl3);
		
		gl3.glBufferSubData(GL3.GL_ARRAY_BUFFER,
							vertexOffset * Vertex.getFloatCount(vertexClass) * Buffers.SIZEOF_FLOAT,
							floatBuffer.capacity() * Buffers.SIZEOF_FLOAT, floatBuffer);
		GL3Error.check(gl3);
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		GL3Error.check(gl3);
	}
	
	/**
	 * Must not map one vbo before unmapping the other first.
	 */
	public FloatBuffer map(GL3 gl3, int access, int vertexOffset, int vertexLength) throws GL3Error {
		
		if (currentMapped != null)
			throw new SeriousError("a buffer is still mapped");
		
		int byteOffset = vertexOffset * Vertex.getFloatCount(vertexClass) * Buffers.SIZEOF_FLOAT,
			byteLength = vertexLength * Vertex.getFloatCount(vertexClass) * Buffers.SIZEOF_FLOAT;
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, glHandle);
		GL3Error.check(gl3);
		
		ByteBuffer buffer = gl3.glMapBufferRange(GL3.GL_ARRAY_BUFFER, byteOffset, byteLength, access);
		GL3Error.check(gl3);
		
		currentMapped = this;
		return buffer.asFloatBuffer();
	}
	
	public void unmap(GL3 gl3) throws GL3Error {
		gl3.glUnmapBuffer(GL3.GL_ARRAY_BUFFER);
		GL3Error.check(gl3);
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		GL3Error.check(gl3);
		
		currentMapped = null;
	}
	
	public void draw(GL3 gl3, int mode) throws GL3Error {
		
		if (currentMapped != null)
			throw new SeriousError("a buffer is still mapped");
		
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, glHandle);

		GL3Error.check(gl3);
        
        int stride = Vertex.getFloatCount(vertexClass) * Buffers.SIZEOF_FLOAT,
        	offset = 0;        
        
        
		for (Vertex.Attrib attrib : Vertex.orderAttribsByIndex(vertexClass)) {
			
			gl3.glEnableVertexAttribArray(attrib.getIndex());
			GL3Error.check(gl3);
	        
	        gl3.glVertexAttribPointer(attrib.getIndex(), attrib.getFloatCount(),
	        						  GL3.GL_FLOAT, false, stride, offset);
			GL3Error.check(gl3);
	        
	        offset += attrib.getFloatCount() * Buffers.SIZEOF_FLOAT;
		}
        
        gl3.glDrawArrays(mode, 0, vertexCount);
		GL3Error.check(gl3);

        for (Vertex.Attrib attrib : Vertex.orderAttribsByIndex(vertexClass)) {
			
			gl3.glDisableVertexAttribArray(attrib.getIndex());
			GL3Error.check(gl3);
        }
        
		gl3.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		GL3Error.check(gl3);
	}
}
