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
import net.cbaakman.occupy.errors.NotReadyError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.load.LoadRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshBoneAnimationState;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.render.GL3MeshRenderer;
import net.cbaakman.occupy.resource.MeshFactoryResource;
import net.cbaakman.occupy.resource.ResourceLinker;
import net.cbaakman.occupy.resource.ResourceLocator;
import net.cbaakman.occupy.resource.ResourceManager;
import net.cbaakman.occupy.resource.WaitResource;

public class InfantryRenderer extends EntityRenderer<Infantry> {
	
	static Logger logger = Logger.getLogger(InfantryRenderer.class);

	private ShaderProgram shaderProgram;
	private GL3MeshRenderer glMeshRenderer;
	private MeshBoneAnimationState animationState;
	private Texture texture;
	
	@Override
	public void orderFrom(ResourceManager resourceManager) {
		
		LoadRecord<Texture> asyncTexture = ResourceLinker.submitTextureJobs(resourceManager, ResourceLocator.getImagePath("infantry"));
		
		LoadRecord<GL3MeshRenderer> asyncMesh = GL3MeshRenderer.submit(resourceManager, "infantry");

		LoadRecord<ShaderProgram> asyncProgram = ResourceLinker.addShaderJobs(resourceManager, ResourceLocator.getVertexShaderPath("infantry"),
															   ResourceLocator.getFragmentShaderPath("infantry"));
		
		resourceManager.submit(new WaitResource(asyncTexture, asyncMesh, asyncProgram) {
			
			@Override
			protected void run(GL3 gl3) throws NotReadyError, InitError {
				
				glMeshRenderer = asyncMesh.get();
				
				texture = asyncTexture.get();
				glMeshRenderer.setTexture("infantry", texture);

				try {
					animationState = new MeshBoneAnimationState(glMeshRenderer.getMeshFactory().getAnimation("walk"), 100);
				} catch (KeyError e) {
					throw new SeriousError(e);
				}
				
				shaderProgram = asyncProgram.get();
			}
		});
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
