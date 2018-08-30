package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class ImageResource extends FileDependentResource<BufferedImage> {

	public ImageResource(String path) {
		super(path);
	}

	@Override
	public BufferedImage read(InputStream is) throws IOException {
		return ImageIO.read(is);
	}
}
