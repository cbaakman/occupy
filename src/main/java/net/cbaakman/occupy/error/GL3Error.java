package net.cbaakman.occupy.error;

import com.jogamp.opengl.GL3;

public class GL3Error extends Exception {

	public GL3Error(int code) {
		super(getErrorString(code));
	}

	public static String getErrorString(int code) {

		switch(code) {
		case GL3.GL_NO_ERROR:
			return "no error";
		case GL3.GL_INVALID_ENUM:
			return "invalid enum";
		case GL3.GL_INVALID_VALUE:
			return "invalid value";
		case GL3.GL_INVALID_OPERATION:
			return "invalid operation";
		case GL3.GL_INVALID_FRAMEBUFFER_OPERATION:
			return "invalid framebuffer operation";
		case GL3.GL_OUT_OF_MEMORY:
			return "out of memory";
		case GL3.GL_STACK_UNDERFLOW:
			return "stack underflow";
		case GL3.GL_STACK_OVERFLOW:
			return "stack overflow";
		default:
			return String.format("unknown error code: 0x%x", code);
		}
	}

}
