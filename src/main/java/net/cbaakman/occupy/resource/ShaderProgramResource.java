package net.cbaakman.occupy.resource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.load.LoadRecord;

public class ShaderProgramResource implements Resource<ShaderProgram> {
	
	private LoadRecord<ShaderCode>[] asyncCodes;
	
	private static int jobCount = 0;
	private int jobId;

	public ShaderProgramResource(LoadRecord<ShaderCode> ... asyncCodes) {
		this.asyncCodes = asyncCodes;
		
		jobId = jobCount;
		jobCount++;
	}

	@Override
	public Set<LoadRecord<?>> getDependencies() {
		Set<LoadRecord<?>> set = new HashSet<LoadRecord<?>>();
		int i;
		for (i = 0; i < asyncCodes.length; i++)
			set.add(asyncCodes[i]);
		return set;
	}
	
	private ShaderProgram shaderProgram;

	@Override
	public ShaderProgram init(GL3 gl3) throws NotReadyError, InitError {
		
		ShaderCode[] codes = new ShaderCode[asyncCodes.length];
		int i;
		for (i = 0; i < asyncCodes.length; i++)
			codes[i] = asyncCodes[i].get();
		
		try {
			shaderProgram = link(gl3, codes);
			return shaderProgram;
		} catch (ShaderLinkError e) {
			throw new InitError(e);
		}
	}
	
	@Override
	public void dispose(GL3 gl3) {
		shaderProgram.destroy(gl3);
	}
	
	public static ShaderProgram link(GL3 gl3, String vertexSource, String fragmentSource)
			throws ShaderCompileError, ShaderLinkError {
		
		ShaderCode vertexCode = ShaderCodeResource.compile(gl3, GL3.GL_VERTEX_SHADER, vertexSource),
				   fragmentCode = ShaderCodeResource.compile(gl3, GL3.GL_FRAGMENT_SHADER, fragmentSource);
		
		return link(gl3, new ShaderCode[] {vertexCode, fragmentCode});
	}
	
	private static ShaderProgram link(GL3 gl3, ShaderCode[] codes) throws ShaderLinkError {
		
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
		return String.format("shader_program_%d", jobId);
	}
}
