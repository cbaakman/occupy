package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.validation.constraints.NotNull;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.load.LoadRecord;

public class TextureResource implements GL3Resource<Texture> {
	
	@NotNull
	private LoadRecord<BufferedImage> asyncImage;
	
	private static int jobCount = 0;
	private int jobId;

	public TextureResource(LoadRecord<BufferedImage> asyncImage) {
		this.asyncImage = asyncImage;
		
		jobId = jobCount;
		jobCount++;
	}
	
	private Texture texture;
	
	@Override
	public Texture init(GL3 gl3, GL3ResourceManager resourceManager) throws NotLoadedError, InitError {
		
		BufferedImage image = asyncImage.get();
		
		TextureData textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), image, true);
		if (textureData.getMustFlipVertically()) {
			ImageUtil.flipImageVertically(image);
			textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), image, true);
		}

		texture = TextureIO.newTexture(gl3, textureData);

		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
		texture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);
		
		return texture;
	}
	
	@Override
	public String toString() {
		return String.format("texture_%d", jobId);
	}

	@Override
	public void dispose(GL3 gl3) {
		if (texture != null)
			texture.destroy(gl3);
	}
}
