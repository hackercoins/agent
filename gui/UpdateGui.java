package gui;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class UpdateGui implements ActionListener, Runnable {

	private JLabel l1 = new JLabel("");
	private JLabel l2 = new JLabel("Please download it from https://www.hackercoins.org.");
	private Button b1 = new Button("Continue");
	
	private JLabel l3 = new JLabel("Could not connect to the Internet.");
	private Button b2 = new Button("Abort");
	
	public boolean done = false;
	private String version = "";
	
	JFrame f = new JFrame();
	JFrame g = new JFrame();
	
	public UpdateGui(String version) {

		this.version = version;
	}
	
	public void showUpdateWindow(String new_version) {
		
		f.setSize(600, 280);
		l1.setText("There is a new version " + new_version + " of the software available.");
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		f.setLocation(dim.width/2-f.getSize().width/2, dim.height/2-f.getSize().height/2);
		
		f.setLayout(null);
		f.setTitle("HackerCoins.org");
		f.setResizable(false);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		URL url = Gui.class.getResource("/resources/logo_new_icon.png");
		ImageIcon img = new ImageIcon(url);
		f.setIconImage(img.getImage());
		
		l1.setBounds(60, 40, 500, 25);
		l1.setFont(new Font("Mono", Font.PLAIN, 20));
		f.add(l1);
		
		l2.setBounds(60, 80, 500, 25);
		l2.setFont(new Font("Mono", Font.PLAIN, 20));
		f.add(l2);

		b1.setBounds(340, 140, 200, 40);
		b1.setFont(new Font("Mono", Font.PLAIN, 20));
		b1.addActionListener(this);
		f.add(b1);
		
		f.setVisible(true);
	}
	
	public void showErrorWindow() {
		
		g.setSize(600, 240);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		g.setLocation(dim.width/2-g.getSize().width/2, dim.height/2-g.getSize().height/2);
		
		g.setLayout(null);
		g.setTitle("HackerCoins.org");
		g.setResizable(false);
		g.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		ImageIcon img = new ImageIcon("icon.png");
		g.setIconImage(img.getImage());
		
		l3.setBounds(80, 60, 500, 25);
		l3.setFont(new Font("Mono", Font.PLAIN, 24));
		g.add(l3);

		b2.setBounds(350, 130, 200, 40);
		b2.setFont(new Font("Mono", Font.PLAIN, 20));
		b2.addActionListener(this);
		g.add(b2);
		
		g.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == b1) {
			
			this.done = true;
			f.setVisible(false);
		}
		
		if(e.getSource() == b2) {
			
			System.exit(0);
		}
	}
	
	@Override
	public void run() {
		
		try {

			URL obj = new URL("https://www.hackercoins.org/version");
	    	HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
	    	con.setRequestMethod("GET");
	
	    	if(con.getResponseCode() == 200) {
	    			
		    	BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		    	StringBuffer buffy = new StringBuffer();
		    	String inputLine;
		    		
		    	if((inputLine = in.readLine()) != null)
		    		buffy.append(inputLine.trim());
		    	in.close();
		    		
		    	String reply = buffy.toString().trim();

		    	if(!reply.equals(version)) {
		    		
		    		showUpdateWindow(reply);
		    		return;
		    	}
		    	
	    	} else { /* showErrorWindow(); return; */}
	    	con.disconnect();
	    	
		} catch (Exception e) { showErrorWindow(); return;}
		this.done = true;
	}
}
