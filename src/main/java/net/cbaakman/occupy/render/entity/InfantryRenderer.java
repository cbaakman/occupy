package net.cbaakman.occupy.render.entity;

import java.nio.FloatBuffer;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.texture.Texture;

import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.errors.NotLoadedError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.load.MeshFactoryLoadable;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshBoneAnimationState;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.render.GL3MeshRenderer;
import net.cbaakman.occupy.resource.ResourceLinker;
import net.cbaakman.occupy.resource.ResourceLocator;
import net.cbaakman.occupy.resource.GL3Resource;
import net.cbaakman.occupy.resource.GL3ResourceInitializer;
import net.cbaakman.occupy.resource.GL3ResourceManager;

public class InfantryRenderer extends EntityRenderer<Infantry> {
	
	static Logger logger = Logger.getLogger(InfantryRenderer.class);

	private ShaderProgram shaderProgram;
	private GL3MeshRenderer glMeshRenderer;
	private MeshBoneAnimationState animationState;
	private Texture texture;
	
	@Override
	public GL3ResourceInitializer pipeResources(Loader loader) throws InitError, NotLoadedError {
		
		final GL3Resource<ShaderProgram> shaderProgramResource = ResourceLinker.forShaderProgram(loader,
			ResourceLocator.getVertexShaderPath("infantry"),
			ResourceLocator.getFragmentShaderPath("infantry")
		);
		
		final GL3Resource<Texture> textureResource = ResourceLinker.forTexture(loader,
				ResourceLocator.getImagePath("infantry"));
		
		final GL3Resource<GL3MeshRenderer> meshResource = ResourceLinker.forMesh(loader,
				ResourceLocator.getMeshPath("infantry"));
		
		return new GL3ResourceInitializer() {
			@Override
			public void run(GL3 gl3, GL3ResourceManager resourceManager) throws InitError, NotLoadedError {
				shaderProgram = resourceManager.submit(gl3, shaderProgramResource);
				glMeshRenderer = resourceManager.submit(gl3, meshResource);
				texture = resourceManager.submit(gl3, textureResource);
								
				try {
					glMeshRenderer.setTexture("infantry", texture);
					animationState = new MeshBoneAnimationState(glMeshRenderer.getMeshFactory().getAnimation("walk"), 100);
				} catch (KeyError e) {
					throw new SeriousError(e);
				}
			}
		};
	}

	public void renderOpaque(GL3 gl3, float[] projectionMatrix,
									  float[] modelViewMatrix, Infantry infantry)
			throws GL3Error {
        
		shaderProgram.useProgram(gl3, true);

        gl3.glBindAttribLocation(shaderProgram.program(), GL3MeshRenderer.POSITION_VERTEX_INDEX, "position");
		GL3Error.check(gl3);

        gl3.glBindAttribLocation(shaderProgram.program(), GL3MeshRenderer.TEXCOORD_VERTEX_INDEX, "texCoord");
		GL3Error.check(gl3);

        gl3.glBindAttribLocation(shaderProgram.program(), GL3MeshRenderer.NORMAL_VERTEX_INDEX, "normal");
		GL3Error.check(gl3);
		
		int projectionMatrixLocation = gl3.glGetUniformLocation(shaderProgram.program(), "projectionMatrix");
		if (projectionMatrixLocation == -1)
			GL3Error.throwMe(gl3);	
		
		gl3.glUniformMatrix4fv(projectionMatrixLocation, 1, false, FloatBuffer.wrap(projectionMatrix));
		GL3Error.check(gl3);

		int modelviewMatrixLocation = gl3.glGetUniformLocation(shaderProgram.program(), "modelviewMatrix");
		if (modelviewMatrixLocation == -1)
			GL3Error.throwMe(gl3);	
		
		float[] resultMatrix = new float[16],
				translationMatrix = new float[16],
				rotationMatrix = new float[16];
		
		Vector3f position = infantry.getPosition();
		FloatUtil.makeTranslation(translationMatrix, true, position.getX(), position.getY(), position.getZ());
		infantry.getOrientation().toMatrix(rotationMatrix, 0);
		
		FloatUtil.multMatrix(modelViewMatrix, translationMatrix, resultMatrix);
		modelViewMatrix = resultMatrix.clone();
		FloatUtil.multMatrix(modelViewMatrix, rotationMatrix, resultMatrix);
		
		gl3.glUniformMatrix4fv(modelviewMatrixLocation, 1, false, FloatBuffer.wrap(resultMatrix));
		GL3Error.check(gl3);
		
        gl3.glActiveTexture(GL3.GL_TEXTURE0);
		GL3Error.check(gl3);
        
        int textureLocation = gl3.glGetUniformLocation(shaderProgram.program(), "meshTexture");
        if (textureLocation == -1)
        	GL3Error.throwMe(gl3);
        
        gl3.glUniform1i(textureLocation, 0);
		GL3Error.check(gl3);

		glMeshRenderer.render(gl3, animationState.getArmatureState(infantry.getMillisecondsExists()));

		shaderProgram.useProgram(gl3, false);
	}
}
