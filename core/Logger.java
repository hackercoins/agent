package core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

/*
 * The logger logs all output from all classes to a file and to the Gui.
 * In non-Gui mode, the output is additionally printed to the console.
 */
public class Logger {
	
	private int maxSize = 200;
	private boolean showConsole = false;
	private LinkedList<String[]> entries = new LinkedList<String[]>();
	
	public Logger() {}
	
	public synchronized void addEntry(String source, String txt) {
		
		if(txt.length() > 128) txt = txt.substring(0, 128) + "...";
		
		long ts = System.currentTimeMillis();
		
		Date date = new Date();
		date.setTime(ts);
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = df.format(date);
		
		if(showConsole)
			System.out.println(timestamp + " / " + source + " / " + txt);
		
		entries.addFirst(new String[]{timestamp, source, txt});
		if(entries.size() > maxSize) entries.removeLast();
		
		try {
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(new File("log.txt"), true));
			pw.println(timestamp + ": " + source + " -> " +txt);
			pw.close();
			
		} catch (FileNotFoundException e) {e.printStackTrace();}
	}
	
	public synchronized LinkedList<String[]> getEntries() {
		return entries;
	}

	public void setConsole(boolean b) {
		showConsole = true;
	}
}
