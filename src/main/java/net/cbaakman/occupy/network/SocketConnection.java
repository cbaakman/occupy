package net.cbaakman.occupy.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.cbaakman.occupy.communicate.Connection;

public class SocketConnection implements Connection {
	
	private Socket socket;
	
	public SocketConnection(Socket socket) {
		this.socket = socket;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public void close() throws IOException {
		socket.close();
	}
	
}
