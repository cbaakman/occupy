package net.cbaakman.occupy.load;

import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;

public class AsciiLoadable extends FileDependentLoadable<String> {
		
	public AsciiLoadable(String path) {
		super(path);
	}

	@Override
	public String read(InputStream is) throws Exception {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "ascii");
		return writer.toString();
	}
}
