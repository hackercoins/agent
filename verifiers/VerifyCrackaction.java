package verifiers;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

import core.Logger;
import cracker.Algorithms;
import storage.Store;

public class VerifyCrackaction {
	
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	private Algorithms algos = new Algorithms();

	/*
	 *  Verifies a crackaction [CRA # id # hash # salt # algorithm # unix_time # version # reward # from # pubKey # signature ]
	 */
	public synchronized boolean verify(String crackaction, Store store, boolean inBlock, Logger log) {

		try {
			
			String[] tokens = crackaction.split("#");
			if(tokens == null) return false;
			if(tokens.length != 11) return false;

			String start = tokens[0];
			
			if(!start.equals("CRA")) {
				
				log.addEntry("VerifyCrackaction>verify()", "The crackaction does not start with CRA.");
				return false;
			}
			
			String id = tokens[1];
			
			MessageDigest md1 = MessageDigest.getInstance("SHA-256");
			md1.update((tokens[2] + "#" + tokens[3] + "#" + tokens[4] + "#" + tokens[5] + "#" + tokens[6] + "#" + tokens[7]  + "#" + tokens[8]).getBytes()); 
			String id_computed = bytesToHex(md1.digest()).substring(0, 32);

			if(id.length() != 32 || !hexPattern.matcher(id).matches() || !id.equals(id_computed)) {

				log.addEntry("VerifyCrackaction>verify()", "The id of the crackaction is invalid: " + id);
				return false;
			}
			
			// check if id of this crackaction is already in the chain
			while(!store.blocks.claim()) { Thread.sleep(10); }
			boolean isInChain = store.blocks.isInChain_id(id);
			store.blocks.release();
			
			if(isInChain) {
				
				log.addEntry("VerifyCrackaction>verify()", "The id of the crackaction " + id + " is already in the chain.");
				return false;
			}
				
			String hash = tokens[2];
			if(hash.length() <= 0 || hash.length() > 128 || !hexPattern.matcher(hash).matches()) {
					
				log.addEntry("VerifyCrackaction>verify()", "The hash of the crackaction " + id + " is invalid.");
				return false;
			}
			
			String salt = tokens[3];
			if(salt.length() <= 0 || salt.length() > 128 || !hexPattern.matcher(salt).matches()) {
					
				log.addEntry("VerifyCrackaction>verify()", "The salt of the crackaction " + id + " is invalid.");
				return false;
			}

			if(!algos.isSupported(tokens[4])) {
					
				log.addEntry("VerifyCrackaction>verify()", "The algorithm of the crackaction " + id + " is not supported: " + tokens[2]);
				return false;
			}
			
			String strTS = tokens[5];
			if(strTS.length() != 10) {
				
				log.addEntry("VerifyCrackaction>verify()", "The timestamp of the crackaction " + id + " is invalid (1): " + strTS);
				return false;
			}
			
			int ts = 0;
			
			// if crackaction is already in a block, do not verify timestamp
			if(!inBlock) {
				
				try {
					
					ts = Integer.valueOf(strTS);
					if(ts < (getCurrentTime() - (60*60*6))) { // older than 6 hours
	
						log.addEntry("VerifyCrackaction>verify()", "The timestamp of the crackaction " + id + " is too old: " + ts + " (max 3 hours).");
						return false;
					}
					
					if(ts > (getCurrentTime() + 300)) { // manipulated
	
						log.addEntry("VerifyCrackaction>verify()", "The timestamp of the crackaction " + id + " might be manipulated: " + ts);
						return false;
					}
	
				} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyCrackaction>verify()", "The timestamp of the crackaction " + id + " is invalid (2): " + strTS); return false;}
			}
			
			String version = tokens[6];
			if(version.length() != 2 || !hexPattern.matcher(version).matches()) {
				
				log.addEntry("VerifyCrackaction>verify()", "The version of the crackaction " + id + " is invalid: " + version);
				return false;
			}
			
			String strValue = tokens[7];
			if(strValue.length() > 11 || strValue.length() <= 0) {
				
				log.addEntry("VerifyCrackaction>verify()", "The reward value of the crackaction " + id + " is invalid (1): " + strValue);
				return false;
			}
			
			double value = -1;
			try { value = Double.parseDouble(strValue);
			} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyCrackaction>verify()", "The reward value of the crackaction " + id + " is invalid (2): " + strValue); return false;}
	
			if(value < 0.0001d || value > 999999.0d) {
				
				log.addEntry("VerifyCrackaction>verify()", "The reward value of the crackaction " + id + " is invalid (3): " + value);
				return false;
			}
			
			// check if reward is high enough (3 coins is minimum)
			if(value < 3.0d) {
				
				log.addEntry("VerifyCrackaction>verify()", "The reward of " + value + " of the crackaction " + id + " is too small (3 coins are minimum).");
				return false;
			}
			
			String fromAddr = tokens[8];
			if(fromAddr.length() != 66 || !fromAddr.startsWith("HC") || !hexPattern.matcher(fromAddr.substring(2)).matches()) {
				
				log.addEntry("VerifyCrackaction>verify()", "The sender's address of the crackaction " + id + " is invalid: " + fromAddr);
				return false;
			}
			
			// also check if fromAddress is hash of used public key
			MessageDigest md2 = MessageDigest.getInstance("SHA-256");
			md2.update(tokens[9].getBytes()); 
			String fromAddr_computed = bytesToHex(md2.digest());
			
			if(!fromAddr.substring(2).equals(fromAddr_computed)) {
				
				log.addEntry("VerifyCrackaction>verify()", "The address fromAddr of the crackaction " + id + " is not the hash of the used public key.");
				return false;
			}

			// check if enough balance is available on this address starting from the given time stamp backwards
			if(ts > 0) {
			
				VerifyBalance verifyBalance = new VerifyBalance();
				
				if(!verifyBalance.verify(fromAddr, value, ts, store, log)) {
						
					log.addEntry("VerifyCrackaction>verify()", "The required balance for the reward of the crackaction " + id + " is not available on this address.");
					return false;
				}
			}

			// check if signature is correct
			try {
				
				byte[] decoded_pub = hexToBytes(tokens[9]);
				byte[] signature = hexToBytes(tokens[10]);
				
				KeyFactory keyFactory = KeyFactory.getInstance("EC");
		        X509EncodedKeySpec puKeySpec = new X509EncodedKeySpec(decoded_pub);
		        PublicKey p = keyFactory.generatePublic(puKeySpec);
		
				byte[] msg = (tokens[0] + "#" + tokens[1] + "#" + tokens[2] + "#" + tokens[3] + "#" + tokens[4] + "#" + tokens[5] + "#" + tokens[6] + "#" + tokens[7] + "#" + tokens[8] + "#" + tokens[9]).getBytes();
					
				Signature rsa = Signature.getInstance("SHA512withECDSA"); 
				rsa.initVerify(p); rsa.update(msg, 0, msg.length);
				
				if(rsa.verify(signature)) {
					return true;
					
				} else log.addEntry("VerifyCrackaction>verify()", "The signature of the crackaction " + id + " is invalid.");
			
			} catch (Exception e) {e.printStackTrace();}
		} catch (Exception e) {e.printStackTrace();}
		
		log.addEntry("VerifyCrackaction>verify()", "Could not add the crackaction: " + crackaction.substring(0, 64));
		return false;
	}

	/*
	 * Converts a byte array into a hexadecimal string.
	 */
	private String bytesToHex(byte[] bytes) {
		
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
	private byte[] hexToBytes(String s) {
		
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
