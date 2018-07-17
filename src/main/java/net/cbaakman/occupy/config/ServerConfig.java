package net.cbaakman.occupy.config;

import lombok.Data;
import net.cbaakman.occupy.network.Address;

@Data
public class ServerConfig {
	private long contactTimeoutMS = 10000;
	
	private int listenPort = 5000;
}
