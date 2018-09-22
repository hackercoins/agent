package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedList;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class WalletStore {
	
	private volatile boolean isFree = true; 
	private File target = new File("wallet");
	
	private String walletPassword = "";
	private LinkedList<String> currentWallet = new LinkedList<String>();
	
	public WalletStore(String p) {
		
		this.walletPassword = p;
	}
	
	public synchronized boolean claim() {
	
		if(isFree) {
			
			isFree = false;
			return true;
		}
		return false;
	}
	
	public synchronized void release() {
		
		isFree = true;
	}
	
	public void addToWallet(String s) {

		try {
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(target, true));
			pw.println(encryptString(s));
			pw.close();
			
			updateWalletEntries();
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void updateWalletEntries() {
		
		currentWallet.clear();
		
		if(target.exists()) {
		
			try {
	
				String inLine;
				BufferedReader in = new BufferedReader(new FileReader(target));
				
				while((inLine = in.readLine()) != null) {
	
					if(inLine.length() > 0) {
						
						String tmp = decryptString(inLine);
						currentWallet.add(tmp);
					}
				}				
				in.close();
				
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	/*
	 * Returns all locally stored addresses in the wallet-file.
	 */
	public LinkedList<String> getMyAddresses() {
		
		LinkedList<String> tmp = new LinkedList<String>();
		
		try {
		
			for(Object o : currentWallet) {
				
				String item = (String) o;
				tmp.add(item.split("#")[0]);
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		return tmp;
	}
	
	/*
	 * Returns the public and the private key of a specific address.
	 * This is used in the helper class to create a signature.
	 */
	public synchronized String[] getKeysOfAddress(String addr) {
		
		String[] tokens = new String[2];
		
		try {
			
			for(String item : currentWallet) {	
				
				if(item.startsWith(addr + "#")) {
					
					String[] tmp = item.split("#");
					tokens[0] = tmp[1];
					tokens[1] = tmp[2];
					break;
				}
			}
		
		} catch (Exception e) { e.printStackTrace(); }
		return tokens;
	}
	
	/* 
	 * Appends the corresponding public key and the computed signature (encoded in hex) to a target string.
	 */
	public String getSignedString(String addr, String content) {
		
		try {
			
			String[] keys = getKeysOfAddress(addr);
						
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			PKCS8EncodedKeySpec prKeySpec = new PKCS8EncodedKeySpec(hexToBytes(keys[1]));
			PrivateKey priv = keyFactory.generatePrivate(prKeySpec);
			content = content + "#" + keys[0]; // add public key

			if(priv != null) {
			
				Signature ecdsa = Signature.getInstance("SHA512withECDSA"); 
				ecdsa.initSign(priv);
				
				byte[] bytes = content.getBytes();
				ecdsa.update(bytes, 0, bytes.length);
		
				byte[] realSig = ecdsa.sign();
				return content + "#" + bytesToHex(realSig);
			}

		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	/*
	 *  Used to encrypt entries in the local wallet (AES256).
	 */
	private synchronized String encryptString(String s) {

		try {

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(walletPassword.toCharArray(), "Pepper".getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secret);
			AlgorithmParameters params = cipher.getParameters();
			
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
			byte[] ciphertext = cipher.doFinal(s.getBytes("UTF-8"));
			
			byte[] encoded1 = Base64.getEncoder().encode(iv);
			byte[] encoded2 = Base64.getEncoder().encode(ciphertext);
			
			return new String(encoded1) + "#" + new String(encoded2);

		} catch(Exception e) { e.printStackTrace(); }
		return null;
	}
	
	/*
	 *  Used to decrypt entries in the local wallet (AES256).
	 */
	private synchronized String decryptString(String s) {
		
		try {
			
			String[] tokens = s.split("#");
			byte[] iv = Base64.getDecoder().decode(tokens[0].getBytes());
			byte[] ciphertext = Base64.getDecoder().decode(tokens[1].getBytes());
			
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(walletPassword.toCharArray(), "Pepper".getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			return new String(cipher.doFinal(ciphertext), "UTF-8");

		} catch(Exception e) { e.printStackTrace(); }
		return null;
	}
	
	/*
	 * Converts a byte array into a hexadecimal string.
	 */
	public synchronized String bytesToHex(byte[] bytes) {
		
		char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    
	    for ( int j = 0; j < bytes.length; j++ ) {
	    	
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	/*
	 * Converts a hexadecimal string into a byte array.
	 */
	public synchronized byte[] hexToBytes(String s) {
		
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
}
