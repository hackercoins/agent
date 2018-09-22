package storage;

import java.util.LinkedList;

import core.Logger;
import verifiers.VerifyClaimaction;
import verifiers.VerifyCrackaction;
import verifiers.VerifyTransaction;

public class Store {

	public ActionStore nodes = new ActionStore();
	public ActionStore transactions = new ActionStore();
	public ActionStore crackactions = new ActionStore();
	public ActionStore claimactions = new ActionStore();
	
	public MyActionStore mytransactions = new MyActionStore("mytransactions");
	public MyActionStore mycrackactions = new MyActionStore("mycrackactions");
	public MyActionStore myclaimactions_hidden = new MyActionStore("myclaimactions_hidden");
	public MyActionStore myclaimactions_public = new MyActionStore("myclaimactions_public");
	public MyActionStore myclaimactions_solvers = new MyActionStore("myclaimactions_solvers");
	public MyActionStore myblocks = new MyActionStore("myblocks");
	
	public ConfigStore config = new ConfigStore();
	public WalletStore wallet = null;
	
	public BlockStore blocks = null;
	
	private Logger log = null;
	
	public Store (String p, Logger log) {
		
		wallet = new WalletStore(p);
		wallet.updateWalletEntries();
		
		blocks = new BlockStore(log);
		this.log = log;
	}
	
	/* 
	 * Get the balance for this address from mined blocks, transactions, crackactions and hidden and public claimactions.
	 */
	public double getMyBalance(String address) {
		
		double balance = 0.0d;
		
		try {

			while(!myblocks.claim()) Thread.sleep(10);
			LinkedList<String> blocks = myblocks.getItems();
			myblocks.release();

			for(String myblock : blocks) {

				if(myblock.contains("#")) {
					
					String[] tokens = myblock.split("#");
						
					if(tokens[2].equals(address))
						balance += Double.parseDouble(tokens[3]);
				}
			}

			while(!mytransactions.claim()) Thread.sleep(10);
			LinkedList<String> transactions = mytransactions.getItems();
			mytransactions.release();
			
			for(String mytransaction : transactions) {
			
				if(mytransaction.contains("#")) {
					
					String[] tokens = mytransaction.split("#");
					
					// from me (count # transaction)
					if(tokens[3].equals(address)) {
						
						balance -= Double.parseDouble(tokens[5]);
					}
					
					// to me (count # transaction) (also subtract transaction fee)
					if(tokens[4].equals(address)) {
						
						balance += (Double.parseDouble(tokens[5]) - 0.1d);
					}
				}
			}

			while(!mycrackactions.claim()) Thread.sleep(10);
			LinkedList<String> crackactions = mycrackactions.getItems();
			mycrackactions.release();

			for(String mycrackaction : crackactions) {
		
				// (count # crackaction)
				if(mycrackaction.contains("#")) {
					
					String[] tokens = mycrackaction.split("#");
					
					if(tokens[9].equals(address)) {
					
						int ts_cra = Integer.valueOf(tokens[6]);
						int ts_now = getCurrentTime();
						
						// if the crackaction is younger than 12 hours subtract all
						if((ts_now - ts_cra) < (60*60*12)) {
					
							balance -= Double.parseDouble(tokens[8]);
						
						// if the crackaction is older than 12 hours	
						} else {
						
							boolean solutionExists = false;
							
							while(!myclaimactions_solvers.claim()) Thread.sleep(10);
							LinkedList<String> solvers = myclaimactions_solvers.getItems();
							myclaimactions_solvers.release();
							
							for(String solver : solvers) {
								if(solver.contains("#" + tokens[2] + "#")) {
									solutionExists = true;
									break;
								}
							}
							
							// if a solution exists, subtract all
							if(solutionExists)
								balance -= Double.parseDouble(tokens[8]);
							
							// if no solution exists after 12 hours, subtract only fee
							else
								balance -= 1.0d;
						}
					}
				}
			}
			
			while(!myclaimactions_hidden.claim()) Thread.sleep(10);
			LinkedList<String> claimactions_hidden = myclaimactions_hidden.getItems();
			myclaimactions_hidden.release();
			
			for(String mycl_hidden : claimactions_hidden) {
		
				// (count # hcl)
				if(mycl_hidden.contains("#")) {
					
					String[] tokens = mycl_hidden.split("#");
					
					if(tokens[8].equals(address))
						balance -= 0.5d;
				}
			}
			
			while(!myclaimactions_public.claim()) Thread.sleep(10);
			LinkedList<String> claimactions_public = myclaimactions_public.getItems();
			myclaimactions_public.release();
			
			for(String mycl_public : claimactions_public) {
		
				// (count # pcl)
				if(mycl_public.contains("#")) {
					
					String[] tokens = mycl_public.split("#");
					
					if(tokens[8].equals(address)) {
					
						balance -= 0.5d;
						balance += (Double.parseDouble(tokens[7]) - 1.0d);
					}
				}
			}

		} catch (Exception e) { e.printStackTrace();}

		return balance;
	}
	
	/* 
	 * Returns sum of mining rewards and cracking rewards from all addresses. This is only used in the Gui.
	 */
	public double[] getMyRewards() {

		double rewards[] = new double[2];
		
		while(!myblocks.claim()) { try {Thread.sleep(10);} catch (Exception e) { e.printStackTrace(); }}
		LinkedList<String> blocks = myblocks.getItems();
		myblocks.release();
		
		for(String myblock : blocks) {
	
			String[] tokens = myblock.split("#");
			rewards[0] += Double.parseDouble(tokens[3]);
		}
		
		while(!myclaimactions_public.claim()) { try {Thread.sleep(10);} catch (Exception e) { e.printStackTrace(); }}
		LinkedList<String> claimactions = myclaimactions_public.getItems();
		myclaimactions_public.release();
		
		for(String myclaimaction : claimactions) {

			String[] tokens = myclaimaction.split("#");
			rewards[1] += (Double.parseDouble(tokens[7]) - 1.0d);
		}

		return rewards;
	}

	public int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
	
	/*
	 *  Returns random transactions, crackactions or claimactions for mining while preferring the oldest.
	 *  This also meets the requirements of 50% transactions in a block.
	 */
	public LinkedList<String> getActionsForMining() {

		LinkedList<String> actions = new LinkedList<String>();
		log.addEntry("Store>getActionsForMining()", "Retrieving actions for mining...");
		
		while(!transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) { e.printStackTrace(); }}
		String[] transactions_tmp = transactions.getItems();
		transactions.release();

		LinkedList<String> transactions_lst = new LinkedList<String>();
		for(int i = 0; i < transactions_tmp.length; i++)
			transactions_lst.add(transactions_tmp[i]);
		
		log.addEntry("Store>getActionsForMining()", "Read " + transactions_lst.size() + " temporary transactions.");

		while(transactions_lst.size() > 100)
			transactions_lst.removeFirst();
		
		log.addEntry("Store>getActionsForMining()", "Reduced actions to " + transactions_lst.size());
		
		VerifyTransaction verifyTransaction = new VerifyTransaction();
		for(String transaction : transactions_lst) {
			if(verifyTransaction.verify(transaction, this, false, log)) {
				actions.add(transaction);
			}
		}
		
		log.addEntry("Store>getActionsForMining()", "Added " + actions.size() + " verified transactions to list of actions.");
		
		// then add up to 50% crackactions
		int number_transactions = actions.size();
		int max_size = number_transactions / 2 - 1;
		
		if(max_size < 0) max_size = 0;
		
		while(!crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) { e.printStackTrace(); }}
		String[] local_crackactions = crackactions.getItems();
		crackactions.release();
		
		int number_crackactions = 0;

		VerifyCrackaction verifyCrackaction = new VerifyCrackaction();
		for(String crackaction : local_crackactions) {
			
			if(number_crackactions >= max_size)
				break;
			
			if(verifyCrackaction.verify(crackaction, this, false, log)) {
				actions.add(crackaction);
				number_crackactions++;
			}
		}
		
		log.addEntry("Store>getActionsForMining()", "Added " + number_crackactions + " verified crackactions to list of actions.");

		max_size = max_size - number_crackactions;
		if(max_size < 0) max_size = 0;
		
		int number_claimactions = 0;
		
		while(!claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) { e.printStackTrace(); }}
		String[] local_claimactions = claimactions.getItems();
		claimactions.release();
		
		VerifyClaimaction verifyClaimaction = new VerifyClaimaction();
		for(String claimaction : local_claimactions) {
			
			if(number_claimactions >= max_size)
				break;
			
			if(verifyClaimaction.verify(claimaction, this, false, log)) {
				actions.add(claimaction);
				number_claimactions++;
			}
		}

		log.addEntry("Store>getActionsForMining()", "Added " + number_claimactions + " verified claimactions to list of actions.");
		
		while(actions.size() > 100)
			actions.removeLast();
		
		log.addEntry("Store>getActionsForMining()", "Found actions: " + actions.size());
		return actions;
	}
}
