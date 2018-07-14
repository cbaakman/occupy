package net.cbaakman.occupy.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import net.cbaakman.occupy.WhileThread;

public abstract class TCPServerThread extends WhileThread {

	ServerSocketChannel tcpChannel;

	public TCPServerThread(int listenPort) throws IOException {
		super(String.format("tcp-server-%d", listenPort));

		tcpChannel = ServerSocketChannel.open();
		tcpChannel.bind(new InetSocketAddress(listenPort));
		tcpChannel.configureBlocking(false);
	}

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

	protected abstract void onConnectionError(Exception e);

	protected abstract void onConnection(Address address, SocketChannel connectionChannel);

	@Override	
	public void stopRunning() {
		super.stopRunning();

		try {
			tcpChannel.close();
		} catch (IOException e) {
			// Should not happen!
			e.printStackTrace();
			System.exit(1);
		}
	}
}
