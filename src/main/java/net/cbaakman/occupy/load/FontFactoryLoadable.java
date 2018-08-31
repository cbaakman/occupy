package net.cbaakman.occupy.load;

import java.io.InputStream;
import net.cbaakman.occupy.font.FontFactory;

public class FontFactoryLoadable extends FileDependentLoadable<FontFactory> {

	public FontFactoryLoadable(String path) {
		super(path);
	}

	@Override
	public FontFactory read(InputStream is) throws Exception {
		return FontFactory.parse(is);
	}
}
