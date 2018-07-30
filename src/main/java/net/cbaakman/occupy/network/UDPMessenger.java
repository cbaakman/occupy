package net.cbaakman.occupy.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import net.cbaakman.occupy.WhileThread;
import net.cbaakman.occupy.communicate.Packet;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorQueue;

public abstract class UDPMessenger {

	private ErrorQueue errorQueue;
	private DatagramChannel channel;
	
	private WhileThread listenerThread = new WhileThread("udp-listen") {
		public void repeat() {
			ByteBuffer buf = ByteBuffer.allocate(2048);
			
			ByteArrayInputStream bais;
			ObjectInputStream ois;
			Packet message;
						
			try {
				InetSocketAddress address = (InetSocketAddress)channel.receive(buf);
				if (address != null) {
					bais = new ByteArrayInputStream(buf.array());
					ois = new ObjectInputStream(bais);
					
					message = (Packet)ois.readObject();
					UDPMessenger.this.onReceive(new Address(address.getAddress(), address.getPort()), message);
				}
			} catch (ClassNotFoundException e) {
				errorQueue.pushError(e);
			} catch (IOException e) {
				errorQueue.pushError(new CommunicationError(e));
			}
		}
	};
	
	public UDPMessenger(ErrorQueue errorQueue) throws IOException {
		this.errorQueue = errorQueue;
		
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		listenerThread.start();
	}
	
	public UDPMessenger(ErrorQueue errorQueue, int listenPort) throws IOException {
		this.errorQueue = errorQueue;
		
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.bind(new InetSocketAddress(listenPort));
		listenerThread.start();
	}
	
	abstract void onReceive(Address address, Packet message);
	
	int getPort() {
		return channel.socket().getLocalPort();
	}
	
	void send(Address receiverAddress, Packet message) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ObjectOutputStream oos = new ObjectOutputStream(baos);
	    oos.writeObject(message);
	    oos.close();

	    ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
	    channel.send(buf, new InetSocketAddress(receiverAddress.getAddress(),
	    										receiverAddress.getPort()));
	}

	void disconnect() throws IOException, InterruptedException {
		listenerThread.stopRunning();
		channel.disconnect();
	}
}
