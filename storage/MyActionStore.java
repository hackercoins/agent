package storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.LinkedList;

public class MyActionStore {
	
	private volatile boolean isFree = true; 
	private File target = null;

	public MyActionStore(String fname) {
		
		this.target = new File(fname);
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
	
	public void addItem(String s) {

		try {
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(target, true));
			pw.println(s);
			pw.close();
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public LinkedList<String> getItems() {

		LinkedList<String> items = new LinkedList<String>();
		
		if(target.exists()) {
			
			try {
	
				String inLine;
				BufferedReader in = new BufferedReader(new FileReader(target));
				
				while((inLine = in.readLine()) != null)
					if(inLine.length() > 0)
						items.add(inLine);	
				
				in.close();
				
			} catch (Exception e) { e.printStackTrace(); }
		}

		return items;
	}
}
