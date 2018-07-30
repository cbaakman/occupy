package net.cbaakman.occupy.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.Data;
import net.cbaakman.occupy.errors.SeriousError;

public class SSLChannel {

	private InputStream is;
	private OutputStream os;
	
	public SSLChannel(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
	}
	
	
	private static KeyPair buildKeyPair() throws NoSuchAlgorithmException {
        final int keySize = 2048;
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);      
        return keyPairGenerator.genKeyPair();
    }

	private static byte[] encrypt(PublicKey key, byte[] data) throws InvalidKeyException,
	                                                                 IllegalBlockSizeException,
	                                                                 BadPaddingException,
	                                                                 NoSuchPaddingException,
	                                                                 NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);  

        return cipher.doFinal(data);  
    }
    
	private static byte[] decrypt(PrivateKey key, byte[] encrypted) throws NoSuchPaddingException, 
																		   InvalidKeyException,
																		   IllegalBlockSizeException,
																		   BadPaddingException,
																		   NoSuchAlgorithmException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        
        return cipher.doFinal(encrypted);
	}
	
	public void send(byte[] data) throws IOException, InvalidKeyException, SeriousError {
		// Wait for public key:
		ObjectInputStream ois = new ObjectInputStream(is);
		PublicKey publicKey;
		try {
			publicKey = (PublicKey)ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new SeriousError(e);
		}
		
		// Encrypt metadata with public key:
		Metadata metadata = new Metadata(data.length);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(metadata);;
		oos.close();
		byte[] encrypted;
		try {
			encrypted = encrypt(publicKey, baos.toByteArray());
		} catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException
				| NoSuchAlgorithmException e) {
			throw new SeriousError(e);
		}

		// Send encrypted metadata:
		os.write(encrypted);
		os.flush();
		
		// Data chunks must not be longer than 245 bytes
		int i, f;
		for (i = 0; i < data.length; i += 245) {
			f = Math.min(i + 245, data.length);
		
			// Wait for public key:
			ois = new ObjectInputStream(is);
			try {
				publicKey = (PublicKey)ois.readObject();
			} catch (ClassNotFoundException e) {
				throw new SeriousError(e);
			}
			
			// Encrypt with public key:
			try {
				encrypted = encrypt(publicKey, Arrays.copyOfRange(data, i, f));
			} catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException
					| NoSuchAlgorithmException e) {
				throw new SeriousError(e);
			}
			
			// Send encrypted data:
			os.write(encrypted);
			os.flush();
		}
	}
	
	public byte[] receive() throws IOException, SeriousError {

		ByteArrayOutputStream aos = new ByteArrayOutputStream();
		
		KeyPair keyPair;
		try {
			keyPair = buildKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new SeriousError(e);
		}
		
		// Send public key:
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(keyPair.getPublic());
		oos.flush();
		
		// Read encrypted metadata:
		byte[] encrypted = new byte[256];
		is.read(encrypted, 0, encrypted.length);
		
		// Decrypt metadata;
		byte[] data;
		try {
			data = decrypt(keyPair.getPrivate(), encrypted);
		} catch (NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| NoSuchAlgorithmException | InvalidKeyException e) {
			throw new SeriousError(e);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
		Metadata metadata;
		try {
			metadata = (Metadata)ois.readObject();
		} catch (ClassNotFoundException e) {
			throw new SeriousError(e);
		}
		ois.close();
		
		// Data is decrypted from chunks of 256 bytes.
		while (aos.size() < metadata.getDataLength()) {			
			try {
				keyPair = buildKeyPair();
			} catch (NoSuchAlgorithmException e) {
				throw new SeriousError(e);
			}
			
			// Send public key:
			oos = new ObjectOutputStream(os);
			oos.writeObject(keyPair.getPublic());
			oos.flush();
			
			// Read encrypted data:
			encrypted = new byte[256];
			int len = is.read(encrypted, 0, encrypted.length);
			if (len <= 0)
				break;
			
			try {
				data = decrypt(keyPair.getPrivate(), encrypted);
			} catch (NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
					| NoSuchAlgorithmException | InvalidKeyException e) {
				throw new SeriousError(e);
			}
			aos.write(data);
		}
		
		return aos.toByteArray();
	}
}
