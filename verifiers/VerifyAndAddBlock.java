package verifiers;

import java.math.RoundingMode;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.regex.Pattern;

import core.Logger;
import storage.Store;

/*
 * A block can have between 10 (min) and 100 (max) transactions, crackactions and claimactions. A block needs to have at least 50% transactions.
 * The reward of a block is 0.1 coin per transaction, 1 coin per crackaction (fee) and 0.5 coin per claimaction (hidden or public).
 * These values are the fee. Accordingly, the minimum reward is 1 coin and the maximum reward is 50 coins + 5 coins = 55 coins (with 50 transactions and 50 crackactions).
 */
public class VerifyAndAddBlock {
	
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");

	// [count # version # toAddr # ts # PoW_prev_block # actions_count # transactions/crackactions[...] # nonce (32) # PoW # difficulty # pubKey # signature]
	public synchronized boolean verify(String block, Store store, Logger log) {

		try {
			
			String[] tokens = block.split("#");
			
			if(tokens == null) {
				log.addEntry("VerifyAndAddBlock>verify()", "Invalid block structure (1).");
				return false;
			}
			if(tokens.length < (11 + 10 * 10)) { // contains 10 transactions/claimactions with 10 tokens
				log.addEntry("VerifyAndAddBlock>verify()", "Invalid block structure (2).");
				return false;
			}
			
			if(tokens.length > (11 + 50 * 11 + 50 * 10)) { // contains 50 crackactions with 11 tokens and 50 transactions with 10 tokens at maximum 
				log.addEntry("VerifyAndAddBlock>verify()", "Invalid block structure (3).");
				return false;
			}
			
			String countStr = tokens[0];
			long count = -1;
			try { count = Long.parseLong(countStr);
			} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "The block count of the block " + count + " is not valid (1)."); return false; }
			
			if(count < 0L) {
				log.addEntry("VerifyAndAddBlock>verify()", "The count of the block " + count + " is not valid (2)."); 
				return false;
			}
			
			// get previous block to check if the count of this block is count+1 in the chain (and get previous PoW and previous timestamp)
			String previousHash_orig = null;
			
			int previous_timestamp = -1;
			
			while(!store.blocks.claim()) Thread.sleep(10);
			String latestBlockStr = store.blocks.getMyLatestBlock();
			store.blocks.release();
			
			if(latestBlockStr != null) {
					
				String[] latestBlock = latestBlockStr.split("#");
					
				long last_count = Long.parseLong(latestBlock[0]);
				previousHash_orig = latestBlock[latestBlock.length-4];
				
				// check if count of last block fits
				if(last_count != count-1) {
						
					log.addEntry("VerifyAndAddBlock>verify()", "The block count of the new block (" + count + ") does not fit to local chain (latest " + last_count + "); it might be already solved."); 
					return false;
				}
			
				previous_timestamp = Integer.valueOf(latestBlock[3]);
				
			} else {
				
				if(count != 0) {
					
					log.addEntry("VerifyAndAddBlock>verify()", "Only the first block 0 can be added without verification of the relation to the previous block."); 
					return false;
				}
			}
			
			String version = tokens[1];
			if(version.length() != 2 || !hexPattern.matcher(version).matches()) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "The version of the block " + count + " is invalid: " + version);
				return false;
			}
			
			String toAddr = tokens[2];
			if(toAddr.length() != 66 || !toAddr.startsWith("HC") || !hexPattern.matcher(toAddr.substring(2)).matches()) {
				log.addEntry("VerifyAndAddBlock>verify()", "The miner's address of the block " + count + " is not valid."); 
				return false;
			}

			// also check if toAddress is hash of used public key
			MessageDigest md1 = MessageDigest.getInstance("SHA-256");
			md1.update(tokens[tokens.length-2].getBytes()); 
			String toAddr_computed = bytesToHex(md1.digest());
			
			if(!toAddr.substring(2).equals(toAddr_computed)) {
				log.addEntry("VerifyAndAddBlock>verify()", "The miner's address of the block " + count + " is not the hash of the used public key.");
				return false;
			}
			
			String strTS = tokens[3];
			if(strTS.length() != 10) {
				log.addEntry("VerifyAndAddBlock>verify()", "The timestamp of the block " + count + " is invalid (1).");
				return false;
			}

			int ts = 0;
			
			try {ts = Integer.valueOf(strTS);
			} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "The timestamp of the block " + count + " is invalid (2): " + strTS); return false; }
			
			// verify if time stamp of this block is bigger than the time stamp of the last local block
			if(count > 0) {
				try {
					
					if(ts <= previous_timestamp) {
						
						log.addEntry("VerifyAndAddBlock>verify()", "The timestamp of the latest local block is not before the timestamp of this new block " + count + ".");
						return false;
					}
	
				} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "Could not retrieve the timestamp of the latest local block."); return false; }
			}

			String previousHash = tokens[4];

			if(previousHash.length() != 64 || !hexPattern.matcher(previousHash).matches()) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "The given previous PoW-hash of the block " + count + " is not valid (1)."); 
				return false;
			}
			
			if(count > 0) { // do not check the genesis block
				
				// get PoW hash of block count-1
				if(previousHash_orig == null) {
					
					log.addEntry("VerifyAndAddBlock>verify()", "The given previous PoW-hash of the block " + count + " is not valid (2).");
					return false;	
				}
				
				if(!previousHash_orig.equals(previousHash)) {
					
					log.addEntry("VerifyAndAddBlock>verify()", "The given previous PoW-hash of the block " + count + " is not valid (3).");
					return false;
				}
			}
			
			String actionCountStr = tokens[5];
			int actionCount = -1;
			try { actionCount= Integer.parseInt(actionCountStr);
			} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "The action count of the block " + count + " is not valid (1)."); return false; }
			
			if(actionCount < 10 || actionCount > 100) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "The action count of the block " + count + " is not valid (2)."); 
				return false;
			}

			// go through the block and save my stuff separately
			LinkedList<String> mytransactions = new LinkedList<String>();
			LinkedList<String> mycrackactions = new LinkedList<String>();
			
			LinkedList<String> myclaimactions_hidden = new LinkedList<String>();
			LinkedList<String> myclaimactions_public = new LinkedList<String>();
			LinkedList<String> myclaimactions_solvers = new LinkedList<String>();
			
			LinkedList<String> ids = new LinkedList<String>();
			
			int transactionCount = 0; // at least 50%
			int crackactionCount = 0;
			int claimactionCount = 0;
			
			// verify available balance in sum; maybe multiple actions from the same address are contained in this block
			HashMap<String, Double> actions_sums = new HashMap<String, Double>();
			
			while(!store.wallet.claim()) Thread.sleep(10);
			LinkedList<String> myaddresses = store.wallet.getMyAddresses();
			store.wallet.release();
			
			while(!store.mycrackactions.claim()) Thread.sleep(10);
			LinkedList<String> mycrackactions_stored = store.mycrackactions.getItems();
			store.mycrackactions.release();

			int position = 6; // this position must be the id of the first action
				
			try {
				
				VerifyTransaction verifyTransaction = new VerifyTransaction();
				VerifyCrackaction verifyCrackaction = new VerifyCrackaction();
				VerifyClaimaction verifyClaimaction = new VerifyClaimaction();
				
				while(position < tokens.length - 7) {
						
					// this is a transaction
					if(tokens[position].equals("TRA")) {
						
						transactionCount++;
						
						String tmp_transaction = "";
						for(int a = 0; a < 10; a++)
							tmp_transaction += tokens[position+a] + "#";
						tmp_transaction = tmp_transaction.substring(0, tmp_transaction.length()-1);
							
						if(count > 0) {
							if(!verifyTransaction.verify(tmp_transaction, store, true, log)) {
									
								log.addEntry("VerifyAndAddBlock>verify()", "Could not verify a transaction (" + tokens[position+1] + ") in the block " + count + " starting at token " + position + "."); 
								return false;
							}
						}
								
						// add id
						ids.add(tokens[position+1]);
							
						// check if this transaction is from or to one of my addresses
						for(int j = 0; j < myaddresses.size(); j++) {
							if(tokens[position+2].equals(myaddresses.get(j)) || tokens[position+3].equals(myaddresses.get(j))) {
								mytransactions.add(count + "#" + tmp_transaction);
								break;
							}
						}
							
						// for verification of sum of actions from same address
						if(actions_sums.containsKey(tokens[position+2])) {
								
							double v = actions_sums.get(tokens[position+2]);
							actions_sums.put(tokens[position+2], (v + Double.valueOf(tokens[position+4])));
								
						} else actions_sums.put(tokens[position+2], Double.valueOf(tokens[position+4]));

						position = position + 10; // go to the next starter
							
					// this is a crackaction
					} else if(tokens[position].equals("CRA")) {
						
						crackactionCount++;
						
						String tmp_crackaction = "";
						for(int a = 0; a < 11; a++)
							tmp_crackaction += tokens[position+a] + "#";
						tmp_crackaction = tmp_crackaction.substring(0, tmp_crackaction.length()-1);

						if(!verifyCrackaction.verify(tmp_crackaction, store, true, log)) {
							
							log.addEntry("VerifyAndAddBlock>verify()", "Could not verify a crackaction (" + tokens[position+1] + ") in the block " + count + " starting at token " + position + "."); 
							return false;
						}
							
						// add id
						ids.add(tokens[position+1]);
							
						// check if this crackaction is define by me
						for(int j = 0; j < myaddresses.size(); j++) {
							if(tokens[position+8].equals(myaddresses.get(j))) {
								mycrackactions.add(count + "#" + tmp_crackaction);
								break;
							}
						}
							
						// for verification of sum of multiple actions from same address
						if(actions_sums.containsKey(tokens[position+8])) {
								
							double v = actions_sums.get(tokens[position+8]);
							actions_sums.put(tokens[position+8], (v + Double.valueOf(tokens[position+7])));
								
						} else actions_sums.put(tokens[position+8], Double.valueOf(tokens[position+7]));

						position = position + 11; // go to the next starter	
					
					// this is a hidden claimaction
					} else if(tokens[position].equals("HCL")) {
					
						claimactionCount++;
						
						String tmp_hcl = "";
						for(int a = 0; a < 10; a++)
							tmp_hcl += tokens[position+a] + "#";
						tmp_hcl = tmp_hcl.substring(0, tmp_hcl.length()-1);

						if(!verifyClaimaction.verify(tmp_hcl, store, true, log)) {
							
							log.addEntry("VerifyAndAddBlock>verify()", "Could not verify a hidden claimaction (" + tokens[position+1] + ") in the block " + count + " starting at token " + position + "."); 
							return false;
						}
						
						// add id
						ids.add(tokens[position+1]);
						
						// check if this HCL is define by me
						for(int j = 0; j < myaddresses.size(); j++) {
							if(tokens[position+7].equals(myaddresses.get(j))) {
								myclaimactions_hidden.add(count + "#" + tmp_hcl);
								break;
							}
						}
							
						// for verification of sum of multiple actions from same address
						if(actions_sums.containsKey(tokens[position+7])) {
								
							double v = actions_sums.get(tokens[position+7]);
							actions_sums.put(tokens[position+7], (v + 0.5d));
								
						} else actions_sums.put(tokens[position+7], 0.5d);

						position = position + 10; // go to the next starter	
					
					// this is a public claimaction
					} else if(tokens[position].equals("PCL")) {

						claimactionCount++;
						
						String tmp_pcl = "";
						for(int a = 0; a < 10; a++)
							tmp_pcl += tokens[position+a] + "#";
						tmp_pcl = tmp_pcl.substring(0, tmp_pcl.length()-1);

						if(!verifyClaimaction.verify(tmp_pcl, store, true, log)) {
							
							log.addEntry("VerifyAndAddBlock>verify()", "Could not verify a public claimaction (" + tokens[position+1] + ") in the block " + count + " starting at token " + position + "."); 
							return false;
						}
						
						// add id
						ids.add(tokens[position+1]);
						
						// check if this PCL is define by me
						for(int j = 0; j < myaddresses.size(); j++) {
							if(tokens[position+7].equals(myaddresses.get(j))) {
								myclaimactions_public.add(count + "#" + tmp_pcl);
								break;
							}
						}
						
						// also check if this PCL solves a crackaction defined by me
						for(int j = 0; j < mycrackactions_stored.size(); j++) {
							if(mycrackactions_stored.get(j).contains("#" + tokens[position+4] + "#")) {
								myclaimactions_solvers.add(count + "#" + tmp_pcl);
								break;
							}
						}
						
						// for verification of sum of multiple actions from same address
						if(actions_sums.containsKey(tokens[position+7])) {
								
							double v = actions_sums.get(tokens[position+7]);
							actions_sums.put(tokens[position+7], (v + 0.5d));
								
						} else actions_sums.put(tokens[position+7], 0.5d);

						position = position + 10; // go to the next starter	
						
					} else {

						log.addEntry("VerifyAndAddBlock>verify()", "Found an unknown action in the block " + count + ": " + tokens[position]); 
						return false;
					}
				}
					
			} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "Error while verifiying transactions and crackactions in the block " + count + "."); return false; }

			// verify balance of actions in sum (multiple transactions from same address within the same block); starting from time stamp of the block
			if(count > 0) {
				
				VerifyBalance verifyBalance = new VerifyBalance();
				
				for(String addr : actions_sums.keySet()) {
					if(!verifyBalance.verify(addr, actions_sums.get(addr), ts, store, log)) {
							
						log.addEntry("VerifyAndAddBlock>verify()", "The address " + addr + " has not enough balance for multiple transactions/crackactions in this block."); 
						return false;
					}
				}
			}

			if(transactionCount < (actionCount / 2)) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "This block needs to have at least 50% transactions (only " + transactionCount + " of " + actionCount + ")."); 
				return false;
			}
			
			String nonce = tokens[tokens.length-5];
			if(nonce.length() != 64 || !hexPattern.matcher(nonce).matches()) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "The nonce of the block " + count + " is not valid."); 
				return false;
			}
			
			String targetPoW = tokens[tokens.length-4]; 
			if(targetPoW.length() != 64 || !hexPattern.matcher(targetPoW).matches()) {
				
				log.addEntry("VerifyAndAddBlock>verify()", "The PoW-hash of the block " + count + " is not valid (1).");
				return false;
			}
			
			if(count > 0) {
				
				int difficulty = -1;
				try { difficulty = Integer.parseInt(tokens[tokens.length-3]);
				} catch(Exception e) { e.printStackTrace(); log.addEntry("VerifyAndAddBlock>verify()", "The difficulty of the block " + count + " is not valid (1)."); return false; }
	
				while(!store.blocks.claim()) Thread.sleep(10);
				int current_diff = store.blocks.getDifficulty();
				store.blocks.release();
				
				if(difficulty != current_diff) {
					
					log.addEntry("VerifyAndAddBlock>verify()", "The difficulty " + difficulty + " of the block " + count + " is not the currently enforced difficulty of " + current_diff + "."); 
					return false;
				}

				
				// check if the PoW hash meets the current difficulty requirement
				Pattern diffPattern = Pattern.compile("[0-7]+");
				if(!diffPattern.matcher(targetPoW.substring(0, difficulty)).matches()) {

					log.addEntry("VerifyAndAddBlock>verify()", "The PoW-hash of the block " + count + " does not meet the required difficulty.");
					return false;
				}
			}
			
			try {
				
				String tmp = "";
				for(int j = 0; j < tokens.length - 4; j++) 
					tmp += tokens[j] + "#";
				tmp = tmp.substring(0, tmp.length()-1);
				
				MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(tmp.getBytes()); 
				byte[] computed_hash = md.digest();
				
				if(!targetPoW.equals(bytesToHex(computed_hash))) {
					
					log.addEntry("VerifyAndAddBlock>verify()", "The PoW-hash of the block " + count + " is not correct (2)."); 
					return false;
				}

				// finally verify signature
				byte[] decoded_pub = hexToBytes(tokens[tokens.length-2]);
				byte[] signature = hexToBytes(tokens[tokens.length-1]);
				
				KeyFactory keyFactory = KeyFactory.getInstance("EC");
		        X509EncodedKeySpec puKeySpec = new X509EncodedKeySpec(decoded_pub);
		        PublicKey p = keyFactory.generatePublic(puKeySpec);
		
				byte[] msg = (tmp + "#" + targetPoW + "#" + tokens[tokens.length-3] + "#" + tokens[tokens.length-2]).getBytes();
					
				Signature rsa = Signature.getInstance("SHA512withECDSA"); 
				rsa.initVerify(p); rsa.update(msg, 0, msg.length);
				
				// finally, if signature is correct, update local files
				if(rsa.verify(signature)) {

					appendBlock(block, transactionCount, crackactionCount, claimactionCount, mytransactions, mycrackactions, myclaimactions_hidden, myclaimactions_public, myclaimactions_solvers, ids, store);
					log.addEntry("VerifyAndAddBlock>verify()", "Successfully added the new block " + count + " to the blockchain.");
					return true;
					
				} else log.addEntry("VerifyAndAddBlock>verify()", "The signature is not valid.");
			
			} catch (Exception e) {e.printStackTrace();}
		} catch (Exception e) {e.printStackTrace();}
		
		log.addEntry("VerifyAndAddBlock>verify()", "Could not add the block: " + block.substring(0, 64));
		return false;
	}
	
	/*
	 *  Adds a new block to the chain and updates myblocks, mytransactions, mycrackactions, myclaimactions_hidden, myclaimactions_public and myclaimactions_solvers.
	 *  Also, it removes actions from their temporary files once they are added to the blockchain.
	 */
	public synchronized void appendBlock(String block, int traCount, int craCount, int clCount, LinkedList<String> mytransactions, LinkedList<String> mycrackactions, LinkedList<String> myclaimactions_hidden, LinkedList<String> myclaimactions_public, LinkedList<String> myclaimactions_solvers, LinkedList<String> ids, Store store) {
		
		while(!store.wallet.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
		LinkedList<String> myaddresses = store.wallet.getMyAddresses();
		store.wallet.release();
		
		String[] tokens = block.split("#");
		
		// check if this block is mined by me; if yes append to myblocks-file
		for(int j = 0; j < myaddresses.size(); j++) {
			if(tokens[2].equals(myaddresses.get(j))) {
				
				double mining_reward = ((double) traCount * 0.1d) + ((double) craCount * 1.0d) +  ((double) clCount * 0.5d);

				String blockStr = tokens[0] + "#" + tokens[3] + "#" + myaddresses.get(j) + "#" + formatValue(mining_reward);
				
				while(!store.myblocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
				store.myblocks.addItem(blockStr);
				store.myblocks.release();
				break;
			}
		}

		if(mytransactions.size() > 0) {

			while(!store.mytransactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }

			for(String s : mytransactions)
				store.mytransactions.addItem(s);
			
			store.mytransactions.release();
		}
		
		if(mycrackactions.size() > 0) {

			while(!store.mycrackactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			
			for(String s : mycrackactions)
				store.mycrackactions.addItem(s);
			
			store.mycrackactions.release();
		}
		
		if(myclaimactions_hidden.size() > 0) {
		
			while(!store.myclaimactions_hidden.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			
			for(String s : myclaimactions_hidden)
				store.myclaimactions_hidden.addItem(s);
			
			store.myclaimactions_hidden.release();
		}
		
		if(myclaimactions_public.size() > 0) {
		
			while(!store.myclaimactions_public.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			
			for(String s : myclaimactions_public)
				store.myclaimactions_public.addItem(s);
			
			store.myclaimactions_public.release();
		}
		
		if(myclaimactions_solvers.size() > 0) {
			
			while(!store.myclaimactions_solvers.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			
			for(String s : myclaimactions_solvers)
				store.myclaimactions_solvers.addItem(s);
			
			store.myclaimactions_solvers.release();
		}
		
		while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
		store.blocks.addBlock(block);
		store.blocks.release();

		// finally remove pending actions (identified by id)
		
		for(String id : ids) {
			
			while(!store.transactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			String[] local_transactions = store.transactions.getItems();
			store.transactions.release();
			
			for(String transaction : local_transactions) {
				
				if(transaction.startsWith("TRA#" + id + "#")) {
					
					while(!store.transactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
					store.transactions.removeItem(transaction);
					store.transactions.release();
					
					break;
				}
			}
			
			while(!store.crackactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			String[] local_crackactions = store.crackactions.getItems();
			store.crackactions.release();
			
			for(String crackaction : local_crackactions) {
				
				if(crackaction.startsWith("CRA#" + id + "#")) {
					
					while(!store.crackactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
					store.crackactions.removeItem(crackaction);
					store.crackactions.release();
					
					break;
				}
			}
			
			while(!store.claimactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
			String[] local_claimactions = store.claimactions.getItems();
			store.claimactions.release();
			
			for(String claimaction : local_claimactions) {
				
				if(claimaction.startsWith("HCL#" + id + "#") || claimaction.startsWith("PCL#" + id + "#")) {
					
					while(!store.claimactions.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); } }
					store.claimactions.removeItem(claimaction);
					store.claimactions.release();
					
					break;
				}
			}
		}
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
	
	private String formatValue(double balance) {
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		String t = df.format(balance);
		return t.replace(",", ".").replace("-", "");
	}
}
