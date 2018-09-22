package gui;

import java.awt.Button;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import core.Logger;

public class LogsPanel {

	private Logger log = null;
	private JPanel panel = new JPanel();
	public JTable table = null;
	
	public Button b1 = new Button("Update");
	
	public LogsPanel(Logger log) {
		this.log = log;
	}
	
	public JPanel get(JFrame frame) {
		
		panel.setLayout(null);
		
		String[] transferTableColumns = {"Timestamp", "Source", "Entry"};
		table = new JTable(new DefaultTableModel(transferTableColumns, 0));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(20);
		table.setFont(new Font("Courier New", Font.PLAIN, 15));
		
		table.getColumnModel().getColumn(0).setMinWidth(200);
		table.getColumnModel().getColumn(0).setMaxWidth(200);
		table.getColumnModel().getColumn(2).setMinWidth(800);

		JScrollPane transferScrollPane = new JScrollPane(table);
		transferScrollPane.setBounds(24, 30, 1552, 800);
		panel.add(transferScrollPane);
		
		b1.setBounds(22, 840, 1554, 60);
		b1.setFont(new Font("Mono", Font.BOLD, 16));
		b1.addActionListener((ActionListener) frame); 
		panel.add(b1);
		
		return panel;
	}

	public void updateTable() {

		DefaultTableModel model = (DefaultTableModel) table.getModel();
		model.getDataVector().removeAllElements();
		
		LinkedList<String[]> addRows = log.getEntries();
		
		for(String[] t1 : addRows) 
			model.addRow(t1);

		model.fireTableDataChanged();
	}
	
	public void update() {
		
		
	}
}
