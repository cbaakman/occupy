package net.cbaakman.occupy.font;

import java.awt.image.BufferedImage;
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
		private float horiz_origin_x = -1.0f,
					  horiz_origin_y = -1.0f,
					  horiz_adv_x = -1.0f;
	}
	
	static Logger logger = Logger.getLogger(FontFactory.class);
	
	/**
	 * all the font's glyphs should fit inside this
	 */
	private BoundingBox boundingBox = new BoundingBox();
	
	/**
	 * Can be overridden by the glyph's value.
	 */
	float horiz_origin_x = 0.0f,
		  horiz_origin_y = 0.0f,
	      horiz_adv_x = 0.0f;
	int units_per_em;
	
	private Map<Character, GlyphFactory> glyphFactories = new HashMap<Character, GlyphFactory>();
	private Map<Character, Map<Character, Float>> hKernTable = new HashMap<Character, Map<Character, Float>>();

	static FontFactory parse(InputStream is) throws IOException,
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
			fontFactory.horiz_adv_x = Float.parseFloat(font.getAttribute("horiz-adv-x"));

		if (font.hasAttribute("horiz-origin-x"))
			fontFactory.horiz_origin_x = Float.parseFloat(font.getAttribute("horiz-origin-x"));

		if (font.hasAttribute("horiz-origin-y"))
			fontFactory.horiz_origin_y = Float.parseFloat(font.getAttribute("horiz-origin-y"));

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
		
		logger.debug(String.format("found hkern %.1f for %s and %s", k, u1.toString(), u2.toString()));
		
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
			glyphFactory.horiz_adv_x = Float.parseFloat(glyphElement.getAttribute("horiz-adv-x"));

		if (glyphElement.hasAttribute("horiz-origin-x"))
			glyphFactory.horiz_origin_x = Float.parseFloat(glyphElement.getAttribute("horiz-origin-x"));

		if (glyphElement.hasAttribute("horiz-origin-y"))
			glyphFactory.horiz_origin_y = Float.parseFloat(glyphElement.getAttribute("horiz-origin-y"));
	
		logger.debug(String.format("parsed glyph for %c", glyphFactory.unicodeId));
		
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
		fontFactory.units_per_em = Integer.parseInt(fontFaceElement.getAttribute("units-per-em"));
		
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
	
	static BufferedImage genGlyphImage(String d) throws TranscoderException {

		BufferedImageTranscoder imageTranscoder = new BufferedImageTranscoder();

	    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 100);
	    imageTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, 100);

	    TranscoderInput input = new TranscoderInput(d);
	    imageTranscoder.transcode(input, null);

	    BufferedImage image = imageTranscoder.getBufferedImage();
	    
	    return image;
	}
}
