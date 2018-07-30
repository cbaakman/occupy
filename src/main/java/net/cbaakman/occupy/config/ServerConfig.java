package net.cbaakman.occupy.config;

import lombok.Data;

@Data
public class ServerConfig {
	private long contactTimeoutMS = 10000;
	
	private int listenPort = 5000;
}
