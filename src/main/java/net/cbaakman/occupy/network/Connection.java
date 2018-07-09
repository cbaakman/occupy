package net.cbaakman.occupy.network;

import java.util.Collection;
import java.util.UUID;

public interface Connection {

	void send(Update update);
	Collection<Update> poll();
	
	UUID getOtherEndId();
}
