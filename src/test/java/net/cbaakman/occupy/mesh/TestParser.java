package net.cbaakman.occupy.mesh;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import net.cbaakman.occupy.errors.FormatError;

public class TestParser {

	@Test
	public void testParseXML() throws IOException,
									  ParserConfigurationException,
									  SAXException, FormatError {
		InputStream is = TestParser.class.getResourceAsStream("/net/cbaakman/occupy/mesh/infantry.xml");
		
		try {
			MeshFactory meshFactory = MeshFactory.parse(is);
		
			assertTrue(meshFactory.getVertices().size() > 0);
			assertTrue(meshFactory.getFaces().size() > 0);
			assertTrue(meshFactory.getSubsets().size() > 0);
			assertTrue(meshFactory.getBones().size() > 0);
			assertTrue(meshFactory.getAnimations().size() > 0);
			assertTrue(meshFactory.getAnimations().get("shoot").getLayers().get(0).getKeys().size() > 0);
		}
		finally {
			is.close();
		}
	}
}
