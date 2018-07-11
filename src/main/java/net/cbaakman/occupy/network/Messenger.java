package net.cbaakman.occupy.network;

import java.util.UUID;

public abstract class Messenger {

	private UUID id = UUID.randomUUID();
	
	public UUID getId() {
		return id;
	}
	
	abstract void onReceive(UUID senderId, Message message);
	
	abstract void send(UUID receiverId, Message message);
}
