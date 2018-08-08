package net.cbaakman.occupy.config;

import java.io.File;
import java.nio.file.Path;

import lombok.Data;

@Data
public class ServerConfig {
	private long contactTimeoutMS = 10000;
	
	private int listenPort = 5000;
	
	private Path dataDir = null;
}
