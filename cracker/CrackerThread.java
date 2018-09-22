package cracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.MessageDigest;
import java.util.Random;

import core.Logger;

/*
 * The cracker thread tries to find a string (and salt) that results in the given hash.
 * Parameters are a hash, a salt, an algorithm, a charset, the maximum length of the string.
 * If a path to a wordlist if given, the wordlist is tried before brute force.
 */
public class CrackerThread implements Runnable {

	private Logger log = null;
	
	private String hash;
	private String salt;
	private String algorithm;
	private String charset;
	
	private int maxlength = 16;
	private Random rand = new Random();
	private String wordlist;
	
	public boolean doCrack = true;
	
	public String crackaction;
	public String solution;
	
	public boolean usesWordlist = true;
	
	public CrackerThread(String hash, String salt, String algorithm, String charset, int max, String crackaction, String wordlist, Logger log) {

		this.log = log;
		
		this.hash = hash;
		this.salt = salt;
		this.algorithm = algorithm;
		this.charset = charset;
		this.maxlength = max;
		
		this.wordlist = wordlist;
		this.crackaction = crackaction;
	}

	@Override
	public void run() {
	
		try {

			int length = 0;
			
			String password = "";
			MessageDigest md = MessageDigest.getInstance(algorithm);

			// first try a wordlist if given
			if(wordlist != null) {
				
				File wlist = new File(wordlist);
				
				if(wlist.exists()) {
					
					log.addEntry("CrackerThread>run()", "Starting to crack " + crackaction.substring(0, 32) + " with given wordlist: " + wlist.getAbsolutePath());	
					
					try {
						
						String inLine;
						BufferedReader in = new BufferedReader(new FileReader(wlist));
															
						while((inLine = in.readLine()) != null) {
			
							if(inLine.length() > 0) {
								
								password = inLine.trim() + salt;
								md.update((password).getBytes());
								String result = bytesToHex(md.digest());
										
								if(result.equals(hash)) {

									log.addEntry("CrackerThread>run()", "Found solution '" + password + "' for crackaction " + crackaction + ".");	
									solution = bytesToHex(password.getBytes());
									doCrack = false;
									break;
								}
							}
						}				
						in.close();
						
						log.addEntry("CrackerThread>run()", "Finished cracking " + crackaction.substring(0, 32) + " with given wordlist: " + wlist.getAbsolutePath());
					
					} catch (Exception e) {e.printStackTrace(); }
					
				} else log.addEntry("CrackerThreadWordlist>run()", "The given wordlist file does not exist (" + wordlist + ").");
			}
			
			usesWordlist = false;
			
			if(doCrack)
				log.addEntry("CrackerThread>run()", "Starting to brute-force the crackaction " + crackaction.substring(0, 32) + " with charset " + charset);	
			
			while(doCrack) {

				password = ""; 
				length = 1 + rand.nextInt(maxlength);

				for(int i = 0; i < length; i++)
					password += charset.charAt(rand.nextInt(charset.length()));
				
				password += salt;
				
				md.update((password).getBytes());
				String result = bytesToHex(md.digest());
						
				if(result.equals(hash)) {

					log.addEntry("CrackerThread>run()", "Found solution '" + password + "' for crackaction " + crackaction + ".");		
					solution = bytesToHex(password.getBytes());
					break;
				}
			}
		} catch (Exception e) {e.printStackTrace();}
	}
	
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
}
