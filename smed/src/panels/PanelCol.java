package panels;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;

import messages.Messages;
import smed.SmedAction;
import seamarks.SeaMark;
import seamarks.SeaMark.*;

public class PanelCol extends JPanel {

	private SmedAction dlg;
	private Ent ent;
	private ButtonGroup colourButtons = new ButtonGroup();
	public JRadioButton delButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/DelButton.png")));
	public JRadioButton addButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/AddButton.png")));
	public JRadioButton whiteButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/WhiteButton.png")));
	public JRadioButton redButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/RedButton.png")));
	public JRadioButton greenButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/GreenButton.png")));
	public JRadioButton yellowButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/YellowButton.png")));
	public JRadioButton orangeButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OrangeButton.png")));
	public JRadioButton amberButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/AmberButton.png")));
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
					if (ent == Ent.LIGHT) {
						if (((String) dlg.panelMain.mark.getLightAtt(Att.CHR, 0)).contains("Al")) {
							if (((button == delButton) && (dlg.panelMain.mark.getLightAtt(Att.ALT, 0) == Col.UNKCOL))
									|| (dlg.panelMain.mark.getLightAtt(Att.COL, 0) == Col.UNKCOL)) {
								dlg.panelMain.mark.setLightAtt(Att.COL, 0, col);
								dlg.panelMain.panelLit.panelChr.col1Label.setBackground(SeaMark.ColMAP.get(col));
							} else {
								dlg.panelMain.mark.setLightAtt(Att.ALT, 0, col);
								dlg.panelMain.panelLit.panelChr.col2Label.setBackground(SeaMark.ColMAP.get(col));
							}
						} else {
							dlg.panelMain.mark.setLightAtt(Att.COL, 0, col);
							dlg.panelMain.panelLit.panelChr.col1Label.setBackground(SeaMark.ColMAP.get(col));
							dlg.panelMain.panelLit.panelChr.col2Label.setBackground(SeaMark.ColMAP.get(col));
						}
						button.setBorderPainted(true);
					} else {
						if (button == delButton) {
							dlg.panelMain.mark.subColour(ent, stackIdx);
						} else if (button == addButton) {
							if (stackCol.size() != 0)
								stackIdx++;
							if (stackCol.size() == 0)
								dlg.panelMain.mark.setColour(ent, col);
							else
								switch (dlg.panelMain.mark.getPattern(ent)) {
								case NOPAT:
									break;
								case BORDER:
								case CROSS:
									if (stackCol.size() < 2)
										dlg.panelMain.mark.addColour(ent, stackIdx, col);
									break;
								case SQUARED:
									if (stackCol.size() < 4)
										dlg.panelMain.mark.addColour(ent, stackIdx, col);
									break;
								default:
									dlg.panelMain.mark.addColour(ent, stackIdx, col);
									break;
								}
						} else {
							dlg.panelMain.mark.setColour(ent, stackIdx, col);
						}
						syncPanel();
					}
				} else {
					button.setBorderPainted(false);
				}
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
				} else {
					button.setBorderPainted(false);
				}
			}
		}
	};

	public PanelCol(SmedAction dia, Ent entity) {
		dlg = dia;
		ent = entity;
		setLayout(null);
		add(getColButton(delButton, 0, 0, 34, 16, Messages.getString("RemColour"), Col.UNKCOL));
		add(getColButton(whiteButton, 0, 16, 34, 16, Messages.getString("White"), Col.WHITE));
		add(getColButton(redButton, 0, 32, 34, 16, Messages.getString("Red"), Col.RED));
		add(getColButton(orangeButton, 0, 48, 34, 16, Messages.getString("Orange"), Col.ORANGE));
		add(getColButton(amberButton, 0, 64, 34, 16, Messages.getString("Amber"), Col.AMBER));
		add(getColButton(yellowButton, 0, 80, 34, 16, Messages.getString("Yellow"), Col.YELLOW));
		add(getColButton(greenButton, 0, 96, 34, 16, Messages.getString("Green"), Col.GREEN));
		add(getColButton(blueButton, 0, 112, 34, 16, Messages.getString("Blue"), Col.BLUE));
		add(getColButton(violetButton, 0, 128, 34, 16, Messages.getString("Violet"), Col.VIOLET));
		if (ent != Ent.LIGHT) {
			add(getColButton(addButton, 0, 144, 34, 16, Messages.getString("AddColour"), Col.BLANK));
			add(getColButton(blackButton, 37, 0, 34, 16, Messages.getString("Black"), Col.BLACK));
			add(getColButton(greyButton, 37, 16, 34, 16, Messages.getString("Grey"), Col.GREY));
			add(getColButton(brownButton, 37, 32, 34, 16, Messages.getString("Brown"), Col.BROWN));
			add(getColButton(magentaButton, 37, 48, 34, 16, Messages.getString("Magenta"), Col.MAGENTA));
			add(getColButton(pinkButton, 37, 64, 34, 16, Messages.getString("Pink"), Col.PINK));

			stack = new JPanel();
			stack.setBorder(BorderFactory.createLineBorder(Color.black, 2));
			stack.setBounds(38, 87, 34, 64);
			stack.setLayout(null);
			add(stack);
		}
	}

	public void trimStack(int max) {
		while (stackCol.size() > max) {
			stackCol.get(stackCol.size() - 1).setSelected(true);
			delButton.doClick();
		}
	}

	public void syncPanel() {
		if (ent == Ent.LIGHT) {
			for (Col col : colours.keySet()) {
				JRadioButton button = colours.get(col);
				if (dlg.panelMain.mark.getLightAtt(Att.COL, 0) == col) {
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
		} else {
			int idx;
			for (idx = 0; dlg.panelMain.mark.getColour(ent, idx) != Col.UNKCOL; idx++) {
				if (stackCol.size() <= idx) {
					stackCol.add(idx, new JRadioButton(new ImageIcon(getClass().getResource("/images/ColourButton.png"))));
					JRadioButton btnI = stackCol.get(idx);
					btnI.setBorder(BorderFactory.createLoweredBevelBorder());
					stack.add(btnI);
					stackColours.add(btnI);
					btnI.addActionListener(alStack);
				}
			}
			while (idx < stackCol.size()) {
				JRadioButton btnI = stackCol.get(idx);
				btnI.removeActionListener(alStack);
				stackColours.remove(btnI);
				stack.remove(btnI);
				stackCol.remove(idx);
			}
			if (stackIdx >= stackCol.size())
				stackIdx = stackCol.size() - 1;
			if (stackIdx < 0)
				stackIdx = 0;
			if (stackCol.size() == 0) {
				stack.repaint();
			} else {
				int height = 60 / stackCol.size();
				for (idx = 0; stackCol.size() > idx; idx++) {
					JRadioButton btnI = stackCol.get(idx);
					btnI.setBounds(2, (2 + (idx * height)), 30, height);
					btnI.setBackground(SeaMark.ColMAP.get(dlg.panelMain.mark.getColour(ent, idx)));
					if (stackIdx == idx) {
						btnI.setBorderPainted(true);
					} else {
						btnI.setBorderPainted(false);
					}
				}
			}
		}
	}

	private JRadioButton getColButton(JRadioButton button, int x, int y, int w, int h, String tip, Col col) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLoweredBevelBorder());
		button.setToolTipText(tr(tip));
		button.addActionListener(alColour);
		colourButtons.add(button);
		colours.put(col, button);
		return button;
	}

}
