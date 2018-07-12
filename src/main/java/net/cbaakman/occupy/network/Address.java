package net.cbaakman.occupy.network;

import java.net.InetAddress;

import lombok.Data;

@Data
public class Address {

	private InetAddress address;
	private int port;
	
	public Address(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
}
