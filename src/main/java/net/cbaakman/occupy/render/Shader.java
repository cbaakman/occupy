package net.cbaakman.occupy.render;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLException;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;

public class Shader {
	
	static Logger logger = Logger.getLogger(Shader.class);
	
	public static int createProgram(GL3 gl3, int[] shaders)
			throws GL3Error, ShaderLinkError {
		
		int program = gl3.glCreateProgram();
		if (program == 0)
			GL3Error.throwMe(gl3);
		
		int error;
		for (int shader : shaders) {
			gl3.glAttachShader(program, shader);
			error = gl3.glGetError();
			if (error != GL3.GL_NO_ERROR) {
				gl3.glDeleteProgram(program);
				throw new GL3Error(error);
			}
		}
		
		gl3.glLinkProgram(program);
		error = gl3.glGetError();
		if (error != GL3.GL_NO_ERROR) {
			gl3.glDeleteProgram(program);
			throw new GL3Error(error);
		}
		
		int[] result = new int[1];
		gl3.glGetProgramiv(program, GL3.GL_LINK_STATUS, result, 0);
		error = gl3.glGetError();
		if (error != GL3.GL_NO_ERROR) {
			gl3.glDeleteProgram(program);
			throw new GL3Error(error);
		}
		
		if (result[0] != GL3.GL_TRUE) {
			int[] logLength = new int[1];
			gl3.glGetShaderiv(program, GL3.GL_INFO_LOG_LENGTH, logLength, 0);
			error = gl3.glGetError();
			if (error != GL3.GL_NO_ERROR) {
				gl3.glDeleteProgram(program);
				throw new GL3Error(error);
			}
			
			byte[] logBytes = new byte[logLength[0]];
			gl3.glGetShaderInfoLog(program, logLength[0], logLength, 0, logBytes, 0);
			error = gl3.glGetError();
			if (error != GL3.GL_NO_ERROR) {
				gl3.glDeleteProgram(program);
				throw new GL3Error(error);
			}
			
			gl3.glDeleteProgram(program);
			throw new ShaderLinkError(new String(logBytes));
		}
		return program;
	}
	
	public static int createProgram(GL3 gl3, String vertexSrc, String fragmentSrc)
			throws ShaderCompileError, ShaderLinkError, GL3Error {

		int vertexShader = 0,
			fragmentShader = 0;
		try {			
			vertexShader = Shader.compile(gl3, GL3.GL_VERTEX_SHADER, vertexSrc);
			fragmentShader = Shader.compile(gl3, GL3.GL_FRAGMENT_SHADER, fragmentSrc);
			
			return Shader.createProgram(gl3, new int[] {vertexShader, fragmentShader});
			
		} catch (ShaderCompileError | ShaderLinkError | GL3Error  e) {
			
			// Doesn't matter if these fail, we already had an error!
			gl3.glDeleteShader(vertexShader);
			gl3.glDeleteShader(fragmentShader);
			
			throw e;
		}
	}

	public static void deleteProgram(GL3 gl3, int program) throws GL3Error {
		gl3.glDeleteProgram(program);
		GL3Error.check(gl3);
	}
	
	public static void deleteShader(GL3 gl3, int shader) throws GL3Error {
		gl3.glDeleteShader(shader);
		GL3Error.check(gl3);
	}
	
	public static int compile(GL3 gl3, int type, String src)
			throws ShaderCompileError, GL3Error {
		
		int shader = gl3.glCreateShader(type);
		if (shader == 0)
			GL3Error.throwMe(gl3);

		gl3.glShaderSource(shader, 1, new String[] {src}, null);
		int error = gl3.glGetError();
		if (error != GL3.GL_NO_ERROR) {
			gl3.glDeleteShader(shader);
			throw new GL3Error(error);
		}
		
		gl3.glCompileShader(shader);
		error = gl3.glGetError();
		if (error != GL3.GL_NO_ERROR) {
			gl3.glDeleteShader(shader);
			throw new GL3Error(error);
		}
		
		int[] result = new int[1];
		gl3.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, result, 0);
		error = gl3.glGetError();
		if (error != GL3.GL_NO_ERROR) {
			gl3.glDeleteShader(shader);
			throw new GL3Error(error);
		}
		
		if (result[0] != GL3.GL_TRUE) {
			int[] logLength = new int[1];
			gl3.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, logLength, 0);
			error = gl3.glGetError();
			if (error != GL3.GL_NO_ERROR) {
				gl3.glDeleteShader(shader);
				throw new GL3Error(error);
			}
			
			byte[] logBytes = new byte[logLength[0]];
			gl3.glGetShaderInfoLog(shader, logLength[0], logLength, 0, logBytes, 0);
			error = gl3.glGetError();
			if (error != GL3.GL_NO_ERROR) {
				gl3.glDeleteShader(shader);
				throw new GL3Error(error);
			}
			
			gl3.glDeleteShader(shader);
			throw new ShaderCompileError(new String(logBytes));
		}
		return shader;
	}
}
