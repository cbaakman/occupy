package net.cbaakman.occupy.authenticate;

import java.io.Serializable;

import lombok.Data;

@Data
public class Credentials implements Serializable {
	private String username, password;
	
	public Credentials(String username, String password) {
		this.username = username;
		this.password = password;
	}
}
