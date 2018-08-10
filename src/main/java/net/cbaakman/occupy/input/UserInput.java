package net.cbaakman.occupy.input;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.apache.log4j.Logger;

import net.cbaakman.occupy.communicate.Client;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.game.Camera;
import net.cbaakman.occupy.math.Vector3f;

public class UserInput extends MemoryKeyListener implements MouseWheelListener {
	
	Logger logger = Logger.getLogger(UserInput.class);
	
	private Client client;

	public UserInput(Client client) {
		this.client = client;
	}

	private final static float CAMERA_MOVE_SPEED = 100.0f,
							   CAMERA_ZOOM_PER_NOTCH = 1.0f,
							   CAMERA_MIN_Y = 25.0f;
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int notches = e.getWheelRotation();
		Camera camera = client.getCamera();
		
		if (notches < 0 && camera.getPosition().getY() <= CAMERA_MIN_Y)
			return;
		
		float[] vIn = new float[] {0.0f, 0.0f, CAMERA_ZOOM_PER_NOTCH * notches},
				vOut = new float[3];
		camera.getOrientation().rotateVector(vOut, 0, vIn, 0);
		
		camera.getPosition().move(vOut);
	}
	
	public void update(float dt) {
		Camera camera = client.getCamera();
		ClientConfig config = client.getConfig();
		
		if (isKeyDown(config.getKeyCameraForward()))
			camera.getPosition().move(new Vector3f(0.0f, 0.0f, -CAMERA_MOVE_SPEED * dt));
		else if (isKeyDown(config.getKeyCameraBack()))
			camera.getPosition().move(new Vector3f(0.0f, 0.0f, CAMERA_MOVE_SPEED * dt));
		if (isKeyDown(config.getKeyCameraLeft()))
			camera.getPosition().move(new Vector3f(-CAMERA_MOVE_SPEED * dt, 0.0f, 0.0f));
		else if (isKeyDown(config.getKeyCameraRight()))
			camera.getPosition().move(new Vector3f(CAMERA_MOVE_SPEED * dt, 0.0f, 0.0f));
	}
}
