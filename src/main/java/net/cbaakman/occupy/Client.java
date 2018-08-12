package net.cbaakman.occupy;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.math.Quaternion;

import java.security.InvalidKeyException;

import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.Connection;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.communicate.ServerInfo;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.communicate.enums.RequestType;
import net.cbaakman.occupy.communicate.enums.ResponseType;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorQueue;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.RenderError;
import net.cbaakman.occupy.errors.SeriousError;
import net.cbaakman.occupy.game.Camera;
import net.cbaakman.occupy.game.PlayerRecord;
import net.cbaakman.occupy.input.UserInput;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.math.Vector3f;
import net.cbaakman.occupy.render.InGameGLEventListener;
import net.cbaakman.occupy.render.LoadGLEventListener;
import net.cbaakman.occupy.resource.ResourceManager;
import net.cbaakman.occupy.security.SSLChannel;

public abstract class Client {
	
	static Logger logger = Logger.getLogger(Client.class);
	
	private Camera camera = new Camera();
	private ResourceManager resourceManager = new ResourceManager(this);
	private UserInput userInput = new UserInput(this);
	private JFrame frame;
	private GLCanvas glCanvas;
	private ErrorQueue errorQueue = new ErrorQueue();
	private boolean running;
	
	private PlayerRecord playerRecord = null;
	
	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	
	protected ClientConfig config;
	
	public Client(ClientConfig config) {
				
		this.config = config;
	}
	
	public static String getVersion() {
		MavenXpp3Reader reader = new MavenXpp3Reader();
		Model model;
		try {
			model = reader.read(new FileReader("pom.xml"));
		} catch (IOException | XmlPullParserException e) {
			throw new SeriousError(e);
		}
		return model.getVersion();
	}
	
	public abstract void sendPacket(Packet packet) throws CommunicationError;
	protected abstract Connection connectToServer() throws CommunicationError;

	protected void onPacket(Packet packet) throws SeriousError {
				
		if (!connectedToServer())
			return;
		
		if (packet.getType().equals(PacketType.UPDATE)) {
			processUpdateFromServer((Update)packet.getData());
		}
		else if (packet.getType().equals(PacketType.LOGOUT)) {
			disconnectFromServer();
		}
		else if (packet.getType().equals(PacketType.PING)) {
			int bounce = (Integer)packet.getData();
			if (bounce > 0) {
				bounce--;
				try {
					sendPacket(new Packet(PacketType.PING, bounce));
				} catch (CommunicationError e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	public ServerInfo getServerInfo() throws CommunicationError {
		Connection connection = connectToServer();
		
		try {
			// Tell the server that we want its info:
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			oos.writeObject(RequestType.GET_SERVER_INFO);
			oos.flush();

			// Read the server's response:
			ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
			ResponseType response = (ResponseType)ois.readObject();
			
			if (response.equals(ResponseType.OK)) {
				// Read info (must create a new objectinputstream here):
				ois = new ObjectInputStream(connection.getInputStream());
				ServerInfo info = (ServerInfo)ois.readObject();
				return info;
			}
			else
				throw new CommunicationError(String.format("unexpected response from server: %s", response.name()));

		} catch (ClassNotFoundException | IOException e) {
			throw new CommunicationError(e);
		} finally {
			// Always try to close the connection to the server.
			try {
				connection.close();
			} catch (IOException e) {
				throw new CommunicationError(e);
			}
		}
	}
	
	public void login(Credentials credentials)
			throws AuthenticationError, CommunicationError {
		Connection connection = connectToServer();
		
		try {
			// Tell the server that we want to log in:
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			oos.writeObject(RequestType.LOGIN);
			oos.flush();

			// Convert credentials to bytes:
			ByteArrayOutputStream credentialsByteStream = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(credentialsByteStream);
			oos.writeObject(credentials);
			credentialsByteStream.close();
			oos.close();

			// Send the credentials to the server over ssl:
			SSLChannel sslChannel = new SSLChannel(connection.getInputStream(), connection.getOutputStream());
			sslChannel.send(credentialsByteStream.toByteArray());
		
			// Read the server's response:
			ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
			ResponseType response = (ResponseType)ois.readObject();
			
			if (response.equals(ResponseType.OK)) {
				// Authentication OK
				playerRecord = new PlayerRecord(credentials.getUsername());
				return;
			}
			else if (response.equals(ResponseType.AUTHENTICATION_ERROR))
				throw new AuthenticationError("server rejected the credentials");
			else
				throw new CommunicationError(String.format("unexpected response from server: %s", response.name()));
						
		} catch (ClassNotFoundException | IOException | InvalidKeyException e) {
			throw new CommunicationError(e);
			
		} finally {
			// Always try to close the connection to the server.
			try {
				connection.close();
			} catch (IOException e) {
				throw new CommunicationError(e);
			}
		}
	}
	
	private void processUpdateFromServer(Update update) {
		Updatable updatable;
		
		synchronized(updatables) {
			updatable = updatables.get(update.getObjectID());
		}
			
		if (updatable == null) {
			try {
				Class<?>[] args = new Class<?>[] {Client.class};
				updatable = update.getObjectClass().getDeclaredConstructor(args).newInstance(this);
				
				synchronized(updatables) {
					updatables.put(update.getObjectID(), updatable);
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException | SeriousError e) {
				errorQueue.pushError(e);
				return;
			}
		}
		
		try {
			Field field = updatable.getDeclaredFieldSinceUpdatable(update.getFieldID());
			
			if (field.isAnnotationPresent(ServerToClient.class) ||
					field.isAnnotationPresent(ClientToServer.class) &&
					!updatable.mayBeUpdatedBy(playerRecord)) {
			
				field.setAccessible(true);
				field.set(updatable, update.getValue());
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
				IllegalAccessException e) {
			errorQueue.pushError(e);
		}
	}
	
	public static void centerFrame(JFrame frame) {
	    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
	    int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
	    int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
	    frame.setLocation(x, y);
	}
	
	public GLCanvas getGLCanvas() {
		return glCanvas;
	}

	protected void initClient() throws InitError {

		if (config.getDataDir() == null)
			throw new SeriousError("data dir not set in config");
		if (!config.getDataDir().toFile().isDirectory()) {
			logger.debug("making " + config.getDataDir());
			config.getDataDir().toFile().mkdirs();
		}
		
		Loader loader = new Loader(config.getLoadConcurrency());
		resourceManager.addAllJobsTo(loader);

		GLProfile profile = GLProfile.get(GLProfile.GL3);
		GLCapabilities capabilities = new GLCapabilities(profile);
	
		glCanvas = new GLCanvas(capabilities);
		
		loader.whenDone(new Runnable() {
			@Override
			public void run() {
				GLEventListener listener0 = glCanvas.getGLEventListener(0);
				glCanvas.removeGLEventListener(listener0);
				
				camera.setPosition(new Vector3f(0.0f, 100.0f, 100.0f));
				camera.setOrientation(new Quaternion().rotateByAngleX((float)Math.toRadians(-45.0)));
					
				glCanvas.addGLEventListener(new InGameGLEventListener(Client.this));
			}
		});
		loader.setErrorQueue(getErrorQueue());
		loader.start();

		glCanvas.setSize(config.getScreenWidth(), config.getScreenHeight());
		
		glCanvas.addGLEventListener(new LoadGLEventListener(loader, this));

		frame = new JFrame();
		glCanvas.addKeyListener(userInput);
		glCanvas.addMouseWheelListener(userInput);
		frame.getContentPane().add(glCanvas);
		frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
            	Client.this.stop();
            }
        });
        frame.setLocationRelativeTo(null);
        frame.setTitle("occupy client");
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
        glCanvas.setFocusable(true);
        centerFrame(frame);
        
        glCanvas.requestFocusInWindow();
	}
	
	protected void update(float dt) throws CommunicationError, RenderError, InitError {

		userInput.update(dt);
		
		if (playerRecord != null) {
			synchronized (updatables) {
				for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
					entry.getValue().updateOnClient(dt);
				}
			}
			
			synchronized(updatables) {
				for (Entry<UUID, Updatable> entry : updatables.entrySet()) {
					UUID objectId = entry.getKey();
					Updatable updatable = entry.getValue();
					
					Class<? extends Updatable> objectClass = updatable.getClass();
					
					for (Field field : objectClass.getDeclaredFields()) {
						
						if (field.isAnnotationPresent(ClientToServer.class)
								&& updatable.mayBeUpdatedBy(playerRecord)) {
							
							field.setAccessible(true);
					
							try {
								Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));
	
								sendPacket(new Packet(PacketType.UPDATE, update));
								
							} catch (IllegalArgumentException | IllegalAccessException e) {
								// Must not happen!
								throw new SeriousError(e);
							}
						}
					}
				}
			}
		}
		
		glCanvas.display();

		errorQueue.throwAnyFirstEncounteredError();
	}

	public void run() throws InitError, RenderError {
		running = true;
		
		initCommunication();
		initClient();
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		
		try {
			while (running) {				
				ticks = System.currentTimeMillis();
				dt = (float)(ticks - ticks0) / 1000;
				ticks0 = ticks;
	
				try {
					update(dt);
				} catch (CommunicationError e) {
					disconnectFromServer();
					
					// TODO: show error screen
					logger.error(e.getMessage(), e);
					
				} catch (InitError | RenderError e) {
					// TODO: show error screen
					logger.error(e.getMessage(), e);
				}
			}
		} finally {  // always try to shut down tidily
			if (connectedToServer())
				disconnectFromServer();

			shutdownClient();
			shutdownCommunication();
		}
	}
	
	protected abstract void shutdownCommunication();

	protected abstract void initCommunication() throws InitError;
	
	protected void shutdownClient() {
		glCanvas.destroy();
		frame.dispose();
	}
	
	public void stop() {
		running = false;
	}
	
	public boolean connectedToServer() {
		return playerRecord != null;
	}
	
	public void disconnectFromServer() {
		playerRecord = null;
		
    	try {
			sendPacket(new Packet(PacketType.LOGOUT, null));
		} catch (CommunicationError e) {
			// Do nothing with this, since we're disconnecting anyway.
		}
    	
    	updatables.clear();
	}

	public ErrorQueue getErrorQueue() {
		return errorQueue;
	}

	public Collection<Updatable> getUpdatables() {
		return updatables.values();
	}

	public Camera getCamera() {
		return camera;
	}

	public ClientConfig getConfig() {
		return config;
	}
	
	public ResourceManager getResourceManager() {
		return resourceManager;
	}
			
}
