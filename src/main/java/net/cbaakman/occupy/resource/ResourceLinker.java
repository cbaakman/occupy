package net.cbaakman.occupy.resource;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.FontStyle;
import net.cbaakman.occupy.load.AsciiLoadable;
import net.cbaakman.occupy.load.FontFactoryLoadable;
import net.cbaakman.occupy.load.FontLoadable;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.load.MeshFactoryLoadable;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.render.GL3MeshRenderer;
import net.cbaakman.occupy.render.GL3Sprite2DRenderer;
import net.cbaakman.occupy.render.GL3TextRenderer;
import net.cbaakman.occupy.render.VertexBuffer;
import net.cbaakman.occupy.render.GL3Sprite2DRenderer.SpriteVertex;

public class ResourceLinker {
	
	static Logger logger = Logger.getLogger(ResourceLinker.class);

	public static GL3Resource<ShaderProgram> forShaderProgram(Loader loader,
												 String vertexShaderPath,
												 String fragmentShaderPath) throws InitError, NotLoadedError {

		LoadRecord<String> asyncVertexSource = loader.submit(new AsciiLoadable(vertexShaderPath));
		LoadRecord<String> asyncFragmentSource = loader.submit(new AsciiLoadable(fragmentShaderPath));
		
		return new GL3Resource<ShaderProgram>() {
			
			private ShaderProgram program;

			@Override
			public ShaderProgram init(GL3 gl3, GL3ResourceManager resourceManager) throws InitError, NotLoadedError {

				final ShaderCode codeVertex = resourceManager.submit(gl3,
									new ShaderCodeResource(GL3.GL_VERTEX_SHADER, asyncVertexSource)),
						   		 codeFragment = resourceManager.submit(gl3,
						   			new ShaderCodeResource(GL3.GL_FRAGMENT_SHADER, asyncFragmentSource));
				
				try {
					program = ShaderProgramResource.link(gl3, codeVertex, codeFragment);
				} catch (ShaderLinkError e) {
					throw new InitError(e);
				}
				return program;
			}

			@Override
			public void dispose(GL3 gl3) {
				if (program != null)
					program.destroy(gl3);
			}
		};
	}
	
	public static GL3Resource<Texture> forTexture(Loader loader, String imagePath)
			throws InitError, NotLoadedError {
		LoadRecord<BufferedImage> asyncImage = loader.submit(new ImageLoadable(imagePath));
		return new TextureResource(asyncImage);
	}
	
	public static GL3Resource<GL3TextRenderer> forTextRenderer(Loader loader,
												  String fontPath, FontStyle style) throws InitError, NotLoadedError {
		
		LoadRecord<FontFactory> asyncFontFactory = loader.submit(new FontFactoryLoadable(fontPath));
		LoadRecord<Font> asyncFont = loader.submit(new FontLoadable(asyncFontFactory, style));
		
		final GL3Resource<ShaderProgram> programResource = forShaderProgram(loader,
									ResourceLocator.getVertexShaderPath("sprite2d"),
									ResourceLocator.getFragmentShaderPath("sprite2d"));
		
		final VertexBufferResource<GL3TextRenderer.GlyphVertex> vboResource =
				new VertexBufferResource<GL3TextRenderer.GlyphVertex>(GL3TextRenderer.GlyphVertex.class,
																	  4,
																	  GL3.GL_DYNAMIC_DRAW);
		
		return new GL3Resource<GL3TextRenderer>() {
			
			private GL3TextRenderer renderer;
			
			@Override
			public GL3TextRenderer init(GL3 gl3, GL3ResourceManager resourceManager) throws NotLoadedError, InitError {
				renderer = new GL3TextRenderer(gl3, asyncFont.get(),
											   resourceManager.submit(gl3, programResource),
											   resourceManager.submit(gl3, vboResource));
				return renderer;
			}

			@Override
			public void dispose(GL3 gl3) {
				try {
					if (renderer != null)
						renderer.dispose(gl3);
				} catch (GL3Error e) {
					logger.error(e.getMessage(), e);
				}
			}
		};
	}
	
	public static GL3Resource<GL3MeshRenderer> forMesh(Loader loader, String meshFactoryPath)
			throws InitError, NotLoadedError {
		
		final LoadRecord<MeshFactory> asyncMesh = loader.submit(new MeshFactoryLoadable(meshFactoryPath));

		return new GL3Resource<GL3MeshRenderer>() {
			
			private GL3MeshRenderer renderer;

			@Override
			public GL3MeshRenderer init(GL3 gl3, GL3ResourceManager resourceManager) throws InitError, NotLoadedError {
				try {
					renderer = new GL3MeshRenderer(gl3, asyncMesh.get());
					return renderer;
				} catch (GL3Error e) {
					throw new InitError(e);
				}
			}

			@Override
			public void dispose(GL3 gl3) {
				if (renderer != null)
					renderer.dispose(gl3);
			}
		};
	}
	
	public static GL3Resource<GL3Sprite2DRenderer> forSprite2D(Loader loader)
			throws InitError, NotLoadedError {
		
		GL3Resource<ShaderProgram> programResource = forShaderProgram(loader,
										ResourceLocator.getVertexShaderPath("sprite2d"),
										ResourceLocator.getFragmentShaderPath("sprite2d"));
		
		VertexBufferResource<SpriteVertex> vboResource = new VertexBufferResource<GL3Sprite2DRenderer.SpriteVertex>(
						GL3Sprite2DRenderer.SpriteVertex.class, 4, GL3.GL_DYNAMIC_DRAW);
				
		return new GL3Resource<GL3Sprite2DRenderer>() {

			@Override
			public GL3Sprite2DRenderer init(GL3 gl3, GL3ResourceManager resourceManager)
					throws InitError, NotLoadedError {
				
				ShaderProgram program = resourceManager.submit(gl3, programResource);
				VertexBuffer<SpriteVertex> vbo = resourceManager.submit(gl3, vboResource);

				return new GL3Sprite2DRenderer(program, vbo);
			}

			@Override
			public void dispose(GL3 gl3) {				
			}
		};
	}
}
