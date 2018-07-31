package net.cbaakman.occupy.render;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;

import lombok.Data;
import net.cbaakman.occupy.math.Vector3f;

@Data
public class BoneTransformation {

	private Quaternion rotation;
	private Vector3f translation;
	
	public BoneTransformation(Quaternion rotation, Vector3f translation) {
		this.rotation = rotation;
		this.translation = translation;
	}

	public void toMatrix(float[] r) {
		
		float[] translationMatrix = new float[16],
			    rotationMatrix = new float[16];
		FloatUtil.makeTranslation(translationMatrix, true,
								  translation.getX(),
								  translation.getY(),
								  translation.getZ());
		rotation.toMatrix(rotationMatrix, 0);
		
		FloatUtil.multMatrix(translationMatrix, rotationMatrix, r);
	}
}
