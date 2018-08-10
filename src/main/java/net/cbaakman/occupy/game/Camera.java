package net.cbaakman.occupy.game;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class Camera {

	private Vector3f position;
	private Quaternion orientation;
	
	public Camera() {
		this.position = new Vector3f();
		this.orientation = new Quaternion();
	}
	
	public float[] getMatrix() {
		float[] worldTransform  = new float[16],
				translation = new float[16],
				rotation = new float[16],
				viewMatrix = new float[16];
		
		FloatUtil.makeTranslation(translation, true,
												position.getX(),
												position.getY(),
												position.getZ());
		orientation.toMatrix(rotation, 0);
		
		FloatUtil.multMatrix(translation, rotation, worldTransform);
		
		FloatUtil.invertMatrix(worldTransform, viewMatrix);
		
		return viewMatrix;
	}
}
