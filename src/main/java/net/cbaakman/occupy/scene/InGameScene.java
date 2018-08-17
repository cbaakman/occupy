package net.cbaakman.occupy.scene;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.SynchronizedPool;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.Update;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.GL3Error;
import net.cbaakman.occupy.errors.KeyError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.errors.ShaderCompileError;
import net.cbaakman.occupy.errors.ShaderLinkError;
import net.cbaakman.occupy.game.Camera;
import net.cbaakman.occupy.game.Entity;
import net.cbaakman.occupy.game.Infantry;
import net.cbaakman.occupy.game.PlayerRecord;
import net.cbaakman.occupy.math.Vector2f;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.render.GLSprite2DRenderer;
import net.cbaakman.occupy.render.entity.EntityRenderer;
import net.cbaakman.occupy.render.entity.InfantryRenderer;
import net.cbaakman.occupy.render.entity.RenderRegistry;

public class InGameScene extends Scene {
	Logger logger = Logger.getLogger(InGameScene.class);

	private PlayerRecord playerRecord = null;
	
	private Client client;

	private RenderRegistry renderRegistry = new RenderRegistry();
	
	private Camera camera = new Camera();

	private SynchronizedPool<UUID, Updatable> updatables = new SynchronizedPool<UUID, Updatable>();
	
	private Texture cursorTexture = null;
	private GLSprite2DRenderer cursorRenderer = null;
	private Vector2f cursorPosition = new Vector2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
	private volatile boolean mouseInCanvas = false;
	
	public InGameScene(Client client, PlayerRecord playerRecord) {
		this.client = client;

		this.playerRecord = playerRecord;

		camera.setPosition(new Vector3f(0.0f, 100.0f, 100.0f));
		camera.setOrientation(new Quaternion().rotateByAngleX((float)Math.toRadians(-45.0)));
	}

	@Override
	public synchronized void display(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
        try {	        
	        gl3.glClearColor(0.0f, 0.5f, 0.5f, 1.0f);
			GL3Error.check(gl3);

			gl3.glClearDepth(1.0f);
			GL3Error.check(gl3);

			gl3.glDepthMask(true);
			GL3Error.check(gl3);
	        
	        gl3.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
			GL3Error.check(gl3);


			float[] projectionMatrix = new float[16];
			FloatUtil.makePerspective(projectionMatrix, 0, true,
									  (float)(Math.PI / 4),
									  ((float)drawable.getSurfaceWidth()) / drawable.getSurfaceHeight(),
									  0.1f, 1000.0f);

			float[] modelViewMatrix = camera.getMatrix();

			gl3.glDisable(GL3.GL_BLEND);
			GL3Error.check(gl3);
			
			gl3.glEnable(GL3.GL_CULL_FACE);
			GL3Error.check(gl3);

			gl3.glEnable(GL3.GL_DEPTH_TEST);
			GL3Error.check(gl3);

			gl3.glDepthMask(true);
			GL3Error.check(gl3);
			
			for (Updatable updatable : updatables.getAll()) {
				if (updatable instanceof Entity) {
					Entity entity = (Entity)updatable;

					EntityRenderer renderer = renderRegistry.getForEntity(entity.getClass());

					renderer.renderOpaque(gl3, projectionMatrix, modelViewMatrix, entity);
				}
			}
			
			gl3.glDepthMask(false);
			GL3Error.check(gl3);
			
			gl3.glEnable(GL3.GL_BLEND);
			GL3Error.check(gl3);
			
			for (Updatable updatable : updatables.getAll()) {
				if (updatable instanceof Entity) {
					Entity entity = (Entity)updatable;

					EntityRenderer renderer = renderRegistry.getForEntity(entity.getClass());

					renderer.renderTransparent(gl3, projectionMatrix, modelViewMatrix, entity);
				}
			}
			
			FloatUtil.makeOrtho(projectionMatrix, 0, true,
								0.0f, (float)drawable.getSurfaceWidth(),
								(float)drawable.getSurfaceHeight(), 0.0f,
								-1.0f, 1.0f);
			
			gl3.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
			GL3Error.check(gl3);
			
			if (mouseInCanvas) {
				cursorRenderer.set(gl3, cursorPosition, 16.0f,
								   0.0f, 1.0f, 1.0f, 0.0f);
				cursorRenderer.render(gl3, projectionMatrix);
			}
			
		} catch (GL3Error | KeyError | IndexOutOfBoundsException  e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			cursorRenderer.cleanUp(gl3);
			
			cursorTexture.destroy(gl3);
			
			renderRegistry.cleanUpAll(gl3);
		} catch (GL3Error e) {
			client.getErrorQueue().pushError(e);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL3 gl3 = drawable.getGL().getGL3();
		
		try {
			renderRegistry.registerForEntity(Infantry.class, new InfantryRenderer(client, gl3));
			
			BufferedImage cursorImage;
			try {
				cursorImage = client.getResourceManager().getImage("cursor_default");
			} catch (KeyError | InterruptedException | ExecutionException e) {
				throw new SeriousError(e);
			}
			TextureData textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), cursorImage, true);
			if (textureData.getMustFlipVertically()) {
				ImageUtil.flipImageVertically(cursorImage);
				textureData = AWTTextureIO.newTextureData(gl3.getGLProfile(), cursorImage, true);
			}

			cursorTexture = TextureIO.newTexture(gl3, textureData);
			cursorTexture.setTexParameterf(gl3, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_NEAREST);
			cursorTexture.setTexParameterf(gl3, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_NEAREST);
			cursorTexture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_S, GL3.GL_REPEAT);
			cursorTexture.setTexParameterf(gl3, GL3.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);

			cursorRenderer = new GLSprite2DRenderer(gl3);
			cursorRenderer.setTexture(cursorTexture);
			
		} catch (GL3Error | SeriousError | ShaderCompileError | ShaderLinkError e) {
			logger.error(e.getMessage(), e);
			
			client.getErrorQueue().pushError(e);
		}
	}
	@Override
	public synchronized void onUpdateFromServer(Update update) {
		
		Updatable updatable = updatables.get(update.getObjectID());
			
		if (updatable == null) {
			try {
				Class<?>[] args = new Class<?>[] {Client.class};
				updatable = update.getObjectClass().getDeclaredConstructor(args).newInstance(client);
				
				updatables.put(update.getObjectID(), updatable);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | SeriousError e) {
				client.getErrorQueue().pushError(e);
				return;
			}
		}
		
		for (Entry<String, Object> entry : update.getFieldValues().entrySet()) {
			String fieldId = entry.getKey();
			Object value = entry.getValue();

			try {
				Field field = updatable.getDeclaredFieldSinceUpdatable(fieldId);
				
				if (field.isAnnotationPresent(ServerToClient.class) ||
						field.isAnnotationPresent(ClientToServer.class) &&
						!updatable.mayBeUpdatedBy(playerRecord)) {
				
					field.setAccessible(true);
					field.set(updatable, value);
				}
				
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
					IllegalAccessException e) {
				client.getErrorQueue().pushError(e);
			}
		}
	}
	private final static float CAMERA_MOVE_SPEED = 100.0f,
			   CAMERA_ZOOM_PER_NOTCH = 5.0f,
			   CAMERA_MIN_Y = 25.0f;

	@Override
	public synchronized void mouseWheelMoved(MouseWheelEvent event) {
		int notches = event.getWheelRotation();
		
		if (notches < 0 && camera.getPosition().getY() <= CAMERA_MIN_Y)
		return;
		
		float[] vIn = new float[] {0.0f, 0.0f, CAMERA_ZOOM_PER_NOTCH * notches},
		vOut = new float[3];
		camera.getOrientation().rotateVector(vOut, 0, vIn, 0);
		
		camera.getPosition().move(vOut);
	}

	
	@Override
	public void mouseMoved(MouseEvent event) {
		cursorPosition = new Vector2f((float)event.getX(), (float)event.getY());
	}

	@Override
	public void mouseDragged(MouseEvent event) {
		cursorPosition = new Vector2f((float)event.getX(), (float)event.getY());
	}

	@Override
	public void mouseEntered(MouseEvent evt) {
		mouseInCanvas = true;
    }

	@Override
    public void mouseExited(MouseEvent evt) {
		mouseInCanvas = false;
    }
	
	public synchronized void update(float dt) {
		ClientConfig config = client.getConfig();
		
		if (isKeyDown(config.getKeyCameraForward()))
			camera.getPosition().move(new Vector3f(0.0f, 0.0f, -CAMERA_MOVE_SPEED * dt));
		else if (isKeyDown(config.getKeyCameraBack()))
			camera.getPosition().move(new Vector3f(0.0f, 0.0f, CAMERA_MOVE_SPEED * dt));
		if (isKeyDown(config.getKeyCameraLeft()))
			camera.getPosition().move(new Vector3f(-CAMERA_MOVE_SPEED * dt, 0.0f, 0.0f));
		else if (isKeyDown(config.getKeyCameraRight()))
			camera.getPosition().move(new Vector3f(CAMERA_MOVE_SPEED * dt, 0.0f, 0.0f));

		for (UUID objectId : updatables.getKeys()) {
			Updatable updatable = updatables.get(objectId);
			
			updatable.updateOnClient(dt);
			
			Class<? extends Updatable> objectClass = updatable.getClass();
			
			Update update = new Update(objectClass, objectId);
			
			for (Field field : objectClass.getDeclaredFields()) {
				
				if (field.isAnnotationPresent(ClientToServer.class)
						&& updatable.mayBeUpdatedBy(playerRecord)) {
					
					field.setAccessible(true);
			
					try {
						update.setValue(field.getName(), field.get(updatable));
						
					} catch (IllegalArgumentException | IllegalAccessException e) {
						// Must not happen!
						throw new SeriousError(e);
					}
				}
			}

			try {
				client.sendPacket(new Packet(PacketType.UPDATE, update));
			} catch (CommunicationError e) {
				client.getErrorQueue().pushError(e);
			}
		}
	}
}
