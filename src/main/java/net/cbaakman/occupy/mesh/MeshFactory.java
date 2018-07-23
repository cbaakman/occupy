package net.cbaakman.occupy.mesh;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.Data;
import net.cbaakman.occupy.errors.ParseError;
import net.cbaakman.occupy.math.Quaternion4f;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class MeshFactory {
	
	@Data
	public class Subset {
		private List<MeshFace> faces = new ArrayList<MeshFace>();
	}
	
	Map<String, MeshBone> bones = new HashMap<String, MeshBone>();
	Map<String, MeshVertex> vertices = new HashMap<String, MeshVertex>();
	Map<String, MeshFace> faces = new HashMap<String, MeshFace>();
	Map<String, Subset> subsets = new HashMap<String, Subset>();
	Map<String, MeshBoneAnimation> animations = new HashMap<String, MeshBoneAnimation>();

	public static MeshFactory parse(InputStream is)
			throws ParserConfigurationException, SAXException,
				   IOException, ParseError, NumberFormatException {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(is);
		is.close();

		Element meshElement = document.getDocumentElement();
		if (!meshElement.getTagName().equalsIgnoreCase("mesh"))
			throw new ParseError("root element must be mesh");
		
		MeshFactory meshFactory = new MeshFactory();
		
		// Parse the vertices first:
		Element vertices = findDirectChildElement(meshElement, "vertices");
		if (vertices == null)
			throw new ParseError("no vertices element found in mesh");
		
		for (Element vertexElement : iterElements(vertices, "vertex")) {
			parseVertex(meshFactory, vertexElement);
		}
		
		// Parse the faces AFTER the vertices:
		Element faces = findDirectChildElement(meshElement, "faces");
		if (faces == null)
			throw new ParseError("no faces element found in mesh");

		for (Element quadElement : iterElements(faces, "quad")) {
			parseQuad(meshFactory, quadElement);
		}
		for (Element triangleElement : iterElements(faces, "triangle")) {
			parseTriangle(meshFactory, triangleElement);
		}
		
		// Parse the subsets AFTER the faces:
		Element subsets = findDirectChildElement(meshElement, "subsets");
		if (subsets == null)
			throw new ParseError("no subsets element found in mesh");

		for (Element subsetElement : iterElements(subsets, "subset")) {
			parseSubset(meshFactory, subsetElement);
		}
		
		// Armatures are optional for meshes.
		Element armature = findDirectChildElement(meshElement, "armature");
		if (armature != null) {
			Element bones = findDirectChildElement(armature, "bones");
			if (bones == null)
				throw new ParseError("armature without bones");
			
			parseBones(meshFactory, bones);
			
			// Animations are optional for armatures, but must be parsed AFTER the bones.
			Element animations = findDirectChildElement(armature, "animations");
			if (animations != null) {
				for (Element animationElement : iterElements(animations, "animation")) {
					parseAnimation(meshFactory, animationElement);
				}
			}
		}
		
		return  meshFactory;
	}

	private static void parseAnimation(MeshFactory meshFactory, Element animationElement)
			throws ParseError, NumberFormatException {
		if (!animationElement.hasAttribute("name"))
			throw new ParseError("animation without name");
		
		String animationId = animationElement.getAttribute("name");
		
		if (!animationElement.hasAttribute("length"))
			throw new ParseError("animation without length");
		
		MeshBoneAnimation animation = new MeshBoneAnimation();
		animation.setLength(Integer.parseInt(animationElement.getAttribute("length")));
		
		for (Element layerElement : iterElements(animationElement, "layer")) {
			
			if (!layerElement.hasAttribute("bone_id"))
				throw new ParseError("bone_id");
			String boneId = layerElement.getAttribute("bone_id");
			MeshBone bone = meshFactory.getBones().get(boneId);
			
			MeshBoneAnimation.Layer layer = animation.new Layer();
			layer.setBone(bone);
			
			for (Element keyElement : iterElements(layerElement, "key")) {
				
				if (!keyElement.hasAttribute("frame"))
					throw new ParseError("key element without frame number");
				int frame = Integer.parseInt(keyElement.getAttribute("frame"));
				
				MeshBoneAnimation.Key key = animation.new Key();

				key.setLocation(parseVector3f(keyElement));
				key.setRotation(parseRotation(keyElement));
				
				layer.getKeys().put(frame, key);
			}
			animation.getLayers().add(layer);
		}
		
		meshFactory.getAnimations().put(animationId, animation);
	}

	private static void parseBones(MeshFactory meshFactory, Element bonesElement)
			throws ParseError, NumberFormatException {
		
		Map<MeshBone, String> parentLookup = new HashMap<MeshBone, String>();
		for (Element boneElement : iterElements(bonesElement, "bone")) {
			
			if (!boneElement.hasAttribute("id"))
				throw new ParseError("missing id on bone element");
			String boneId = boneElement.getAttribute("id");
			
			MeshBone bone = new MeshBone();
			
			if (boneElement.hasAttribute("parent_id")) {
				parentLookup.put(bone, boneElement.getAttribute("parent_id"));
			}
			
			bone.setHeadPosition(parseVector3f(boneElement));
			bone.setTailPosition(parseTail(boneElement));
			
			Element vertices = findDirectChildElement(boneElement, "vertices");
			if (vertices != null) {
				for (Element vertexElement : iterElements(vertices, "vertex")) {
					if (!vertexElement.hasAttribute("id"))
						throw new ParseError("bone vertex has no id");
					
					String vertexId = vertexElement.getAttribute("id");
					bone.getVertices().add(meshFactory.getVertices().get(vertexId));
				}
			}
			
			meshFactory.getBones().put(boneId, bone);
		}
		
		// At this point all bones should be parsed, connect them!
		for (Entry<MeshBone, String> parentLookupEntry : parentLookup.entrySet()) {
			MeshBone bone = parentLookupEntry.getKey();
			String parentId = parentLookupEntry.getValue();
			
			bone.setParent(meshFactory.getBones().get(parentId));
		}
	}

	private static void parseSubset(MeshFactory meshFactory, Element subsetElement) throws ParseError {
		String subsetId;
		if (subsetElement.hasAttribute("name"))
			subsetId = subsetElement.getAttribute("name");
		else if (subsetElement.hasAttribute("id"))
			subsetId = subsetElement.getAttribute("id");
		else
			throw new ParseError("no id on subset");
		
		Subset subset = meshFactory.new Subset();
		
		Element faces = findDirectChildElement(subsetElement, "faces");
		if (faces == null)
			throw new ParseError("no faces tag in subset");

		for (Element quadElement : iterElements(faces, "quad")) {
			if (!quadElement.hasAttribute("id"))
				throw new ParseError("id missing in subset quad");
			String faceId = quadElement.getAttribute("id");
			subset.getFaces().add(meshFactory.getFaces().get(faceId));
		}
		for (Element triangleElement : iterElements(faces, "triangle")) {
			if (!triangleElement.hasAttribute("id"))
				throw new ParseError("id missing in subset triangle");
			String faceId = triangleElement.getAttribute("id");
			subset.getFaces().add(meshFactory.getFaces().get(faceId));
		}
		
		meshFactory.subsets.put(subsetId, subset);
	}

	private static void parseTriangle(MeshFactory meshFactory, Element triangleElement)
			throws ParseError, NumberFormatException {
		if (!triangleElement.hasAttribute("id"))
			throw new ParseError("id missing on triangle");
		
		String faceId = triangleElement.getAttribute("id");
		
		int countCorners = 0;
		float[][] texels = new float[3][2];
		String[] vertex_ids = new String[3];
		for (Element cornerElement : iterElements(triangleElement, "corner")) {
			
			if (countCorners < 3) {
				if (!cornerElement.hasAttribute("tex_u"))
					throw new ParseError("tex_u missing on corner");
				texels[countCorners][0] = Float.parseFloat(cornerElement.getAttribute("tex_u"));
	
				if (!cornerElement.hasAttribute("tex_v"))
					throw new ParseError("tex_v missing on corner");
				texels[countCorners][1] = Float.parseFloat(cornerElement.getAttribute("tex_v"));
				
				if (!cornerElement.hasAttribute("vertex_id"))
					throw new ParseError("vertex id missing on corner");
				vertex_ids[countCorners] = cornerElement.getAttribute("vertex_id");
			}
			
			countCorners++;
		}
		if(countCorners != 3)
			throw new ParseError(String.format("triangle element with %d corners", countCorners));
		
		MeshFace face = new MeshFace(meshFactory.getVertices().get(vertex_ids[0]),
									 meshFactory.getVertices().get(vertex_ids[1]),
									 meshFactory.getVertices().get(vertex_ids[2]),
									 new Vector2f(texels[0][0], texels[0][1]),
									 new Vector2f(texels[1][0], texels[1][1]),
									 new Vector2f(texels[2][0], texels[2][1]));
		
		face.setSmooth(triangleElement.hasAttribute("smooth") &&
					   !triangleElement.getAttributeNode("smooth").equals("0") &&
					   !triangleElement.getAttributeNode("smooth").equals("false"));
		
		meshFactory.faces.put(faceId, face);
	}


private static void parseQuad(MeshFactory meshFactory, Element quadElement)
			throws ParseError, NumberFormatException {
		if (!quadElement.hasAttribute("id"))
			throw new ParseError("id missing on quad");
		
		String faceId = quadElement.getAttribute("id");
		
		int countCorners = 0;
		float[][] texels = new float[4][2];
		String[] vertex_ids = new String[4];
		for (Element cornerElement : iterElements(quadElement, "corner")) {

			if (countCorners < 4) {
				if (!cornerElement.hasAttribute("tex_u"))
					throw new ParseError("tex_u missing on corner");
				texels[countCorners][0] =Float.parseFloat(cornerElement.getAttribute("tex_u"));
	
				if (!cornerElement.hasAttribute("tex_v"))
					throw new ParseError("tex_v missing on corner");
				texels[countCorners][1] = Float.parseFloat(cornerElement.getAttribute("tex_v"));
				
				if (!cornerElement.hasAttribute("vertex_id"))
					throw new ParseError("vertex id missing on corner");
				vertex_ids[countCorners] = cornerElement.getAttribute("vertex_id");
			}
			
			countCorners++;
		}
		if(countCorners != 4)
			throw new ParseError(String.format("quad element with %d corners", countCorners));
		
		MeshFace face = new MeshFace(meshFactory.getVertices().get(vertex_ids[0]),
									 meshFactory.getVertices().get(vertex_ids[1]),
									 meshFactory.getVertices().get(vertex_ids[2]),
									 meshFactory.getVertices().get(vertex_ids[3]),
									 new Vector2f(texels[0][0], texels[0][1]),
									 new Vector2f(texels[1][0], texels[1][1]),
									 new Vector2f(texels[2][0], texels[2][1]),
									 new Vector2f(texels[3][0], texels[3][1]));
		
		face.setSmooth(quadElement.hasAttribute("smooth") &&
					   !quadElement.getAttributeNode("smooth").equals("0") &&
					   !quadElement.getAttributeNode("smooth").equals("false"));
		
		meshFactory.faces.put(faceId, face);
	}

	private static void parseVertex(MeshFactory meshFactory, Element vertexElement)
			throws ParseError, NumberFormatException {
		if (!vertexElement.hasAttribute("id"))
			throw new ParseError("id missing on vertex");
		
		String vertexId = vertexElement.getAttribute("id");
		
		MeshVertex vertex = new MeshVertex();
		
		Element positionElement = findDirectChildElement(vertexElement, "pos");
		if (positionElement == null)
			throw new ParseError("no position in vertex");
		
		vertex.setPosition(parseVector3f(positionElement));
		
		Element normalElement = findDirectChildElement(vertexElement, "norm");
		if (positionElement == null)
			throw new ParseError("no position in vertex");
		
		vertex.setNormal(parseVector3f(normalElement));
		
		meshFactory.vertices.put(vertexId, vertex);
	}

	private static Vector3f parseVector3f(Element element) throws ParseError, NumberFormatException {

		if (!element.hasAttribute("x") || !element.hasAttribute("y") || !element.hasAttribute("z"))
			throw new ParseError(String.format("missing x, y or z on %s element", element.getTagName()));
		
		return new Vector3f(Float.parseFloat(element.getAttribute("x")),
							Float.parseFloat(element.getAttribute("y")),
							Float.parseFloat(element.getAttribute("z")));
	}

	private static Vector3f parseTail(Element element) throws ParseError, NumberFormatException {
		
		if (!element.hasAttribute("tail_x") || !element.hasAttribute("tail_y") || !element.hasAttribute("tail_z"))
			throw new ParseError(String.format("missing tail_x, tail_y or tail_z on %s element", element.getTagName()));
		
		return new Vector3f(Float.parseFloat(element.getAttribute("tail_x")),
							Float.parseFloat(element.getAttribute("tail_y")),
							Float.parseFloat(element.getAttribute("tail_z")));
	}
	private static Quaternion4f parseRotation(Element element) throws ParseError, NumberFormatException {

		if (!element.hasAttribute("rot_x") || !element.hasAttribute("rot_y") ||
				!element.hasAttribute("rot_z") || !element.hasAttribute("rot_w"))
			throw new ParseError(String.format("missing rot_x, rot_y, rot_z or rot_w on %s element", element.getTagName()));
		
		return new Quaternion4f(Float.parseFloat(element.getAttribute("rot_x")),
								Float.parseFloat(element.getAttribute("rot_y")),
								Float.parseFloat(element.getAttribute("rot_z")),
								Float.parseFloat(element.getAttribute("rot_w")));
	}

	private static Iterable<Element> iterElements(Element parentElement, String tagName) {
		
		final NodeList nodeList = parentElement.getElementsByTagName(tagName);
		
		return new Iterable<Element> () {

			@Override
			public Iterator<Element> iterator() {
				return new Iterator<Element> () {
					
					private int i = 0;

					@Override
					public boolean hasNext() {
						return i < nodeList.getLength();
					}

					@Override
					public Element next() {
						Element element = (Element)nodeList.item(i);
						i++;
						return element;
					}
				};
			}
		};
	}

	static Element findDirectChildElement(Element element, String tagName) {
		NodeList nodeList = element.getChildNodes();
		int i;
		for (i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)childNode;
				if (childElement.getTagName().equalsIgnoreCase(tagName)) {
					return childElement;
				}
			}
		}
		return null;
	}
}
