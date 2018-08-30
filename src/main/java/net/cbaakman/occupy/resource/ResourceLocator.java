package net.cbaakman.occupy.resource;

import org.apache.log4j.Logger;


public class ResourceLocator {
	
	static Logger logger = Logger.getLogger(ResourceLocator.class);
	
	private static String resourcePath = "/net/cbaakman/occupy";

	public static String getImagePath(String name) {
		return String.format("%s/image/%s.png", resourcePath, name);
	}
	
	public static String getFontPath(String name) {
		return String.format("%s/font/%s.svg", resourcePath, name);
	}
	
	public static String getMeshPath(String name) {
		return String.format("%s/mesh/%s.xml", resourcePath, name);
	}

	public static String getVertexShaderPath(String name) {
		return String.format("%s/shader/%s.vsh", resourcePath, name);
	}

	public static String getFragmentShaderPath(String name) {
		return String.format("%s/shader/%s.fsh", resourcePath, name);
	}
}
