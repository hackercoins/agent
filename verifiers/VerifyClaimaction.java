package verifiers;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

public class VerifyClaimaction {
	
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");

	/*
	 *  Verifies tokens and signature of a hidden or public claimaction [(H|P)CL # id # version # ts # crackaction_id # solution # reward # from_addr # pubKey # signature ]
	 *  If its a public claimaction, the solution needs to be verified as well (depending on previous crackaction and previous hidden claimaction)
	 */
	public synchronized boolean verify(String claimaction, Store store, boolean inBlock, Logger log) {
		
		boolean isHidden = true;
		
		try {
		
			String[] tokens = claimaction.split("#");
			
			if(tokens == null) {
				log.addEntry("VerifyClaimaction>verify()", "The claimaction does not have any tokens.");
				return false; 
			}
			
			if(tokens.length != 10) {

				log.addEntry("VerifyClaimaction>verify()", "The claimaction does not have ten tokens.");
				return false;
			}
			
			String start = tokens[0];
			if(start.equals("HCL")) isHidden = true;
			else if(start.equals("PCL")) isHidden = false;
			else {
				
				log.addEntry("VerifyClaimaction>verify()", "The claimaction does not start with HCL or PCL.");
				return false;
			}

			// verify the id
			String id = tokens[1];
			MessageDigest md1 = MessageDigest.getInstance("SHA-256");
			md1.update((tokens[2] +"#"+ tokens[3] +"#"+ tokens[4] +"#"+ tokens[5] +"#"+ tokens[6] +"#"+ tokens[7]).getBytes()); 
			String id_computed = bytesToHex(md1.digest()).substring(0, 32);
			
			if(id.length() != 32 || !hexPattern.matcher(id).matches() || !id.equals(id_computed)) {
				
				log.addEntry("VerifyClaimaction>verify()", "The id of the claimaction is invalid: " + id);
				return false;
			}
			
			// verify if the id of this id is already in the chain (this goes back latest 512 blocks maximum)
			while(!store.blocks.claim()) Thread.sleep(10);
			boolean isInChain = store.blocks.isInChain_id(id);
			store.blocks.release();
			
			if(isInChain) {
				
				log.addEntry("VerifyClaimaction>verify()", "The id of the claimaction " + id + " is already in the chain.");
				return false;
			}

			// verify the version; this can be used later in order to deploy updates
			String version = tokens[2];
			if(version.length() != 2 || !hexPattern.matcher(version).matches()) {
				
				log.addEntry("VerifyClaimaction>verify()", "The version of the claimaction " + id + " is invalid: " + version);
				return false;
			}
			
			String strTS = tokens[3];
			if(strTS.length() != 10) {
				
				log.addEntry("VerifyClaimaction>verify()", "The timestamp of the claimaction " + id + " is invalid (1): " + strTS);
				return false;
			}

			int ts = 0;
			
			// if the claimaction is not in a block, it cannot be older than 6 hours
			if(!inBlock) {
			
				try {
					
					ts = Integer.valueOf(strTS);
					
					if(ts < (getCurrentTime() - (60*60*6))) { // older than 6 hours and not mined to a block; this is not allowed
						
						log.addEntry("VerifyClaimaction>verify()", "The timestamp of the claimaction " + id + " is too old: " + ts);
						return false;
					}
					
					if(ts > (getCurrentTime() + 600)) { // seems to be manipulated (10 minutes too early)
	
						log.addEntry("VerifyClaimaction>verify()", "The timestamp of the claimaction " + id + " seems to be manipulated: " + ts);
						return false;
					}
					
				} catch(Exception e) { e.printStackTrace(); System.out.println("VerifyClaimaction>verify(): The timestamp of the claimaction " + id + " is invalid (2): " + strTS); return false; }
			}
			
			// verify format of the crackaction id
			String crackaction_id = tokens[4];
			if(crackaction_id.length() != 32 || !hexPattern.matcher(crackaction_id).matches()) {
				
				log.addEntry("VerifyClaimaction>verify()", "The crackaction id of the claimaction " + id + " is invalid.");
				return false;
			}
			
			// reward is only used in public claimaction, there is needs to be the reward of the crackaction it references, in hidden claimaction it is 0
			String reward = tokens[6];
			
			if(isHidden) {
				
				if(!reward.equals("0")) {
					
					log.addEntry("VerifyClaimaction>verify()", "The reward field of the HCL is not 0.");
					return false;
				}
			}

			// verify the format of the creator's address
			String fromAddr = tokens[7];
			if(fromAddr.length() != 66 || !fromAddr.startsWith("HC") || !hexPattern.matcher(fromAddr.substring(2)).matches()) {
				
				log.addEntry("VerifyClaimaction>verify()", "The creator's address of the claimaction " + id + " is invalid: " + fromAddr);
				return false;
			}
			
			// also verify that the creator's address is the hash of public key that is used in the signature
			MessageDigest md2 = MessageDigest.getInstance("SHA-256");
			md2.update(tokens[8].getBytes()); 
			String fromAddr_computed = bytesToHex(md2.digest());
			
			if(!fromAddr.substring(2).equals(fromAddr_computed)) {
				
				log.addEntry("VerifyClaimaction>verify()", "The creator's adddress of the claimaction " + id + " is not the hash of the used public key.");
				return false;
			}
			
			// check if enough balance is available on this address to pay the fee of 0.5 coins (HCL and PCL need to cost fee for mining)
			if(ts > 0) {
				
				VerifyBalance verifyBalance = new VerifyBalance();
				
				if(!verifyBalance.verify(fromAddr, 0.5d, ts, store, log)) {
						
					log.addEntry("VerifyClaimaction>verify()", "The required balance of 0.5 for the claimaction " + id + " is not available on the used address.");
					return false;
				}
			}
			
			String solution = tokens[5];
			
			if(isHidden) { // if this is a HCL only verify format of hashed solution
			
				// verify hash of address+solution (sha512)
				if(solution.length() != 128 || !hexPattern.matcher(solution).matches()) {
					
					log.addEntry("VerifyClaimaction>verify()", "The hidden solution of the claimaction " + id + " is invalid.");
					return false;
				}
			
			// if this is a PCL, retrieve HCL and CRA from chain and verify solution and reward (go from ts of PCL max 12 hours backwards in the chain)
			// also check if there is already another valid PCL; if yes do not add
			} else {
				
				if(hexPattern.matcher(solution).matches() && solution.length() <= 128) {
					
					// search for the crackaction and retrieve these four tokens
					String crackaction_hash = null;
					String crackaction_salt = "";
					String crackaction_algo = null;
					String crackaction_reward = null;
					
					// search for the oldest HCL and verify address and the hidden solution
					String hidden_claimaction_solution = null;
					
					while(!store.blocks.claim()) Thread.sleep(10);
					long blockCount = store.blocks.getMyLatestBlockCount();
					store.blocks.release();
					
					boolean proceed = true;
					
					// go up to 12 hours back through the chain until the crackaction or another PCL targeting the crackaction was found
					while(blockCount >= 0 && proceed) {
							
						String latest_hidden_claimaction_solution = null;
						
						while(!store.blocks.claim()) Thread.sleep(10);
						String block = store.blocks.getBlock(blockCount);
						store.blocks.release();
						
						String[] block_tokens = block.split("#");
						//log.addEntry("VerifyClaimaction>verify()", "Checking block: " + block_tokens[0]);
							
						// skip if current block is older than 12 hours from the timestamp of the PCL
						int block_ts = Integer.parseInt(block_tokens[3]);
						if((ts - block_ts) > (60*60*12)) {
								
							log.addEntry("VerifyClaimaction>verify(PCL)", "Investigated the blockchain 12 hours backwards. Now stopping: Could not add.");
							return false;
						}
							
						// this block contains either the crackaction or a HCL/PCL targeting the crackaction
						if(block.contains("#" + crackaction_id + "#")) {
							
							log.addEntry("VerifyClaimaction>verify()", "The block contains the target crackaction id...");
							
							for(int i = 4; i < block_tokens.length-3; i++) {
									
								if(block_tokens[i].equals(crackaction_id)) {
										
									// this is another valid PCL; we can break here
									if(block_tokens[i-4].equals("PCL")) {
											
										log.addEntry("VerifyClaimaction>verify(PCL)", "Found another PCL for crackaction " + crackaction_id + " in block " + block_tokens[0] + ". Skipping...");
										return false;
									}
										
									// this is the required HCL, needs to be the latest HCL right after the crackaction, so continuously replace it until crackaction was found
									if(block_tokens[i-4].equals("HCL")) {

										log.addEntry("VerifyClaimaction>verify(PCL)", "Found a HCL for the " + crackaction_id + " in block " + block_tokens[0] + ".");
											
										if(block_tokens[i+3].equals(fromAddr)) {
												
											log.addEntry("VerifyClaimaction>verify(PCL)", "This is the required HCL from same address like the PCL " + block_tokens[i+3] + ".");
											latest_hidden_claimaction_solution = block_tokens[i+1];
										}
									}
										
									// this is the required crackaction
									if(block_tokens[i-1].equals("CRA")) {
											
										hidden_claimaction_solution = latest_hidden_claimaction_solution;

										crackaction_hash = block_tokens[i+1];
											
										if(block_tokens[i+2].equals("0")) crackaction_salt = ""; 
										else crackaction_salt = block_tokens[i+2];
											
										crackaction_algo = block_tokens[i+3];
										crackaction_reward = block_tokens[i+6];
											
										log.addEntry("VerifyClaimaction>verify(PCL)", "Finally, found crackaction with matching id in block " + block_tokens[0] + ".");
										proceed = false; 
										break;
									}
								}
							}
						}
						blockCount--;
					}
										
					// finally, verify the entire solution
					if(crackaction_hash != null && crackaction_algo != null && crackaction_reward != null && hidden_claimaction_solution != null) {
						
						log.addEntry("VerifyClaimaction>verify(PCL)", "Verifying the entire solution...");
						
						// check public solution
						MessageDigest md3 = MessageDigest.getInstance(crackaction_algo);
						
						String sol = new String(hexToBytes(solution));
						if(crackaction_salt.length() > 0) {
							sol += new String(hexToBytes(crackaction_salt));
						}

						md3.update(sol.getBytes());
						String computed_hash = bytesToHex(md3.digest());
						
						if(!crackaction_hash.equals(computed_hash)) {
							
							log.addEntry("VerifyClaimaction>verify(PCL)", "The solution of the PCL " + id + " is not correct.");
							return false;
						}
						
						// check hidden solution
						MessageDigest md4 = MessageDigest.getInstance("SHA-512");
						md4.update((fromAddr+solution).getBytes()); // no need to convert from hex to string
						String hidden_computed_solution = bytesToHex(md4.digest());
						
						if(!hidden_claimaction_solution.equals(hidden_computed_solution)) {
							
							log.addEntry("VerifyClaimaction>verify(PCL)", "The solution of the HCL belonging to the PCL " + id + " is not correct.");
							return false;
						}
						
						// the reward needs to be the same as the reward in the crackaction; this simplifies computing the balance for an address
						if(!reward.equals(crackaction_reward)) {
							
							log.addEntry("VerifyClaimaction>verify(PCL)", "The reward of the PCL " + id + " is not the reward of the crackaction.");
							return false;
						}
						
						log.addEntry("VerifyClaimaction>verify()", "The given hidden solution of the HCL matches.");
					}
				}
			}
			
			// finally verify the signature of the claimaction (of course the signature also includes the public key)
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
					
					//log.addEntry("VerifyClaimaction>verify()", "Successfully verified the signature of the claimaction.");
					return true;
					
				} else log.addEntry("VerifyClaimaction>verify()", "The signature of the claimaction " + id + " is invalid.");
			
			} catch (Exception e) {e.printStackTrace();}
		} catch (Exception e) {e.printStackTrace();}

		log.addEntry("VerifyClaimaction>verify()", "Could not add the claimaction with id " + claimaction.substring(0, 64) + "...");
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
