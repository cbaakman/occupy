package net.cbaakman.occupy.errors;

import com.jogamp.opengl.GL3;

public class GL3Error extends Exception {
	
	public GL3Error(int glErrorCode) {
		super(getGL3ErrorString(glErrorCode));
	}

	public static String getGL3ErrorString(int glErrorCode) {

		switch(glErrorCode) {
		case GL3.GL_NO_ERROR:
			return "gl no error";
		case GL3.GL_INVALID_ENUM:
			return "gl invalid enum";
		case GL3.GL_INVALID_VALUE:
			return "gl invalid value";
		case GL3.GL_INVALID_OPERATION:
			return "gl invalid operation";
		case GL3.GL_INVALID_FRAMEBUFFER_OPERATION:
			return "gl invalid framebuffer operation";
		case GL3.GL_OUT_OF_MEMORY:
			return "gl out of memory";
		case GL3.GL_STACK_UNDERFLOW:
			return "gl stack underflow";
		case GL3.GL_STACK_OVERFLOW:
			return "gl stack overflow";
		default:
			return String.format("gl unknown error code: 0x%x", glErrorCode);
		}
	}
	
	public static void check(GL3 gl3) throws GL3Error {
		int glError = gl3.glGetError();
		if (glError != GL3.GL_NO_ERROR)
			throw new GL3Error(glError);
	}
	
	public static void throwMe(GL3 gl3) throws GL3Error {
		throw new GL3Error(gl3.glGetError());
	}
}
