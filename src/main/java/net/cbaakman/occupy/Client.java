package net.cbaakman.occupy;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.security.InvalidKeyException;

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
import net.cbaakman.occupy.game.PlayerRecord;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.resource.ResourceManager;
import net.cbaakman.occupy.scene.InGameScene;
import net.cbaakman.occupy.scene.LoadScene;
import net.cbaakman.occupy.scene.Scene;
import net.cbaakman.occupy.security.SSLChannel;

public abstract class Client {
	
	static Logger logger = Logger.getLogger(Client.class);
	
	private ResourceManager resourceManager = new ResourceManager(this);
	private JFrame frame;
	private GLCanvas glCanvas;
	private ErrorQueue errorQueue = new ErrorQueue();
	
	private Scene currentScene = null;
	private PlayerRecord loggedInPlayerRecord = null;
	
	/**
	 * Executes jobs that have to be executed in the main thread, where openGL runs.
	 */
	private JobScheduler mainThreadScheduler = new JobScheduler();
	
	private volatile boolean running;
	
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
				
		if (!loggedIn())
			return;
		
		if (packet.getType().equals(PacketType.UPDATE)) {

			if (currentScene != null)
				currentScene.onUpdateFromServer((Update)packet.getData());
		}
		else if (packet.getType().equals(PacketType.LOGOUT)) {
			logout();
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
				loggedInPlayerRecord = new PlayerRecord(credentials.getUsername());
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
				// TODO: show login screen
				while (!loggedIn()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						errorQueue.pushError(e);
						return;
					}
				}
				
				switchScene(new InGameScene(Client.this, loggedInPlayerRecord));
			}
		});
		loader.setErrorQueue(getErrorQueue());

		glCanvas.setSize(config.getScreenWidth(), config.getScreenHeight());
		
		Toolkit t = Toolkit.getDefaultToolkit();
	    Image i = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
	    Cursor noCursor = t.createCustomCursor(i, new Point(0, 0), "none"); 
		glCanvas.setCursor(noCursor);
		
		switchScene(new LoadScene(loader, this));
		
		loader.start();

		frame = new JFrame();
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
        frame.toFront();
	}
	
	protected void update(float dt) throws CommunicationError, RenderError, InitError {

		mainThreadScheduler.executeAll();
		
		if (currentScene != null)
			currentScene.update(dt);

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
					logout();
					
					// TODO: show error screen
					logger.error(e.getMessage(), e);
					
				} catch (InitError | RenderError e) {
					// TODO: show error screen
					logger.error(e.getMessage(), e);
				}
			}
		} finally {  // always try to shut down tidily
			if (loggedIn())
				logout();

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
	
	public void logout() {
		loggedInPlayerRecord = null;
    	try {
			sendPacket(new Packet(PacketType.LOGOUT, null));
		} catch (CommunicationError e) {
			// Do nothing with this, since we're disconnecting anyway.
		}
	}

	public ErrorQueue getErrorQueue() {
		return errorQueue;
	}
	
	public boolean loggedIn() {
		return loggedInPlayerRecord != null;
	}

	public ClientConfig getConfig() {
		return config;
	}
	
	public ResourceManager getResourceManager() {
		return resourceManager;
	}
	
	public void switchScene(Scene scene) {
		mainThreadScheduler.schedule(new Runnable(){

			@Override
			public void run() {
				if (glCanvas.getGLEventListenerCount() > 0) {
					GLEventListener listener0 = glCanvas.getGLEventListener(0);
					glCanvas.removeGLEventListener(listener0);
				}
				glCanvas.addGLEventListener(scene);
				
				for (KeyListener keyListener : glCanvas.getKeyListeners()) {
					glCanvas.removeKeyListener(keyListener);
				}
				glCanvas.addKeyListener(scene);
				
				for (MouseWheelListener mouseWheelListener : glCanvas.getMouseWheelListeners()) {
					glCanvas.removeMouseWheelListener(mouseWheelListener);
				}
				glCanvas.addMouseWheelListener(scene);
				
				for (MouseMotionListener mouseMotionListener : glCanvas.getMouseMotionListeners()) {
					glCanvas.removeMouseMotionListener(mouseMotionListener);
				}
				glCanvas.addMouseMotionListener(scene);
				
				for (MouseListener mouseListener : glCanvas.getMouseListeners()) {
					glCanvas.removeMouseListener(mouseListener);
				}
				glCanvas.addMouseListener(scene);
				
				currentScene = scene;
			}
		});
	}
}
