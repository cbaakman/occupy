package net.cbaakman.occupy.resource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;

import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.load.LoadRecord;

public class ShaderCodeResource implements Resource<ShaderCode> {

	private int shaderType;
	private LoadRecord<String> asyncSource;
	
	public ShaderCodeResource(int shaderType, LoadRecord<String> asyncSource) {
		this.shaderType = shaderType;
		this.asyncSource = asyncSource;
	}
	
	@Override
	public Set<LoadRecord<?>> getDependencies() {
		Set<LoadRecord<?>> set = new HashSet<LoadRecord<?>>();
		set.add(asyncSource);
		return set;
	}
	
	private ShaderCode shaderCode;
	
	@Override
	public ShaderCode init(GL3 gl3) throws InitError, NotReadyError {
		
		try {
			shaderCode = compile(gl3, shaderType, asyncSource.get());
			return shaderCode;
		} catch (ShaderCompileError e) {
			throw new InitError(e);
		}
	}
	
	public static ShaderCode compile(GL3 gl3, int shaderType, String source) throws ShaderCompileError {

		String[] s = new String[] {source};
		String[][] ss = new String[][] {s};
		
		ShaderCode code = new ShaderCode(shaderType, 1, ss);
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		if (!code.compile(gl3, new PrintStream(os))) {
			throw new ShaderCompileError(new String(os.toByteArray()));
		}
		
		return code;
	}

	@Override
	public void dispose(GL3 gl3) {
		shaderCode.destroy(gl3);
	}
	
	@Override
	public String toString() {
		return String.format("shadertype_%d:%s", shaderType, asyncSource.toString());
	}
}
