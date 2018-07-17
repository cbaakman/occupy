package net.cbaakman.occupy.config;

import java.net.InetAddress;

import lombok.Data;
import net.cbaakman.occupy.network.Address;

@Data
public class ClientConfig {
	private long contactTimeoutMS = 10000;
	
	private Address serverAddress = new Address(InetAddress.getLoopbackAddress(), 5000);
}
