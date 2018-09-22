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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.WindowConstants;

public class PasswordFrame extends JFrame implements ActionListener, KeyListener, Runnable {

	private JPasswordField t1 = new JPasswordField();
	
	private JLabel l1 = new JLabel("Please enter your password:");
	private JLabel l3 = new JLabel("");
	
	private Button b1 = new Button("Continue");
	private String password = null;
	
	public PasswordFrame() {
		
		this.setSize(700, 220);
		
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
		
		t1.addKeyListener(new KeyAdapter() {
			
		    public void keyPressed(KeyEvent e) {l3.setText(""); }  
		});
		
		l3.setBounds(60, 100, 400, 40);
		l3.setFont(new Font("Mono", Font.ITALIC, 16));
		l3.setForeground(Color.lightGray);
		this.add(l3);
		
		b1.setBounds(460, 100, 200, 40);
		b1.setFont(new Font("Mono", Font.PLAIN, 20));
		b1.addActionListener(this);
		t1.addKeyListener(this);
		this.add(b1);

		this.setVisible(true);
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		if(evt.getSource() == b1) check();
	}
	
	private void check() {
		
		String p1 = String.valueOf(t1.getPassword());
		
		if(p1.length() < 8) {
			
			l3.setText("The entered password is not correct.");
			t1.setText("");
			return;
		}
		
		// read the wallet and try to decrypt the first string; if it works (three tokens) continue
		File target = new File("wallet");
		String s = "";
		
		try {

			if(target.exists()) {

				String inLine;
				BufferedReader in = new BufferedReader(new FileReader(target));
													
				if((inLine = in.readLine()) != null) {
					s = inLine;
				}				
				in.close();
			}
			
			String[] tokens = s.split("#");
			byte[] iv = Base64.getDecoder().decode(tokens[0].getBytes());
			byte[] ciphertext = Base64.getDecoder().decode(tokens[1].getBytes());
			
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(p1.toCharArray(), "Pepper".getBytes(), 65536, 256);
			SecretKey tmp = factory.generateSecret(spec);
			SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
			String decrypted = new String(cipher.doFinal(ciphertext), "UTF-8");
            
            String[] tokens2 = decrypted.split("#");
            
            if(tokens2 != null) {
            	if(tokens2[0].length() == 66) {
            		if(tokens2[0].startsWith("HC")) {
            			
            			password = p1;
            			this.setVisible(false);
            		}
            	}
            }
        
		} catch (Exception e) {}
		
		l3.setText("The entered password is not correct.");
		t1.setText("");
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
