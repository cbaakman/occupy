package net.cbaakman.occupy.map;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.xml.sax.SAXException;

import net.cbaakman.occupy.errors.FormatError;
import net.cbaakman.occupy.game.GameMap;

public class TestMap {

	@Test
	public void testUnwrap() throws NumberFormatException,
									IOException,
									ParserConfigurationException,
									SAXException, FormatError {
		InputStream is = TestMap.class.getResourceAsStream("/map/testmap.zip");
		try {
			GameMap map = GameMap.unwrap(is);
			assertTrue(map.getTextureMap().size() > 0);
		}
		finally {
			is.close();
		}
	}
}
