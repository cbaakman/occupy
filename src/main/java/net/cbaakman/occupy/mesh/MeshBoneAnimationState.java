package net.cbaakman.occupy.mesh;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jogamp.opengl.math.Quaternion;

import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.mesh.MeshBoneAnimation.Key;
import net.cbaakman.occupy.render.BoneTransformation;

public class MeshBoneAnimationState {

	private MeshBoneAnimation animation;

	private float frame = 0.0f;
	
	private float speed;
	
	public MeshBoneAnimationState(MeshBoneAnimation animation, float speed) {
		this.animation = animation;
		this.speed = speed;
	}
	
	public void reset() {
		frame = 0.0f;
	}
	
	public void update(float dt) {
		frame += speed * dt;
		if (frame > animation.getLength())
			frame -= animation.getLength();
	}

	public Map<String, BoneTransformation> getAnimationState() {
		
		Map<String, BoneTransformation> transformations = new HashMap<String, BoneTransformation>();
		for (MeshBoneAnimation.Layer layer : animation.getLayers()) {
			
			String boneId = layer.getBone().getId();
			
			int framePrev = Integer.MIN_VALUE,
				frameNext = Integer.MAX_VALUE,
				frameFirst = Integer.MAX_VALUE,
				frameLast = Integer.MIN_VALUE;
			
			for (Entry<Integer, MeshBoneAnimation.Key> entry : layer.getKeys().entrySet()) {
				int keyFrame = entry.getKey();
				
				if (keyFrame < frame && keyFrame > framePrev)
					framePrev = keyFrame;
				if (keyFrame > frame && keyFrame < frameNext)
					frameNext = keyFrame;
				if (keyFrame < frameFirst)
					frameFirst = keyFrame;
				if (keyFrame > frameLast)
					frameLast = keyFrame;
			}
			
			float distanceToPrev;
			if (framePrev < 0) {
				framePrev = frameLast;
				distanceToPrev = frame + animation.getLength() - frameLast;
			}
			else
				distanceToPrev = frame - framePrev;
			
			float distanceToNext;
			if (frameNext > animation.getLength()) {
				frameNext = frameFirst;
				distanceToNext = animation.getLength() - frame + frameFirst;
			}
			else
				distanceToNext = frameNext - frame;
			
			MeshBoneAnimation.Key keyPrev = layer.getKeys().get(framePrev),
								  keyNext = layer.getKeys().get(frameNext);
			
			transformations.put(boneId, interPolateTransform(keyPrev, keyNext, distanceToPrev, distanceToNext));
		}
		
		return transformations;
	}
	
	private BoneTransformation interPolateTransform(Key keyPrev, Key keyNext, float distanceToPrev, float distanceToNext) {

		
		float total = distanceToPrev + distanceToNext;

		// fPrev + fNext = 1.0
		float fPrev, fNext;
		
		if (total > 0.0f) {
			fPrev = distanceToPrev / total;
			fNext = distanceToNext / total;
		}
		else {
			fPrev = 0.5f;
			fNext = 0.5f;
		}
		
		Quaternion rotation = new Quaternion();
		rotation.setSlerp(keyPrev.getRotation(), keyNext.getRotation(), fPrev);
		
		Vector3f translation = keyPrev.getTranslation().multiplyBy(fNext)
						  .add(keyNext.getTranslation().multiplyBy(fPrev));
		
		return new BoneTransformation(rotation, translation);
	}
}
