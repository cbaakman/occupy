package net.cbaakman.occupy.authenticate;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.xml.bind.DatatypeConverter;

import net.cbaakman.occupy.errors.SeriousError;

public class Authenticator {

	private File hashFile;
	
	public Authenticator(File hashFile) {
		this.hashFile = hashFile;
	}
	
	private static String getHash(Credentials credentials) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			throw new SeriousError(e);
		}
		
		digest.update(credentials.getPassword().getBytes());

        return DatatypeConverter.printHexBinary(digest.digest());
	}
	
	public void add(Credentials credentials) throws IOException {
		
		if (!hashFile.isFile())
			hashFile.createNewFile();
		
		RandomAccessFile in = new RandomAccessFile(hashFile, "rw");
		FileLock lock;
		try {
			lock = in.getChannel().lock();
		} catch (IOException e) {
			in.close();
			throw e;
		}
		
		try {
			Properties properties = new Properties();
			properties.load(Channels.newInputStream(in.getChannel()));
			
			properties.put(credentials.getUsername(), getHash(credentials));
			in.seek(0);
			properties.store(Channels.newOutputStream(in.getChannel()), "");
		}
		finally {
			lock.release();
			in.close();
		}
	}
	
	public boolean authenticate(Credentials credentials) throws IOException {

		if (!hashFile.isFile())
			return false;

		RandomAccessFile in = new RandomAccessFile(hashFile, "rw");
		FileLock lock;
		try {
			lock = in.getChannel().lock();
		} catch (IOException e) {
			in.close();
			throw e;
		}
		
		try {
			Properties properties = new Properties();
			properties.load(Channels.newInputStream(in.getChannel()));
			
			return properties.containsKey(credentials.getUsername()) &&
				   properties.get(credentials.getUsername()).equals(getHash(credentials));
		}
		finally {
			lock.release();
			in.close();
		}
	}
}
