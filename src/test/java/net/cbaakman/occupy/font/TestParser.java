package net.cbaakman.occupy.font;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import net.cbaakman.occupy.errors.FormatError;

public class TestParser {

	@Test
	public void testParseSVG() throws IOException,
									  ParserConfigurationException,
									  SAXException, FormatError {
		InputStream is = TestParser.class.getResourceAsStream("/net/cbaakman/occupy/font/lumean.svg");
		try {
			FontFactory fontFactory = FontFactory.parse(is);
		}
		finally {
			is.close();
		}
	}
}
