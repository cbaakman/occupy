package net.cbaakman.occupy.mesh;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import net.cbaakman.occupy.errors.ParseError;

public class TestParser {

	@Test
	public void testParseXML() throws IOException,
									  ParserConfigurationException,
									  SAXException, ParseError {
		MeshFactory meshFactory = MeshFactory.parse(TestParser.class.getResourceAsStream("/mesh/infantry.xml"));
		
		assertTrue(meshFactory.getVertices().size() > 0);
		assertTrue(meshFactory.getFaces().size() > 0);
		assertTrue(meshFactory.getSubsets().size() > 0);
		assertTrue(meshFactory.getBones().size() > 0);
		assertTrue(meshFactory.getAnimations().size() > 0);
		assertTrue(meshFactory.getAnimations().get("shoot").getLayers().get(0).getKeys().size() > 0);
	}
}
