package oseam.panels;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

import oseam.Messages;
import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.*;

public class PanelCol extends JPanel {

	private OSeaMAction dlg;
	private ActionListener act;
	private Ent ent;
	private ButtonGroup colourButtons = new ButtonGroup();
	public JRadioButton offButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OffButton.png")));
	public JRadioButton addButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/AddButton.png")));
	public JRadioButton whiteButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/WhiteButton.png")));
	public JRadioButton redButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/RedButton.png")));
	public JRadioButton orangeButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OrangeButton.png")));
	public JRadioButton amberButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/AmberButton.png")));
	public JRadioButton yellowButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/YellowButton.png")));
	public JRadioButton greenButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/GreenButton.png")));
	public JRadioButton blueButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BlueButton.png")));
	public JRadioButton violetButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/VioletButton.png")));
	public JRadioButton blackButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BlackButton.png")));
	public JRadioButton greyButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/GreyButton.png")));
	public JRadioButton brownButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BrownButton.png")));
	public JRadioButton magentaButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/MagentaButton.png")));
	public JRadioButton pinkButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/PinkButton.png")));
	public EnumMap<Col, JRadioButton> colours = new EnumMap<Col, JRadioButton>(Col.class);
	private ActionListener alColour = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (Col col : colours.keySet()) {
				JRadioButton button = colours.get(col);
				if (button.isSelected()) {
					if (dlg.mark != null) {
						dlg.mark.setColour(ent, col);
						if (ent != Ent.LIGHT) {
							if (button == offButton) {
								stack.remove(stackCol.get(stackIdx));
								stackCol.remove(stackIdx);
							} else if (button == addButton) {
								stackCol.add(stackIdx, new JRadioButton());
								stackCol.get(stackIdx).setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
								stack.add(stackCol.get(stackIdx));
							}
							for (int i = 0; stackCol.size() > i; i++) {
								stackCol.get(i).setBounds(37, (89 + (i * (60 / stackCol.size()))), 30, (60 / stackCol.size()));
								stackCol.get(i).setBackground(dlg.mark.ColMAP.get(dlg.mark.getColour(ent, i)));
							}
						}
					}
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
		}
	};
	private JPanel stack;
	private ButtonGroup stackColours = new ButtonGroup();
	private ArrayList<JRadioButton> stackCol = new ArrayList<JRadioButton>();
	private int stackIdx = 0;
	private ActionListener alStack = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (int i = 0; stackCol.size() > i; i++) {
				JRadioButton button = stackCol.get(i);
				if (button.isSelected()) {
					stackIdx = i;
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
		}
	};

	public PanelCol(OSeaMAction dia, ActionListener al, Ent entity) {
		dlg = dia;
		act = al;
		ent = entity;
		this.setLayout(null);
		this.add(getColButton(offButton, 0, 0, 34, 16, Messages.getString("NoColour"), Col.UNKNOWN), null);
		this.add(getColButton(whiteButton, 0, 16, 34, 16, Messages.getString("White"), Col.WHITE), null);
		this.add(getColButton(redButton, 0, 32, 34, 16, Messages.getString("Red"), Col.RED), null);
		this.add(getColButton(orangeButton, 0, 48, 34, 16, Messages.getString("Orange"), Col.ORANGE), null);
		this.add(getColButton(amberButton, 0, 64, 34, 16, Messages.getString("Amber"), Col.AMBER), null);
		this.add(getColButton(yellowButton, 0, 80, 34, 16, Messages.getString("Yellow"), Col.YELLOW), null);
		this.add(getColButton(greenButton, 0, 96, 34, 16, Messages.getString("Green"), Col.GREEN), null);
		this.add(getColButton(blueButton, 0, 112, 34, 16, Messages.getString("Blue"), Col.BLUE), null);
		this.add(getColButton(violetButton, 0, 128, 34, 16, Messages.getString("Violet"), Col.VIOLET), null);
		if (ent != Ent.LIGHT) {
			this.add(getColButton(addButton, 0, 144, 34, 16, Messages.getString("AddColour"), Col.UNKNOWN), null);
			this.add(getColButton(blackButton, 35, 0, 34, 16, Messages.getString("Black"), Col.BLACK), null);
			this.add(getColButton(greyButton, 35, 16, 34, 16, Messages.getString("Grey"), Col.GREY), null);
			this.add(getColButton(brownButton, 35, 32, 34, 16, Messages.getString("Brown"), Col.BROWN), null);
			this.add(getColButton(magentaButton, 35, 48, 34, 16, Messages.getString("Magenta"), Col.MAGENTA), null);
			this.add(getColButton(pinkButton, 35, 64, 34, 16, Messages.getString("Pink"), Col.PINK), null);

			stack = new JPanel();
			stack.setBorder(BorderFactory.createLineBorder(Color.black));
			stack.setBounds(37, 89, 30, 60);
			this.add(stack);
			if (dlg.mark != null) {
				for (int i = 0; dlg.mark.getColour(ent, i) != Col.UNKNOWN; i++) {
					stackCol.add(new JRadioButton());
				}
			}
		}
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
