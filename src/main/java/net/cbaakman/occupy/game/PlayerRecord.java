package net.cbaakman.occupy.game;

import lombok.Data;

@Data
public class PlayerRecord {

	public PlayerRecord(String name) {
		this.name = name;
	}

	private String name;
}
