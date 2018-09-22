package miner;

import java.security.MessageDigest;
import java.util.Random;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

/*
 * A miner thread is started by the miner and tries to find a valid proof-of-work hash regarding the current difficulty.
 * Once a valid proof-of-work is found, the miner thread signs the block based on the given address and returns.
 */
public class MinerThread implements Runnable {
	
	private Random rand = new Random();
	private Logger log = null;
	private Store store = null;
	
	private String targetString = null;
	private int difficulty = 0;
	private String mineAddr;

	private Pattern diffPattern = null;
	
	public String minedBlock = null;
	public boolean doMine = true;

	public MinerThread(String newBlock, int diff, String mineAddr, Logger log, Store store) {

		this.targetString = newBlock;
		this.difficulty = diff;
		this.mineAddr = mineAddr;
		this.log = log;
		this.diffPattern = Pattern.compile("[0-7]+");
		this.store = store;
	}

	@Override
	public void run() {

		try {
			
			String hash = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", nonce = "";
			String charset = "0123456789ABCDEF";
			MessageDigest digest = MessageDigest.getInstance("SHA-256");

			while(!diffPattern.matcher(hash.substring(0, difficulty)).matches()) {
				
				nonce = "";
				for(int i = 0; i < 64; i++)
					nonce += charset.charAt(rand.nextInt(charset.length()));

				hash = bytesToHex(digest.digest((targetString + "#" + nonce).getBytes()));
				if(!doMine) break;
			}

			if(diffPattern.matcher(hash.substring(0, difficulty)).matches()) {
			
				log.addEntry("MinerThread>run()", "Found valid nonce (" + nonce + ") for new block " + targetString.split("#")[0]);
				String newBlock = targetString + "#" + nonce + "#" + hash + "#" + difficulty;
				
				while(!store.wallet.claim()) Thread.sleep(10);
				String newBlock_signed = store.wallet.getSignedString(mineAddr, newBlock);
				store.wallet.release();
				
				minedBlock = newBlock_signed;
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
