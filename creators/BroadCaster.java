package creators;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import core.Logger;
import storage.Store;

/*
 * This class is used to broadcast a new created transaction, crackaction, claimaction or solved blocks directly to 
 * other nodes in order to avoid loosing it if the creator goes offline directly after creation.
 */
public class BroadCaster implements Runnable {
	
	private Store store = null;
	private Logger log = null;
	
	private String request = null;
	
	public BroadCaster(Store store, String request, Logger log) {
		
		this.store = store;
		this.request = request;
		this.log = log;
	}

	@Override
	public void run() {
		
		// always broadcast to me first
		Socket clientSocket = null;
		DataOutputStream outToServer = null;
		BufferedReader inFromServer = null;
		
		try {	
			
			clientSocket = new Socket("127.0.0.1", 31337);
			clientSocket.setSoTimeout(8000);
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeBytes(request + "\n");
			
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			String reply = inFromServer.readLine();
				
		} catch (Exception e) {}
		
		finally {
				
			try {
				
				if(outToServer != null) outToServer.close();
				if(inFromServer != null) inFromServer.close();
				if(clientSocket != null) clientSocket.close();
						
			} catch (IOException e) {}
		}
		
		while(!store.nodes.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace(); }}
		String[] targetNodes = store.nodes.getItems();
		store.nodes.release();

		if(targetNodes != null) {
			if(targetNodes.length > 0) {
				
				for(int i = 0; i < targetNodes.length; i++) {
					
					String targetNode = targetNodes[i];
					
					Socket clientSocket02 = null;
					DataOutputStream outToServer02 = null;
					BufferedReader inFromServer02 = null;
					
					try {	
						
						clientSocket02 = new Socket(targetNode, 31337);
							
						 outToServer02 = new DataOutputStream(clientSocket02.getOutputStream());
						outToServer02.writeBytes(request + "\n");

						log.addEntry("Broadcaster>run()", "Broadcasting " + request.substring(0, 32) + " to " + targetNode);
						
						inFromServer02 = new BufferedReader(new InputStreamReader(clientSocket02.getInputStream()));
						String reply = inFromServer02.readLine();
							
					} catch (Exception e) {log.addEntry("Broadcaster>run()", "Could not broadcast to the remote node: " + targetNode);}
					
					finally {
						
						try {
								
							if(outToServer02 != null) outToServer02.close();
							if(inFromServer02 != null) inFromServer02.close();
							if(clientSocket02 != null) clientSocket02.close();
									
						} catch (IOException e) {}
					}
				}
			}
		}
	}
}
