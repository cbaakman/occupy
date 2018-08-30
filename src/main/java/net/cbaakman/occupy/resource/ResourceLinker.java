package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;

public class ResourceLinker {

	public static LoadRecord<ShaderProgram> addShaderJobs(ResourceManager resourceManager, String vertexShaderPath, String fragmentShaderPath) {

		LoadRecord<String> asyncVertexSource = resourceManager.submit(new AsciiResource(vertexShaderPath));
		LoadRecord<String> asyncFragmentSource = resourceManager.submit(new AsciiResource(fragmentShaderPath));
			
		LoadRecord<ShaderCode> asyncVertexShader = resourceManager.submit(new ShaderCodeResource(GL3.GL_VERTEX_SHADER, asyncVertexSource));
		LoadRecord<ShaderCode> asyncFragmentShader = resourceManager.submit(new ShaderCodeResource(GL3.GL_FRAGMENT_SHADER, asyncFragmentSource));
		
		return resourceManager.submit(new ShaderProgramResource(asyncVertexShader, asyncFragmentShader));
	}
	
	public static LoadRecord<Texture> submitTextureJobs(ResourceManager resourceManager, String imagePath) {
		
		LoadRecord<BufferedImage> asyncImage = resourceManager.submit(new ImageResource(imagePath));
		return resourceManager.submit(new TextureResource(asyncImage));
	}
	
	public static LoadRecord<Font> submitFontJobs(ResourceManager resourceManager, String fontPath, FontStyle style) {
		
		LoadRecord<FontFactory> asyncFontFactory = resourceManager.submit(new FontFactoryResource(fontPath));
		return resourceManager.submit(new FontResource(asyncFontFactory, style));
	}
}
