package storage;

import java.util.Collections;
import java.util.LinkedList;

public class ActionStore {
	
	private volatile boolean isFree = true; 
	private LinkedList<String> data = new LinkedList<String>();

	public ActionStore() {}
	
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
			
			if(!data.contains(s))
				data.add(s);
		
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void addItems(String[] s) {
		
		try {
			
			for(String t : s)
				if(!data.contains(t))
					data.add(t);
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void addItems(LinkedList<String> s) {
		
		try {
			
			for(String t : s)
				if(!data.contains(t))
					data.add(t);
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public String[] getItems() {
		
		Collections.shuffle(data);
		String[] tmp = new String[data.size()];
		
		try {
		
			for(int i = 0; i < data.size(); i++)
				tmp[i] = data.get(i);
		
		} catch (Exception e) { e.printStackTrace(); }
		
		return tmp;
	}
	
	public boolean containsItem(String s) {
		
		return data.contains(s);
	}
	
	public void removeItem(String s) {
		
		try {
			
			data.remove(s);
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	// This returns all ids stored in the transaction, crackaction or claimaction file.
	public LinkedList<String> getIDsForRequest() {

		LinkedList<String> ids = new LinkedList<String>();
		
		try {
		
			for(String item : data) 
				ids.add(item.trim().split("#")[1]);
			
		} catch (Exception e) { e.printStackTrace(); }
		
		return ids;
	}
	
	// This returns up to 64 unknown actions (which means actions contained in ids are not returned). 
	public LinkedList<String> getUnknownActions(String[] ids) {

		LinkedList<String> unknown_items = new LinkedList<String>();
		
		try {
		
			for(String item : data) {
				
				boolean found = false;
				
				String[] tokens = item.split("#");
				
				for(String id : ids) {
					if(id.equals(tokens[1])) {
						found = true;
						break;
					}
				}
				
				if(!found) unknown_items.add(item);
				if(unknown_items.size() >= 64) break;
			}
			
		} catch (Exception e) { e.printStackTrace(); }
		return unknown_items;
	}
}
