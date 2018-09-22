package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;

import core.Logger;

public class BlockStore {
	
	private Logger log = null;
	private volatile boolean isFree = true; 
	
	private LinkedList<String> chain = new LinkedList<String>();
	
	private String storedChainFileName = "";
	private LinkedList<String> storedChainFile = new LinkedList<String>();
	
	public BlockStore(Logger log) {
		
		this.log = log;
	}
	
	public synchronized boolean claim() {
	
		if(isFree) {
			
			isFree = false;
			return true;
		}
		return false;
	}
	
	public synchronized void release() {
		
		isFree = true;
	}
	
	/*
	 * Keep latest 1024 blocks also in memory; write every block to local file; a block file saves 256 blocks
	 */
	public void addBlock(String block) {
		
		// get count of latest block and verify the count of this new block
		
		String latest_block = null;
		long latest_block_count = -1;
		long this_block_count = -1;
		
		if(chain.size() > 0)
				latest_block = chain.getLast();
		
		if(latest_block != null) {
			
			try {
				
				latest_block_count = Long.valueOf(latest_block.split("#")[0]);
				
			} catch (Exception e) { e.printStackTrace(); }
		}
		
		try {
			
			this_block_count = Long.valueOf(block.split("#")[0]);
			
		} catch (Exception e) { e.printStackTrace(); }
		
		if((latest_block_count + 1) == this_block_count) {

			chain.add(block);
			
			if(chain.size() > 1024) chain.removeFirst();
			
			String[] tokens = block.split("#");
			int fcount = ((int) Long.parseLong(tokens[0]) / 256) + 1;
			File targetFile = new File("chain" + fcount);
	
			try {
					
				PrintWriter pw = new PrintWriter(new FileOutputStream(targetFile, true));
				pw.println(block);
				pw.close();
					
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	/*
	 * loadChain: load latest files into queue (once during startup)
	 */
	public void loadChain() {

		int file_count = 0;

		for(File f : new File(".").listFiles()) {
			if(f.getName().startsWith("chain")) {
				int s = Integer.parseInt(f.getName().substring("chain".length()));
				if(s > file_count) file_count = s;
			}
		}
		
		int count = file_count - 4;
		if(count < 1) count = 1;
		
		for(int i = count; i <= file_count; i++) {
			
			try {

				String inLine;
				BufferedReader in = new BufferedReader(new FileReader(new File("chain" + i)));
					
				while((inLine = in.readLine()) != null)
					if(inLine.length() > 0)
						chain.add(inLine);
				in.close();
					
			} catch (Exception e) { e.printStackTrace(); }
		}
	}
	
	/*
	 * This returns the count of the latest locally stored block.
	 */
	public long getMyLatestBlockCount() {

		String latest_block = getMyLatestBlock();

		try {
		
			if(latest_block != null)
				return Long.parseLong(latest_block.split("#")[0]);
	
		} catch (Exception e) {e.printStackTrace();}
		
		return -1;
	}
	
	/*
	 * This returns the latest locally stored block.
	 */
	public String getMyLatestBlock() {

		if(chain.size() > 0) return chain.getLast();
		return null;
	}
	
	public String getBlock(long n) {
		
		String block = null;
		
		// get from chain in memory
		for(String tmp : chain) {
			
			if(tmp.startsWith(n + "#")) {
				
				block = tmp;
				break;
			}
		}
		
		// get block from locally stored files
		if(block == null) {
		
			int fcount = ((int) n / 256) + 1;
			File targetFile = new File("chain" + fcount);
			
			if(storedChainFileName.equals("chain" + fcount)) {
				
				for(String s : storedChainFile) {
					if(s.startsWith(n + "#")) {
						
						block = s;
						break;
					}
				}
				
			} else {
			
				if(targetFile.exists()) {

					storedChainFileName = "chain" + fcount;
					storedChainFile.clear();
					
					try {
						
						String inLine;
						BufferedReader in = new BufferedReader(new FileReader(targetFile));
						
						while((inLine = in.readLine()) != null) {
							if(inLine.length() > 0) {
								if(inLine.startsWith(n + "#")) {
									
									block = inLine;
								}
								storedChainFile.add(inLine);
							}
						}
						in.close();
						
					} catch (Exception e) { e.printStackTrace(); }
				}
			}
		}

		return block;
	}
	
	/*
	 * Simply checks up to last three chain files; this is used to figure out if a transaction or crackaction is already in the chain.
	 * Checking the last three blocks is sufficient since a transaction or crackaction which is not mined also times out after 3/15 hours.
	 */
	public boolean isInChain_id(String id) {
		
		for(String block : chain)
			if(block.contains("TRA#" + id + "#") || block.contains("CRA#" + id + "#") || block.contains("HCL#" + id + "#") || block.contains("PCL#" + id + "#"))
				return true;

		return false;
	}
	
	/*
	 * Retrieves the current difficulty from latest 8 valid blocks (if possible) (24 is minimum difficulty).
	 * The difficulty is used for mining and for verifying new blocks and can only change +1/-1.
	 * It is only increased or decreased if the latest 8 blocks have the same difficulty.
	 */
	public int getDifficulty() {
		
		// first get difficulty of last block in chain, difficulty can only vary +1 or -1
		String[] latest_block = getMyLatestBlock().split("#");
		int latest_diff = Integer.valueOf(latest_block[latest_block.length-3]);
		
		// the genesis block uses less than 24
		if(latest_diff < 24) latest_diff = 24;
		
		long blockCount = getMyLatestBlockCount();
		
		if(blockCount < 9) return 24; // minimal difficulty
	
		LinkedList<String> blocks = new LinkedList<String>();
		for(long i = blockCount;  i >= (blockCount - 8); i--)
			blocks.add(getBlock(i));
		
		int[] timestamps = new int[9];
			
		// we can only change difficulty if the same difficulty value was used in the previous eight blocks
		for(int a = 0; a < 9; a++) {
				
			String[] tokens = blocks.get(a).split("#");
			timestamps[a] = Integer.parseInt(tokens[3]);
			
			if(a < 8) {

				// if difficulty is not equal to difficulty of latest nine blocks, break
				int diff = Integer.valueOf(tokens[tokens.length-3]);
					
				if(diff != latest_diff) 
					return latest_diff;
			}
			
		}
			
		int time_sum = timestamps[0] - timestamps[8];
			
		log.addEntry("Helper>getDifficulty()", "Required time for the last eight blocks: " + (time_sum / 60) + " minutes");
			
		// if solving time in the network for the last 8 blocks higher than 3 hours (22.5 minutes per block), decrease difficulty
		if(time_sum > (60 * 60 * 3)) {
			
			latest_diff--;
			if(latest_diff < 24) latest_diff = 24;
			log.addEntry("Helper>getDifficulty()", "(Decreased) difficulty to: " + latest_diff + ".");
			
		// if solving time in the network for the last 8 blocks lower than 1 hour (7.5 minutes per block), increase difficulty
		} else if(time_sum < (60 * 60 * 1)) {
				
			latest_diff++;
			log.addEntry("Helper>getDifficulty()", "Increased difficulty to: " + latest_diff + ".");
		}
		
		return latest_diff;
	}
	
	/*
	 *  Get unsolved crackactions from the blockchain which are younger than 12 hours
	 */
	public LinkedList<String> getCrackactionsFromChain() {
		
		LinkedList<String> crackactions = new LinkedList<String>();
		LinkedList<String> crackactions_ids_of_pcls = new LinkedList<String>();
		
		Object[] temp_blocks = chain.toArray();

		for(int i = temp_blocks.length-1; i >= 0; i--) {
			
			String tmp = (String) temp_blocks[i];
			String[] tokens = tmp.split("#");
						
			int ts = Integer.parseInt(tokens[3]);
			if(getCurrentTime() - ts > (60*60*12))
				return crackactions;
						
			int a = 6;
			while(a < (tokens.length - 11)) {
							
				if(tokens[a].equals("PCL")) {
								
					crackactions_ids_of_pcls.add(tokens[a+4]);
								
				} else if(tokens[a].equals("CRA")) {
								
					if(!crackactions_ids_of_pcls.contains(tokens[a+1])) {
								
						String  cra_tmp = "";
						for(int z = 0; z < 11; z++)
							cra_tmp += tokens[a+z] + "#";
						cra_tmp = cra_tmp.substring(0, cra_tmp.length()-1);
						crackactions.add(cra_tmp);
						a = a + 1;
					}	
				}
				a = a + 10;
			}
		}
		return crackactions;
	}
	
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
