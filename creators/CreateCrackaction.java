package creators;

import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import core.Logger;
import cracker.Algorithms;
import storage.Store;

/*
 * This class create a new crackaction. The parts are verified and a signature is computed.
 * A resulting error message is displayed in the gui.
 */
public class CreateCrackaction {

	private Algorithms algos = new Algorithms();
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	
	// [CRA # id # hash # salt # algorithm # unix_time # version # reward # from_addr # pubKey # signature ]
	public synchronized String create(String hash, String salt, String algorithm, String reward, String from, Store store, Logger log) {
		
		if(hash.length() <= 0 || hash.length() > 128 || !hexPattern.matcher(hash).matches())
			return "Error: The hash of the crackaction is invalid.";
		
		if(salt.length() <= 0 || salt.length() > 128 || !hexPattern.matcher(salt).matches())
			return "Error: The salt of the crackaction is invalid.";
		
		if(!algos.isSupported(algorithm))
			return "Error: The given algorithm is not supported.";
		
		if(reward.length() > 11 || reward.length() <= 0)
			return "Error: The defined reward value is invalid (1): " + reward;

		double value = -1;
		try { value = Double.parseDouble(reward);
		} catch(Exception e) { e.printStackTrace(); return "Error: The defined reward value is invalid (2): " + reward; }

		if(value < 3.0d)
			return "Error: The defined reward is invalid; minimum is 3 coins (3): " + value;

		// check if required balance is available
		if(value > store.getMyBalance(from))
			return "Error: Currently, you don't have enough balance on this address.";
		
		if(from.length() != 66 || !from.startsWith("HC") || !hexPattern.matcher(from.substring(2)).matches())
			return "Error: The sender's address is not valid.";

		if(hash.length() != algos.getLengthOf(algorithm))
			return "Error: The hash does not have the expected length of " + algos.getLengthOf(algorithm) + " for " + algorithm + " data.";
		
		// build crackaction string
		String crackaction = hash + "#" + salt + "#" + algorithm + "#" + getCurrentTime() + "#01#" + formatValue(value) + "#" + from;
		
		// compute and add id
		String id = "";
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((crackaction).getBytes());
			id = bytesToHex(md.digest()).substring(0, 32);
			crackaction = id + "#" + crackaction;
			
		} catch (Exception e) { e.printStackTrace(); }
		
		crackaction = "CRA#" + crackaction;
			
		// add public key and signature
		while(!store.wallet.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
		crackaction = store.wallet.getSignedString(from, crackaction);
		store.wallet.release();

		if(crackaction != null) {

			new Thread(new BroadCaster(store, "publishCrackaction:" + crackaction, log)).start();
			return "Successfully sent the crackaction " + id + ".";
		}
		return "Error: Could not send the cracksaction.";
	}
	
	/* 
	 * This module formats the value used in transactions and crackactions.
	 * It's important to round down and not to round up (in order to avoid the creation of new coins).
	 */
	public synchronized String formatValue(double balance) {
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		String t = df.format(balance);
		return t.replace(",", ".").replace("-", "");
	}
	
	/*
	 * Returns a unix timetamp with precision to seconds.
	 */
	public synchronized int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
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
}
