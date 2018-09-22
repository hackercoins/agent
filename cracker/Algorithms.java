package cracker;

/*
 * This class defines the supported algorithms that can be used in crackactions.
 * In addition to each algorithm name, it defines the length of the corresponding hash string in hexadecimal.
 */
public class Algorithms {

	public Algorithms() {}
	
	public synchronized boolean isSupported(String s) {
		
		if(s.equals("MD5")) return true;
		if(s.equals("SHA-1")) return true;
		if(s.equals("SHA-256")) return true;
		if(s.equals("SHA-384")) return true;
		if(s.equals("SHA-512")) return true;
		return false;
	}

	public synchronized int getLengthOf(String s) {
		
		if(s.equals("MD5")) return 32;
		if(s.equals("SHA-1")) return 40;
		if(s.equals("SHA-256")) return 64;
		if(s.equals("SHA-384")) return 96;
		if(s.equals("SHA-512")) return 128;
		return 0;
	}
}
