package net.cbaakman.occupy.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class LocalConnection implements Connection {
	
	private List<Update> inComing = new ArrayList<Update>();
	private LocalConnection otherEnd;
	private Updater listener;
	
	public static Client Connect(Server server) {
		LocalConnection serverSide = new LocalConnection(),
						clientSide = new LocalConnection();
		
		Client client = new Client(clientSide);
		server.addClientConnection(serverSide);
		
		serverSide.setOtherEnd(clientSide);
		serverSide.setListener(server);

		clientSide.setOtherEnd(serverSide);
		clientSide.setListener(client);
		
		return client;
	}
	
	private LocalConnection() {
	}
	
	private void setOtherEnd(LocalConnection otherEnd) {
		this.otherEnd = otherEnd;
	}
	
	private void setListener(Updater listener) {
		this.listener = listener;
	}

	public void send(Update update) {
		otherEnd.addInComing(update);
	}

	public synchronized void addInComing(Update update) {
		inComing.add(update);
	}

	public synchronized Collection<Update> poll() {		
		List<Update> r = inComing;
		inComing = new ArrayList<Update>();
		
		return r;
	}

	public UUID getOtherEndId() {
		return otherEnd.getListenerId();
	}

	private UUID getListenerId() {
		return listener.getId();
	}
}
