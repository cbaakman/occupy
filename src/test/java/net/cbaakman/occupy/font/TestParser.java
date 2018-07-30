package net.cbaakman.occupy.font;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import net.cbaakman.occupy.errors.FormatError;

public class TestParser {

	@Test
	public void testParseSVG() throws IOException,
									  ParserConfigurationException,
									  SAXException, FormatError {
		FontFactory fontFactory = FontFactory.parse(TestParser.class.getResourceAsStream("/font/Lumean.svg"));
	}
}
