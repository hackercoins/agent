package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Random;

import core.Logger;
import storage.Store;
import verifiers.VerifyAndAddBlock;
import verifiers.VerifyClaimaction;
import verifiers.VerifyCrackaction;
import verifiers.VerifyTransaction;

/*
 * This class processes incoming connection of other nodes. 
 * It creates a reply based on the request in a simple client/server relationship.
 * Connections are closed once the request is processed.
 */
public class ServerThread implements Runnable {

	private Socket socket = null;
    private Store store = null; 
    private Logger log = null;
    
    public ServerThread(Socket socket, Store store, Logger log) {
	
    	this.socket = socket;
    	this.store = store;
    	this.log = log;
	}

	@Override
    public void run() {
    	
		BufferedReader inFromClient = null;
		DataOutputStream outToClient = null;
		String source_ip = null;
		
		try {
	    	
	    	socket.setSoTimeout(8000);
	    	
			source_ip = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1).trim();
			inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		            	
			String input_str = inFromClient.readLine().trim();
			//log.addEntry("ServerThread>run()", "Received request: " + input_str);
			
			String reply = getReply(input_str);
		            		
			outToClient = new DataOutputStream(socket.getOutputStream());
			outToClient.writeBytes(reply + '\n');
			//log.addEntry("ServerThread>run()", "Replied with: " + reply);

		} catch (Exception e) {
			
			e.printStackTrace();

		} finally {
	        	
	        try {
	        	
				if(inFromClient != null) inFromClient.close();
				if(outToClient != null) outToClient.close();
				if(socket != null) socket.close();
					
	        } catch (Exception e) {}
		}
	        
		if(source_ip != null) {

			checkNode(source_ip); // add node if server is reachable
    	}
    }

    // these request-types exist (separator for tokens in requests is %)
	private String getReply(String req) {
	
		try {

			// asked for content from another node
			if(req.equals("getNodes")) return reply_getNodes();
			
			else if(req.startsWith("getTransactions:")) return reply_getUnknownActions(req.substring("getTransactions:".length()), "transactions");
			else if(req.startsWith("getCrackactions:")) return reply_getUnknownActions(req.substring("getCrackactions:".length()), "crackactions");
			else if(req.startsWith("getClaimactions:")) return reply_getUnknownActions(req.substring("getClaimactions:".length()), "claimactions");
			
			else if(req.startsWith("getBlock:")) return reply_getBlock(req.substring("getBlock:".length()));
			
			else if(req.equals("getBlockCount")) {
				
				while(!store.blocks.claim()) Thread.sleep(10);
				String tmp = String.valueOf(store.blocks.getMyLatestBlockCount());
				store.blocks.release();
				return tmp;
			}
			
			// broadcasted single item from another node
			else if(req.startsWith("publishTransaction:")) return reply_publishTransaction(req.substring("publishTransaction:".length()));
			else if(req.startsWith("publishCrackaction:")) return reply_publishCrackaction(req.substring("publishCrackaction:".length()));
			else if(req.startsWith("publishClaimaction:")) return reply_publishClaimaction(req.substring("publishClaimaction:".length()));
			else if(req.startsWith("publishBlock:")) return reply_publishBlock(req.substring("publishBlock:".length()));
			
			// pushed content from another node (and ask first what is required)
			else if(req.equals("askTransactions")) return reply_askIDs("transactions");
			else if(req.equals("askCrackactions")) return reply_askIDs("crackactions");
			else if(req.equals("askClaimactions")) return reply_askIDs("claimactions");
			
			else if(req.startsWith("pushTransactions:")) return reply_pushTransactions(req.substring("pushTransactions:".length()));
			else if(req.startsWith("pushCrackactions:")) return reply_pushCrackactions(req.substring("pushCrackactions:".length()));
			else if(req.startsWith("pushClaimactions:")) return reply_pushClaimactions(req.substring("pushClaimactions:".length()));
			else if(req.startsWith("pushBlock:")) return reply_publishBlock(req.substring("pushBlock:".length()));
			
			else if(req.equals("ping")) return "pong";

			log.addEntry("ServerThread>getReply()", "Received an unknown request: " + req.substring(0, 128));
			return "Error: Unknown request.";
			
		} catch (Exception e) {}
		
		log.addEntry("ServerThread>getReply()", "Received a malformed request: " + req);
		return "Error: Malformed request.";
	}

	// reply with up to 32 random nodes
	private String reply_getNodes() {

		while(!store.nodes.claim()) { try { Thread.sleep(10); } catch (Exception e) { e.printStackTrace(); }}
		String[] nodes = store.nodes.getItems();
		store.nodes.release();
		
		String tmp = "";
		int max = 0;
			
		for(int i = 0; i < tmp.length(); i++) {
				
			tmp += nodes[i] + "%";
				
			max++;
			if(max > 32) break;
		}

		if(tmp.length() > 0)
			tmp = tmp.substring(0, tmp.length()-1);

		return tmp;
	}
	
	// reply with transactions, crackactions or claimactions
	private String reply_getUnknownActions(String ids, String target) {

		String tmp = "";
		
		try {

			String[] ids_array = null;
			if(ids.contains("%")) ids_array = ids.split("%");
			else ids_array = new String[]{ids};
			
			LinkedList<String> items = null;
				
			if(target.equals("transactions")) {
					
				while(!store.transactions.claim()) Thread.sleep(10);
				items = store.transactions.getUnknownActions(ids_array);
				store.transactions.release();
			}
				
			else if(target.equals("crackactions")) {
					
				while(!store.crackactions.claim()) Thread.sleep(10);
				items = store.crackactions.getUnknownActions(ids_array);
				store.crackactions.release();
			}
				
			else if(target.equals("claimactions")) {
					
				while(!store.claimactions.claim()) Thread.sleep(10);
				items = store.claimactions.getUnknownActions(ids_array);
				store.claimactions.release();
			}
	
			for(String item : items) tmp += item + "%";
				
			if(tmp.length() > 0)
				tmp = tmp.substring(0, tmp.length()-1);
			
		} catch (Exception e) {e.printStackTrace();}
		
		return tmp;
	}
	
	// return block from chain if available
	private String reply_getBlock(String tmp) {

		String result = "Error.";

		try {

			long targetBlock = Long.parseLong(tmp);
			
			while(!store.blocks.claim()) Thread.sleep(10);
			String block = store.blocks.getBlock(targetBlock);
			store.blocks.release();
			
			if(block != null)
				result = block;

		} catch (Exception e) {e.printStackTrace();}
		return result;
	}

	private String reply_publishTransaction(String transaction) {
		
		log.addEntry("ServerThread>run()", "Received a new published transaction (" + transaction.substring(0, 32) + "...).");
		
		VerifyTransaction vt = new VerifyTransaction();
		
		if(vt.verify(transaction, store, false, log)) {
		
			while(!store.transactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
			store.transactions.addItem(transaction);
			store.transactions.release();
		}
		return "Ok.";
	}
	
	private String reply_publishCrackaction(String crackaction) {
		
		log.addEntry("ServerThread>run()", "Received a new published crackaction (" + crackaction.substring(0, 32) + "...).");
		
		VerifyCrackaction vc = new VerifyCrackaction();
		
		if(vc.verify(crackaction, store, false, log)) {
		
			while(!store.crackactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
			store.crackactions.addItem(crackaction);
			store.crackactions.release();
		}
		return "Ok.";
	}

	private String reply_publishClaimaction(String claimaction) {
		
		log.addEntry("ServerThread>run()", "Received a new published claimaction (" + claimaction.substring(0, 32) + "...).");
		
		VerifyClaimaction vc = new VerifyClaimaction();
		
		if(vc.verify(claimaction, store, false, log)) {
		
			while(!store.claimactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
			store.claimactions.addItem(claimaction);
			store.claimactions.release();
		}
		
		return "Ok.";
	}
	
	private String reply_publishBlock(String block) {
		
		log.addEntry("ServerThread>run()", "Received a new published block (" + block.substring(0, 16) + "...)");
		
		VerifyAndAddBlock vb = new VerifyAndAddBlock();
		vb.verify(block, store, log);

		return "Ok.";
	}
	
	// return IDs of actions this node has stored
	private String reply_askIDs(String type) {

		LinkedList<String> ids = null;
		
		if(type.equals("transactions")) {
			
			while(!store.transactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			ids = store.transactions.getIDsForRequest();
			store.transactions.release();
		}
		
		if(type.equals("crackactions")) {
			
			while(!store.crackactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			ids = store.crackactions.getIDsForRequest();
			store.crackactions.release();
		}
		
		if(type.equals("claimactions")) {
			
			while(!store.claimactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			ids = store.claimactions.getIDsForRequest();
			store.claimactions.release();
		}
		
		String reply = "";
		
		if(ids != null) {
			if(ids.size() > 0) {
				
				for(String t : ids) reply += t + "%";
				reply = reply.substring(0, reply.length()-1);
			}
		}
		
		return reply;
	}
	
	private String reply_pushTransactions(String s) {
		
		String[] items = null;
		if(s.contains("%")) items = s.split("%");
		else items = new String[]{s};
		
		if(items != null) {
			
			log.addEntry("ServerThread>run()", "Received a push request for transactions with " + items.length + " items.");
			
			while(!store.transactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			store.transactions.addItems(items);
			store.transactions.release();
		}
		return "Ok.";
	}
	
	private String reply_pushCrackactions(String s) {
		
		String[] items = null;
		if(s.contains("%")) items = s.split("%");
		else items = new String[]{s};
		
		if(items != null) {
			
			log.addEntry("ServerThread>run()", "Received a push request for crackactions with " + items.length + " items.");
			
			while(!store.crackactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			store.crackactions.addItems(items);
			store.crackactions.release();
		}
		return "Ok.";
	}
	
	private String reply_pushClaimactions(String s) {
		
		String[] items = null;
		if(s.contains("%")) items = s.split("%");
		else items = new String[]{s};
		
		if(items != null) {
			
			log.addEntry("ServerThread>run()", "Received a push request for claimactions with " + items.length + " items.");
			
			while(!store.claimactions.claim()) {try { Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
			store.claimactions.addItems(items);
			store.claimactions.release();
		}
		return "Ok.";
	}
	
	// add the node if online
    private void checkNode(String source_ip) {

    	if(source_ip.equals("127.0.0.1")) return;
    	
    	Random rand = new Random();
    	
    	if(rand.nextInt(3) == 1) {
    	
    		Socket clientSocket = null;
        	DataOutputStream outToServer = null;
        	BufferedReader inFromServer = null;
    		
			try {	
				
				clientSocket = new Socket(source_ip, 31337);
					
				outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeBytes("ping\n");
				
				inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String reply = inFromServer.readLine();
				
				if(reply.trim().equals("pong")) {
					
					while(!store.nodes.claim()) Thread.sleep(10);
					store.nodes.addItem(source_ip);
					store.nodes.release();
				}
					
			} catch (Exception e) {}
			finally {
						
				try {
						
					if(outToServer != null) outToServer.close();
					if(inFromServer != null) inFromServer.close();
					if(clientSocket != null) clientSocket.close();
							
				} catch (Exception e) {}
			}
    	}
	}
}
