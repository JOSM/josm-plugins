package oseam.panels;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JRadioButton;

import java.util.EnumMap;

import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.Col;
import oseam.seamarks.SeaMark.Ent;

public class PanelCol extends JPanel {

	private OSeaMAction dlg;
	private Ent ent;
	private ButtonGroup colourButtons = new ButtonGroup();
	public JRadioButton offButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OffButton.png")));
	public JRadioButton whiteButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/WhiteButton.png")));
	public JRadioButton redButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/RedButton.png")));
	public JRadioButton orangeButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OrangeButton.png")));
	public JRadioButton amberButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/AmberButton.png")));
	public JRadioButton yellowButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/YellowButton.png")));
	public JRadioButton greenButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/GreenButton.png")));
	public JRadioButton blueButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BlueButton.png")));
	public JRadioButton violetButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/VioletButton.png")));
	public JRadioButton blackButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BlackButton.png")));
	private EnumMap<Col, JRadioButton> colours = new EnumMap<Col, JRadioButton>(Col.class);
	private ActionListener alColour = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (Col col : colours.keySet()) {
				JRadioButton button = colours.get(col);
				if (button.isSelected()) {
					if (dlg.mark != null) dlg.mark.setColour(ent, col);
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
		}
	};

	public PanelCol(OSeaMAction dia, Ent entity) {
		dlg = dia;
		ent = entity;
		this.setLayout(null);
		this.add(getColButton(offButton, 0, 0, 34, 16, "No colour", Col.UNKNOWN), null);
		this.add(getColButton(whiteButton, 0, 16, 34, 16, "White", Col.WHITE), null);
		this.add(getColButton(redButton, 0, 32, 34, 16, "Red", Col.RED), null);
		this.add(getColButton(orangeButton, 0, 48, 34, 16, "Orange", Col.ORANGE), null);
		this.add(getColButton(amberButton, 0, 64, 34, 16, "Amber", Col.AMBER), null);
		this.add(getColButton(yellowButton, 0, 80, 34, 16, "Yellow", Col.YELLOW), null);
		this.add(getColButton(greenButton, 0, 96, 34, 16, "Green", Col.GREEN), null);
		this.add(getColButton(blueButton, 0, 112, 34, 16, "Blue", Col.BLUE), null);
		this.add(getColButton(violetButton, 0, 128, 34, 16, "Violet", Col.VIOLET), null);
		this.add(getColButton(blackButton, 0, 144, 34, 16, "Black", Col.BLACK), null);
	}

	public void clearSelections() {
		colourButtons.clearSelection();
		offButton.doClick();
	}

	public void enableAll(boolean state) {
		for (JRadioButton button : colours.values()) {
			button.setEnabled(state);
		}
	}

	private JRadioButton getColButton(JRadioButton button, int x, int y, int w, int h, String tip, Col col) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
		button.setToolTipText(tr(tip));
		button.addActionListener(alColour);
		colourButtons.add(button);
		colours.put(col, button);
		return button;
	}

}
