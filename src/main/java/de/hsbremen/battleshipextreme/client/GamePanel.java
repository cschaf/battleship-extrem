package de.hsbremen.battleshipextreme.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GamePanel extends JPanel {

	private JLabel labelInfo;
	private JPanel panelGameArea;
	private BoardPanel panelEnemyBoard;
	private BoardPanel panelPlayerBoard;

	private JRadioButton radioButtonHorizontalOrientation;
	private JRadioButton radioButtonVerticalOrientation;
	private JComboBox comboBoxEnemySelection;
	private JButton buttonApplyEnemy;
	private JTextArea textAreaGameLog;
	private JTextArea textAreaChatWindow;
	private JTextField textFieldChatMessage;
	private JButton buttonSendMessage;
	
	private JLabel[] labelShipCount;
	private JLabel[][] labelShip;
	private JRadioButton[] radioButtonShipSelection;

	//////////////////////////////////////////////////////////////////
	// Constructor
	//////////////////////////////////////////////////////////////////
	
	public GamePanel() {
		
		int size = 10;
		
		this.setLayout(new BorderLayout());
		
		// Panel fuer die Spielsteuerung
		//navigationPanel = new NavigationPanel();
		this.add(createNavigationPanel(), BorderLayout.WEST);
		
		// Panel fuer die eigentliche Spielflaeche
		panelGameArea = new JPanel(new BorderLayout());
		this.add(panelGameArea);
		
		// Label fuer die Spielinformationen
		labelInfo = new JLabel("Hier stehen aktuelle Spielinformationen!", SwingConstants.CENTER);
		labelInfo.setFont(new Font("Tahoma", Font.BOLD, 24));
		labelInfo.setBackground(Color.orange);
		labelInfo.setOpaque(true);
		panelGameArea.add(labelInfo, BorderLayout.NORTH);
		
		// Spielbrett fuer den Gegner
		panelEnemyBoard = new BoardPanel("Enemy", size);
		panelGameArea.add(panelEnemyBoard, BorderLayout.WEST);

		// Spielbrett fuer den Player
		panelPlayerBoard = new BoardPanel("Yours", size);
		panelGameArea.add(panelPlayerBoard, BorderLayout.EAST);
	}
	
	//////////////////////////////////////////////////////////////////
	// create Panels
	//////////////////////////////////////////////////////////////////
	
	private JPanel createNavigationPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		
		// ship selection
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		
		panel.add(createShipSelectionPanel(), c);
		
		// orientation panel
		c.gridy++;
		panel.add(createOrientationPanel(), c);
		
		// enemy panel
		c.gridy++;
		panel.add(createEnemySelectionPanel(), c);
		
		// game log panel
		c.gridy++;
		c.weighty = 1;
		panel.add(createGameLogPanel(), c);
		
		// chat panel
		c.gridy++;
		c.weighty = 0;
		panel.add(createChatPanel(), c);

		return panel;
	}

	private JPanel createShipSelectionPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Your Ships"));
		
		// GridBagLayout Einschr�nkungen
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;

		// Labels f�r die Anzahl der Schiffe
		labelShipCount = new JLabel[4];

		for (int i = 0; i < labelShipCount.length; i++) {
			c.insets = new Insets(0, 3, 0, 5);
			c.gridx = 0;
			c.gridy = i;
			labelShipCount[i] = new JLabel("0x");
			labelShipCount[i].setFont(new Font("Tahoma", Font.BOLD, 15));
			panel.add(labelShipCount[i], c);
		}

		// Labels f�r die Schiffe
		labelShip = new JLabel[4][];
		labelShip[0] = new JLabel[5];
		labelShip[1] = new JLabel[4];
		labelShip[2] = new JLabel[3];
		labelShip[3] = new JLabel[2];

		for (int y = 0; y < labelShip.length; y++) {
			for (int x = 0; x < labelShip[y].length; x++) {
				c.insets = new Insets(0, 0, 1, 1);
				c.gridx = x + 1;
				c.gridy = y;
				c.ipadx = c.ipady = 30;
				labelShip[y][x] = new JLabel();
				labelShip[y][x].setBackground(Color.BLUE);
				labelShip[y][x].setOpaque(true);
				panel.add(labelShip[y][x], c);
			}
		}

		// RadioButtons fuer Schiffauswahl
		c.ipadx = 0;
		c.ipady = 0;

		radioButtonShipSelection = new JRadioButton[4];
		radioButtonShipSelection[0] = new JRadioButton("Destroyer");
		radioButtonShipSelection[1] = new JRadioButton("Frigate");
		radioButtonShipSelection[2] = new JRadioButton("Corvette");
		radioButtonShipSelection[3] = new JRadioButton("Submarine");

		ButtonGroup group = new ButtonGroup();

		for (int i = 0; i < radioButtonShipSelection.length; i++) {
			c.insets = new Insets(0, 5, 0, 0);
			c.weightx = 1;
			c.gridx = 6;
			c.gridy = i;
			radioButtonShipSelection[i].setOpaque(false);
			panel.add(radioButtonShipSelection[i], c);
			group.add(radioButtonShipSelection[i]);
		}

		radioButtonShipSelection[0].setSelected(true);

		return panel;
	}
	
	private JPanel createOrientationPanel() {
		JPanel panel = new JPanel(new GridLayout(1, 2));
		panel.setBorder(BorderFactory.createTitledBorder("Orientation"));
		
		radioButtonHorizontalOrientation = new JRadioButton("Horizontal");
		radioButtonVerticalOrientation = new JRadioButton("Vertical");
		
		ButtonGroup group = new ButtonGroup();
		group.add(radioButtonHorizontalOrientation);
		group.add(radioButtonVerticalOrientation);
		
		radioButtonHorizontalOrientation.setSelected(true);
		
		panel.add(radioButtonHorizontalOrientation);
		panel.add(radioButtonVerticalOrientation);
		
		return panel;
	}
	
	private JPanel createEnemySelectionPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Enemys"));
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		
		comboBoxEnemySelection = new JComboBox();
		panel.add(comboBoxEnemySelection, c);
		
		c.weightx = 0;
		c.gridx++;
		buttonApplyEnemy = new JButton("OK");
		panel.add(buttonApplyEnemy, c);
		
		return panel;
	}
	
	private JPanel createGameLogPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Game log"));
		
		textAreaGameLog = new JTextArea();
		textAreaGameLog.setEditable(false);
		textAreaGameLog.setRows(10);
		textAreaGameLog.setLineWrap(true);
		textAreaGameLog.setWrapStyleWord(true);
		
		JScrollPane scroll = new JScrollPane(textAreaGameLog);
		panel.add(scroll);
		
		return panel;
	}
	
	private JPanel createChatPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Chat"));
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;

		// Chat Window
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 2;
		
		textAreaChatWindow = new JTextArea();
		textAreaChatWindow.setEditable(false);
		textAreaChatWindow.setRows(6);
		textAreaChatWindow.setLineWrap(true);
		textAreaChatWindow.setWrapStyleWord(true);
		
		JScrollPane scroll = new JScrollPane(this.textAreaChatWindow);
		panel.add(scroll, c);
		
		// Chat Message
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		
		textFieldChatMessage = new JTextField();
		panel.add(textFieldChatMessage, c);
		
		// send Button
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0;
		c.weighty = 0;
		
		buttonSendMessage = new JButton("send");
		panel.add(buttonSendMessage, c);
		
		return panel;
	}
	
	//////////////////////////////////////////////////////////////////
	// get Components
	//////////////////////////////////////////////////////////////////
	
	public void setPanelEnemyBoard(BoardPanel panelEnemyBoard) {
		panelGameArea.remove(this.panelEnemyBoard);
		this.panelEnemyBoard = panelEnemyBoard;
		panelGameArea.add(this.panelEnemyBoard, BorderLayout.WEST);
	}

	public void setPanelPlayerBoard(BoardPanel panelPlayerBoard) {
		panelGameArea.remove(this.panelPlayerBoard);
		this.panelPlayerBoard = panelPlayerBoard;
		panelGameArea.add(this.panelPlayerBoard, BorderLayout.EAST);
	}

	public JLabel getLabelInfo() {
		return labelInfo;
	}

	public BoardPanel getPanelEnemyBoard() {
		return panelEnemyBoard;
	}

	public BoardPanel getPanelPlayerBoard() {
		return panelPlayerBoard;
	}

	public JRadioButton getRadioButtonHorizontalOrientation() {
		return radioButtonHorizontalOrientation;
	}

	public JRadioButton getRadioButtonVerticalOrientation() {
		return radioButtonVerticalOrientation;
	}

	public JComboBox getComboBoxEnemySelection() {
		return comboBoxEnemySelection;
	}

	public JButton getButtonApplyEnemy() {
		return buttonApplyEnemy;
	}

	public JTextArea getTextAreaGameLog() {
		return textAreaGameLog;
	}

	public JTextArea getTextAreaChatWindow() {
		return textAreaChatWindow;
	}

	public JTextField getTextFieldChatMessage() {
		return textFieldChatMessage;
	}

	public JButton getButtonSendMessage() {
		return buttonSendMessage;
	}
	
	/* Ship Count Labels */
	
	public JLabel getLabelDestroyerShipCount() {
		return labelShipCount[0];
	}

	public JLabel getLabelFrigateShipCount() {
		return labelShipCount[1];
	}

	public JLabel getLabelCorvetteShipCount() {
		return labelShipCount[2];
	}

	public JLabel getLabelSubmarineShipCount() {
		return labelShipCount[3];
	}

	/* Ship Labels */
	
	public JLabel[] getLabelDestroyer() {
		return labelShip[0];
	}
	
	public JLabel[] getLabelFrigate() {
		return labelShip[1];
	}
	
	public JLabel[] getLabelCorvette() {
		return labelShip[2];
	}
	
	public JLabel[] getLabelSubmarine() {
		return labelShip[3];
	}
	
	/* Radio Buttons */
	
	public JRadioButton getRadioButtonDestroyer() {
		return radioButtonShipSelection[0];
	}

	public JRadioButton getRadioButtonFrigate() {
		return radioButtonShipSelection[1];
	}

	public JRadioButton getRadioButtonCorvette() {
		return radioButtonShipSelection[2];
	}

	public JRadioButton getRadioButtonSubmarine() {
		return radioButtonShipSelection[3];
	}
}