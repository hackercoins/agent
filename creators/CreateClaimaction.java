package creators;

import java.security.MessageDigest;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

/*
 * This class creates new claimaction (hidden or public). The parts are verified and a signature is computed. 
 * Claimactions are only created by the cracker in the background.
 */
public class CreateClaimaction implements Runnable {

	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	private String crackaction_id;
	private String solution;
	private String reward;
	private String fromAddr;

	private Store store;
	private Logger log;
	
	/*
	 *  Creates a HCL first and then waits for the next upcoming blocks in order to create the corresponding PCL.
	 *  [(HCL|PCL) # id # version # timestamp # crackaction_id # solution # reward # fromAddr # pubKey # signature ]
	 */
	public CreateClaimaction(String crackaction_id, String solution, String reward, String fromAddr, Store store, Logger log) {

		this.crackaction_id = crackaction_id;
		this.solution = solution;
		this.reward = reward;
		this.fromAddr = fromAddr;
		this.store = store;
		this.log = log;
	}

	@Override
	public void run() {
		
		if(crackaction_id.length() != 32 || !hexPattern.matcher(crackaction_id).matches()) {
			
			log.addEntry("CreateClaimaction>run()", "The crackaction id of the claimaction is invalid.");
			return;
		}
		
		if(!hexPattern.matcher(solution).matches()) {
			
			log.addEntry("CreateClaimaction>run()", "The solution of the claimaction is not in hexadecimal.");
			return;
		}
		
		// first send the hidden claimaction
		String hidden_solution = fromAddr + solution;
			
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update((hidden_solution).getBytes());
			hidden_solution = bytesToHex(md.digest());
				
		} catch (Exception e) { e.printStackTrace(); }

		if(fromAddr.length() != 66 || !fromAddr.startsWith("HC") || !hexPattern.matcher(fromAddr.substring(2)).matches()) {
			
			log.addEntry("CreateClaimaction>run()", "The claimaction has no correct address.");
			return ;
		}

		// build the string for the HCL
		String hidden_claimaction = "01#" + getCurrentTime() + "#" + crackaction_id + "#" + hidden_solution + "#0#" + fromAddr;
		
		// compute and add id
		String id = "";
		
		try {
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((hidden_claimaction).getBytes());
			id = bytesToHex(md.digest()).substring(0, 32);
			hidden_claimaction = id + "#" + hidden_claimaction;
			
		} catch (Exception e) { e.printStackTrace(); }
		
		hidden_claimaction = "HCL#" + hidden_claimaction;
		
		while(!store.wallet.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
		hidden_claimaction = store.wallet.getSignedString(fromAddr, hidden_claimaction);
		store.wallet.release();

		if(hidden_claimaction != null) {

			new Thread(new BroadCaster(store, "publishClaimaction:" + hidden_claimaction, log)).start();
			log.addEntry("CreateClaimaction>run()", "Successfully created a HCL width id " + id + " for crackaction " + crackaction_id + "." );
			
			// once the HCL is in the chain, create the corresponding PCL
			while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
			String latest_block = store.blocks.getMyLatestBlock();
			store.blocks.release();
			
			int count = 0;
			
			while(!(latest_block.contains("#" + hidden_solution + "#0#" + fromAddr + "#"))) {
				
				try {Thread.sleep(60 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
				
				while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
				latest_block = store.blocks.getMyLatestBlock();
				store.blocks.release();
				
				count++;
				
				if(count >= 300) {
					
					log.addEntry("CreateClaimaction>run()", "Could not find the created HCL on the blockchain after 6 hours waiting. Skipping." );
					return;
				}
			}
			log.addEntry("CreateClaimaction>run()", "Found the sent HCL with id " + id + " in block " + latest_block.substring(0, 32) + "..." );
			
			try {Thread.sleep(5 * 60 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
			
			String public_claimaction = "01#" + getCurrentTime() + "#" + crackaction_id + "#" + solution + "#" + reward + "#" + fromAddr;
			
			// compute new id
			
			try {
				
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update((public_claimaction).getBytes());
				id = bytesToHex(md.digest()).substring(0, 32);
				public_claimaction = id + "#" + public_claimaction;
				
			} catch (Exception e) { e.printStackTrace(); }
			
			public_claimaction = "PCL#" + public_claimaction;
			
			while(!store.wallet.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
			public_claimaction = store.wallet.getSignedString(fromAddr, public_claimaction);
			store.wallet.release();

			if(public_claimaction != null) {

				new Thread(new BroadCaster(store, "publishClaimaction:" + public_claimaction, log)).start();
				log.addEntry("CreateClaimaction>run()", "Successfully created the PCL with id " + id + " for crackaction " + crackaction_id + "." );
			}
		}
	}

	// Converts a byte array into a hexadecimal string.
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
	
	// returns a unix timetamp with precision to seconds.
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
