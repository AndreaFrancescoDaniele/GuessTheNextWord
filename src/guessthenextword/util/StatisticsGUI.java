package guessthenextword.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StatisticsGUI extends JFrame {

	private static final long serialVersionUID = -6663521691956083579L;
	
	//==> Fields
	
	private JPanel contentPane;
	public JTextField currentJobField;
	public JTextField percentageField;
	public JTextField fileNameField;
	public JProgressBar threadBufferProgressBar;
	public JProgressBar fileBufferProgressBar;
	public JProgressBar percentageProgressBar;
	public JTextField etaField;
	public JTextField currentStepField;
	public JTextField threadBufferField;
	public JTextField fileBufferField;


	
	//==> Constructors
	
	public StatisticsGUI() {
		setTitle("GuessTheNextWord - Statistics GUI");
		setPreferredSize(new Dimension(300, 100));
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setPreferredSize(new Dimension(300, 100));
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel lblThreadBuffer = new JLabel("Thread Buffer");
		lblThreadBuffer.setHorizontalAlignment(SwingConstants.CENTER);
		lblThreadBuffer.setFont(new Font("Dialog", Font.BOLD, 11));
		lblThreadBuffer.setBounds(12, 12, 97, 15);
		contentPane.add(lblThreadBuffer);
		
		threadBufferProgressBar = new JProgressBar();
		threadBufferProgressBar.setValue(30);
		threadBufferProgressBar.setOrientation(SwingConstants.VERTICAL);
		threadBufferProgressBar.setBounds(41, 39, 46, 208);
		contentPane.add(threadBufferProgressBar);
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		separator.setBounds(121, 12, 10, 248);
		contentPane.add(separator);
		
		JLabel label = new JLabel("File Buffer");
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setFont(new Font("Dialog", Font.BOLD, 11));
		label.setBounds(138, 13, 97, 15);
		contentPane.add(label);
		
		fileBufferProgressBar = new JProgressBar();
		fileBufferProgressBar.setMaximum(1);
		fileBufferProgressBar.setOrientation(SwingConstants.VERTICAL);
		fileBufferProgressBar.setBounds(167, 40, 46, 207);
		contentPane.add(fileBufferProgressBar);
		
		JSeparator separator_1 = new JSeparator();
		separator_1.setOrientation(SwingConstants.VERTICAL);
		separator_1.setBounds(247, 13, 10, 248);
		contentPane.add(separator_1);
		
		JLabel lblCurrentJob = new JLabel("Current:");
		lblCurrentJob.setForeground(Color.GRAY);
		lblCurrentJob.setBounds(269, 79, 159, 15);
		contentPane.add(lblCurrentJob);
		
		currentJobField = new JTextField();
		currentJobField.setEditable(false);
		currentJobField.setForeground(Color.BLACK);
		currentJobField.setBorder(null);
		currentJobField.setBackground(UIManager.getColor("Button.background"));
		currentJobField.setFont(new Font("Dialog", Font.BOLD, 13));
		currentJobField.setHorizontalAlignment(SwingConstants.CENTER);
		currentJobField.setBounds(255, 96, 185, 19);
		contentPane.add(currentJobField);
		currentJobField.setColumns(10);
		
		JLabel lblPercentage = new JLabel("Percentage:");
		lblPercentage.setForeground(Color.GRAY);
		lblPercentage.setBounds(269, 178, 159, 15);
		contentPane.add(lblPercentage);
		
		percentageField = new JTextField();
		percentageField.setEditable(false);
		percentageField.setHorizontalAlignment(SwingConstants.CENTER);
		percentageField.setForeground(Color.BLACK);
		percentageField.setFont(new Font("Dialog", Font.PLAIN, 10));
		percentageField.setColumns(10);
		percentageField.setBorder(null);
		percentageField.setBackground(UIManager.getColor("Button.background"));
		percentageField.setBounds(398, 197, 30, 19);
		contentPane.add(percentageField);
		
		JLabel lblFile = new JLabel("File:");
		lblFile.setForeground(Color.GRAY);
		lblFile.setBounds(269, 133, 159, 15);
		contentPane.add(lblFile);
		
		fileNameField = new JTextField();
		fileNameField.setEditable(false);
		fileNameField.setHorizontalAlignment(SwingConstants.CENTER);
		fileNameField.setForeground(Color.BLACK);
		fileNameField.setFont(new Font("Dialog", Font.BOLD, 13));
		fileNameField.setColumns(10);
		fileNameField.setBorder(null);
		fileNameField.setBackground(UIManager.getColor("Button.background"));
		fileNameField.setBounds(260, 149, 180, 19);
		contentPane.add(fileNameField);
		
		percentageProgressBar = new JProgressBar();
		percentageProgressBar.setBounds(270, 199, 125, 14);
		contentPane.add(percentageProgressBar);
		
		JLabel lblEta = new JLabel("ETA:");
		lblEta.setForeground(Color.GRAY);
		lblEta.setBounds(269, 225, 159, 15);
		contentPane.add(lblEta);
		
		etaField = new JTextField();
		etaField.setHorizontalAlignment(SwingConstants.CENTER);
		etaField.setForeground(Color.BLACK);
		etaField.setFont(new Font("Dialog", Font.BOLD, 14));
		etaField.setEditable(false);
		etaField.setColumns(10);
		etaField.setBorder(null);
		etaField.setBackground(UIManager.getColor("Button.background"));
		etaField.setBounds(274, 241, 134, 19);
		contentPane.add(etaField);
		
		JLabel lblCurrentStep = new JLabel("Current Step:");
		lblCurrentStep.setHorizontalAlignment(SwingConstants.CENTER);
		lblCurrentStep.setForeground(Color.GRAY);
		lblCurrentStep.setBounds(269, 12, 159, 15);
		contentPane.add(lblCurrentStep);
		
		currentStepField = new JTextField();
		currentStepField.setHorizontalAlignment(SwingConstants.CENTER);
		currentStepField.setForeground(Color.BLACK);
		currentStepField.setFont(new Font("Dialog", Font.BOLD, 14));
		currentStepField.setEditable(false);
		currentStepField.setColumns(10);
		currentStepField.setBorder(null);
		currentStepField.setBackground(UIManager.getColor("Button.background"));
		currentStepField.setBounds(280, 35, 134, 19);
		contentPane.add(currentStepField);
		
		threadBufferField = new JTextField();
		threadBufferField.setHorizontalAlignment(SwingConstants.CENTER);
		threadBufferField.setForeground(Color.BLACK);
		threadBufferField.setFont(new Font("Dialog", Font.PLAIN, 10));
		threadBufferField.setEditable(false);
		threadBufferField.setColumns(10);
		threadBufferField.setBorder(null);
		threadBufferField.setBackground(UIManager.getColor("Button.background"));
		threadBufferField.setBounds(20, 250, 87, 19);
		contentPane.add(threadBufferField);
		
		fileBufferField = new JTextField();
		fileBufferField.setHorizontalAlignment(SwingConstants.CENTER);
		fileBufferField.setForeground(Color.BLACK);
		fileBufferField.setFont(new Font("Dialog", Font.PLAIN, 10));
		fileBufferField.setEditable(false);
		fileBufferField.setColumns(10);
		fileBufferField.setBorder(null);
		fileBufferField.setBackground(UIManager.getColor("Button.background"));
		fileBufferField.setBounds(146, 250, 87, 19);
		contentPane.add(fileBufferField);
	}//StatisticsGUI
	
	
	
	//==> Methods
	
	@Override
	public void dispose(){
		int a = Statistics.intNote("fileBufferLength");
		int b = Statistics.intNote("threadBufferLength");
		//
		boolean canClose = false;
		while( !canClose ){
			if( a+b <= 0 ){
				canClose = true;
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) { canClose = true; }
			}
			a = Statistics.intNote("fileBufferLength");
			b = Statistics.intNote("threadBufferLength");
		}
		setVisible(false);
		super.dispose();
	}//dispose
	
}//StatisticsGUI
