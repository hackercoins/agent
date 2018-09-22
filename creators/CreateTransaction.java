package creators;

import java.math.RoundingMode;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

/*
 * This class creates new transactions. The parts are verified and a signature is computed.
 * A resulting error message is displayed in the gui.
 */
public class CreateTransaction {

	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	private Pattern subjectPattern = Pattern.compile("[A-Za-z0-9]+");
	
	// [TRA # id # from_addr # to_addr # amount # unix_time # subject # version # pubKey # signature ]
	public synchronized String create(String fromAddr, String toAddr, String subject, String strValue, Store store, Logger log) {

		if(fromAddr.length() != 66 || !fromAddr.startsWith("HC") || !hexPattern.matcher(fromAddr.substring(2)).matches())
			return "Error: The sender's address is not valid.";
			
		if(toAddr.length() != 66 || !toAddr.startsWith("HC") || !hexPattern.matcher(toAddr.substring(2)).matches())
			return "Error: The receiver's address is not valid.";

		if(toAddr.equals(fromAddr))
			return "Error: The receiver's address is the sender's address.";
		
		// get and verify subject
		if(subject.length() != 0)
			if(subject.length() > 16 || !subjectPattern.matcher(subject).matches())
				return "Error: The subject is invalid (max 16 [A-Za-z0-9]).";
			
		// get and verify value (max length 999999.1111
		if(strValue.length() > 11 || strValue.length() <= 0) 
			return "Error: The value is invalid (1): " + strValue;
		
		double value = -1;
		try { value = Double.parseDouble(strValue);
		} catch(Exception e) { e.printStackTrace(); return "The value is invalid (2): " + strValue; }

		if(value < 0.1d || value > 99999999.0d)
			return "The value is invalid (minimum is > 0.1): " + value;
		
		// check if required balance is available
		if(value > store.getMyBalance(fromAddr))
			return "Error: Currently, you don't have enough balance.";
		
		// build transaction string
		String transaction = fromAddr + "#" + toAddr + "#" + formatValue(value) + "#" + getCurrentTime() + "#" + subject + "#01";
		
		// compute and add id
		String id = "";
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((transaction).getBytes());
			id = bytesToHex(md.digest()).substring(0, 32);
			transaction = id + "#" + transaction;
			
		} catch (Exception e) { e.printStackTrace(); }
		
		transaction = "TRA#" + transaction;
		
		// add public key and signature
		while(!store.wallet.claim()) { try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
		transaction = store.wallet.getSignedString(fromAddr, transaction);
		store.wallet.release();
		
		if(transaction != null) {

			// save string to local transactions file
			new Thread(new BroadCaster(store, "publishTransaction:" + transaction, log)).start();
			return "Successfully sent transaction " + id + ".";
		}
		return "Error: Could not send the transaction.";
	}
	
	/* 
	 * This module formats the value used in transactions and crackactions.
	 * It's important to round down and not to round up (in order to avoid the creation of new coins).
	 */
	private String formatValue(double balance) {
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		String t = df.format(balance);
		return t.replace(",", ".").replace("-", "");
	}
	
	private int getCurrentTime() {
		
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
