package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import net.cbaakman.occupy.WhileThread;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorQueue;

public abstract class TCPServer {

	private ServerSocketChannel tcpChannel;
	private WhileThread listenThread;
	private ErrorQueue errorQueue;
	
	public TCPServer(ErrorQueue errorQueue, int listenPort) throws IOException {
		this.errorQueue = errorQueue;
		
		tcpChannel = ServerSocketChannel.open();
		tcpChannel.bind(new InetSocketAddress(listenPort));
		tcpChannel.configureBlocking(false);
		
		listenThread = new WhileThread(String.format("tcp-server-%d", listenPort)) {

			@Override
			protected void repeat() {
				try {
					SocketChannel connectionChannel = tcpChannel.accept();
					
					if (connectionChannel != null) {
						InetSocketAddress remoteAddres = (InetSocketAddress)connectionChannel.getRemoteAddress();
						
						Address address = new Address(remoteAddres.getAddress(),
													  remoteAddres.getPort());
						
						onConnection(address, connectionChannel);
					}
				} catch(IOException | CommunicationError e) {
					errorQueue.pushError(e);
				}
			}
		};
		listenThread.start();
	}

	protected abstract void onConnection(Address address, SocketChannel connectionChannel) throws CommunicationError;

	public void disconnect() throws IOException, InterruptedException {

		listenThread.stopRunning();
		tcpChannel.close();
	}
}
