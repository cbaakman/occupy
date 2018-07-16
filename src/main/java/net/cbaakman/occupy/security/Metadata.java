package net.cbaakman.occupy.security;

import java.io.Serializable;

import lombok.Data;

@Data
public class Metadata implements Serializable {
	private int dataLength;
	
	public Metadata(int dataLength) {
		this.dataLength = dataLength;
	}
}
