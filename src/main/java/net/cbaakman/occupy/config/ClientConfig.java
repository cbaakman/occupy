package net.cbaakman.occupy.config;

import java.awt.event.KeyEvent;
import java.io.File;
import java.net.InetAddress;
import java.nio.file.Path;

import lombok.Data;
import net.cbaakman.occupy.network.Address;

@Data
public class ClientConfig {
	private long contactTimeoutMS = 10000;
	
	private Address serverAddress = new Address(InetAddress.getLoopbackAddress(), 5000);
	
	private int screenWidth = 800,
				screenHeight = 600;
	
	private int loadConcurrency = 10;

	private Path dataDir = null;
	
	private int keyCameraLeft = KeyEvent.VK_A,
			    keyCameraRight = KeyEvent.VK_D,
			    keyCameraForward = KeyEvent.VK_W,
			    keyCameraBack = KeyEvent.VK_S;
}
