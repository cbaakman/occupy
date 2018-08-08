package net.cbaakman.occupy.authenticate;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import junit.framework.TestCase;

public class TestAuthenticate extends TestCase {
	
	private File file;
	
	@Override
	public void setUp() throws IOException {
		file = File.createTempFile("authenticate", "test");
	}
	
	@Override
	public void tearDown() {
		file.delete();
	}

	@Test
	public void test() throws IOException {
		Credentials credentials = new Credentials("hello", "world");
		
		Authenticator authenticator = new Authenticator(file);
		authenticator.add(credentials);
		
		assertTrue(authenticator.authenticate(credentials));
	}
}
