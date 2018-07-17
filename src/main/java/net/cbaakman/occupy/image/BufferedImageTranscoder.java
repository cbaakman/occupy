package net.cbaakman.occupy.image;

import java.awt.image.BufferedImage;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;

public class BufferedImageTranscoder extends ImageTranscoder
{
	@Override
	public BufferedImage createImage(int w, int h)
	{
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	}

	@Override
	public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException
	{
		this.img = img;
	}

	public BufferedImage getBufferedImage()
	{
		return img;
	}
	private BufferedImage img = null;
}