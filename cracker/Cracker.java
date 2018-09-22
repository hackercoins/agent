package cracker;

import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import core.Logger;
import creators.CreateClaimaction;
import storage.Store;

/*
 * The cracker selects crackactions depending on the given options from the blockchain. 
 * It only selects crackactions which are not older than 12 hours.
 * Once a crackaction is solved, a hidden claimaction and one block later, a public claimaction is automatically created.
 */
public class Cracker implements Runnable {

	private Store store = null;
	private Logger log = null;
	
	private ExecutorService executor = null;
	private LinkedList<String> testedWithWordlist = new LinkedList<String>();

	public Cracker(Store store, Logger log) {

		this.store = store;
		this.log = log;
	}
	
	@Override
	public void run() {
		
		while(true) {
			
			try {Thread.sleep(15 * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			
			while(!store.config.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
			String tmp1 = store.config.getConfigOption("cracking=");
			String crackingAddress = store.config.getConfigOption("crackingAddr=");
			String tmp2 = store.config.getConfigOption("crackingOption=");
			store.config.release();
			
			int crackingThreads = Integer.parseInt(tmp1);
			int crackingOption = Integer.parseInt(tmp2);
			
			if(crackingThreads > 0 && crackingAddress != null && store.getMyBalance(crackingAddress) >= 1.0d) {

				String[] selected_crackactions = new String[crackingThreads];

				// get unsolved crackactions from blockchain
				
				while(!store.blocks.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
				LinkedList<String> crackactions_start = store.blocks.getCrackactionsFromChain();
				store.blocks.release();
					
				if(crackactions_start.size() == 0) continue;

				// ensure that no HCL/PCL of myself exists in the claimactions file
				LinkedList<String> crackactions = new LinkedList<String>();
				
				while(!store.claimactions.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
				String[] claimactions = store.claimactions.getItems();
				store.claimactions.release();

				while(!store.wallet.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
				LinkedList<String> addresses = store.wallet.getMyAddresses();
				store.wallet.release();
				
				for(String crackaction : crackactions_start) {
					
					boolean doAdd = true;
					String craID = crackaction.split("#")[1];
					
					for(String claimaction : claimactions) {
						if(claimaction.contains(craID)) {
							for(String address : addresses) {
								
								// if this is a crackaction that is already claimed by me
								if(claimaction.contains(address)) {
									doAdd = false;
									break;
								}
							}
						}
					}
					
					if(doAdd) crackactions.add(crackaction);
				}

					
				if(crackingOption == 0) { // select random crackactions
						
					Collections.shuffle(crackactions);
						
					int start = 0;
					for(int i = 0; i < crackingThreads; i++) {
							
						selected_crackactions[i] = crackactions.get(start);
						if(start < crackactions.size() -1)
							start++;
					}
					
					log.addEntry("Cracker>run()", "Selected " + crackingThreads + " crackactions randomly.");
						
				} else if(crackingOption == 1) { // prefer crackactions with high rewards
						
					LinkedList<String> selected = new LinkedList<String>();
						
					for(int a = 0; a < crackingThreads; a++) {
						
						String tmp_crackaction = null;
						double tmp_maxReward = 0.0d;
							
						for(String c : crackactions) {
								
							if(!selected.contains(c)) {
									
								String[] tokens = c.split("#");
								double reward = Double.parseDouble(tokens[7]);
									
								if(reward > tmp_maxReward) {
										
									tmp_maxReward = reward;
									tmp_crackaction = c;
								}
							}
						}
						
						if(tmp_crackaction != null)
							selected.add(tmp_crackaction);
					}
						
					int start = 0;
					for(int i = 0; i < crackingThreads; i++) {
							
						selected_crackactions[i] = selected.get(start);
						if(start < selected.size() -1)
							start++;
					}
					
					log.addEntry("Cracker>run()", "Selected " + crackingThreads + " crackactions based on highest rewards.");
					
				} else if(crackingOption == 2) { // prefer crackactions with easy algorithms
						
					LinkedList<String> selected = new LinkedList<String>();
						
					for(String c : crackactions) {
							
						String[] tokens = c.split("#");
							
						if(tokens[4].equals("MD5") || tokens[4].equals("SHA-1")) {
							selected.add(c);
						}
					}
					int start = 0;
					for(int i = 0; i < crackingThreads; i++) {
							
						selected_crackactions[i] = selected.get(start);
						if(start < selected.size() -1)
							start++;
					}
					
					log.addEntry("Cracker>run()", "Selected " + crackingThreads + " crackactions based on easiest algorithms.");
				}
				
				while(!store.config.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
				String wordlist = store.config.getConfigOption("crackingWordlistPath=");
				String charset = store.config.getConfigOption("crackingCharset=");
				String tmp3 = store.config.getConfigOption("crackingMaxLength=");
				store.config.release();
				
				int maxLength = Integer.parseInt(tmp3);
					
				executor = Executors.newFixedThreadPool(crackingThreads);
				CrackerThread[] crackers = new CrackerThread[crackingThreads];
					
				for(int i = 0; i < crackingThreads; i++) {

					String[] tokens = selected_crackactions[i].split("#");

					String hash = tokens[2];
						
					String salt = tokens[3];
					if(salt.equals("0")) salt = "";
						
					String algorithm = tokens[4];
						
					if(testedWithWordlist.contains((hash+salt))) wordlist = null;
					crackers[i] = new CrackerThread(hash, salt, algorithm, charset, maxLength, selected_crackactions[i], wordlist, log);
				}
				
				for(int a = 0; a < crackingThreads; a++)
					executor.execute(new Thread(crackers[a]));
				
				log.addEntry("Cracker>run()", "Started cracking with " + crackingThreads + " threads.");

				int countTime = 0; // 15 minutes max without reloading (15x4)
				
				String target_crackaction = null;
				String target_solution = null;
					
				while(countTime < 60 && target_solution == null) {
						
					for(int a = 0; a < crackingThreads; a++) {
						if(crackers[a].solution != null) {
								
							target_solution = crackers[a].solution;
							target_crackaction = crackers[a].crackaction;
						}
					}
						
					try {Thread.sleep(15 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
					
					// verify every 15 seconds if crackaction is still enabled
					while(!store.config.claim()) {try {Thread.sleep(10); } catch (InterruptedException e) {e.printStackTrace();}}
					String tmp4 = store.config.getConfigOption("cracking=");
					store.config.release();
					
					if(Integer.valueOf(tmp4) == 0) break;
					countTime++;
				}
					
				// wait until wordlist cracking is also finished by all threads
				boolean oneUsesWordlist = true;
					
				while(oneUsesWordlist) {
					
					oneUsesWordlist = false;
						
					for(int a = 0; a < crackingThreads; a++)
						if(crackers[a].usesWordlist)
							oneUsesWordlist = true;

					try {Thread.sleep(5 * 1000);} catch (InterruptedException e) {e.printStackTrace();}
				}
					
				for(int a = 0; a < crackingThreads; a++)
					crackers[a].doCrack = false;
					
				executor.shutdown();
				while(!executor.isTerminated())
					try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}

				if(target_solution != null) {
					if(store.getMyBalance(crackingAddress) >= 1.0d) {
						
						String[] tokens = target_crackaction.split("#");
						CreateClaimaction ccl = new CreateClaimaction(tokens[1], target_solution, tokens[7], crackingAddress, store, log);
						new Thread(ccl).start(); // this creates a HCL and once the HCL is mined, it creates the corresponding PCL
						log.addEntry("Cracker>run()", "Started the HCL and PCL creation process.");
						
					} else log.addEntry("Cracker>run()", "Not enough balance to create a HCL.");
				}
			}
		}
	}
}
