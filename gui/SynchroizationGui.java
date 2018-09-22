package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;

public class SynchroizationGui extends JFrame implements Runnable {

	public double progress = 0;
	private JProgressBar progressBar = new JProgressBar(0, 100);
	
	public SynchroizationGui() {
		
		this.setSize(800, 200);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
		
		this.setLayout(null);
		this.setTitle("Synchronizing...");
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		progressBar.setBounds(50, 40, 700, 80);
		progressBar.setForeground(Color.LIGHT_GRAY);
		progressBar.setFont(new Font("Mono", Font.BOLD, 20));
		this.add(progressBar);
		
		URL url = Gui.class.getResource("/resources/logo_new_icon.png");
		ImageIcon img = new ImageIcon(url);
		this.setIconImage(img.getImage());

		this.setVisible(true);
	}

	@Override
	public void run() {
		
		while(true) {

			progressBar.setValue((int) progress);
			if(progress >= 100) break;
			try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
		}
		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		this.setVisible(false);
	}
}
