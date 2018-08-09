package net.cbaakman.occupy.network;

import java.net.InetAddress;
import java.nio.ByteBuffer;

import lombok.Data;
import net.cbaakman.occupy.Identifier;

@Data
public class Address implements Identifier {

	private InetAddress address;
	private int port;
	
	public Address(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}
	
	@Override
	public int hashCode() {
		ByteBuffer wrapped = ByteBuffer.wrap(address.getAddress());
		return wrapped.getInt() + 31 * port;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof Address) {
			Address otherAddress = (Address)other;
			
			return otherAddress.getAddress().equals(this.address) &&
				   otherAddress.getPort() == this.port;
		}
		else return false;
	}
	
	@Override
	public String toString() {
		return String.format("%s:%d", address.toString(), port);
	}
}
