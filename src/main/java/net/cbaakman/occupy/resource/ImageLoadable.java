package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import net.cbaakman.occupy.load.FileDependentLoadable;

public class ImageLoadable extends FileDependentLoadable<BufferedImage> {

	public ImageLoadable(String path) {
		super(path);
	}

	@Override
	public BufferedImage read(InputStream is) throws IOException {
		return ImageIO.read(is);
	}
}
