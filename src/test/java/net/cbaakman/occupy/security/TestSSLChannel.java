package net.cbaakman.occupy.security;

import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.Test;

import net.cbaakman.occupy.errors.SeriousError;

public class TestSSLChannel {

	@Test
	public void test() throws InvalidKeyException,
							  NoSuchPaddingException,
							  IllegalBlockSizeException,
							  BadPaddingException,
							  IOException, NoSuchAlgorithmException, SeriousError {
		
		final byte[] sent = new byte[1024];
		
		Random r = new Random();
		r.nextBytes(sent);
		
		final PipedOutputStream osThis = new PipedOutputStream();
		final PipedInputStream isThis = new PipedInputStream(osThis);

		final PipedOutputStream osOther = new PipedOutputStream();
		final PipedInputStream isOther = new PipedInputStream(osOther);
		
		Thread otherChannelThread = new Thread("other-channel") {
			public void run() {
				
				SSLChannel channel = new SSLChannel(isThis, osOther);
				
				try {
					channel.send(sent);
				} catch (InvalidKeyException | IOException | SeriousError e) {
					throw new RuntimeException(e);
				}
			}
		};
		otherChannelThread.start();
		
		
		SSLChannel channel = new SSLChannel(isOther, osThis);
		
		byte[] received = channel.receive();
		
		assertArrayEquals(received, sent);
	}
}
