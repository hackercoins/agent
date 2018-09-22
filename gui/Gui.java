package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import core.Logger;
import creators.CreateAddress;
import creators.CreateCrackaction;
import creators.CreateTransaction;
import storage.Store;

public class Gui extends JFrame implements ActionListener, Runnable {
	
	private boolean loaded = false;
	
	private BalancesPanel p1 = null;
	private TransactionsPanel p2 = null;
	private CrackactionsPanel p3 = null;
	private OptionsPanel p4 = null;
	private LogsPanel p5 = null;
	private InfosPanel p6 = null;
	
	private Store store = null;
	private Logger log = null;
	
	private Pattern hexPattern = Pattern.compile("[A-F0-9]+");
	
	public Gui(Store store, Logger log) {
		
		this.store = store;
		this.log = log;
		
		this.p1 = new BalancesPanel(store);
		this.p2 = new TransactionsPanel(store);
		this.p3 = new CrackactionsPanel(store);
		this.p4 = new OptionsPanel(store);
		this.p5 = new LogsPanel(log);
		this.p6 = new InfosPanel();
	
		JFrame frame = new JFrame();
		frame.setSize(1600, 1000);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);

		frame.setTitle("HackerCoins.org (Version 0.1b)");
		frame.setResizable(false);

		URL url = Gui.class.getResource("/resources/logo_new_icon.png");
		ImageIcon img = new ImageIcon(url);
		frame.setIconImage(img.getImage());

		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.setFont(new Font("Mono", Font.PLAIN, 20));
		frame.getContentPane().add(tabbedPane);
		
		tabbedPane.addTab("Balances", p1.get(this));
		tabbedPane.addTab("Transactions", p2.get(this));
		tabbedPane.addTab("Crackactions", p3.get(this));
		tabbedPane.addTab("Options", p4.get(this));
		tabbedPane.addTab("Logs", p5.get(this));
		tabbedPane.addTab("Infos", p6.get(this));

	    ChangeListener changeListener = new ChangeListener() {
	        
	    	public void stateChanged(ChangeEvent changeEvent) {

	    		if(tabbedPane.getSelectedIndex() == 0) {
	    			
	    			p1.loadBalances();
	    		}
	    			
	    		if(tabbedPane.getSelectedIndex() == 1) {
	    			
	    			p2.updateTransferTableData();
	    			p2.send_Subject_Field.setText("");
	    			p2.send_To_Field.setText("");
	    			p2.send_Value_Field.setText("");
	    			p2.currentStatus.setForeground(Color.black);
	    			p2.currentStatus.setText("\n\n\t\tWelcome to your transactions.");
	    			p2.transferTable.getSelectionModel().clearSelection();
	    		}
	    		
	    		if(tabbedPane.getSelectedIndex() == 2) {

	    			p3.updateTransferTableData();
	    			p3.send_Value_Field.setText("3.0");
	    			p3.hashArea.setText("Please copy a hash in hex format into this field (e.g. a SHA256 hash)."); p3.hashAreaCleared = false;
	    			p3.saltArea.setText("Please copy a salt in hex format into this field.The salt is appened to each string that is tested.\nA salt is not required. Leave empty if there is no salt."); p3.saltAreaCleared = false;
	    			p3.currentStatus.setForeground(Color.black);
	    			p3.currentStatus.setText("\n\n\t\tWelcome to your crackactions.");
	    			p3.transferTable.getSelectionModel().clearSelection();
	    		}
	    		
	    		if(tabbedPane.getSelectedIndex() == 3) {
	    			
	    			p4.loadAddresses();
	    			p4.loadConfig();
	    		}
	    		
	    		if(tabbedPane.getSelectedIndex() == 4) {
	    			
	    			p5.updateTable();
	    		}
	        }
	    };
	    tabbedPane.addChangeListener(changeListener);

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		p1.loadBalances();
		p2.updateTransferTableData();
		p3.updateTransferTableData();
		p4.loadAddresses();
		p4.loadConfig();
		
		loaded = true;
	}
	
	public void actionPerformed(ActionEvent e) {
		
		if(e.getSource() == p1.buttonCreateAddress) {
			
			CreateAddress createAddress = new CreateAddress();
			String nAddr = createAddress.create(store, log);
			p2.send_From_Field.addItem(nAddr);
			p1.loadBalances();
		}
		
		if(e.getSource() == p1.buttonCopyToClipboard) {

			int s = p1.table.getSelectedRow();
			
			if(s >= 0) {
			
				String content = (String) p1.table.getValueAt(s, 0);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
			}
		}

		if(e.getSource() == p1.buttonUpdateBalances) {

			p1.loadBalances();
		}

		if(e.getSource() == p2.buttonSend) {
			
			p2.buttonSend.setEnabled(false);
			
			String senderAddr = ((String) p2.send_From_Field.getSelectedItem()).trim();
			String receiverAddr = p2.send_To_Field.getText().trim();
			String subject = p2.send_Subject_Field.getText();
			String valueStr = p2.send_Value_Field.getText().trim();

			CreateTransaction createTransaction = new CreateTransaction();
			String result = createTransaction.create(senderAddr, receiverAddr, subject, valueStr, store, log);
			
			p2.currentStatus.setFont(new Font("Mono", Font.BOLD, 18));
			p2.currentStatus.setForeground(Color.red);
			
			p2.currentStatus.setText("\n\n\t" + result);

			try {Thread.sleep(500);} catch (InterruptedException e1) {e1.printStackTrace();}
			p2.updateTransferTableData();

			p2.send_To_Field.setText("");
			p2.send_Value_Field.setText("");
			p2.send_Subject_Field.setText("");
			
			p2.buttonSend.setEnabled(true);
			
		}

		if(e.getSource() == p2.buttonUpdate) {

			p2.updateTransferTableData();
			p2.currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
			p2.currentStatus.setText("\n\n\tUpdated all transactions from and to your addresses.");
		}
		
		if(e.getSource() == p2.b1) {

			int s = p2.transferTable.getSelectedRow();
			p2.currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
			
			if(s >= 0) {
			
				String content = (String) p2.transferTable.getValueAt(s, 1);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p2.currentStatus.setText("\n\n\tCopied ID of the selected transaction.");
				
			} else p2.currentStatus.setText("\n\n\tYou need to select a transaction from the table.");
		}

		if(e.getSource() == p2.b2) {

			int s = p2.transferTable.getSelectedRow();
			p2.currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
			
			if(s >= 0) {
			
				String content = (String) p2.transferTable.getValueAt(s, 3);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p2.currentStatus.setText("\n\n\tCopied the sender's address of the selected transaction.");
				
			} else p2.currentStatus.setText("\n\n\tYou need to select a transaction from the table.");
		}
		
		if(e.getSource() == p2.b3) {

			int s = p2.transferTable.getSelectedRow();
			p2.currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
			
			if(s >= 0) {
			
				String content = (String) p2.transferTable.getValueAt(s, 4);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p2.currentStatus.setText("\n\n\tCopied the receiver's address of the selected transaction.");
				
			} else p2.currentStatus.setText("\n\n\tYou need to select a transaction from the table.");
		}

		if(e.getSource() == p3.buttonSend) {
			
			String senderAddr = ((String) p3.send_From_Field.getSelectedItem()).trim();
			String valueStr = p3.send_Value_Field.getText().trim();
			String algo = (String) p3.algo_Box.getSelectedItem();
			
			String hash = p3.hashArea.getText().trim().toUpperCase().replaceAll("\\s+","");
			
			String salt = "0";
			
			if(salt.length() > 2) {
				if(hexPattern.matcher(hash).matches()) {
					salt = p3.saltArea.getText().trim().toUpperCase().replaceAll("\\s+","");
				}
			}

			CreateCrackaction createCrackaction = new CreateCrackaction();
			String result = createCrackaction.create(hash, salt, algo, valueStr, senderAddr, store, log);

			p3.buttonSend.setEnabled(false);
			p3.currentStatus.setForeground(Color.red);
			p3.currentStatus.setText("\n\n\t" + result);
			p3.updateTransferTableData();
			
			try {Thread.sleep(1000);} catch (InterruptedException e1) {e1.printStackTrace();}
			p3.buttonSend.setEnabled(true);

			p3.send_Value_Field.setText("");
			p3.hashArea.setText("");
			p3.saltArea.setText("");
		}
		
		if(e.getSource() == p3.buttonUpdate) {

			p3.updateTransferTableData();
			p3.currentStatus.setText("\n\n\tUpdated all crackactions from your addresses and solved by you.");
		}

		if(e.getSource() == p3.b1) {

			int s = p3.transferTable.getSelectedRow();
			
			if(s >= 0) {
			
				String content = (String) p3.transferTable.getValueAt(s, 1);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p3.currentStatus.setText("\n\n\tCopied ID of the selected crackaction.");
				
			} else p3.currentStatus.setText("\n\n\tYou need to select a crackaction from the table.");
		}

		if(e.getSource() == p3.b2) {

			int s = p3.transferTable.getSelectedRow();
			
			if(s >= 0) {
			
				String content = (String) p3.transferTable.getValueAt(s, 3);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p3.currentStatus.setText("\n\n\tCopied the sender's address of the selected crackaction.");
				
			} else p3.currentStatus.setText("\n\n\tYou need to select a crackaction from the table.");
		}
		
		if(e.getSource() == p3.b3) {

			int s = p3.transferTable.getSelectedRow();
			
			if(s >= 0) {
			
				String content = (String) p3.transferTable.getValueAt(s, 5);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p3.currentStatus.setText("\n\n\tCopied the hash of the selected crackaction.");
				
			} else p3.currentStatus.setText("\n\n\tYou need to select a crackaction from the table.");
		}
		
		if(e.getSource() == p3.b4) {

			int s = p3.transferTable.getSelectedRow();
			
			if(s >= 0) {
			
				String content = (String) p3.transferTable.getValueAt(s, 8);
				StringSelection stringSelection = new StringSelection(content);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(stringSelection, null);
				p3.currentStatus.setText("\n\n\tCopied the solution of the selected crackaction.");
				
			} else p3.currentStatus.setText("\n\n\tYou need to select a crackaction from the table.");
		}
		
		if(e.getSource() == p4.cracking_Box1 || e.getSource() == p4.cracking_Box2 || e.getSource() == p4.mining_Box1 || e.getSource() == p4.mining_Box2
				|| e.getSource() == p4.wordListField || e.getSource() == p4.selectOption_1_Box || e.getSource() == p4.selectOption_2_Box || e.getSource() == p4.selectOption_3_Box
					|| e.getSource() == p4.charsetField || e.getSource() == p4.length_Box) {
			
			if(e.getSource() == p4.selectOption_1_Box) {
				
				if(p4.selectOption_1_Box.isSelected()) {
					
					p4.selectOption_2_Box.setSelected(false);
					p4.selectOption_3_Box.setSelected(false);
				}
			}
			
			if(e.getSource() == p4.selectOption_2_Box) {
				
				if(p4.selectOption_2_Box.isSelected()) {
					
					p4.selectOption_1_Box.setSelected(false);
					p4.selectOption_3_Box.setSelected(false);
				}
			} 
			
			if(e.getSource() == p4.selectOption_3_Box) {
				
				if(p4.selectOption_3_Box.isSelected()) {
					
					p4.selectOption_1_Box.setSelected(false);
					p4.selectOption_2_Box.setSelected(false);
				}
			}
			
			if(loaded && p4.options_loaded)
				p4.writeConfig();
		}
		
		if(e.getSource()== p4.wordListButton) {
			
			JFileChooser Chooser = new JFileChooser();
			Chooser.setMultiSelectionEnabled(true);
			Chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			int result = Chooser.showDialog(this,"Open/Save");
			
			if(result == JFileChooser.APPROVE_OPTION) {
				
				File file = Chooser.getSelectedFile();
				p4.wordListField.setText(file.getAbsolutePath());
				p4.writeConfig();
			}
		}
		
		if(e.getSource() == p5.b1)
			p5.updateTable();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
}
