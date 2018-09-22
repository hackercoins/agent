package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;

public class ConfigStore {
	
	private volatile boolean isFree = true; 
	private File target = new File("config");

	public ConfigStore() {}
	
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
	 * Returns a config option from the local config file.
	 */
	public String getConfigOption(String option) {
		
		String item = null;
		
		try {
			
			if(target.exists()) {
			
				String inLine;
				BufferedReader in = new BufferedReader(new FileReader(target));
				
				while((inLine = in.readLine()) != null) {
	
					if(inLine.length() > 0) {
						
						if(inLine.startsWith(option)) {
							
							item = inLine.substring(option.length()).trim();
							if(item.length() == 0) item = null;
						}
					}
				}				
				in.close();
			
			// create default config and call function again
			} else {
				
				PrintWriter pw = new PrintWriter(target);
				
				pw.println("mining=0");
				pw.println("miningAddr=");
				pw.println("cracking=0");
				pw.println("crackingAddr=");
				pw.println("crackingWordlistPath=");
				pw.println("crackingOption=0");
				pw.println("crackingCharset=ABCDEFGHIJKLMNOPQRSTUVQXYZabcdefghijklmnopqrstuvwxyz0123456789");
				pw.println("crackingMaxLength=6");
				
				pw.close();

				return getConfigOption(option);
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		return item;
	}
	
	/*
	 * Sets a config option to the local config file.
	 */
	public void setConfigOption(String option, String value) {
		
		LinkedList<String> items = new LinkedList<String>();
		
		try {

			String inLine;
			BufferedReader in = new BufferedReader(new FileReader(target));
			
			while((inLine = in.readLine()) != null) {

				if(inLine.length() > 0) {
					
					if(!inLine.startsWith(option))
						items.add(inLine);
				}
			}				
			in.close();
			
			if(value != null) items.add(option + "=" + value);
			else items.add(option + "=");
			
			PrintWriter pw = new PrintWriter(target);
			for(String s : items) pw.println(s);
			pw.close();

		} catch (Exception e) { e.printStackTrace(); }
	}
}
