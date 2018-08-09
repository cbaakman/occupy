package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;

import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;
import net.cbaakman.occupy.load.LoadJob;
import net.cbaakman.occupy.mesh.MeshFactory;

public class Resource {

	public static LoadJob<BufferedImage> getImageJob(String name) {
		
		final String path = String.format("/net/cbaakman/occupy/image/%s.png", name);
		
		return new LoadJob<BufferedImage>() {

			@Override
			public BufferedImage call() throws Exception {
				InputStream inputStream = Resource.class.getResourceAsStream(path);
				if (inputStream == null)
					throw new FileNotFoundException(path);
				
				try {
					return ImageIO.read(inputStream);
				} finally {
					inputStream.close();
				}
			}
		};
	}
	
	public static LoadJob<FontFactory> getFontFactoryJob(String name) {
		
		final String path = String.format("/net/cbaakman/occupy/font/%s.svg", name);
		
		return new LoadJob<FontFactory>() {

			@Override
			public FontFactory call() throws Exception {
				InputStream inputStream = Resource.class.getResourceAsStream(path);
				if (inputStream == null)
					throw new FileNotFoundException(path);

				try {
					return FontFactory.parse(inputStream);
				} finally {
					inputStream.close();
				}
			}
		};
	}
	
	public static LoadJob<Font> getFontJob(Future<FontFactory> ftFontFactory, FontStyle style) {
		
		return new LoadJob<Font>() {
			
			@Override
			public boolean isReady() {
				return ftFontFactory.isDone();
			}

			@Override
			public Font call() throws Exception {
				return ftFontFactory.get().generateFont(style);
			}
		};
	}
	
	public static LoadJob<MeshFactory> getMeshJob(String name) {
		
		final String path = String.format("/net/cbaakman/occupy/mesh/%s.xml", name);
		
		return new LoadJob<MeshFactory>() {

			@Override
			public MeshFactory call() throws Exception {
				
				InputStream inputStream = Resource.class.getResourceAsStream("/net/cbaakman/occupy/mesh/infantry.xml");
				if (inputStream == null)
					throw new FileNotFoundException(path);
				
				try {
					return MeshFactory.parse(inputStream);
				} finally {
					inputStream.close();
				}
			}
		};
	}
}
