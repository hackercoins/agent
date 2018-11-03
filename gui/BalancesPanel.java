package gui;

import java.awt.Button;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import server.Server;
import storage.Store;

public class BalancesPanel {
	
	public JTable table = null;
	public JLabel currentBalance = new JLabel("Loading your balances...", SwingConstants.CENTER);
	public JLabel currentRewards = new JLabel("", SwingConstants.CENTER);
	
	private Store store = null;
	
	public Button buttonCreateAddress = new Button("Create new address");
	public Button buttonCopyToClipboard = new Button("Copy marked address to clipboard");
	public Button buttonUpdateBalances = new Button("Update all balances");
	
	public BalancesPanel(Store store) {
		
		this.store = store;
	}
	
	public JPanel get(JFrame frame) {
		
		JPanel panel = new JPanel();
		panel.setLayout(null);
		
		try {

			URL url = Server.class.getResource("/resources/logo_new_small.png");
			JLabel imglabel = new JLabel(new ImageIcon(url));
			imglabel.setBounds(60, 25, 160, 160);
			panel.add(imglabel);
			
		} catch (Exception e) {e.printStackTrace();}

		currentBalance.setBounds(240, 20, 1370, 140);
		currentBalance.setBorder(new EmptyBorder(40, 40, 40, 40));
		currentBalance.setFont(new Font("Mono", Font.PLAIN, 48));
		panel.add(currentBalance);
		
		currentRewards.setBounds(240, 120, 1370, 40);
		currentRewards.setFont(new Font("Mono", Font.PLAIN, 20));
		currentRewards.setOpaque(true);
		panel.add(currentRewards);

		String[] transferTableColumns = {"Your address", "Balance of this address"};
		table = new JTable(new DefaultTableModel(transferTableColumns, 0));
		table.setFont(new Font("Courier New", Font.PLAIN, 24));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(1200);
		table.getColumnModel().getColumn(1).setPreferredWidth(342);
		table.setRowHeight(40);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		JScrollPane transferScrollPane = new JScrollPane(table);
		transferScrollPane.setFont(new Font("Courier New", Font.PLAIN, 18));
		transferScrollPane.setBounds(24, 210, 1546, 600);
		panel.add(transferScrollPane);
		
		buttonCreateAddress.setBounds(24, 830, 400, 60);
		buttonCreateAddress.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonCreateAddress.addActionListener((ActionListener) frame);
		panel.add(buttonCreateAddress);
		
		buttonCopyToClipboard.setBounds(600, 830, 400, 60);
		buttonCopyToClipboard.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonCopyToClipboard.addActionListener((ActionListener) frame);
		panel.add(buttonCopyToClipboard);
		
		buttonUpdateBalances.setBounds(1170, 830, 400, 60);
		buttonUpdateBalances.setFont(new Font("Mono", Font.PLAIN, 16));
		buttonUpdateBalances.addActionListener((ActionListener) frame);
		panel.add(buttonUpdateBalances);
		
		return panel;
	}
	
	public void loadBalances() {

		while(!store.wallet.claim()) {try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}}
		LinkedList<String> addresses = store.wallet.getMyAddresses();
		store.wallet.release();
		
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.getDataVector().removeAllElements();
		
		double sum = 0.0d;
		for(String addr : addresses) {
			
			sum += store.getMyBalance(addr);
			model.addRow(new String[]{addr, formatValue(store.getMyBalance(addr))});
		}
		
		currentBalance.setText("Your total balance: " + formatValue(sum));
		
		double[] rewards = store.getMyRewards();
		currentRewards.setText("(reward of mining: " + formatValue(rewards[0]) + " / reward of cracking: " + formatValue(rewards[1]) + ")");
	}
	
	private String formatValue(double balance) {
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.DOWN);
		String t = df.format(balance);
		return t.replace(",", ".").replace("-", "");
	}
}
