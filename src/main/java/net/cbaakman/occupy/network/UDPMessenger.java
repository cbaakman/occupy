package net.cbaakman.occupy.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.UUID;

import javax.annotation.PreDestroy;

public abstract class UDPMessenger extends Messenger {

	private DatagramSocket socket;
	private Thread listenerThread;
	
	
	public UDPMessenger(int listenPort) throws SocketException {
		socket = new DatagramSocket(listenPort);
		
		listenerThread = new Thread() {
			public void run() {
				
				while (!Thread.currentThread().isInterrupted()) {
					
					DatagramPacket packet = new DatagramPacket(receiveData, receiveData.length);
					
					synchronized(socket) {
						try {
							socket.receive(packet);
							
							
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		listenerThread.start();
	}
	
	@Override
	void send(UUID receiverId, Message message) {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(getId());
	    oos.writeObject(message);
	    oos.close();
	    
	    Contact contact = getContact(receiverId);

		DatagramPacket packet = new DatagramPacket(baos.toByteArray(), baos.size(), contact.getAddress(), contact.getPort());

		synchronized(socket) {
			socket.send(packet);
		}
	}

	@PreDestroy
	void disconnect() {
		socket.disconnect();
		listenerThread.interrupt();
	}
}
