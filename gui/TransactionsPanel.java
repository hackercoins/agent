package gui;

import java.awt.Button;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import storage.Store;

public class TransactionsPanel {
	
	public Button buttonSend = new Button("Send transaction");
	public Button buttonUpdate = new Button("Update table");
	
	public Button b1 = new Button("Copy id to clipboard");
	public Button b2 = new Button("Copy sender to clipboard");
	public Button b3 = new Button("Copy receiver to clipboard");
	public Button b4 = new Button("Copy subject to clipboard");
	
	public JTextArea currentStatus = new JTextArea("Loading...");
	
	public JLabel transferTableLabel = new JLabel("Transactions from and to your addresses:");
	public JTable transferTable = null;

	public JLabel send_From = new JLabel("From:");
	public JComboBox<String> send_From_Field = null;
	
	public JLabel send_To = new JLabel("To:");
	public JTextField send_To_Field = new JTextField(128);
	
	public JLabel send_Subject = new JLabel("Subject:");
	public JTextField send_Subject_Field = new JTextField(128);
	public JLabel send_Subject2 = new JLabel("[A-Za-z0-9]");
	
	public JLabel send_Value = new JLabel("Value:");
	public JTextField send_Value_Field = new JTextField(32);
	public JLabel send_Value2 = new JLabel("(0.1 fee is subtracted)");
	
	public JLabel blocklabel = new JLabel("");
	
	private Store store = null;
	
	public TransactionsPanel(Store store) {
		
		this.store = store;
	}
	
	public JPanel get(JFrame frame) {
		
		JPanel panel1 = new JPanel();
		panel1.setLayout(null);
		
		// transferTable label
		transferTableLabel.setBounds(24, 170, 600, 40);
		transferTableLabel.setFont(new Font("Mono", Font.PLAIN, 18));
		panel1.add(transferTableLabel);
		
		// transferTable
		String[] transferTableColumns = {"Block", "Id", "Timestamp", "From", "To", "Value", "Subject"};
		transferTable = new JTable(new DefaultTableModel(transferTableColumns, 0));
		transferTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		transferTable.setFont(new Font("Courier New", Font.PLAIN, 16));
		transferTable.setRowHeight(20);
		
		transferTable.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
	        public void valueChanged(ListSelectionEvent event) {
	        	try {
	        		
	    			currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
		            String id = transferTable.getValueAt(transferTable.getSelectedRow(), 1).toString();
		            String from_addr = transferTable.getValueAt(transferTable.getSelectedRow(), 3).toString();
		            String to_addr = transferTable.getValueAt(transferTable.getSelectedRow(), 4).toString();
		            String subject = transferTable.getValueAt(transferTable.getSelectedRow(), 6).toString();
		            currentStatus.setForeground(Color.black);
		            currentStatus.setText("\n    ID:\t\t" + id + "\n    FROM:\t" + from_addr + "\n    TO:\t\t" + to_addr + "\n    SUBJECT:\t" + subject);
	        	} catch(Exception e) {}
	        }
	    });
		
		// transferTable panel
		JScrollPane transferScrollPane = new JScrollPane(transferTable);
		transferScrollPane.setFont(new Font("Courier New", Font.PLAIN, 16));
		transferScrollPane.setBounds(24, 220, 1546, 532);
		panel1.add(transferScrollPane);
		
		// sending panel (panel #1)
		send_From_Field = new JComboBox<String>(loadAddresses());
		send_From.setFont(new Font("Mono", Font.BOLD, 16));
		send_From.setBounds(24, 40, 50, 25); 
		panel1.add(send_From);
		send_From_Field.setFont(new Font("Courier New", Font.BOLD, 16));
		send_From_Field.setBounds(100, 40, 1120, 25); 
		panel1.add(send_From_Field);
		
		send_To.setFont(new Font("Mono", Font.BOLD, 16));
		send_To.setBounds(24, 80, 50, 25); 
		panel1.add(send_To);
		send_To_Field.setFont(new Font("Courier New", Font.BOLD, 16));
		send_To_Field.setBounds(100, 80, 1120, 25); 
		panel1.add(send_To_Field);
		
		send_Subject.setBounds(24, 120, 80, 25); 
		panel1.add(send_Subject);
		send_Subject.setFont(new Font("Mono", Font.BOLD, 16));
		
		send_Subject_Field.setBounds(100, 120, 300, 25); 
		panel1.add(send_Subject_Field);
		send_Subject_Field.setFont(new Font("Mono", Font.BOLD, 16));
		
		send_Subject_Field.addKeyListener(new KeyAdapter() {
			
		    public void keyReleased(KeyEvent e) {
		    	
		        if (send_Subject_Field.getText().length() > 32)
		        	e.consume();

		        send_Subject_Field.setText(send_Subject_Field.getText().replaceAll("[^A-Za-z0-9]", ""));
		    }  
		});

		send_Subject2.setBounds(410, 120, 100, 25); 
		panel1.add(send_Subject2);
		send_Subject2.setFont(new Font("Mono", Font.BOLD, 16));

		send_Value.setBounds(545, 120, 50, 25); 
		panel1.add(send_Value);
		send_Value.setFont(new Font("Mono", Font.BOLD, 16));
		
		send_Value_Field.setBounds(600, 120, 100, 25); 
		panel1.add(send_Value_Field);
		send_Value_Field.setFont(new Font("Mono", Font.BOLD, 16));
		
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
		
		send_Value2.setBounds(710, 120, 200, 25); 
		panel1.add(send_Value2);
		send_Value2.setFont(new Font("Mono", Font.BOLD, 16));
		
		buttonSend.setBounds(1290, 40, 280, 40);
		buttonSend.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonSend.addActionListener((ActionListener) frame);
		panel1.add(buttonSend);

		buttonUpdate.setBounds(1290, 90, 280, 40);
		buttonUpdate.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonUpdate.addActionListener((ActionListener) frame);
		panel1.add(buttonUpdate);
		
		blocklabel.setBounds(1340, 150, 280, 25);
		blocklabel.setFont(new Font("Mono", Font.PLAIN, 16));
		panel1.add(blocklabel);
		
		// status panel
		currentStatus.setBounds(24, 770, 900, 120);
		currentStatus.setBorder(BorderFactory.createLineBorder(Color.gray.LIGHT_GRAY, 2));
		currentStatus.setFont(new Font("Courier New", Font.PLAIN, 16));
		currentStatus.setOpaque(true);
		currentStatus.setEditable(false);
		currentStatus.setText("\n\n\t\tWelcome to your transactions.");
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
	
	// read transactions.tmp
	public void updateTransferTableData() {
			
		// get own addresses of wallet
		while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> addresses = store.wallet.getMyAddresses();
		store.wallet.release();
		
		// first read transactions file for own pending transactions
		LinkedList<String[]> addRows = new LinkedList<String[]>();
		LinkedList<String> addedIDs = new LinkedList<String>();
		
		while(!store.transactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		String[] transactions = store.transactions.getItems();
		store.transactions.release();
		
		for(String myaddr : addresses) {
		
			for(String transaction : transactions) {
			
				String[] tokens = transaction.split("#");

				if(tokens[2].equals(myaddr) || tokens[3].equals(myaddr)) {
					
					if(!addedIDs.contains(tokens[1])) {
						
						addRows.add(new String[]{"pending", tokens[1], tokens[5], tokens[2], tokens[3], tokens[4], tokens[6]});
						addedIDs.add(tokens[1]);
					}
				}
			}
		}
		
		while(!store.mytransactions.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> mytrans = store.mytransactions.getItems();
		store.mytransactions.release();
		
		// do not show more than 512 transactions for performance reasons
		while(mytrans.size() > 512) mytrans.removeFirst();
	
		while(!store.blocks.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		long local_count = store.blocks.getMyLatestBlockCount();
		store.blocks.release();

		blocklabel.setText("Latest block: " + local_count);
		
		for(String mytransaction : mytrans) {
					
			String[] tokens = mytransaction.split("#");
			
			if(!addedIDs.contains(tokens[2])) {
				
				addRows.add(new String[]{tokens[0], tokens[2], tokens[6], tokens[3], tokens[4], tokens[5], tokens[7]});
				addedIDs.add(tokens[2]);
			}
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
}
