package core;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import gui.SynchroizationGui;
import gui.UpdateGui;

import storage.Store;
import verifiers.VerifyAndAddBlock;
import verifiers.VerifyClaimaction;
import verifiers.VerifyCrackaction;
import verifiers.VerifyTransaction;

/*
 * The node class is the main class of the Hacker-Coins agent software.
 * It continuously retrieves transactions, crackactions, claimactions and blocks from other nodes and pushes own items as well.
 * Furthermore, it continuously re-verifies locally stored items.
 */
public class Node implements Runnable {

	private int sleep = 2;
	
	public boolean isSynchro = false;
	private boolean hasGui = false;
	
	private String version = "01";
	
	private Logger log = null;
	private Store store = null;
	private Random rand = new Random();
	
	private Pattern ipv4Pattern = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);
	private Pattern ipv6Pattern = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}", Pattern.CASE_INSENSITIVE);

	public Node(boolean g, Logger log, Store store) {

		this.hasGui = g;
		this.log = log;
		this.store = store;
	}
	
	@Override
	public void run() {

		SynchroizationGui sg = null;

		if(hasGui) {
			
			sg = new SynchroizationGui();
			new Thread(sg).start();
		}
		
		if(hasGui) sg.progress += 1;
		else log.addEntry("Node>Node()", "Starting...");
		
		getNodes();
		if(hasGui) sg.progress += 6;
		
		// check for an update or at least if there is a connection
		if(hasGui) {
			
			UpdateGui ug = new UpdateGui(version); new Thread(ug).start();
			while(!ug.done) try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
		}
		
		// load locally stored blocks before sync
		while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		store.blocks.loadChain();
		long local_count = store.blocks.getMyLatestBlockCount();
		store.blocks.release();
		
		long remote_count = getRemoteBlockCount();
		
		if(hasGui) sg.progress += 3;
		
		log.addEntry("Node>Node()", "Synchronizing: local_count: " + local_count + " / remote_count: " + remote_count);

		// stepwise request and verify missing blocks while updating progress
		// try to download chain directly from the first two entry nodes via http; if this does not work, download it from other nodes
		if(remote_count > local_count) {
			
			double diff = (double) (90.0d / (remote_count - local_count));
			
			try {
				
				// first get a target node from which the chain can be downloaded
		    	LinkedList<String> downloadNodes = new LinkedList<String>();
				
				URL obj = new URL("https://www.hackercoins.org/nodes");
		    	HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		    	
		    	con.setRequestMethod("GET");
		
		    	if(con.getResponseCode() == 200) {
		    			
			    	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			    	String inputLine;
			    	
			    	while((inputLine = in.readLine()) != null) {
			    		downloadNodes.add(inputLine.trim());
			    	}
			    	in.close();
		    	}
		    	con.disconnect();
		    	
		    	// then try to download it from node one or from node two
		    	if(downloadNodes.size() >= 2) {
		    	
		    		log.addEntry("Node>Node()", "Synchronizing through web server...");
		    		
			    	Random rand = new Random();
			    	String downloadNode = downloadNodes.get(rand.nextInt(2));
			    	
			    	URL obj02 = new URL("http://" + downloadNode + "/chain/0");
			    	HttpURLConnection con02 = (HttpURLConnection) obj02.openConnection();
			    	con02.setRequestMethod("GET");
			
			    	if(con02.getResponseCode() == 200) {
			    			
				    	BufferedReader in = new BufferedReader(new InputStreamReader(con02.getInputStream()));
				    	String inputLine;
				    	
				    	VerifyAndAddBlock ab = new VerifyAndAddBlock();
				    	
				    	while((inputLine = in.readLine()) != null) {
				    		
				    		String[] tokens = inputLine.trim().split("#");
				    		long c = Long.valueOf(tokens[0]);
				    		
				    		if(c > local_count) {
				    			
				    			ab.verify(inputLine.trim(), store, log);
				    			
				    			while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				    			local_count = store.blocks.getMyLatestBlockCount();
				    			store.blocks.release();

				    			if(hasGui) sg.progress += diff;
								else log.addEntry("Node>Node()", "Synchronizing: local_count: " + local_count + " / remote_count: " + remote_count + " (http)");
				    		}
				    	}
				    	in.close();
			    	}
			    	con.disconnect();
		    	}
		    	
			} catch (Exception e) {
				e.printStackTrace();
				log.addEntry("Node>Node()", "Could not download chain directly, retrieving chain from other nodes.");
			}
		}
		
		while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		local_count = store.blocks.getMyLatestBlockCount();
		store.blocks.release();
		
		remote_count = getRemoteBlockCount();
		int breakCount = 0;
		
		// if this did not work completely, download blocks from other nodes
		if(remote_count > local_count) {
			
			double diff = (double) (90.0d / (remote_count - local_count));
			long last_count = local_count;
			
			while(remote_count > local_count) {
				
				try {Thread.sleep(200);} catch (InterruptedException e1) {e1.printStackTrace();}
				
				getBlock();
				
				while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				local_count = store.blocks.getMyLatestBlockCount();
				store.blocks.release();
				
				if(local_count > last_count) {
				
					if(hasGui) sg.progress += diff;
					else log.addEntry("Node>Node()", "Synchronizing: local_count: " + local_count + " / remote_count: " + remote_count);
					last_count = local_count;
				}
				
				if(local_count == remote_count) breakCount++;
				if(breakCount > 128) {
					
					log.addEntry("Node>Node()", "Error while synchronizing...");
					break;
				}
				
				while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				String[] nodes = store.nodes.getItems();
				store.nodes.release();
				
				if(nodes.length < 2) {
					
					try {Thread.sleep(2000);} catch (InterruptedException e1) {e1.printStackTrace();}
					getNodes();
				}
			}
		}
		if(hasGui) sg.progress = 100;
		isSynchro = true;

		while(true) {

			reVerifyAll();
			
			getNodes(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			getBlock(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			
			getTransactions(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			getCrackactions(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			getClaimactions(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}

			pushBlock(); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			pushActions("Transactions", "transactions"); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			pushActions("Crackactions", "crackactions"); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
			pushActions("Claimactions", "claimactions"); try { Thread.sleep(sleep * 1000); } catch (InterruptedException e) {e.printStackTrace();}
		}
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
							clientSocket.setSoTimeout(4000);
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

								outToServer.close();
								inFromServer.close();
								clientSocket.close();
								
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

	/*
	 *  Pushes transactions to another node while asking for required IDs first.
	 */
	private void pushActions(String request, String type) {
		
		// select a target node
		while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] nodes = store.nodes.getItems();
		store.nodes.release();
		
		// get all items of type
		String[] items = null;
		
		if(type.equals("transactions")) {
			
			while(!store.transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
			items =  store.transactions.getItems();
			store.transactions.release();
		}
		
		if(type.equals("crackactions")) {
			
			while(!store.crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
			items =  store.crackactions.getItems();
			store.crackactions.release();
		}
		
		if(type.equals("claimactions")) {
			
			while(!store.claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
			items =  store.claimactions.getItems();
			store.claimactions.release();
		}

		if(nodes.length > 0 && items.length > 0) {

			String targetNode = nodes[rand.nextInt(nodes.length)];
			
			try {
			
				// ask the node which items it already has (ids)
				String reply = sendRequestTarget("ask" + request, targetNode);
				
				if(reply != null) {
					
					String sendString = "";
					String[] asked_ids = null;
					
					if(reply.contains("%")) asked_ids = reply.split("%");
					else asked_ids = new String[]{reply};
	
					for(String item : items) {
						
						boolean doAdd = true;
						for(int i = 0; i < asked_ids.length; i++) {
							if(item.contains("#" + asked_ids[i] + "#")) {
								doAdd = false;
								break;
							}
						}
						if(doAdd) sendString += item + "%";
					}
					
					if(sendString.length() > 0) {
						sendRequestTarget("push" + request + ":" + sendString.substring(0, sendString.length()-1), targetNode);
					}
				}
			} catch(Exception e) { e.printStackTrace(); }
		}
	}

	/* 
	 * Pushes a block to a random node while asking for the required block count first.
	 */
	private void pushBlock() {

		while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] nodes = store.nodes.getItems();
		store.nodes.release();
		
		while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		long local_count = store.blocks.getMyLatestBlockCount();
		store.blocks.release();

		if(nodes.length > 0 && local_count > -1) {

			String targetNode = nodes[rand.nextInt(nodes.length)];
			
			try {
			
				// ask the node which items it already has
				String reply = sendRequestTarget("getBlockCount", targetNode);
				
				if(reply != null) {
				
					long remote_count = Long.parseLong(reply);
					
					if(local_count > remote_count) {
						
						long targetBlock = remote_count + 1;
						
						while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
						String block = store.blocks.getBlock(targetBlock);
						store.blocks.release();

						if(block != null)
							sendRequestTarget("pushBlock:" + block, targetNode);
					}
				}
			} catch(Exception e) { e.printStackTrace(); }
		}
	}

	/*
	 * Retrieves nodes and adds a node if the server can be reached an TCP port 31337.
	 * If there are no nodes available directly, it retrieves initial nodes from the web page of Hacker-Coins.
	 */
	private void getNodes() {
		
		while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] nodes = store.nodes.getItems();
		store.nodes.release();
			
		// get nodes from http if there are no other nodes in local node file
		if(nodes.length == 0) {

			log.addEntry("Node>getNodes()", "Could not retrieve nodes from another node. Trying to get some nodes from the web page.");

			try {

				URL obj = new URL("https://www.hackercoins.org/nodes");
		    	HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		    	con.setRequestMethod("GET");
		
		    	if(con.getResponseCode() == 200) {
		    			
			    	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			    	StringBuffer buffy = new StringBuffer();
			    	String inputLine;
			    		
			    	while((inputLine = in.readLine()) != null)
			    		buffy.append(inputLine.trim() + "%");
			    	in.close();
			    		
			    	String reply = buffy.toString().trim();
			    	reply = reply.substring(0, reply.length()-1);

					while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
					
					if(reply != null) {
						if(reply.contains("%")) store.nodes.addItems(reply.split("%"));
						else store.nodes.addItem(reply);
					}
					
					store.nodes.release();
		    	}
		    	con.disconnect();

				//dman.replaceItems(new String[]{"5.189.167.81", "5.189.170.164", "173.249.55.85", "173.249.56.128", "173.212.233.168"}, "nodes");
		    	
			} catch (Exception e) {e.printStackTrace();}
			
		} else {
			
			String reply = sendRequest("getNodes");
			
			if(reply != null) {
				
				if(reply.length() > 8) {
				
					LinkedList<String> ips = new LinkedList<String>();
					
					if(reply.contains("%")) {
						
						String[] tmp = reply.split("%");
						for(String t : tmp)
							if(isIPAddress(t))
								ips.add(t);
					
					} else {
						
						if(isIPAddress(reply))
							ips.add(reply);
					}
	
					while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
					store.nodes.addItems(ips);
					store.nodes.release();
				}
			}
		}
	}
	
	/*
	 *  Retrieves up to 64 unknown transactions from a random remote node.
	 */
	private void getTransactions() {
		
		while(!store.transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		LinkedList<String> ids = store.transactions.getIDsForRequest();
		store.transactions.release();

		String request = "getTransactions:";
		
		if(ids.size() > 0) {
			
			for(String t : ids) request += t + "%";
			request = request.substring(0, request.length()-1);
		}
			
		String reply = sendRequest(request);
	
		if(reply != null) {
			
			if(reply.length() > 16) {
			
				VerifyTransaction vt = new VerifyTransaction();
				
				LinkedList<String> items = new LinkedList<String>();
				
				try {
					
					if(reply.contains("%")) {
						
						String[] tmp = reply.split("%");
						
						for(String t : tmp)
							if(vt.verify(t, store, false, log))
								items.add(t);
					
					} else {
						
						if(vt.verify(reply, store, false, log))
							items.add(reply);
					}
					
				} catch (Exception e) {e.printStackTrace(); }
				
				while(!store.transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				for(String item : items)
					store.transactions.addItem(item);
				store.transactions.release();
			}
		}
	}
	
	/*
	 * Retrieves up to 64 unknown crackactions (solved and unsolved) from random remote node.
	 */
	private void getCrackactions() {
		
		while(!store.crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		LinkedList<String> ids = store.crackactions.getIDsForRequest();
		store.crackactions.release();

		String request = "getCrackactions:";
		
		if(ids.size() > 0) {
			
			for(String t : ids) request += t + "%";
			request = request.substring(0, request.length()-1);
		}
			
		String reply = sendRequest(request);

		if(reply != null) {
			
			if(reply.length() > 16) {
				
				VerifyCrackaction vc = new VerifyCrackaction();
				
				LinkedList<String> items = new LinkedList<String>();
				
				try {
				
					if(reply.contains("%")) {
						
						String[] tmp = reply.split("%");
						
						for(String t : tmp)
							if(vc.verify(t, store, false, log))
								items.add(t);
						
					} else {
						
						if(vc.verify(reply, store, false, log))
							items.add(reply);
					}
				
				} catch (Exception e) {e.printStackTrace();}
				
				while(!store.crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				for(String item : items)
					store.crackactions.addItem(item);
				store.crackactions.release();
			}
		}
	}
	
	/*
	 *  Retrieves up to 64 unknown claimactions from random remote node.
	 */
	private void getClaimactions() {
		
		while(!store.claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		LinkedList<String> ids = store.claimactions.getIDsForRequest();
		store.claimactions.release();

		String request = "getClaimactions:";
		
		if(ids.size() > 0) {
			
			for(String t : ids) request += t + "%";
			request = request.substring(0, request.length()-1);
		}
			
		String reply = sendRequest(request);
	
		if(reply != null) {
			
			if(reply.length() > 16) {
				
				VerifyClaimaction vc = new VerifyClaimaction();
				
				LinkedList<String> items = new LinkedList<String>();
				
				try {
				
					if(reply.contains("%")) {
						
						String[] tmp = reply.split("%");
						
						for(String t : tmp)
							if(vc.verify(t, store, false, log))
								items.add(t);
						
					} else {
						
						if(vc.verify(reply, store, false, log))
							items.add(reply);
					}
				
				} catch (Exception e) {e.printStackTrace();}
				
				while(!store.claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				for(String item : items)
					store.claimactions.addItem(item);
				store.claimactions.release();
			}
		}
	}
	
	/*
	 *  Asks a random remote node for a new block based on the local chain count +1
	 */
	private void getBlock() {

		try {

			while(!store.blocks.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
			long my_count = store.blocks.getMyLatestBlockCount();
			store.blocks.release();
			
			String reply = sendRequest("getBlock:" + (my_count+1));
	
			if(reply != null) {

				if(new VerifyAndAddBlock().verify(reply, store, log)) {
					
					log.addEntry("Node>getBlock()", "Verified and added the new block " + Long.valueOf(reply.split("#")[0]));
					
				} else {
					
					log.addEntry("Node>getBlock()", "Could not verify the downloaded block.");
				}
			}
			
		} catch (Exception e) {e.printStackTrace();}
	}
	
	/*
	 *  Removes saved actions if they are already in the chain, timed-out or if balance is gone.
	 *  It verifies also the balance of multiple transaction/crackactions of the same address in sum.
	 */
	private void reVerifyAll() {
		
		while(!store.transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] transactions = store.transactions.getItems();
		store.transactions.release();
		
		VerifyTransaction verifyTransaction = new VerifyTransaction();
			
		for(String transaction : transactions) {
			if(!verifyTransaction.verify(transaction, store, false, log)) {
				
				while(!store.transactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				store.transactions.removeItem(transaction);
				store.transactions.release();
			}
		}

		while(!store.crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] crackactions = store.crackactions.getItems();
		store.crackactions.release();

		VerifyCrackaction verifyCrackaction = new VerifyCrackaction();
			
		for(String crackaction : crackactions) {
			if(!verifyCrackaction.verify(crackaction, store, false, log)) {
				
				while(!store.crackactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				store.crackactions.removeItem(crackaction);
				store.crackactions.release();
			}
		}
		
		while(!store.claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] claimactions = store.claimactions.getItems();
		store.claimactions.release();
		
		VerifyClaimaction verifyClaimaction = new VerifyClaimaction();
			
		for(String claimaction : claimactions) {
			if(!verifyClaimaction.verify(claimaction, store, false, log)) {
				
				while(!store.claimactions.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
				store.claimactions.removeItem(claimaction);
				store.claimactions.release();
			}
		}
	}
	
	/*
	 *  Sends a request to a target node and returns reply.
	 */
	private String sendRequestTarget(String req, String targetNode) {

		String reply = null;
		Socket clientSocket = null;
		DataOutputStream outToServer = null;
		BufferedReader inFromServer = null;
		
		try {

			clientSocket = new Socket(targetNode, 31337);
			clientSocket.setSoTimeout(8000);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeBytes(req + "\n");
						
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			reply = inFromServer.readLine().trim();
			
		} catch (Exception e) {

			log.addEntry("Node>sendRequestTarget()", "Could not connect to node " + targetNode + ". Removing " + targetNode + " from node-file.");
			
			while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e1) {e1.printStackTrace(); }}
			store.nodes.removeItem(targetNode);
			store.nodes.release();
			
		} finally {
			
			try {
					
				if(outToServer != null) outToServer.close();
				if(inFromServer != null) inFromServer.close();
				if(clientSocket != null) clientSocket.close();
					
			} catch (Exception e) {}
		}
		return reply;
	}
	
	/*
	 *  Sends a request to a random node and returns reply.
	 */
	private String sendRequest(String req) {

		String reply = null;
		
		while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] targetNodes = store.nodes.getItems();
		store.nodes.release();

		if(targetNodes != null) {
			
			if(targetNodes.length > 0) {

				String targetNode = targetNodes[rand.nextInt(targetNodes.length)];
				Socket clientSocket = null;
				DataOutputStream outToServer = null;
				BufferedReader inFromServer = null;
				
				try {

					clientSocket = new Socket(targetNode, 31337);
					clientSocket.setSoTimeout(8000);
					outToServer = new DataOutputStream(clientSocket.getOutputStream());
					outToServer.writeBytes(req + "\n");
					
					inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					reply = inFromServer.readLine();
					
					if(reply.contains("Error")) return null;

				} catch (Exception e) {

					log.addEntry("Node>sendRequest()", "Could not connect to node " + targetNode + ". Removing " + targetNode + " from node-file.");
					
					while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e1) {e1.printStackTrace(); }}
					store.nodes.removeItem(targetNode);
					store.nodes.release();
					
				} finally {
					
					try {
						
						if(outToServer != null) outToServer.close();
						if(inFromServer != null) inFromServer.close();
						if(clientSocket != null) clientSocket.close();
							
					} catch (Exception e) {}
				}
			}
		}

		return reply;
	}
	
	/*
	 *  Verifies an ipv4 or ipv6 address (from other nodes).
	 */
	private boolean isIPAddress(String ipAddress) {
		
		Matcher m1 = ipv4Pattern.matcher(ipAddress);
	    Matcher m2 = ipv6Pattern.matcher(ipAddress);

		if (m1.matches() || m2.matches())
			return true;
		
		log.addEntry("Helper>isIpAddress()", ipAddress + " is not a valid IP address.");
		return false;
	}
}
