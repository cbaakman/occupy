package net.cbaakman.occupy.network;

import java.io.IOException;
import java.io.OutputStream;
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
import net.cbaakman.occupy.config.Config;
import net.cbaakman.occupy.errors.CommunicationError;
import net.cbaakman.occupy.errors.ErrorHandler;
import net.cbaakman.occupy.errors.InitError;
import net.cbaakman.occupy.network.annotations.ClientToServer;
import net.cbaakman.occupy.network.annotations.ServerToClient;
import net.cbaakman.occupy.network.enums.MessageType;

public class NetworkClient extends Client {
	
	private Address serverAddress;
	private UDPMessenger udpMessenger;
	private boolean running;
	
	public NetworkClient(ErrorHandler errorHandler, Address serverAddress) {
		super(errorHandler);
		
		this.serverAddress = serverAddress;
	}

	public void run() throws InitError {
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
		
		long ticks0 = System.currentTimeMillis(),
			 ticks;
		float dt;
		running = true;
		
		while (running) {
			ticks = System.currentTimeMillis();
			dt = (float)(ticks - ticks0) / 1000;
			
			update(dt);
		}
		
		try {
			udpMessenger.disconnect();
		} catch (IOException e) {
			// Not supposed to happen
			e.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void connectToServer() {
		try {			
			Socket socket = new Socket(serverAddress.getAddress(),
									   serverAddress.getPort());
			
			OutputStream os = socket.getOutputStream();
			os.write(new byte[] {1,1,1,1,1,0});
			
			// .. transfer login data .. //
			
			socket.close();
			
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
