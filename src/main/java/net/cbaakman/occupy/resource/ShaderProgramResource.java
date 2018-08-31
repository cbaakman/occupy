package net.cbaakman.occupy.resource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.util.StringHashable;

public class ShaderProgramResource extends StringHashable implements GL3Resource<ShaderProgram> {
		
	private static int jobCount = 0;
	private int jobId;
	
	private Set<ShaderCodeResource> codeResources = new HashSet<ShaderCodeResource>();

	public ShaderProgramResource(LoadRecord<String> asyncVertexSource,
								 LoadRecord<String> asyncFragmentSource) {
		codeResources.add(new ShaderCodeResource(GL3.GL_VERTEX_SHADER, asyncVertexSource));
		codeResources.add(new ShaderCodeResource(GL3.GL_FRAGMENT_SHADER, asyncFragmentSource));
		
		jobId = jobCount;
		jobCount++;
	}
	
	private ShaderProgram shaderProgram;
	
	private List<ShaderCode> codes = new ArrayList<ShaderCode>();

	public ShaderProgramResource(ShaderCode ... codes) {
		for (ShaderCode code : codes)
			this.codes.add(code);
	}

	@Override
	public ShaderProgram init(GL3 gl3, GL3ResourceManager resourceManager) throws NotLoadedError, InitError {		
		try {
			shaderProgram = link(gl3, codes);
			return shaderProgram;
		} catch (ShaderLinkError e) {
			throw new InitError(e);
		}
	}

	@Override
	public void dispose(GL3 gl3) {
		if (shaderProgram != null)
			shaderProgram.destroy(gl3);
	}
	
	public static ShaderProgram link(GL3 gl3, String vertexSource, String fragmentSource)
			throws ShaderCompileError, ShaderLinkError {
		
		ShaderCode vertexCode = ShaderCodeResource.compile(gl3, GL3.GL_VERTEX_SHADER, vertexSource),
				   fragmentCode = ShaderCodeResource.compile(gl3, GL3.GL_FRAGMENT_SHADER, fragmentSource);
		
		return link(gl3, vertexCode, fragmentCode);
	}

	public static ShaderProgram link(GL3 gl3, ShaderCode ... codes) throws ShaderLinkError {

		ShaderProgram program = new ShaderProgram();
		
		for (ShaderCode code : codes) {
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			if (!program.add(gl3, code, new PrintStream(os)) ) {
				throw new ShaderLinkError(new String(os.toByteArray()));
			}
		}

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if (!program.link(gl3, new PrintStream(os))) {
			throw new ShaderLinkError(new String(os.toByteArray()));
		}
		
		return program;
	}
	
	private static ShaderProgram link(GL3 gl3, List<ShaderCode> codes) throws ShaderLinkError {
		
		ShaderProgram program = new ShaderProgram();
		
		for (ShaderCode code : codes) {
			
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			if (!program.add(gl3, code, new PrintStream(os)) ) {
				throw new ShaderLinkError(new String(os.toByteArray()));
			}
		}

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if (!program.link(gl3, new PrintStream(os))) {
			throw new ShaderLinkError(new String(os.toByteArray()));
		}
		
		return program;
	}

	@Override
	public String toString() {		
		return String.format("shader_program:%s", codeResources.toString());
	}
}
