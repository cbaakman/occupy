package net.cbaakman.occupy.communicate;

import java.io.Serializable;

import lombok.Data;
import net.cbaakman.occupy.communicate.enums.PacketType;

@Data
public class Packet implements Serializable {

	private PacketType type;
	private Serializable data;

	public Packet(PacketType type, Serializable data) {
		this.type = type;
		this.data = data;
	}
}
