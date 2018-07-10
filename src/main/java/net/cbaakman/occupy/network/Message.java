package net.cbaakman.occupy.network;

import java.io.Serializable;

import lombok.Data;
import net.cbaakman.occupy.network.enums.MessageType;

@Data
public class Message implements Serializable {

	private MessageType type;
	private Serializable data;
}
