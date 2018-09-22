package gui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

public class PasswordInitFrame extends JFrame implements ActionListener, KeyListener, Runnable {

	private JPasswordField t1 = new JPasswordField();
	private JPasswordField t2 = new JPasswordField();
	
	private JLabel l1 = new JLabel("Please define your password:");
	private JLabel l2 = new JLabel("Please retype your password:");
	private JLabel l3 = new JLabel("Your password needs to have at least 8 characters.");
	
	private Button b1 = new Button("Continue");
	private String password = null;
	
	public PasswordInitFrame() {
		
		this.setSize(700, 280);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		this.setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
		
		this.setLayout(null);
		this.setTitle("HackerCoins.org");
		this.setResizable(false);
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		
		URL url = Gui.class.getResource("/resources/logo_new_icon.png");
		ImageIcon img = new ImageIcon(url);
		this.setIconImage(img.getImage());
		
		l1.setBounds(40, 40, 300, 25);
		l1.setFont(new Font("Mono", Font.PLAIN, 20));
		this.add(l1);
		
		t1.setBounds(360, 40, 300, 25);
		t1.setFont(new Font("Mono", Font.PLAIN, 20));
		this.add(t1);
		
		l2.setBounds(40, 80, 300, 25);
		l2.setFont(new Font("Mono", Font.PLAIN, 20));
		this.add(l2);
		
		t2.setBounds(360, 80, 300, 25);
		t2.setFont(new Font("Mono", Font.PLAIN, 20));
		this.add(t2);
		
		t2.addKeyListener(new KeyAdapter() {
			
		    public void keyPressed(KeyEvent e) {l3.setText(""); }  
		});
		
		l3.setBounds(40, 140, 400, 40);
		l3.setFont(new Font("Mono", Font.ITALIC, 16));
		l3.setForeground(Color.lightGray);
		this.add(l3);
		
		b1.setBounds(460, 140, 200, 40);
		b1.setFont(new Font("Mono", Font.PLAIN, 20));
		b1.addActionListener(this);
		this.add(b1);

		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == b1) check();
	}
	
	private void check() {
		
		String p1 = String.valueOf(t1.getPassword());
		String p2 = String.valueOf(t2.getPassword());
		
		if(!p1.equals(p2)) {
			
			l3.setText("The two entered passwords are not equal.");
			return;
		}
		
		if(p1.length() < 8) {
			
			l3.setText("Your password needs to have at least 8 characters.");
			return;
		}
		password = p1;
		this.setVisible(false);
	}

	public String getPassword() {
		
		return password;
	}

	public boolean isSet() {
		
		if(password == null)
			return false;
		
		return true;
	}
	
	@Override
	public void run() {}

	@Override
	public void keyPressed(KeyEvent evt) {
		
		if (evt.getKeyCode()==KeyEvent.VK_ENTER) check();
	}

	@Override
	public void keyReleased(KeyEvent evt) {}

	@Override
	public void keyTyped(KeyEvent evt) {}
}
