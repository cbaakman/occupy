package net.cbaakman.occupy.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.UUID;

import javax.annotation.PreDestroy;

public abstract class UDPMessenger {

	private DatagramSocket socket;
	private Thread listenerThread;
	
	public UDPMessenger(int listenPort) throws SocketException {
		socket = new DatagramSocket(listenPort);
		
		listenerThread = new Thread() {
			public void run() {
				
				byte[] data = new byte[1024];
				DatagramPacket packet = new DatagramPacket(data, data.length);
				
				ByteArrayInputStream bais;
				ObjectInputStream ois;
				Message message;
				
				while (!Thread.currentThread().isInterrupted()) {
					try {
						synchronized(socket) {
							socket.receive(packet);
						}
						
						bais = new ByteArrayInputStream(packet.getData());
						ois = new ObjectInputStream(bais);
						
						message = (Message)ois.readObject();
						onReceive(packet.getAddress(), packet.getPort(), message);
						
					} catch (IOException | ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		};
		listenerThread.start();
	}
	
	abstract void onReceive(InetAddress senderAddress, int senderPort, Message message);
	
	void send(InetAddress receiverAddress, int receiverPort, Message message) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(message);
		    oos.close();

		    DatagramPacket packet = new DatagramPacket(baos.toByteArray(), baos.size(), receiverAddress, receiverPort);

			synchronized(socket) {
				socket.send(packet);
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@PreDestroy
	void disconnect() {
		synchronized(socket) {
			socket.disconnect();
		}
		listenerThread.interrupt();
	}
}
