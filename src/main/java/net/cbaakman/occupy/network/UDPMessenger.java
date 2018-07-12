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

import net.cbaakman.occupy.Message;

public abstract class UDPMessenger {

	private DatagramSocket socket;
	private Thread listenerThread = new Thread() {
		public void run() {
			
			byte[] data = new byte[2048];
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
					onReceive(new Address(packet.getAddress(), packet.getPort()), message);
					
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	};
	
	public UDPMessenger() throws SocketException {
		socket = new DatagramSocket();
		listenerThread.start();
	}
	
	public UDPMessenger(int listenPort) throws SocketException {
		socket = new DatagramSocket(listenPort);
		listenerThread.start();
	}
	
	abstract void onReceive(Address address, Message message);
	
	void send(Address receiverAddress, Message message) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
		    ObjectOutputStream oos = new ObjectOutputStream(baos);
		    oos.writeObject(message);
		    oos.close();

		    DatagramPacket packet = new DatagramPacket(baos.toByteArray(), baos.size(), receiverAddress.getAddress(), receiverAddress.getPort());

			synchronized(socket) {
				socket.send(packet);
			}	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@PreDestroy
	void disconnect() {
		listenerThread.interrupt();
		
		synchronized(socket) {
			socket.disconnect();
		}
	}
}
