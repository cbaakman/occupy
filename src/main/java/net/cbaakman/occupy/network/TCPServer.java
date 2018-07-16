package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import net.cbaakman.occupy.WhileThread;

public abstract class TCPServer {

	ServerSocketChannel tcpChannel;
	WhileThread listenThread;

	public TCPServer(int listenPort) throws IOException {
		
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
				} catch (IOException e) {
					onConnectionError(e);
				}
			}
		};
		listenThread.start();
	}

	protected abstract void onConnectionError(Exception e);

	protected abstract void onConnection(Address address, SocketChannel connectionChannel);

	public void disconnect() throws IOException, InterruptedException {

		listenThread.stopRunning();
		tcpChannel.close();
	}
}
