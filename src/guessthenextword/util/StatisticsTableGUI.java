package guessthenextword.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StatisticsTableGUI extends JDialog {

	private static final long serialVersionUID = -7002080056970534856L;
	
	//==> Fields
	
	private JPanel contentPane;
	private Queue<String[]> data = new ConcurrentLinkedQueue<String[]>();
	public JTextField stopWordsExclField;
	private JLabel lblCombinedFileLoading;
	public JTextField intelliIOField;
	private JTable table;


	
	//==> Constructors
	
	public StatisticsTableGUI(final JFrame parent) {
		super(parent);
		setTitle("Auxiliary TableFrame");
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		Rectangle r = parent.getBounds();
		setBounds(r.x+r.width+4, r.y, 350, 300);
		//
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JSeparator separator = new JSeparator();
		separator.setBounds(10, 160, 324, 2);
		contentPane.add(separator);
		
		JLabel lblOptimizations = new JLabel("Optimization rules:");
		lblOptimizations.setForeground(Color.GRAY);
		lblOptimizations.setBounds(12, 168, 209, 15);
		contentPane.add(lblOptimizations);
		
		stopWordsExclField = new JTextField();
		stopWordsExclField.setHorizontalAlignment(SwingConstants.LEFT);
		stopWordsExclField.setForeground(Color.BLACK);
		stopWordsExclField.setFont(new Font("Dialog", Font.BOLD, 13));
		stopWordsExclField.setEditable(false);
		stopWordsExclField.setColumns(10);
		stopWordsExclField.setBorder(null);
		stopWordsExclField.setBackground(UIManager.getColor("Button.background"));
		stopWordsExclField.setBounds(35, 212, 287, 19);
		contentPane.add(stopWordsExclField);
		
		JLabel lblStopwordsExclusion = new JLabel("Words exclusion:");
		lblStopwordsExclusion.setForeground(Color.GRAY);
		lblStopwordsExclusion.setBounds(30, 195, 209, 15);
		contentPane.add(lblStopwordsExclusion);
		
		lblCombinedFileLoading = new JLabel("Intelligent I/O:");
		lblCombinedFileLoading.setForeground(Color.GRAY);
		lblCombinedFileLoading.setBounds(30, 234, 209, 15);
		contentPane.add(lblCombinedFileLoading);
		
		intelliIOField = new JTextField();
		intelliIOField.setHorizontalAlignment(SwingConstants.LEFT);
		intelliIOField.setForeground(Color.BLACK);
		intelliIOField.setFont(new Font("Dialog", Font.BOLD, 13));
		intelliIOField.setEditable(false);
		intelliIOField.setColumns(10);
		intelliIOField.setBorder(null);
		intelliIOField.setBackground(UIManager.getColor("Button.background"));
		intelliIOField.setBounds(35, 249, 287, 19);
		contentPane.add(intelliIOField);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(0, 0, 345, 155);
		contentPane.add(scrollPane);
		
		table = new JTable();
		table.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
				{null, null},
			},
			new String[] {"Thread file", "Progress"}
		));
		table.setPreferredSize(new Dimension(350, 130));
		table.setEnabled(false);
		table.setAutoscrolls(false);
		scrollPane.setViewportView(table);
		
		//add listener to the parent frame
		parent.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentMoved(ComponentEvent e) {
				Rectangle r = parent.getBounds();
				StatisticsTableGUI.this.setBounds(r.x+r.width+4, r.y, 350, 300);
			}
		});
	}//StatisticsTableGUI

	public void clearTable(){
		data.clear();
	}//clearTable
	
	public void addRow(String threadFile, int progress){
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%02d", progress)+"%\t | ");
		int prog = (int)((((double)progress)/100)*13);
		for( int i=0; i<prog; i++ ){
			sb.append('>');
		}
		data.add( new String[]{ threadFile, sb.toString() } );
	}//addRow
	
	
	public void refresh(){
		int dim = data.size();
		Object[][] objs = new Object[dim][2];
		int k = 0;
		for( String[] elem : data ){
			objs[k][0] = elem[0];
			objs[k][1] = elem[1];
			//
			k++;
		}
		//
		table.setModel(new DefaultTableModel(objs, new String[] {"Thread file", "Progress"}){
			private static final long serialVersionUID = 8529615666516384980L;
			@SuppressWarnings("rawtypes")
			Class[] columnTypes = new Class[] {
				String.class, String.class
			};
			public Class<?> getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});
	}//refresh
	
}//StatisticsTableGUI
