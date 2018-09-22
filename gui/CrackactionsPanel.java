package gui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import storage.Store;

public class CrackactionsPanel implements MouseListener {
	
	public Button buttonSend = new Button("Send crackaction");
	public Button buttonUpdate = new Button("Update table");
	
	public JTextArea currentStatus = new JTextArea("Loading...");
	
	public Button b1 = new Button("Copy id to clipboard");
	public Button b2 = new Button("Copy sender to clipboard");
	public Button b3 = new Button("Copy hash to clipboard");
	public Button b4 = new Button("Copy solution to clipboard");
	
	public JLabel transferTableLabel = new JLabel("Crackactions from your addresses and cracked by you:");
	public JTable transferTable = null;

	public JLabel send_From = new JLabel("From:");
	public JComboBox<String> send_From_Field = null;
	
	public JLabel send_Value = new JLabel("Reward:");
	public JTextField send_Value_Field = new JTextField(32);
	public JLabel send_Value2 = new JLabel("(minimum 3 coins, 1 coin fee)");
	
	private JLabel algo_Label = new JLabel("Algorithm used to create data:");
	private String[] algo_comboString = {"MD5",  "SHA-1", "SHA-256", "SHA-384", "SHA-512"};
	public JComboBox algo_Box = new JComboBox(algo_comboString);
	
	private JLabel hashArea_Label = new JLabel("Hash:");
	private JLabel saltArea_Label = new JLabel("Salt:");
	
	public JTextArea hashArea = new JTextArea(128, 2);
	public JTextArea saltArea = new JTextArea(128, 2);
	
	public JTextArea info = new JTextArea(16, 4);

	private Store store = null;
	
	public boolean hashAreaCleared = false;
	public boolean saltAreaCleared = false;
	
	public CrackactionsPanel(Store store) {
		
		this.store = store;
	}
	
	public JPanel get(JFrame frame) {
		
		JPanel panel1 = new JPanel();
		panel1.setLayout(null);
		
		transferTableLabel.setBounds(24, 260, 600, 40);
		transferTableLabel.setFont(new Font("Verdana", Font.PLAIN, 18));
		panel1.add(transferTableLabel);
		
		String[] transferTableColumns = {"Status", "Id", "Timestamp", "From", "Reward", "Hash", "Salt", "Algorithm", "Solution"};
		transferTable = new JTable(new DefaultTableModel(transferTableColumns, 0));
		
		transferTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
	        public void valueChanged(ListSelectionEvent event) {
	        	try {
		            String id = transferTable.getValueAt(transferTable.getSelectedRow(), 1).toString();
		            String from_addr = transferTable.getValueAt(transferTable.getSelectedRow(), 3).toString();
		            
		            String hash = transferTable.getValueAt(transferTable.getSelectedRow(), 5).toString();
		            if(hash.length() > 64) hash = hash.substring(0, 64) + "...";
		            
		            String solution = transferTable.getValueAt(transferTable.getSelectedRow(), 8).toString();
		            
		            if(solution.length() < 1) solution = "[ not found yet ]";
		            currentStatus.setText("\n    ID:\t\t" + id + "\n    FROM:\t" + from_addr + "\n    HASH:\t" + hash  + "\n    SOLUTION:\t" + solution);
	        	} catch(Exception e) {}
	        }
	    });
		
		JScrollPane transferScrollPane = new JScrollPane(transferTable);
		transferScrollPane.setFont(new Font("Mono", Font.PLAIN, 16));
		transferScrollPane.setBounds(24, 320, 1546, 432);
		panel1.add(transferScrollPane);
		
		// sending panel (panel #1)
		send_From_Field = new JComboBox<String>(loadAddresses());
		send_From.setFont(new Font("Mono", Font.BOLD, 16));
		send_From.setBounds(24, 40, 50, 25); 
		panel1.add(send_From);
		send_From_Field.setFont(new Font("Courier New", Font.BOLD, 16));
		send_From_Field.setBounds(100, 40, 1120, 25);
		panel1.add(send_From_Field);

		send_Value.setBounds(24, 80, 70, 25); 
		panel1.add(send_Value);
		send_Value.setFont(new Font("Mono", Font.BOLD, 16));
		
		send_Value_Field.setBounds(100, 80, 100, 25);
		panel1.add(send_Value_Field);
		send_Value_Field.setFont(new Font("Mono", Font.BOLD, 16));
		send_Value_Field.setText("3.0");

		send_Value_Field.addKeyListener(new KeyAdapter() {
			
		    public void keyReleased(KeyEvent e) {
		    	
		        if (send_Value_Field.getText().length() > 16)
		        	e.consume();
		        
		        send_Value_Field.setText(send_Value_Field.getText().replaceAll("[\\,]", "."));
		        send_Value_Field.setText(send_Value_Field.getText().replaceAll("[^0-9\\.]", ""));
		        
		        int count = send_Value_Field.getText().length() - send_Value_Field.getText().replace(".", "").length();
		        if(count > 1) send_Value_Field.setText(send_Value_Field.getText().substring(0, send_Value_Field.getText().length()-1));
		    }  
		});
		
		send_Value2.setBounds(220, 78, 340, 25); 
		panel1.add(send_Value2);
		send_Value2.setFont(new Font("Mono", Font.BOLD, 16));
		
		algo_Label.setBounds(780, 80, 240, 25); 
		panel1.add(algo_Label);
		algo_Label.setFont(new Font("Mono", Font.BOLD, 16));
		
		algo_Box.setBounds(1040, 80, 180, 25); 
		panel1.add(algo_Box);
		algo_Box.setFont(new Font("Mono", Font.PLAIN, 16));
		
		hashArea_Label.setBounds(24, 130, 80, 25); 
		panel1.add(hashArea_Label);
		hashArea_Label.setFont(new Font("Mono", Font.BOLD, 16));
		
		hashArea.setBounds(100, 130, 1120, 40); 
		panel1.add(hashArea);
		hashArea.setFont(new Font("Courier New", Font.PLAIN, 15));
		hashArea.setLineWrap(true);
		hashArea.setWrapStyleWord(true);
		hashArea.setText("Please copy a hash in hex format into this field (e.g. a SHA256 hash).");
		
		hashArea.addKeyListener(new KeyAdapter() {
			
		    public void keyReleased(KeyEvent e) {
		    	
		        if (hashArea.getText().length() == 32) algo_Box.setSelectedIndex(0);
		        else if (hashArea.getText().length() == 40) algo_Box.setSelectedIndex(1);
		        else if (hashArea.getText().length() == 64) algo_Box.setSelectedIndex(2);
		        else if (hashArea.getText().length() == 96) algo_Box.setSelectedIndex(3);
		        else if (hashArea.getText().length() == 128) algo_Box.setSelectedIndex(4);
		    }  
		});
		
		hashArea.addMouseListener(this);
		
		saltArea_Label.setBounds(24, 190, 80, 25); 
		panel1.add(saltArea_Label);
		saltArea_Label.setFont(new Font("Mono", Font.BOLD, 16));
		
		saltArea.setBounds(100, 190, 1120, 40); 
		panel1.add(saltArea);
		saltArea.setFont(new Font("Courier New", Font.PLAIN, 15));
		saltArea.setLineWrap(true);
		saltArea.setWrapStyleWord(true);
		saltArea.setText("Please copy a salt in hex format into this field.The salt is appened to each string that is tested.\nA salt is not required. Leave empty if there is no salt.");
		
		saltArea.addMouseListener(this);
		
		buttonSend.setBounds(1290, 40, 280, 40);
		buttonSend.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonSend.addActionListener((ActionListener) frame);
		panel1.add(buttonSend);

		buttonUpdate.setBounds(1290, 90, 280, 40);
		buttonUpdate.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonUpdate.addActionListener((ActionListener) frame);
		panel1.add(buttonUpdate);
		
		info.setBounds(1290, 160, 270, 80);
		info.setFont(new Font("Mono", Font.PLAIN, 14));
		info.setEditable(false);
		info.setBackground(null);
		info.setText("If your sent crackaction will be solved,\nthe reward is transferred to the solver.\nIf it is not solved in 12 hours, one\ncoin of the reward is lost.");
		panel1.add(info);
		
		// status panel
		currentStatus.setBounds(24, 770, 900, 120);
		currentStatus.setBorder(BorderFactory.createLineBorder(Color.gray.LIGHT_GRAY, 2));
		currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
		currentStatus.setOpaque(true);
		currentStatus.setEditable(false);
		currentStatus.setText("\n\n\t\tWelcome to your crackactions.");
		panel1.add(currentStatus);
		
		b1.setBounds(950, 770, 300, 50);
		b1.setFont(new Font("Mono", Font.PLAIN, 14));
		b1.addActionListener((ActionListener) frame);
		panel1.add(b1);

		b2.setBounds(950, 840, 300, 50);
		b2.setFont(new Font("Mono", Font.PLAIN, 14));
		b2.addActionListener((ActionListener) frame);
		panel1.add(b2);
		
		b3.setBounds(1270, 770, 300, 50);
		b3.setFont(new Font("Mono", Font.PLAIN, 14));
		b3.addActionListener((ActionListener) frame);
		panel1.add(b3);
		
		b4.setBounds(1270, 840, 300, 50);
		b4.setFont(new Font("Mono", Font.PLAIN, 14));
		b4.addActionListener((ActionListener) frame);
		panel1.add(b4);
		
		return panel1;
	}

	public void updateTransferTableData() {
			
		// get own addresses of wallet
		while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> addresses = store.wallet.getMyAddresses();
		store.wallet.release();
		
		LinkedList<String[]> addRows = new LinkedList<String[]>();
		LinkedList<String> alreadyAddedIDs = new LinkedList<String>();
		
		// crackactions waiting for mining (solved and unsolved)
		while(!store.crackactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		String[] crackactions = store.crackactions.getItems();
		store.crackactions.release();

		for(String crackaction : crackactions) {
			
			String[] tokens = crackaction.split("#");
			
			for(String myaddr : addresses) {

				// from me
				if(tokens[8].equals(myaddr)) {
					
					if(!alreadyAddedIDs.contains(tokens[1])) {
						
						String salt = tokens[3]; if(salt.equals("0")) salt = "";
						addRows.add(new String[]{"pending", tokens[1], tokens[5], tokens[8], tokens[7], tokens[2], salt, tokens[4], ""});
					}
					alreadyAddedIDs.add(tokens[1]);
				}
			}
		}
		
		// crackactions waiting for mining (solved and unsolved)
		while(!store.mycrackactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> mycracks = store.mycrackactions.getItems();
		store.mycrackactions.release();
		
		// crackactions waiting for mining (solved and unsolved)
		while(!store.myclaimactions_solvers.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> claimactions_solvers = store.myclaimactions_solvers.getItems();
		store.myclaimactions_solvers.release();
		
		for(String crackaction : mycracks) {
			
			String[] tokens = crackaction.split("#");
			String salt = tokens[4]; if(salt.equals("0")) salt = "";
			String solution = null;
			int ts = Integer.parseInt(tokens[6]);
			
			for(String claimaction : claimactions_solvers)
				if(tokens[2].equals(claimaction.split("#")[5]))
					solution = new String(hexToBytes(claimaction.split("#")[6]));
			
			if(solution != null)
				addRows.add(new String[]{"solved (" + tokens[0] + ")", tokens[2], tokens[6], tokens[9], tokens[8], tokens[3], salt, tokens[5], solution});
			else if((getCurrentTime() - ts) > (60*60*12)) 
				addRows.add(new String[]{"unsolved (" + tokens[0] + ")", tokens[2], tokens[6], tokens[9], tokens[8], tokens[3], salt, tokens[5], ""});
			else
				addRows.add(new String[]{"pending (" + tokens[0] + ")", tokens[2], tokens[6], tokens[9], tokens[8], tokens[3], salt, tokens[5], ""});
		}
		
		// add other crackactions which are solved by this client
		while(!store.myclaimactions_public.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> myclaimactions_public = store.myclaimactions_public.getItems();
		store.myclaimactions_public.release();
		
		for(String pcl : myclaimactions_public) {
			
			String[] tokens = pcl.split("#");
			addRows.add(new String[]{"solved by you (" + tokens[0] + ")", tokens[5], tokens[4], "[hidden]", tokens[7], "[hidden]", "[hidden]", "[hidden]", "[hidden]"});
		}
		
		LinkedList<String[]> addRows_sorted = new LinkedList<String[]>();
		
		int max = 0;
		String[] maxItem = null;
		int size = addRows.size();
		
		for(int i = 0; i < size; i++) {
			
			for(String[] item : addRows) {
				
				if(Integer.parseInt(item[2]) > max) {
					
					maxItem = item;
					max = Integer.parseInt(item[2]);
				}
			}
			
			addRows.remove(maxItem);
			maxItem[2] = getDateFromTS(max);
			addRows_sorted.add(maxItem);
			max = 0;
		}
		
		// do not show more than 1024 crackactions for performance reasons
		while(addRows_sorted.size() > 1024) addRows_sorted.removeLast();
				
		DefaultTableModel model = (DefaultTableModel) transferTable.getModel();
		model.getDataVector().removeAllElements();
			
		for(String[] t1 : addRows_sorted) {
				
			model.addRow(t1);
		}

		model.fireTableDataChanged();
	}
	
	private String getDateFromTS(int ts) {
		
		Date date = new Date(ts); date.setTime((long) ts * 1000L);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return df.format(date);
	}
	
	private String[] loadAddresses() {
		
		while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> addresses = store.wallet.getMyAddresses();
		store.wallet.release();
		
		String[] tmp = new String[addresses.size()];
		for(int i = 0; i < addresses.size(); i++)
			tmp[i] = addresses.get(i);
		
		return tmp;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {

		if(!hashAreaCleared && arg0.getSource() == hashArea) {
			
			hashArea.setText("");
			hashAreaCleared = true;
		}	
		
		if(!saltAreaCleared && arg0.getSource() == saltArea) {
			
			saltArea.setText("");
			saltAreaCleared = true;
		}	
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {
		
		if(!hashAreaCleared && arg0.getSource() == hashArea) {
			
			hashArea.setText("");
			hashAreaCleared = true;
		}	
		
		if(!saltAreaCleared && arg0.getSource() == saltArea) {
			
			saltArea.setText("");
			saltAreaCleared = true;
		}	
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		
		if(!hashAreaCleared && arg0.getSource() == hashArea) {
			
			hashArea.setText("");
			hashAreaCleared = true;
		}	
		
		if(!saltAreaCleared && arg0.getSource() == saltArea) {
			
			saltArea.setText("");
			saltAreaCleared = true;
		}	
	}
	
	private byte[] hexToBytes(String s) {
		
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
	
	private int getCurrentTime() {
		
		return (int) (System.currentTimeMillis() / 1000L);
	}
}
