package net.cbaakman.occupy.render;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.math.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.jogamp.common.nio.Buffers;

import lombok.Data;

public class Vertex {
	
	static Logger logger = Logger.getLogger(Vertex.class);
	
	public static class Attrib {
		private int index;
		private Field field;
		
		public Attrib(int index, Field field) {
			this.index = index;
			this.field = field;
			
			this.field.setAccessible(true);
		}
		
		public int getIndex( ) {
			return index;
		}
		
		public Class<?> getTypeClass() {
			return field.getType();
		}
		
		public Object getValueFor(Vertex vertex) {
			try {
				return field.get(vertex);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				SeriousErrorHandler.handle(e);
				return null;
			}
		}
		
		public int getFloatCount() {
			if (getTypeClass().equals(Vector3f.class))
				return 3;
			else if (getTypeClass().equals(Vector2f.class))
				return 2;
			else
				return 0;
		}
	}

	public static int getFloatCount(Class<? extends Vertex> vertexClass) {
		
		int count = 0;
		for (Attrib attrib : orderAttribsByIndex(vertexClass)) {
			count += attrib.getFloatCount();
		}
		return count;
	}

	public static <T extends Vertex> FloatBuffer wrapInBuffer(List<T> vertices, Class<T> vertexClass) {
			
		
		FloatBuffer buffer = ByteBuffer.allocateDirect(Buffers.SIZEOF_FLOAT * vertices.size() * Vertex.getFloatCount(vertexClass))
							 .order(ByteOrder.nativeOrder()).asFloatBuffer();
		
		try{
			for (T vertex : vertices) {
				for (Attrib attrib : orderAttribsByIndex(vertexClass)) {
					if (attrib.getTypeClass().equals(Vector3f.class)) {
						
						Vector3f vec = (Vector3f)attrib.getValueFor(vertex);
						
						buffer.put(vec.getX()).put(vec.getY()).put(vec.getZ());
					}
					else if(attrib.getTypeClass().equals(Vector2f.class)) {
						
						Vector2f vec = (Vector2f)attrib.getValueFor(vertex);
						
						buffer.put(vec.getX()).put(vec.getY());
					}
				}
			}
		} catch (IllegalArgumentException e) {
			SeriousErrorHandler.handle(e);
		}
		
		buffer.rewind();
		
		return buffer;
	}
	
	public static <T extends Vertex> FloatBuffer wrapInBuffer(T[] vertices, Class<T> vertexClass) {
		
		return wrapInBuffer(Arrays.asList(vertices), vertexClass);
	}
	
	public static List<Attrib> orderAttribsByIndex(Class<? extends Vertex> vertexClass) {
		
		List<Attrib> attributes = new ArrayList<Attrib>();
		for (Field field : vertexClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(VertexAttrib.class)) {
				attributes.add(new Attrib(field.getAnnotation(VertexAttrib.class).index(), field));
			}
		}
		
		attributes.sort(
			new Comparator<Attrib>() {
				@Override
				public int compare(Attrib a1, Attrib a2) {
					return Integer.compare(a1.getIndex(), a2.getIndex());
				}
			}
		);
		
		return attributes;
	}
}
