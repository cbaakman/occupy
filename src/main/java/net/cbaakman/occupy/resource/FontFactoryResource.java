package net.cbaakman.occupy.resource;

import java.io.InputStream;
import net.cbaakman.occupy.font.FontFactory;

public class FontFactoryResource extends FileDependentResource<FontFactory> {

	public FontFactoryResource(String path) {
		super(path);
	}

	@Override
	public FontFactory read(InputStream is) throws Exception {
		return FontFactory.parse(is);
	}
}
