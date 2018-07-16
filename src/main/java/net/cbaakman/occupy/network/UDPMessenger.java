package net.cbaakman.occupy.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.util.UUID;

import javax.annotation.PreDestroy;

import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.WhileThread;
import net.cbaakman.occupy.errors.CommunicationError;

public abstract class UDPMessenger {

	private DatagramChannel channel;
	private WhileThread listenerThread = new WhileThread("udp-listen") {
		public void repeat() {
			ByteBuffer buf = ByteBuffer.allocate(2048);
			
			ByteArrayInputStream bais;
			ObjectInputStream ois;
			Message message;
						
			try {
				InetSocketAddress address = (InetSocketAddress)channel.receive(buf);
				if (address != null) {
					bais = new ByteArrayInputStream(buf.array());
					ois = new ObjectInputStream(bais);
					
					message = (Message)ois.readObject();
					UDPMessenger.this.onReceive(new Address(address.getAddress(), address.getPort()), message);
				}
			} catch (ClassNotFoundException | IOException e) {
				UDPMessenger.this.onReceiveError(e);
			}
		}
	};
	
	public UDPMessenger() throws IOException {
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		listenerThread.start();
	}
	
	public UDPMessenger(int listenPort) throws IOException {
		channel = DatagramChannel.open();
		channel.configureBlocking(false);
		channel.bind(new InetSocketAddress(listenPort));
		listenerThread.start();
	}
	
	abstract void onReceive(Address address, Message message);
	abstract void onReceiveError(Exception e);
	
	int getPort() {
		return channel.socket().getLocalPort();
	}
	
	void send(Address receiverAddress, Message message) throws IOException {
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
