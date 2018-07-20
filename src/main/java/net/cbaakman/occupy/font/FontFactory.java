package net.cbaakman.occupy.font;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.Data;
import net.cbaakman.occupy.errors.ParseError;
import net.cbaakman.occupy.image.BufferedImageTranscoder;

public class FontFactory {
	
	@Data
	private class GlyphFactory {
		private String name = "",
					   d = "";  // svg path description, may be empty for whitespace
		private char unicodeId;

		/**
		 * Negative means not used.
		 */
		private float horizOriginX = -1.0f,
					  horizOriginY = -1.0f,
					  horizAdvX = -1.0f;
	}
	
	static Logger logger = Logger.getLogger(FontFactory.class);
	
	/**
	 * all the font's glyphs should fit inside this
	 */
	private BoundingBox boundingBox = new BoundingBox(0.0f,0.0f,0.0f,0.0f);
	
	/**
	 * Can be overridden by the glyph's value.
	 */
	float horizOriginX = 0.0f,
		  horizOriginY = 0.0f,
	      horizAdvX = 0.0f;
	int unitsPerEM;
	
	private Map<Character, GlyphFactory> glyphFactories = new HashMap<Character, GlyphFactory>();
	private Map<Character, Map<Character, Float>> hKernTable = new HashMap<Character, Map<Character, Float>>();

	public static FontFactory parse(InputStream is) throws IOException,
													ParserConfigurationException,
													SAXException, ParseError,
													NumberFormatException {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.parse(is);
		
		Element svg = document.getDocumentElement();
		if (!svg.getTagName().equalsIgnoreCase("svg"))
			throw new ParseError("root element must be svg");
		
		Element defs = findDirectChildElement(svg, "defs");
		if (defs == null)
			throw new ParseError("svg element must contain a defs element");
		
		// Pick the 1st font tag in the defs:
		Element font = findDirectChildElement(defs, "font");
		if (font == null)
			throw new ParseError("defs element must contain at least one font element");

		FontFactory fontFactory = new FontFactory();
		
		if (font.hasAttribute("horiz-adv-x"))
			fontFactory.horizAdvX = Float.parseFloat(font.getAttribute("horiz-adv-x"));

		if (font.hasAttribute("horiz-origin-x"))
			fontFactory.horizOriginX = Float.parseFloat(font.getAttribute("horiz-origin-x"));

		if (font.hasAttribute("horiz-origin-y"))
			fontFactory.horizOriginY = Float.parseFloat(font.getAttribute("horiz-origin-y"));

		Element fontFace = findDirectChildElement(font, "font-face");
		if (fontFace == null)
			throw new ParseError("font element must contain a font-face element");
		parseFontFace(fontFace, fontFactory);
		
		NodeList nodeList = font.getChildNodes();
		int i;
		
		// Must read the glyphs before the hkern entries, to resolve references.
		for (i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)childNode;
				if (childElement.getTagName().equalsIgnoreCase("glyph")) {
					parseGlyph(childElement, fontFactory);
				}
			}
		}
		for (i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)childNode;
				if (childElement.getTagName().equalsIgnoreCase("hkern")) {
					parseHKern(childElement, fontFactory);
				}
			}
		}
		
		return fontFactory;
	}
	
	private static void parseHKern(Element hkernElement, FontFactory fontFactory) throws ParseError,
																						 UnsupportedEncodingException {
		if (!hkernElement.hasAttribute("k"))
			throw new ParseError("k-attribute missing in hkern tag");
		float k = Float.parseFloat(hkernElement.getAttribute("k"));
		
		if (!hkernElement.hasAttribute("g1") && !hkernElement.hasAttribute("u1"))
			throw new ParseError("group 1 is missing in hkern tag");
		
		List<Character> u1 = new ArrayList<Character>();
		if (hkernElement.hasAttribute("u1"))
			for (String u : Arrays.asList(hkernElement.getAttribute("u1").split(","))) {
				u1.add(parseUnicode(u));
			}
		if (hkernElement.hasAttribute("g1"))
			for (String name : Arrays.asList(hkernElement.getAttribute("g1").split(","))) {
				char unicodeId = 0;
				for (GlyphFactory glyphFactory : fontFactory.glyphFactories.values()) {
					if (glyphFactory.name.equals(name)) {
						unicodeId = glyphFactory.unicodeId;
						break;
					}
				}
				if (unicodeId == 0)
					throw new ParseError(String.format("hkern entry g1 refers to unknown glyph: %s", name));
				u1.add(unicodeId);
			}

		if (!hkernElement.hasAttribute("g2") && !hkernElement.hasAttribute("u2"))
			throw new ParseError("group 2 is missing in hkern tag");

		List<Character> u2 = new ArrayList<Character>();
		if (hkernElement.hasAttribute("u2"))
			for (String u : Arrays.asList(hkernElement.getAttribute("u2").split(","))) {
				u2.add(parseUnicode(u));
			}
		if (hkernElement.hasAttribute("g2"))
			for (String name : Arrays.asList(hkernElement.getAttribute("g2").split(","))) {
				char unicodeId = 0;
				for (GlyphFactory glyphFactory : fontFactory.glyphFactories.values()) {
					if (glyphFactory.name.equals(name)) {
						unicodeId = glyphFactory.unicodeId;
						break;
					}
				}
				if (unicodeId == 0)
					throw new ParseError(String.format("hkern entry g2 refers to unknown glyph: %s", name));
				u2.add(unicodeId);
			}
		
		// Fill in the values in the table:
		for (char left : u1) {
			if (!fontFactory.hKernTable.containsKey(left))
				fontFactory.hKernTable.put(left, new HashMap<Character, Float>());

			for (char right : u2) {
				
				fontFactory.hKernTable.get(left).put(right, k);
			}
		}
	}

	private static void parseGlyph(Element glyphElement, FontFactory fontFactory) throws ParseError,
																						 NumberFormatException,
																						 UnsupportedEncodingException {
		if (!glyphElement.hasAttribute("unicode"))
			return;
		
		FontFactory.GlyphFactory glyphFactory = fontFactory.new GlyphFactory();
		glyphFactory.unicodeId = parseUnicode(glyphElement.getAttribute("unicode"));
		
		if (glyphElement.hasAttribute("glyph-name")) {
			glyphFactory.name = glyphElement.getAttribute("glyph-name");
		}

		if (glyphElement.hasAttribute("d")) {
			glyphFactory.d = glyphElement.getAttribute("d");
		}

		if (glyphElement.hasAttribute("horiz-adv-x"))
			glyphFactory.horizAdvX = Float.parseFloat(glyphElement.getAttribute("horiz-adv-x"));

		if (glyphElement.hasAttribute("horiz-origin-x"))
			glyphFactory.horizOriginX = Float.parseFloat(glyphElement.getAttribute("horiz-origin-x"));

		if (glyphElement.hasAttribute("horiz-origin-y"))
			glyphFactory.horizOriginY = Float.parseFloat(glyphElement.getAttribute("horiz-origin-y"));
		
		fontFactory.glyphFactories.put(glyphFactory.unicodeId, glyphFactory);
	}

	private static char parseUnicode(String value) throws UnsupportedEncodingException {
		if (value.length() == 1)  // ascii
			return value.charAt(0);
		
		else if (value.startsWith("&#") && value.endsWith(";")) {
			
			if (value.charAt(2) == 'x') {  // hex
				int i = Integer.parseInt(value.substring(3, value.length() - 1), 16);
				return (char)i;
			}
			else {  // decimal
				int i = Integer.parseInt(value.substring(2, value.length() - 1), 10);
				return (char)i;
			}
		}
		else {  // treat as utf-8
			String utf8 = new String(value.getBytes(), "UTF-8");
			return utf8.charAt(0);
		}
	}

	private static void parseFontFace(Element fontFaceElement, FontFactory fontFactory) throws ParseError,
																							   NumberFormatException {
		if (!fontFaceElement.hasAttribute("units-per-em"))
			throw new ParseError("font-face is missing required attribute units-per-em");
		fontFactory.unitsPerEM = Integer.parseInt(fontFaceElement.getAttribute("units-per-em"));
		
		if (!fontFaceElement.hasAttribute("bbox"))
			throw new ParseError("font-face is missing required attribute bbox");
		String[] bbs = fontFaceElement.getAttribute("bbox").split("\\s+");
		fontFactory.boundingBox.setLeft(Float.parseFloat(bbs[0]));
		fontFactory.boundingBox.setBottom(Float.parseFloat(bbs[1]));
		fontFactory.boundingBox.setRight(Float.parseFloat(bbs[2]));
		fontFactory.boundingBox.setTop(Float.parseFloat(bbs[3]));
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
	
	public Font generateFont(float size) throws TranscoderException {
		float multiply = size / unitsPerEM;
		
		Font font = new Font();
		font.setSize(size);
		font.setBoundingBox(new BoundingBox(multiply * boundingBox.getLeft(),
										    multiply * boundingBox.getBottom(),
										    multiply * boundingBox.getRight(),
										    multiply * boundingBox.getTop()));
		font.setHorizOriginX(multiply * horizOriginX);
		font.setHorizOriginY(multiply * horizOriginY);
		font.setHorizAdvX(multiply * horizAdvX);
		
		// Fill in the hkern table.
		for (Character c1 : hKernTable.keySet()) {
			if (!font.getHKernTable().containsKey(c1))
				font.getHKernTable().put(c1, new HashMap<Character, Float>());
			
			for (Character c2 : hKernTable.get(c1).keySet()) {
				font.getHKernTable().get(c1).put(c2, multiply * hKernTable.get(c1).get(c2));
			}
		}
		
		for (Character c : glyphFactories.keySet()) {
			GlyphFactory glyphFactory = glyphFactories.get(c);
			
			Glyph glyph = new Glyph();
			if (!glyphFactory.getD().isEmpty())
				glyph.setImage(generateGlyphImage(boundingBox, multiply, glyphFactory.getD()));
			
			glyph.setHorizAdvX(multiply * glyphFactory.getHorizAdvX());
			glyph.setHorizOriginX(multiply * glyphFactory.getHorizOriginX());
			glyph.setHorizOriginY(multiply * glyphFactory.getHorizOriginY());
			glyph.setName(glyphFactory.getName());
			glyph.setUnicodeId(glyphFactory.getUnicodeId());
			
			font.getGlyphs().put(c, glyph);
		}
		
		return font;
	}
	
	static private BufferedImage generateGlyphImage(BoundingBox bbox, float multiply, String d) throws TranscoderException {

		BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();
		
		float origX = bbox.getLeft() * multiply,
			  origY = bbox.getBottom() * multiply,
			  imageWidth = bbox.getWidth() * multiply,
			  imageHeight = bbox.getHeight() * multiply;
		
		String svg = String.format("<?xml version=\"1.0\" standalone=\"no\"?>" +
								   "<svg width=\"%f\" height=\"%f\">" +
								   "<g transform=\"translate(%f %f) scale(%f)\">" +
								   "<path d=\"%s\" fill=\"white\"/></g></svg>",
								   imageWidth, imageHeight,
	   							   -origX, -origY,
	   							   multiply, d);

	    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float)Math.ceil(imageWidth));
	    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float)Math.ceil(imageHeight));
	    
	    TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svg.getBytes()));
	    imageTranscoder.transcode(input, null);

	    BufferedImage image = imageTranscoder.getBufferedImage();
	    
	    return image;
	}
}
