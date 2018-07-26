package net.cbaakman.occupy.communicate;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;

import org.apache.log4j.Logger;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import java.security.InvalidKeyException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;

import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.Update;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.communicate.enums.PacketType;
import net.cbaakman.occupy.communicate.enums.RequestType;
import net.cbaakman.occupy.communicate.enums.ResponseType;
import net.cbaakman.occupy.config.ClientConfig;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.font.Font;
import net.cbaakman.occupy.font.FontFactory;
import net.cbaakman.occupy.font.SVGStyle;
import net.cbaakman.occupy.load.LoadJob;
import net.cbaakman.occupy.load.Loader;
import net.cbaakman.occupy.mesh.MeshFactory;
import net.cbaakman.occupy.render.ClientGLEventListener;
import net.cbaakman.occupy.render.LoadGLEventListener;
import net.cbaakman.occupy.security.SSLChannel;

public abstract class Client {
	
	Logger logger = Logger.getLogger(Client.class);
	
	JFrame frame;
	GLCanvas glCanvas;

	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	
	protected ErrorHandler errorHandler;
	protected ClientConfig config;
	
	public Client(ErrorHandler errorHandler, ClientConfig config) {
		this.errorHandler = errorHandler;
		this.config = config;
	}

	public abstract void run() throws InitError;
	
	public abstract void sendPacket(Packet packet);
	protected abstract Connection connectToServer() throws CommunicationError;

	protected void onPacket(Packet message) {
		
		if (message.getType().equals(PacketType.UPDATE)) {
			processUpdateFromServer((Update)message.getData());
		}
		else if (message.getType().equals(PacketType.LOGOUT)) {
			disconnect();
		}
	}
	
	public void login(Credentials credentials) throws AuthenticationError {
		Connection connection;
		try {
			connection = connectToServer();
		} catch (CommunicationError e) {
			onCommunicationError(e);
			return;
		}
		
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
				return;
			}
			else if (response.equals(ResponseType.AUTHENTICATION_ERROR))
				throw new AuthenticationError();
			
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
				| NoSuchPaddingException | ClassNotFoundException e) {
			// Not supposed to happen!
			SeriousErrorHandler.handle(e);
			
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
			
		} finally {
			// Always try to close the connection to the server.
			try {
				connection.close();
			} catch (IOException e) {
				onCommunicationError(new CommunicationError(e));
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
				updatable = update.getObjectClass().newInstance();
				
				synchronized(updatables) {
					updatables.put(update.getObjectID(), updatable);
				}
			} catch (InstantiationException | IllegalAccessException e) {
				// Must never happen!
				SeriousErrorHandler.handle(e);
			}
		}
		
		try {
			Field field = update.getObjectClass().getField(update.getFieldID());
			
			if (field.isAnnotationPresent(ServerToClient.class)) {
			
				field.setAccessible(true);
				field.set(updatable, update.getValue());
			}
			
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException |
				IllegalAccessException e) {
			// Must never happen!
			SeriousErrorHandler.handle(e);
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

	protected void onInit() throws InitError {
		
		Loader loader = new Loader(config.getLoadConcurrency());
		
		final Future<FontFactory> fFactory = loader.add(new LoadJob<FontFactory>() {

			@Override
			public FontFactory call() throws Exception {
				return FontFactory.parse(Client.class.getResourceAsStream("/font/Lumean.svg"));
			}
		});
		final Future<Font> fFont = loader.add(new LoadJob<Font>(){

			@Override
			public Font call() throws Exception {
				return fFactory.get().generateFont(36, new SVGStyle());
			}

			@Override
			public boolean isReady() {
				return fFactory.isDone();
			}
		});
		Future<BufferedImage> fInfantryImage = loader.add(new LoadJob<BufferedImage>(){

			@Override
			public BufferedImage call() throws Exception {
				return ImageIO.read(Client.class.getResourceAsStream("/image/infantry.png"));
			}
		});
		Future<MeshFactory> fInfantryMeshFactory = loader.add(new LoadJob<MeshFactory>() {

			@Override
			public MeshFactory call() throws Exception {
				return MeshFactory.parse(Client.class.getResourceAsStream("/mesh/infantry.xml"));
			}
		});
		loader.whenDone(new Runnable() {
			@Override
			public void run() {
				GLEventListener listener0 = glCanvas.getGLEventListener(0);
				glCanvas.removeGLEventListener(listener0);
				
				try {
					glCanvas.addGLEventListener(new ClientGLEventListener(Client.this, fFont.get()));
				} catch (InterruptedException | ExecutionException e) {

					SeriousErrorHandler.handle(e);
				}
			}
		});
		loader.start();

		GLProfile profile = GLProfile.get(GLProfile.GL3);
		GLCapabilities capabilities = new GLCapabilities(profile);
	
		glCanvas = new GLCanvas(capabilities);
		glCanvas.setSize(config.getScreenWidth(), config.getScreenHeight());
		
		glCanvas.addGLEventListener(new LoadGLEventListener(loader));
		
		logger.debug(String.format("screen width: %d, screen height: %d",
								   glCanvas.getWidth(), glCanvas.getHeight()));

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
        centerFrame(frame);
        
        glCanvas.requestFocusInWindow();
	}
	
	protected void update(float dt) {
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
					
					if (field.isAnnotationPresent(ClientToServer.class)) {
						
						field.setAccessible(true);
				
						try {
							Update update = new Update(objectClass, objectId, field.getName(), field.get(updatable));

							sendPacket(new Packet(PacketType.UPDATE, update));
							
						} catch (IllegalArgumentException | IllegalAccessException e) {
							// Must not happen!
							SeriousErrorHandler.handle(e);
						}
					}
				}
			}
		}
		
		glCanvas.display();
	}
	
	protected void onShutdown() {
		
		disconnect();
		glCanvas.destroy();
		frame.dispose();
	}
	
	public abstract void stop();
	
	public void disconnect() {
    	sendPacket(new Packet(PacketType.LOGOUT, null));
    	
    	updatables.clear();
	}
	
	protected void onCommunicationError(CommunicationError e) {
		synchronized(e) {
			errorHandler.handle(e);
		}
	}
}
