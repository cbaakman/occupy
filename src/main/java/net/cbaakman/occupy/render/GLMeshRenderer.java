package net.cbaakman.occupy.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.texture.Texture;

import lombok.Data;
import net.cbaakman.occupy.annotations.VertexAttrib;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshFace;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.mesh.MeshFactory.Subset;
import net.cbaakman.occupy.mesh.MeshVertex;

public class GLMeshRenderer {
	
	Logger logger  = Logger.getLogger(GLMeshRenderer.class);
	
	public static final int POSITION_VERTEX_INDEX = 0,
							TEXCOORD_VERTEX_INDEX = 1,
							NORMAL_VERTEX_INDEX = 2;
	
	@Data
	public static class MeshRenderVertex extends Vertex {

		@VertexAttrib(index=POSITION_VERTEX_INDEX)
		Vector3f position = new Vector3f();
		
		@VertexAttrib(index=TEXCOORD_VERTEX_INDEX)
		Vector2f texCoord = new Vector2f();

		@VertexAttrib(index=NORMAL_VERTEX_INDEX)
		Vector3f normal = new Vector3f();

		public MeshRenderVertex(Vector3f position, Vector3f normal, Vector2f texCoord) {
			this.position = position;
			this.normal = normal;
			this.texCoord = texCoord;
		}
	}

	private MeshFactory meshFactory;
	private Map<String, Texture> textureMap = new HashMap<String, Texture>();
	private Map<String, VertexBuffer<MeshRenderVertex>> vboMap = new HashMap<String, VertexBuffer<MeshRenderVertex>>();
	
	public GLMeshRenderer(GL3 gl3, MeshFactory meshFactory) throws GL3Error {
		this.meshFactory = meshFactory;
		
		for (Entry<String, Subset> entry : meshFactory.getSubsets().entrySet()) {
			
			String subsetId = entry.getKey();
			Subset subset = entry.getValue();
			int vertexCount = 0;
			for (MeshFace face : subset.getFaces()) {
				
				if (face.getVertices().size() == 3)
					vertexCount += 3;
				else if (face.getVertices().size() == 4)
					vertexCount += 6;
				else
					throw new SeriousError(String.format("face has %d vertices", face.getVertices().size()));
			}
			
			vboMap.put(subsetId, VertexBuffer.create(gl3, MeshRenderVertex.class, vertexCount, GL3.GL_DYNAMIC_DRAW));
		}
	}
	
	public void cleanup(GL3 gl3) throws GL3Error {
		for (VertexBuffer<MeshRenderVertex> vbo : vboMap.values())
			vbo.cleanup(gl3);
	}
	
	
	public void setTexture(String subsetId, Texture texture) {
		if (meshFactory.getSubsets().containsKey(subsetId)) {
			textureMap.put(subsetId, texture);
		}
	}
	
	public void render(GL3 gl3) throws GL3Error {
		renderSubsetsWith(gl3, meshFactory.getVertices());
	}

	
	public void render(GL3 gl3, Map<String, BoneTransformation> transformations) throws GL3Error {
		
		Map<String, MeshVertex> vertices = meshFactory.getTransformedVertices(transformations);
		
		renderSubsetsWith(gl3, vertices);
	}
	
	private void renderSubsetsWith(GL3 gl3, Map<String, MeshVertex> verticesToUse) throws GL3Error {

		for (Entry<String, Subset> entry : meshFactory.getSubsets().entrySet()) {

			String subsetId = entry.getKey();
			Subset subset = entry.getValue();
			
			if (textureMap.containsKey(subsetId)) {
				textureMap.get(subsetId).enable(gl3);;
				textureMap.get(subsetId).bind(gl3);
				
				renderSubset(gl3, subsetId, subset, verticesToUse);

				textureMap.get(subsetId).disable(gl3);;
			}
		}
	}

	private void renderSubset(GL3 gl3, String subsetId, Subset subset, Map<String, MeshVertex> verticesToUse) throws GL3Error {

		VertexBuffer<MeshRenderVertex> vbo = vboMap.get(subsetId);
		
		FloatBuffer buffer = vbo.map(gl3, GL3.GL_MAP_WRITE_BIT, 0, vbo.getVertexCount());
		
		for (MeshFace face : subset.getFaces()) {
			MeshRenderVertex[] vertices = new MeshRenderVertex[face.getVertices().size()];
			for (int i = 0; i < face.getVertices().size(); i++) {
				Vector2f texCoord = face.getTexCoords().get(i);
				MeshVertex meshVertex = verticesToUse.get(face.getVertices().get(i).getId());
				
				vertices[i] = new MeshRenderVertex(meshVertex.getPosition(), meshVertex.getNormal(), texCoord);
			}
			
			if (!face.isSmooth()) {
				
				Vector3f averageNormal = new Vector3f(0.0f, 0.0f, 0.0f);
				for (int i = 0; i < vertices.length; i++)
					averageNormal = averageNormal.add(vertices[i].getNormal());
				averageNormal = averageNormal.divide(vertices.length);
				
				for (int i = 0; i < vertices.length; i++)
					vertices[i].setNormal(averageNormal);
			}
			
			if (vertices.length == 4) {
				Vertex.push(buffer, vertices[0], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[1], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[2], MeshRenderVertex.class);

				Vertex.push(buffer, vertices[0], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[2], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[3], MeshRenderVertex.class);
			}
			else if (vertices.length == 3) {
				Vertex.push(buffer, vertices[0], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[1], MeshRenderVertex.class);
				Vertex.push(buffer, vertices[2], MeshRenderVertex.class);
			}
			else
				throw new SeriousError(String.format("face has %d vertices", vertices.length));
		}
		
		vbo.unmap(gl3);
		
		vbo.draw(gl3, GL3.GL_TRIANGLES);
	}
}
