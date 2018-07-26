package net.cbaakman.occupy.render;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;

import net.cbaakman.occupy.error.GL3Error;
import net.cbaakman.occupy.error.ShaderCompileError;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;

public class Shader {
	
	static Logger logger = Logger.getLogger(Shader.class);
	
	public static int createProgram(GL3 gl3, int[] shaders) throws InitError {
		
		int program = gl3.glCreateProgram();
		if (program == 0)
			throw new InitError(new GL3Error(gl3.glGetError()));
		
		for (int shader : shaders) {
			gl3.glAttachShader(program, shader);
		}
		
		gl3.glLinkProgram(program);
		
		IntBuffer result = IntBuffer.allocate(1);
		gl3.glGetProgramiv(program, GL3.GL_LINK_STATUS, result);
		if (result.get(0) != GL3.GL_TRUE) {

			IntBuffer logLength = IntBuffer.allocate(1);
			gl3.glGetShaderiv(program, GL3.GL_INFO_LOG_LENGTH, logLength);
			
			ByteBuffer logBytes = ByteBuffer.allocate(logLength.get(0));
			gl3.glGetShaderInfoLog(program, logLength.get(0), logLength, logBytes);
			
			gl3.glDeleteProgram(program);
			try {
				throw new InitError(new String(logBytes.array(), "ascii"));
			} catch (UnsupportedEncodingException e) {
				SeriousErrorHandler.handle(e);
			}
		}
		return program;
	}
	
	public static int compile(GL3 gl3, int type, String src) throws ShaderCompileError {
		int shader = gl3.glCreateShader(type);

		gl3.glShaderSource(shader, 1, new String[] {src}, null);
		gl3.glCompileShader(shader);
		IntBuffer result = IntBuffer.allocate(1);
		gl3.glGetShaderiv(shader, GL3.GL_COMPILE_STATUS, result);
		if (result.get(0) != GL3.GL_TRUE) {

			IntBuffer logLength = IntBuffer.allocate(1);
			gl3.glGetShaderiv(shader, GL3.GL_INFO_LOG_LENGTH, logLength);
			
			ByteBuffer logBytes = ByteBuffer.allocate(logLength.get(0));
			gl3.glGetShaderInfoLog(shader, logLength.get(0), logLength, logBytes);
			
			gl3.glDeleteShader(shader);
			try {
				throw new ShaderCompileError(new String(logBytes.array(), "ascii"));
			} catch (UnsupportedEncodingException e) {
				SeriousErrorHandler.handle(e);
			}
		}
		return shader;
	}
}
