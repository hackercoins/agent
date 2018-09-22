package miner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import core.Logger;
import creators.BroadCaster;
import storage.Store;
import verifiers.VerifyBalance;

/*
 * The miner selects up to 100 transactions/crackactions (solved and unsolved), builds a temporary block and starts N mining threads
 * in order to find a valid proof-of-work hash (SHA256) by continuously changing the nonce. A block needs to have 10 transactions/crackactions at minimum.
 * The transactions/crackactions are verified and the balance of multiple transaction/crackactions from the same address are verified in sum.
 * Once a valid proof-of-work is found, the new block is added to the local blockchain and directly broadcasted to other nodes.
 */
public class Miner implements Runnable {

	private Store store = null;
	private Logger log = null;
	private ExecutorService executor = null;

	public Miner(Store store, Logger log) {

		this.store = store;
		this.log = log;
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			try {Thread.sleep(10 * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			int mThreads = 0;
			
			try {
				
				while(!store.config.claim()) Thread.sleep(10);
				String tmp = store.config.getConfigOption("mining=");
				store.config.release();
				
				mThreads = Integer.valueOf(tmp);
				
			} catch (Exception e) {}
				
			if(mThreads > 0) {
				
				log.addEntry("Miner>run()", "Starting miner...");
				
				// check if this node is synchronized
				long r_count = getRemoteBlockCount();
				
				while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); }}
				long l_count = store.blocks.getMyLatestBlockCount();
				store.blocks.release();
				
				if(r_count < 0)
					continue;
				
				if(r_count > l_count)
					continue;

				
				boolean skip = false;
			
				// get up to 100 transactions from transactions-file (preferring oldest)
				LinkedList<String> actions = store.getActionsForMining();
				
				// a crackaction and a HCL or PCL are not allowed within the same block
				// also, a HCL and PCL referring to the same crackaction is not allowed
				// accordingly, a crackactions_id should not appear more than once in the block string
				
				LinkedList<String> ids = new LinkedList<String>();
				LinkedList<String> filtered_actions = new LinkedList<String>();
				
				for(String action: actions) {

					String[] tokens = action.split("#");
					
					if(tokens[0].equals("PCL") || tokens[0].equals("HCL")) {
						
						if(!ids.contains(tokens[4])) {
							
							ids.add(tokens[4]);
							filtered_actions.add(action);
						}
					}
					
					if(tokens[0].equals("CRA")) {
						
						if(!ids.contains(tokens[1])) {
							
							ids.add(tokens[1]);
							filtered_actions.add(action);
						}
					}
					
					if(tokens[0].equals("TRA")) {
						
						if(!ids.contains(tokens[1])) {
							
							ids.add(tokens[1]);
							filtered_actions.add(action);
						}
					}
				}
				
				actions = filtered_actions;
				
				if(actions.size() < 10) continue;
				log.addEntry("Miner>run()", "Loaded " + actions.size() + " transactions, crackactions and claimactions for mining.");

				// check balance for sum of multiple actions of the same address
				HashMap<String, Double> actions_sums = new HashMap<String, Double>();
				
				for(String action : actions) {
					
					String tokens[] = action.split("#");
					
					// transaction
					if(tokens[0].equals("TRA")) {
						
						if(actions_sums.containsKey(tokens[2])) {
							
							double v = actions_sums.get(tokens[2]);
							actions_sums.put(tokens[2], (v + Double.valueOf(tokens[4])));
							
						} else actions_sums.put(tokens[2], Double.valueOf(tokens[4]));
						
					// crackaction
					} else if(tokens[0].equals("CRA")) {

						if(actions_sums.containsKey(tokens[8])) {
							
							double v = actions_sums.get(tokens[8]);
							actions_sums.put(tokens[8], (v + Double.valueOf(tokens[7])));
							
						} else actions_sums.put(tokens[8], Double.valueOf(tokens[7]));
						
					// claimaction
					} else if(tokens[0].equals("HCL") || tokens[0].equals("PCL")) {
						
						if(actions_sums.containsKey(tokens[7])) {
							
							double v = actions_sums.get(tokens[7]);
							actions_sums.put(tokens[7], (v + 0.5d));
							
						} else actions_sums.put(tokens[7], 0.5d);
					}
				}

				// verify the balance of multiple transactions/crackactions from the same address in sum
				VerifyBalance verifyBalance = new VerifyBalance();
				
				for(String addr : actions_sums.keySet()) {
					if(!verifyBalance.verify(addr, actions_sums.get(addr), getCurrentTime(), store, log)) {
						log.addEntry("Miner>run()", "The address " + addr + " has not enough balance for multiple actions in this block.");
						skip = true;
					}					
				}
				
				if(skip) continue;
				
				while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); }}
				String latesBlock = store.blocks.getMyLatestBlock();
				store.blocks.release();

				String[] tmp = latesBlock.split("#");
				
				while(!store.config.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); }}
				String address = store.config.getConfigOption("miningAddr=");
				store.config.release();
				
				if(tmp != null && address != null) {

					long last_count = -1;
					try { last_count = Long.valueOf(tmp[0]);
					} catch(Exception e) { e.printStackTrace(); }
						
					if(last_count >= 0) {
							
						String last_pow = tmp[tmp.length-4];
						
						while(!store.blocks.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); }}
						int difficulty = store.blocks.getDifficulty();
						store.blocks.release();
						
						log.addEntry("Miner>run()", "Using the current difficulty of " + difficulty + ".");
						
						String newBlock = last_count+1 + "#01#" + address + "#" + getCurrentTime() + "#" + last_pow + "#" + actions.size() + "#";
						for(String action : actions)
							newBlock += action + "#";
						newBlock = newBlock.substring(0, newBlock.length()-1);
						
						executor = Executors.newFixedThreadPool(mThreads);
						MinerThread[] miners = new MinerThread[mThreads];
						
						for(int a = 0; a < mThreads; a++)
							miners[a] = new MinerThread(newBlock, difficulty, address, log, store);

						for(int a = 0; a < mThreads; a++)
								executor.execute(new Thread(miners[a]));
						
						log.addEntry("Miner>run()", "Started mining with " + mThreads + " threads.");
						
						String minedBlock = null;
						int timeCount = 0;
						
						while(minedBlock == null) {

							try {Thread.sleep(10 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
							
							while(!store.config.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace(); }}
							String tmp1 = store.config.getConfigOption("mining=");
							store.config.release();
							
							if(Integer.valueOf(tmp1) == 0) break;
							if(getRemoteBlockCount() != last_count) break;
							
							timeCount++;
							if(timeCount > 30) break; // 5 minutes

							for(int a = 0; a < mThreads; a++)
								if(miners[a].minedBlock != null)
									minedBlock = miners[a].minedBlock;
						}
						
						for(int a = 0; a < mThreads; a++)
							miners[a].doMine = false;
						
						shutdownAndAwaitTermination(executor);

						log.addEntry("Miner>run()", "All mining threads finished.");
						
						if(minedBlock != null) {

							log.addEntry("Miner>run()", "A new block was verified and broadcastet to other nodes");
							
							long newBlockCount = Long.valueOf(minedBlock.split("#")[0]);
							
							if((getRemoteBlockCount() +1 ) == newBlockCount) // verify again
								new Thread(new BroadCaster(store, "publishBlock:" + minedBlock, log)).start();
						}
					}
				}
			}
		}
	}

	private void shutdownAndAwaitTermination(ExecutorService pool) {
		
	    pool.shutdown();
	    
	    try {
	    	
	        if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
	        	
	            pool.shutdownNow(); 
	            pool.awaitTermination(2, TimeUnit.SECONDS);
	        }
	        
	    } catch (InterruptedException ie) {pool.shutdownNow();}
	}
	
	/*
	 *  Tries to get the highest block count of up to 7 random nodes. Needs to have at least four nodes.
	*/
	private long getRemoteBlockCount() {

		int sources = 0;
		long remote_count = -1;
				
		try {
			
			while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
			String[] targetNodes = store.nodes.getItems();
			store.nodes.release();

			if(targetNodes != null) {
				if(targetNodes.length > 0) {
						
					for(int i = 0; i < targetNodes.length; i++) {
					
						Socket clientSocket = null;
						DataOutputStream outToServer = null;
						BufferedReader inFromServer = null;
						
						try {
							
							clientSocket = new Socket(targetNodes[i], 31337);
							clientSocket.setSoTimeout(8000);
							outToServer = new DataOutputStream(clientSocket.getOutputStream());
							outToServer.writeBytes("getBlockCount\n");
									
							inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
							String reply = inFromServer.readLine();
								
							if(reply != null) {
									
								long tmp_remote_count = Long.valueOf(reply.trim());
								if(tmp_remote_count > remote_count) remote_count = tmp_remote_count;
								sources++;
							}
								
						} catch (Exception e) {}
						finally {
							
							try {

								if(outToServer != null) outToServer.close();
								if(inFromServer != null) inFromServer.close();
								if(clientSocket != null) clientSocket.close();
								
							} catch (Exception e) {}
						}
							
						if(sources >= 7) break;
					}
				}
			}
				
			if(sources < 3) {
					
				log.addEntry("Helper>getRemoteBlockCount()", "Could not retrieve block count from at least three different nodes.");
				return -1; // needs to have at least 3 nodes
			}
						
		} catch (Exception e) {log.addEntry("Helper>getRemoteBlockCount()", "Could not verify the count of the solved block.");}
			
		log.addEntry("Helper>getRemoteBlockCount()", "Checked for remote block count which is " + remote_count + ".");
		return remote_count;
	}
	
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
