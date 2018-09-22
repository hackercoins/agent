package server;

import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.regex.Pattern;

import core.Logger;
import core.Node;
import core.Robot;
import cracker.Cracker;
import creators.CreateAddress;
import gui.Gui;
import gui.PasswordFrame;
import gui.PasswordInitFrame;
import miner.Miner;
import storage.Store;

/*
 * This class listens on the TCP port 31337 for new incoming connections of other nodes.
 * New connections are processed in a thread safe queue.
 */
public class Server implements Runnable {
	
	private static Logger log = new Logger();
	private static Store store = null;
	
	public Server(String p) {

		store = new Store(p, log);
	}
	
	@Override
    public void run(){

        try {
        	
        	ServerSocket s = new ServerSocket(31337);
        	log.addEntry("Server>run()", "Server started on TCP port 31337.");

	        while(true) {
	        	
	        	Socket socket = s.accept();

	            try {

	            	new Thread(new ServerThread(socket, store, log)).start();
	                
	            } catch (Exception e) { e.printStackTrace(); socket.close();}
	        }
	        
        } catch (Exception e) { e.printStackTrace(); }
    }
	
	public static void main(String[] args) {
		
		if(args.length == 0) {

			String passwd = null;

			// ask for password (once if there is a wallet, twice if there is no wallet)
			if(!(new File("wallet").exists())) {
				
				PasswordInitFrame pf = new PasswordInitFrame();
				new Thread(pf).start();
	
				while(!pf.isSet())
					try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}
				
				passwd = pf.getPassword();
				
			} else {
				
				PasswordFrame pf = new PasswordFrame();
				new Thread(pf).start();
				
				while(!pf.isSet())
					try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace();}

				passwd = pf.getPassword();
			}
			
			Server server = new Server(passwd);
			new Thread(server).start();

			Node node = new Node(true, log, store);
			new Thread(node).start();
			
			// then synchronize the local chain and check for updates
			while(!node.isSynchro) try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
			
			new Thread(new Gui(store, log)).start();
			new Thread(new Miner(store, log)).start();
			new Thread(new Cracker(store, log)).start();
			
		}  else if(args.length == 4) { // no Gui mode

			if(args[0].startsWith("mining=") && args[1].startsWith("cracking=") && args[2].startsWith("send=") && args[3].startsWith("passwd=")) {

				Server server = new Server(args[3].substring("passwd=".length()));
				new Thread(server).start();

				// create a wallet if it does not already exist
				if(!(new File("wallet").exists()))
					(new CreateAddress()).create(store, log);

				Node node = new Node(false, log, store);
				new Thread(node).start();
				
				try {
					
					log.setConsole(true);
					
					if(!new File("config").exists()) {
					
						while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
						LinkedList<String> addr = store.wallet.getMyAddresses();
						store.wallet.release();
						
						String wordlist = "";
						for(File f : new File(".").listFiles()) {
							if(f.getName().startsWith("wordlist_")) {
								wordlist = f.getAbsolutePath().replace("/./", "/");
								break;
							}
						}
						
						String[] items = new String[]{args[0], "miningAddr=" + addr.getFirst(), args[1], "crackingAddr=" + addr.getFirst(), "crackingWordlistPath=" + wordlist, "crackingOption=0", "crackingCharset=ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", "crackingMaxLength=7"};

						PrintWriter pw = new PrintWriter(new File("config"));
						for(String item : items) pw.println(item);
						pw.close();
					}
					
					String toAddr = args[2].substring("addr=".length());
					Pattern addrPattern = Pattern.compile("^[A-F0-9]++$");
					if(toAddr.length() != 66 || !toAddr.startsWith("HC") || !addrPattern.matcher(toAddr.substring(2)).matches()) showUsage();

					new Thread(new Miner(store, log)).start();
					new Thread(new Cracker(store, log)).start();
					
					// additionally start Robot thread that sends coins once they are available
					new Thread(new Robot(store, log, toAddr)).start();
					
				} catch (Exception e) { showUsage(); }
			} else { showUsage(); }
		} else { showUsage(); }
	}
	
	public static void showUsage() {
		
		System.out.println("\n\n Usage: java -jar HackerCoins.jar mining=<threads> cracking=<threads> send=<rewards-to-this-address> passwd=<wallet-password>\n\n");
		System.exit(0);
	}
}