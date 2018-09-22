package gui;

import java.awt.Button;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import storage.Store;

public class OptionsPanel {
	
	private JLabel mining_Label1 = new JLabel("Participate in mining:");
	private String[] mining_comboString1 = {"no mining",  "1 thread", "2 threads", "3 threads", "4 threads", "5 threads", "6 threads", "7 threads", "8 threads", "9 threads", "10 threads", "11 threads", "12 threads", "13 threads", "14 threads", "15 threads", "16 threads"};
	public JComboBox mining_Box1 = new JComboBox(mining_comboString1);
	
	private JLabel mining_Label2 = new JLabel("Mining reward on this address:");
	public JComboBox mining_Box2 = new JComboBox();
	
	private JLabel cracking_Label1 = new JLabel("Participate in cracking:");
	private String[] cracking_comboString1 = {"no cracking",  "1 thread", "2 threads", "3 threads", "4 threads", "5 threads", "6 threads", "7 threads", "8 threads", "9 threads", "10 threads", "11 threads", "12 threads", "13 threads", "14 threads", "15 threads", "16 threads"};
	public JComboBox cracking_Box1 = new JComboBox(cracking_comboString1);
	
	private JLabel cracking_Label2 = new JLabel("Cracking reward on this address:");
	public JComboBox cracking_Box2 = new JComboBox();
	
	private JLabel wordListLabel = new JLabel("Always use a wordlist first:");
	public JTextField wordListField = new JTextField();
	public Button wordListButton = new Button("select");
	
	private JLabel selectOption_1_Label = new JLabel("Select crackactions randomly for cracking");
	public JCheckBox selectOption_1_Box = new JCheckBox();
	
	private JLabel selectOption_2_Label = new JLabel("Always prefer crackactions with highest stated rewards");
	public JCheckBox selectOption_2_Box = new JCheckBox();
	
	private JLabel selectOption_3_Label = new JLabel("Always prefer crackactions with easiest algorithm");
	public JCheckBox selectOption_3_Box = new JCheckBox();
	
	private JLabel charsetLabel = new JLabel("Define a charset for cracking:");
	public JTextArea charsetField = new JTextArea(40, 2);
	
	private JLabel length_Label1 = new JLabel("Define a max-length that is tried:");
	private String[] length_comboString = {"6 characters",  "7 characters", "8 characters", "9 characters", "10 characters", "11 characters", "12 characters", "13 characters", "14 characters", "15 characters", "16 characters"};
	public JComboBox length_Box = new JComboBox(length_comboString);

	JPanel panel = new JPanel();
	
	private Store store = null;
	
	boolean options_loaded = false;
	
	public OptionsPanel(Store store) {
		
		this.store = store;
	}
	
	public JPanel get(JFrame frame) {
		
		options_loaded = false;
		panel.setLayout(null);
		
		mining_Label1.setBounds(120, 100, 200, 20);
		mining_Label1.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(mining_Label1);
		
		mining_Box1.setSelectedIndex(0);
		mining_Box1.setBounds(420, 100, 160, 20);
		mining_Box1.setFont(new Font("Courier New", Font.BOLD, 16));
		mining_Box1.addActionListener((ActionListener) frame);
		panel.add(mining_Box1);
		
		mining_Label2.setBounds(120, 140, 300, 20);
		mining_Label2.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(mining_Label2);
		
		mining_Box2 = new JComboBox(new String[]{"no addresses available"});
		mining_Box2.setBounds(420, 140, 830, 20);
		mining_Box2.setFont(new Font("Courier New", Font.BOLD, 16));
		mining_Box2.addActionListener((ActionListener) frame);
		panel.add(mining_Box2);
		
		cracking_Label1.setBounds(120, 220, 200, 20);
		cracking_Label1.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(cracking_Label1);
		
		cracking_Box1.setSelectedIndex(0);
		cracking_Box1.setBounds(420, 220, 160, 20);
		cracking_Box1.setFont(new Font("Courier New", Font.BOLD, 16));
		cracking_Box1.addActionListener((ActionListener) frame);
		panel.add(cracking_Box1);
		
		cracking_Label2.setBounds(120, 260, 300, 20);
		cracking_Label2.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(cracking_Label2);
		
		cracking_Box2 = new JComboBox(new String[]{"no addresses available"});
		cracking_Box2.setBounds(420, 260, 830, 20);
		cracking_Box2.setFont(new Font("Courier New", Font.BOLD, 16));
		cracking_Box2.addActionListener((ActionListener) frame);
		panel.add(cracking_Box2);
		
		wordListLabel.setBounds(120, 360, 300, 20);
		wordListLabel.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(wordListLabel);

		wordListField.setBounds(420, 360, 720, 20);
		wordListField.setFont(new Font("Courier New", Font.PLAIN, 16));
		wordListField.addActionListener((ActionListener) frame);

		wordListField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e) {writeConfig();}

			@Override
			public void insertUpdate(DocumentEvent e) {writeConfig();}

			@Override
			public void removeUpdate(DocumentEvent e) {writeConfig();}
		});

		panel.add(wordListField);
		
		wordListButton.setBounds(1160, 352, 90, 28);
		wordListButton.setFont(new Font("Mono", Font.PLAIN, 16));
		wordListButton.addActionListener((ActionListener) frame);
		panel.add(wordListButton);
		
		selectOption_1_Box.setBounds(420, 410, 40, 20);
		selectOption_1_Box.addActionListener((ActionListener) frame);
		panel.add(selectOption_1_Box);
		
		selectOption_1_Label.setBounds(460, 410, 400, 20);
		selectOption_1_Label.setFont(new Font("Mono", Font.PLAIN, 16));
		panel.add(selectOption_1_Label);
		
		selectOption_2_Box.setBounds(420, 440, 40, 20);
		selectOption_2_Box.addActionListener((ActionListener) frame);
		panel.add(selectOption_2_Box);
		
		selectOption_2_Label.setBounds(460, 440, 400, 20);
		selectOption_2_Label.setFont(new Font("Mono", Font.PLAIN, 16));
		panel.add(selectOption_2_Label);
		
		selectOption_3_Box.setBounds(420, 470, 40, 20);
		selectOption_3_Box.addActionListener((ActionListener) frame);
		panel.add(selectOption_3_Box);
		
		selectOption_3_Label.setBounds(460, 470, 400, 20);
		selectOption_3_Label.setFont(new Font("Mono", Font.PLAIN, 16));
		panel.add(selectOption_3_Label);

		charsetLabel.setBounds(120, 540, 300, 20);
		charsetLabel.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(charsetLabel);

		charsetField.setBounds(420, 540, 830, 40);
		charsetField.setFont(new Font("Courier New", Font.PLAIN, 16));
		charsetField.setLineWrap(true);
		charsetField.setWrapStyleWord(true);
		
		charsetField.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent e) {writeConfig();}

			@Override
			public void insertUpdate(DocumentEvent e) {writeConfig();}

			@Override
			public void removeUpdate(DocumentEvent e) {writeConfig();}
		});

		panel.add(charsetField);

		length_Label1.setBounds(120, 620, 300, 20);
		length_Label1.setFont(new Font("Mono", Font.BOLD, 16));
		panel.add(length_Label1);

		length_Box.setBounds(420, 620, 200, 20);
		length_Box.setFont(new Font("Courier New", Font.BOLD, 16));
		length_Box.addActionListener((ActionListener) frame);
		panel.add(length_Box);
		
		options_loaded = true;
		return panel;
	}
	
	public void loadConfig() {

		options_loaded = false;
		String charset = "";
		
		while(!store.config.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}

		try {
			
			int index1 = Integer.valueOf(store.config.getConfigOption("mining="));
			mining_Box1.setSelectedIndex(index1);
			
			mining_Box2.setSelectedItem(store.config.getConfigOption("miningAddr="));
	
			int index2 = Integer.valueOf(store.config.getConfigOption("cracking="));
			cracking_Box1.setSelectedIndex(index2);
	
			cracking_Box2.setSelectedItem(store.config.getConfigOption("crackingAddr="));
			wordListField.setText(store.config.getConfigOption("crackingWordlistPath="));
	
			int option = Integer.parseInt(store.config.getConfigOption("crackingOption="));
			
			if(option == 0) {
									
				selectOption_1_Box.setSelected(true);
				selectOption_2_Box.setSelected(false);
				selectOption_3_Box.setSelected(false);
			}
			
			if(option == 1) {
									
				selectOption_1_Box.setSelected(false);
				selectOption_2_Box.setSelected(true);
				selectOption_3_Box.setSelected(false);
			}
			
			if(option == 2) {
									
				selectOption_1_Box.setSelected(false);
				selectOption_2_Box.setSelected(false);
				selectOption_3_Box.setSelected(true);
			}
			
			charset = store.config.getConfigOption("crackingCharset=");
			
			int index3 = Integer.valueOf(store.config.getConfigOption("crackingMaxLength=")) - 6;
			length_Box.setSelectedIndex(index3);
		
		} catch (Exception e) {e.printStackTrace(); }

		options_loaded = true;
		store.config.release();
		
		charsetField.setText(charset);
	}

	
	public void writeConfig() {
		
		while(!store.config.claim()) { try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}}
		
		try {
			
			store.config.setConfigOption("mining", String.valueOf(mining_Box1.getSelectedIndex()));
			store.config.setConfigOption("miningAddr", String.valueOf(mining_Box2.getSelectedItem()));
			store.config.setConfigOption("cracking", String.valueOf(cracking_Box1.getSelectedIndex()));
			store.config.setConfigOption("crackingAddr", String.valueOf(cracking_Box2.getSelectedItem()));
			store.config.setConfigOption("crackingWordlistPath", wordListField.getText().trim());

			int crackOption = 0;
			if(selectOption_1_Box.isSelected()) crackOption = 0; // random
			if(selectOption_2_Box.isSelected()) crackOption = 1; // best reward
			if(selectOption_3_Box.isSelected()) crackOption = 2; // easiest algorithm
			store.config.setConfigOption("crackingOption", String.valueOf(crackOption));
			
			store.config.setConfigOption("crackingCharset", charsetField.getText().trim());
			store.config.setConfigOption("crackingMaxLength", String.valueOf(length_Box.getSelectedIndex() + 6));
			
		} catch (Exception e) { e.printStackTrace(); }
		
		store.config.release();
	}
	
	public void loadAddresses() {
		
		while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> data = store.wallet.getMyAddresses();
		store.wallet.release();

		String[] tmp = new String[data.size()];
		for(int i = 0; i < data.size(); i++)
			tmp[i] = data.get(i);

		if(tmp.length > 0) {
			
			DefaultComboBoxModel model1 = new DefaultComboBoxModel(tmp);
			mining_Box2.setModel(model1);
			mining_Box2.revalidate();
			mining_Box2.repaint();
			
			DefaultComboBoxModel model2 = new DefaultComboBoxModel(tmp);
			cracking_Box2.setModel(model2);
			cracking_Box2.revalidate();
			cracking_Box2.repaint();
		}
	}
}
