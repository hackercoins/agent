package core;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.LinkedList;

import creators.CreateTransaction;
import storage.Store;

/*
 * The robot class automatically send available balance to a given address.
 * This feature is only used in the non-Gui mode.
 */
public class Robot implements Runnable {
	
	private Store store = null;
	private Logger log = null;
	private String targetAddress = null;
	
	public Robot(Store store, Logger log, String addr) {
		
		this.store = store;
		this.log = log;
		this.targetAddress = addr;
	}

	@Override
	public void run() {
		
		while(true) {
			
			while(!store.wallet.claim()) { try { Thread.sleep(10); } catch (Exception e) {e.printStackTrace();}}
			LinkedList<String> addresses = store.wallet.getMyAddresses();
			store.wallet.release();
			
			for(String address : addresses) {
			
				double value = store.getMyBalance(address);
				
				if(value > 0.1d) {
					
					CreateTransaction ct = new CreateTransaction();
					ct.create(address, targetAddress, "SentByRobot", formatValue(value), store, log);
					log.addEntry("Robot>run()", "Sent " + formatValue(value) + " to the address " + targetAddress + ".");
				}
			}
			try {Thread.sleep(1000 * 60 * 60);} catch (InterruptedException e) {e.printStackTrace();} // wait until a block is solved
		}
	}
	
	private String formatValue(double balance) {
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		String t = df.format(balance);
		return t.replace(",", ".").replace("-", "");
	}
}
