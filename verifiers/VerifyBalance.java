package verifiers;

import core.Logger;
import storage.Store;

public class VerifyBalance {
	
	/*
	 *  This class verifies if an address owns the given balance.
	 *  For this, the local chain is investigated starting from the given time stamp backwards.
	 */
	public synchronized boolean verify(String addr, double value, int ts, Store store, Logger log) {	
			
		double tmp_balance = 0.0d;
			
		try {
				
			// get highest number of chain files (they start with chain1)
			while(!store.blocks.claim()) Thread.sleep(10);
			long max = store.blocks.getMyLatestBlockCount();
			store.blocks.release();
			
			// search in blocks from highest to lowest chain file
			while(max >= 0) {
				
				while(!store.blocks.claim()) Thread.sleep(10);
				String temp_block = store.blocks.getBlock(max);
				store.blocks.release();
				
				String[] tokens = temp_block.split("#");
					
				int traCount = 0;
				int craCount = 0;
				int clCount = 0;
					
				int position = 6; // id of first action in a block
					
				// skip this block if it was created after the given time stamp
				if(Integer.valueOf(tokens[3]) > ts) {
					
					max--;
					continue;
				}
					
				try {
					
					while(position < tokens.length - 7) {
						
						// this is a transaction
						if(tokens[position].equals("TRA")) {
								
							traCount++;
										
							// sent from the address
							if(tokens[position+2].equals(addr)) tmp_balance = tmp_balance - Double.valueOf(tokens[position+4]);
										
							// sent to the addresses (minus 0.1 fee)
							if(tokens[position+3].equals(addr)) tmp_balance = tmp_balance + (Double.valueOf(tokens[position+4]) - 0.1d);

							// jump to next id
							position = position + 10;	

						} else if(tokens[position].equals("CRA")) {

							craCount++;
							
							// if created by this address
							if(tokens[position+8].equals(addr)) {
									
								int cra_ts = Integer.parseInt(tokens[position+5]);
									
								//  if older than 12 hours and not solved in 12 hours after creation, return all (minus 1.0 fee)
								if(getCurrentTime() - cra_ts > (60*60*12) && !isSolved(tokens[position+1], cra_ts, store))
										tmp_balance = tmp_balance + (Double.valueOf(tokens[position+7]) - 1.0d);
								
								//  younger than 12 hours or solved, subtract all
								else
									tmp_balance = tmp_balance - Double.valueOf(tokens[position+7]);
							}
							// jump to next id
							position = position + 11;
								
						} else if(tokens[position].equals("HCL") || tokens[position].equals("PCL") ) {
							
							clCount++;
								
							// if created by this address, subtract fee of 0.5 coins
							if(tokens[position+6].equals(addr))
								tmp_balance = tmp_balance - 0.5d;
								
							// jump to next id
							position = position + 10;
						}
					}
					
				} catch(Exception e) { e.printStackTrace(); log.addEntry("verifyBalance()", "Error while investigating transactions and crackactions in the block."); return false; }
						
				// also check if this block is mined by the address
				if(tokens[2].equals(addr)) {
						
					double mining_reward = ((double) traCount * 0.1d) + ((double) craCount * 1.0d) + ((double) clCount * 0.5d);
					tmp_balance += mining_reward;
				}
					
				// we can skip once the required balance is reached
				if(tmp_balance >= value) return true;
				
				max--; // search in next previous block
			}

		} catch (Exception e) {e.printStackTrace();}
		log.addEntry("verifyBalance()", "The address " + addr + " has not enough balance (required: " + value + " / owned: " + tmp_balance + " / ts: " + ts + ").");
		return false;
	}
	
	/*
	 * Check all blocks up to 12 hours after the creation of the crackaction for a PCL referencing this crackaction's id.
	 */
	private boolean isSolved(String crackaction_id, int crackaction_ts, Store store) {

		int start = crackaction_ts + (60*60*12);
		
		while(!store.blocks.claim()) { try {Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
		long max = store.blocks.getMyLatestBlockCount();
		store.blocks.release();
		
		// search in blocks from highest to lowest
		while(max >= 0) {
			
			while(!store.blocks.claim()) { try {Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
			String block = store.blocks.getBlock(max);
			store.blocks.release();
			
			String[] tokens = block.split("#");
				
			// skip this block if it was created after the given time stamp
			if(Integer.valueOf(tokens[3]) > start) return false;
				
			// skip the block if we searched in all blocks until the crackactions_ts
			if(Integer.valueOf(tokens[3]) <= crackaction_ts) return false;

			for(int i = 6; i < tokens.length - 6; i++) {
					
				if(tokens[i].equals("PCL")) {
					if(tokens[i+4].equals(crackaction_id))
						return true;
				}
			}
			
			max--;
		}
		return false;
	}
	
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
