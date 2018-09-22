package gui;

import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class InfosPanel {
	
	JPanel panel = new JPanel();
	
	public InfosPanel() {}
	
	public JPanel get(JFrame frame) {

		panel.setLayout(null);

		try {

			BufferedImage image = ImageIO.read(new File("resources/logo_new_big.png"));
			JLabel imglabel = new JLabel(new ImageIcon(image));
			imglabel.setBounds(180, 160, 324, 312);
			panel.add(imglabel);
			
		} catch (Exception e) {e.printStackTrace();}
		
		JTextArea area = new JTextArea();
		area.setBounds(640, 160, 800, 400);
		area.setFont(new Font("Mono", Font.PLAIN, 22));
		area.setEditable(false);
		area.setOpaque(false);
		
		String s = "";
		s += "HackerCoins is a free open-source cryptocurrency.\n";
		s += "You can use the agent software to create transaction or crackactions.\n\n";
		s += "A crackaction is the announcement of a reward for cracking a hash/salt.\n";
		s += "Similar to transactions, crackactions are mined to the blockchain as well.\n\n";
		
		s += "With this agent software, you can also mine blocks and solve crackactions.\n";
		s += "Both opportunities allow to earn new Hacker-Coins.\n\n";
		
		s += "If you have any questions, feel free to contact the Hacker-Coins-Team.\n";
		s += "Of course you can also contribute to the project.\n\n";
		s += "https://www.hackercoins.org\n";
		
		area.setText(s);
		panel.add(area);
		
		JTextArea area2 = new JTextArea();
		area2.setBounds(180, 660, 1200, 200);
		area2.setFont(new Font("Mono", Font.PLAIN, 26));
		area2.setEditable(false);
		area2.setOpaque(false);
		
		String s2 = "";
		s2 += "The HackerCoins software is published under the terms of the MIT license\n";
		s2 += "(https://opensource.org/licenses/MIT) and without any warranty.\n";
		
		area2.setText(s2);
		panel.add(area2);
		
		return panel;
	}
}
