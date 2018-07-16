package net.cbaakman.occupy;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.swing.JFrame;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.authenticate.Credentials;
import net.cbaakman.occupy.enums.MessageType;
import net.cbaakman.occupy.errors.AuthenticationError;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.errors.SeriousErrorHandler;
import net.cbaakman.occupy.render.ClientGLEventListener;

public abstract class Client {
	
	JFrame frame;
	GLCanvas glCanvas;

	private Map<UUID, Updatable> updatables = new HashMap<UUID, Updatable>();
	
	private ErrorHandler errorHandler;
	
	public Client(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public abstract void run() throws InitError;
	
	public abstract void login(Credentials credentials) throws AuthenticationError;
	public abstract void sendMessage(Message message);

	protected void onMessage(Message message) {
		
		if (message.getType().equals(MessageType.UPDATE)) {
			processUpdateFromServer((Update)message.getData());
		}
		else if (message.getType().equals(MessageType.LOGOUT)) {
			disconnect();
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

	protected void onInit() throws InitError {

		GLProfile profile = GLProfile.get(GLProfile.GL3);
		GLCapabilities capabilities = new GLCapabilities(profile);
	
		glCanvas = new GLCanvas(capabilities);
		glCanvas.addGLEventListener(new ClientGLEventListener());
		glCanvas.setSize(800, 600);

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

							sendMessage(new Message(MessageType.UPDATE, update));
							
						} catch (IllegalArgumentException | IllegalAccessException e) {
							// Must not happen!
							SeriousErrorHandler.handle(e);
						}
					}
				}
			}
		}
	}
	
	protected void onShutdown() {
		
		disconnect();
		glCanvas.destroy();
		frame.dispose();
	}
	
	public abstract void stop();
	
	public void disconnect() {
    	sendMessage(new Message(MessageType.LOGOUT, null));
    	
    	updatables.clear();
	}
	
	protected void onCommunicationError(CommunicationError e) {
		synchronized(e) {
			errorHandler.handle(e);
		}
	}
}
