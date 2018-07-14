package net.cbaakman.occupy.network;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import java.util.UUID;

import net.cbaakman.occupy.Client;
import net.cbaakman.occupy.Message;
import net.cbaakman.occupy.Updatable;
import net.cbaakman.occupy.Update;
import net.cbaakman.occupy.annotations.ClientToServer;
import net.cbaakman.occupy.annotations.ServerToClient;
import net.cbaakman.occupy.config.Config;
import net.cbaakman.occupy.enums.MessageType;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;

public class NetworkClient extends Client {
	
	private Address serverAddress;
	private UDPMessenger udpMessenger;
	private boolean running;
	
	public NetworkClient(ErrorHandler errorHandler, Address serverAddress) {
		super(errorHandler);
		
		this.serverAddress = serverAddress;
	}
	
	private void initNetwork() throws InitError {
		try {
			udpMessenger = new UDPMessenger() {
				@Override
				void onReceive(Address address, Message message) {
					if (!address.getAddress().equals(NetworkClient.this.serverAddress))
						return;

					NetworkClient.this.onMessage(message);
				}

				@Override
				void onReceiveError(Exception e) {
					onCommunicationError(new CommunicationError(e));
				}
			};
		} catch (IOException e) {
			throw new InitError(e);
		}
	}
	
	private void closeNetwork() {
		try {
			udpMessenger.disconnect();
		} catch (IOException e) {
			// Not supposed to happen
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void run() throws InitError {
		initNetwork();
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		running = true;
		
		while (running) {
			ticks = System.currentTimeMillis();
			dt = (float)(ticks - ticks0) / 1000;
			
			update(dt);
		}
		
		closeNetwork();
	}

	@Override
	public void connectToServer() {
		try {			
			Socket socket = new Socket(serverAddress.getAddress(),
									   serverAddress.getPort());
			if (socket.isConnected()) {
			
				OutputStream os = socket.getOutputStream();
				
				// .. transfer login data .. //
				
				socket.close();
			}
			else
				onCommunicationError(
					new CommunicationError(
						String.format("not connected to server at %s %d",
									  serverAddress.getAddress(),
									  serverAddress.getPort())));
			
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}

	@Override
	protected void sendMessage(Message message) {
		try {
			udpMessenger.send(serverAddress, message);
		} catch (IOException e) {
			onCommunicationError(new CommunicationError(e));
		}
	}

	@Override
	protected void stop() {
		running = false;
	}
}
