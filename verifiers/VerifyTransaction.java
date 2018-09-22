package verifiers;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

public class VerifyTransaction {
	
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	private Pattern subjectPattern = Pattern.compile("[A-Za-z0-9]+");

	/*
	 *  Verifies tokens and signature of a transaction [TRA # id # from_addr # to_addr # amount # unix_time # subject # version # pubKey # signature ]
	 *  If the transactions is not yet mined to a block, it is valid for 3 hours. The balance is verified starting from the time stamp of the transaction backwards.
	 */
	public synchronized boolean verify(String transaction, Store store, boolean inBlock, Logger log) {
		
		try {

			String[] tokens = transaction.split("#");
			
			if(tokens == null) {
				log.addEntry("VerifyTransaction>verify()", "The transaction does not have any tokens.");
				return false; 
			}
			
			if(tokens.length != 10) {

				log.addEntry("VerifyTransaction>verify()", "The transaction does not have nine tokens.");
				return false;
			}
			
			String start = tokens[0];
			if(!start.equals("TRA")) {
				
				log.addEntry("VerifyTransaction>verify()", "The transaction does not start with TRA.");
				return false;
			}
			
			// verify the id
			String id = tokens[1];
			MessageDigest md1 = MessageDigest.getInstance("SHA-256");
			md1.update((tokens[2] +"#"+ tokens[3] +"#"+ tokens[4] +"#"+ tokens[5] +"#"+ tokens[6] +"#"+ tokens[7]).getBytes()); 
			String id_computed = bytesToHex(md1.digest()).substring(0, 32);
			
			if(id.length() != 32 || !hexPattern.matcher(id).matches() || !id.equals(id_computed)) {
				
				log.addEntry("VerifyTransaction>verify()", "The id of the transaction is invalid: " + id);
				return false;
			}
			
			// verify if the id of this transaction is already in the chain (this goes back latest 512 blocks maximum)
			while(!store.blocks.claim()) Thread.sleep(10);
			boolean isInChain = store.blocks.isInChain_id(id);
			store.blocks.release();
			
			if(isInChain) {
				
				//log.addEntry("VerifyTransaction>verify()", "The id of the transaction " + id + " is already in the chain.");
				return false;
			}
			
			// verify the format of the sender's address
			String fromAddr = tokens[2];
			if(fromAddr.length() != 66 || !fromAddr.startsWith("HC") || !hexPattern.matcher(fromAddr.substring(2)).matches()) {
				
				log.addEntry("VerifyTransaction>verify()", "The sender's address of the transaction " + id + " is invalid: " + fromAddr);
				return false;
			}
			
			// also verify if the sender's address is the hash of public key that is used for signature
			MessageDigest md2 = MessageDigest.getInstance("SHA-256");
			md2.update(tokens[8].getBytes()); 
			String fromAddr_computed = bytesToHex(md2.digest());
			
			if(!fromAddr.substring(2).equals(fromAddr_computed)) {
				
				log.addEntry("VerifyTransaction>verify()", "The sender's adddress of the transaction " + id + " is not the hash of the used public key.");
				return false;
			}

			// verify the format of the receiver's address
			String toAddr = tokens[3];
			if(toAddr.length() != 66 || !toAddr.startsWith("HC") || !hexPattern.matcher(toAddr.substring(2)).matches()) {
				
				log.addEntry("VerifyTransaction>verify()", "The receiver's address of the transaction " + id + " is invalid: " + toAddr);
				return false;
			}
			
			// sending coins among the same addresses is not allowed
			if(fromAddr.equals(toAddr)) {
				
				log.addEntry("VerifyTransaction>verify()", "The sender's address of the transaction " + id + " equals the receiver's address. This is not allowed.");
				return false;
			}

			// verify the format of the coins in this transaction
			String strValue = tokens[4]; // 0.1 - 999999.9999
			if(strValue.length() > 11 || strValue.length() <= 0) {
				
				log.addEntry("VerifyTransaction>verify()", "The value of the transaction " + id + " is invalid (1): " + strValue);
				return false;
			}

			double value = -1;
			try { value = Double.parseDouble(strValue);
			} catch(Exception e) { e.printStackTrace(); System.out.println("verifyTransaction(): The value of the transaction " + id + " is invalid (2): " + strValue); return false; }
			
			if(value < 0.1d) {
				
				log.addEntry("VerifyTransaction>verify()", "The value of the transaction " + id + " is too small (minimum is 0.1)");
				return false;
			}

			String strTS = tokens[5];
			if(strTS.length() != 10) {
				
				log.addEntry("VerifyTransaction>verify()", "The timestamp of the transaction " + id + " is invalid (1): " + strTS);
				return false;
			}

			int ts = 0;
			
			// if the transaction is not in a block, it can not be older than 6 hours
			if(!inBlock) {
			
				try {
					
					ts = Integer.valueOf(strTS);
					
					if(ts < (getCurrentTime() - (60*60*6))) { // older than 6 hours and not mined to a block; this is not allowed
						
						log.addEntry("VerifyTransaction>verify()", "The timestamp of the transaction " + id + " is too old: " + ts);
						return false;
					}
					
					if(ts > (getCurrentTime() + 600)) { // seems to be manipulated (10 minutes too early)
	
						log.addEntry("VerifyTransaction>verify()", "The timestamp of the transaction " + id + " seems to be manipulated: " + ts);
						return false;
					}
					
				} catch(Exception e) { e.printStackTrace(); System.out.println("Verfifier>verifyTransaction(): The timestamp is invalid (2): " + strTS); return false; }
			}

			// check if enough balance is available on this address starting backwards from the used time stamp
			if(ts > 0) {
			
				VerifyBalance verifyBalance = new VerifyBalance();
				
				if(!verifyBalance.verify(fromAddr, value, ts, store, log)) {
						
					log.addEntry("VerifyTransaction>verify()", "The required balance of the transaction " + id + " is not available on the used address.");
					return false;
				}
			}

			// verify the format of the subject
			String subject = tokens[6];
			
			if(subject.length() > 0) {
				if(subject.length() > 16 || !subjectPattern.matcher(subject).matches()) {
					
					log.addEntry("VerifyTransaction>verify()", "The subject of the transaction " + id + " is too long or invalid: " + subject);
					return false;
				}
			}

			// verify the given version; this can be used later in order to deploy updates
			String version = tokens[7];
			if(version.length() != 2 || !hexPattern.matcher(version).matches()) {
				
				log.addEntry("VerifyTransaction>verify()", "The version of the transaction " + id + " is invalid: " + version);
				return false;
			}

			// finally verify the signature of the transaction (of course this also includes the public key)
			try {
			
				byte[] decoded_pub = hexToBytes(tokens[8]);
				byte[] signature = hexToBytes(tokens[9]);
				
				KeyFactory keyFactory = KeyFactory.getInstance("EC");
		        X509EncodedKeySpec puKeySpec = new X509EncodedKeySpec(decoded_pub);
		        PublicKey p = keyFactory.generatePublic(puKeySpec);
		
				byte[] msg = (tokens[0] + "#" + tokens[1] + "#" + tokens[2] + "#" + tokens[3] + "#" + tokens[4] + "#" + tokens[5] + "#" + tokens[6] + "#" + tokens[7] + "#" + tokens[8]).getBytes();
					
				Signature rsa = Signature.getInstance("SHA512withECDSA"); 
				rsa.initVerify(p); rsa.update(msg, 0, msg.length);
				
				if(rsa.verify(signature)) {
					return true;
					
				} else log.addEntry("VerifyTransaction>verify()", "The signature of the transaction " + id + " is invalid.");
			
			} catch (Exception e) {e.printStackTrace();}
		} catch (Exception e) {e.printStackTrace();}
		
		log.addEntry("VerifyTransaction>verify()", "Could not add the transaction: " + transaction.substring(0, 64));
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
