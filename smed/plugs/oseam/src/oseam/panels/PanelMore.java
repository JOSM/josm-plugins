package oseam.panels;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;

import oseam.Messages;
import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.*	;

public class PanelMore extends JPanel {

	private OSeaMAction dlg;
	public PanelCol panelCol;
	private ActionListener alType;

	private ButtonGroup patButtons = new ButtonGroup();
	public JRadioButton noneButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/NoneButton.png")));
	public JRadioButton horizButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/HorizontalButton.png")));
	public JRadioButton vertButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/VerticalButton.png")));
	public JRadioButton diagButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/DiagonalButton.png")));
	public JRadioButton squareButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/SquaredButton.png")));
	public JRadioButton borderButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BorderButton.png")));
	public EnumMap<Pat, JRadioButton> patterns = new EnumMap<Pat, JRadioButton>(Pat.class);
	private ActionListener alPat = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (Pat pat : patterns.keySet()) {
				JRadioButton button = patterns.get(pat);
				if (button.isSelected()) {
					dlg.mark.setPattern(Ent.BODY, pat);
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
			if (dlg.mark != null)
				dlg.mark.paintSign();
		}
	};

	public PanelMore(OSeaMAction dia) {
		dlg = dia;
		this.setLayout(null);
		panelCol = new PanelCol(dlg, alType, Ent.LIGHT);
		panelCol.setBounds(new Rectangle(0, 0, 34, 160));
		this.add(panelCol, null);
		this.add(getPatButton(noneButton, 36, 0, 27, 27, "NoPat", Pat.NONE), null);
		this.add(getPatButton(horizButton, 36, 26, 27, 27, "HorizPat", Pat.HORIZ), null);
		this.add(getPatButton(vertButton, 36, 52, 27, 27, "VertPat", Pat.VERT), null);
		this.add(getPatButton(diagButton, 36, 78, 27, 27, "DiagPat", Pat.DIAG), null);
		this.add(getPatButton(squareButton, 36, 104, 27, 27, "SquarePat", Pat.SQUARE), null);
		this.add(getPatButton(borderButton, 36, 130, 27, 27, "BorderPat", Pat.BORDER), null);

	}

	public void clearSelections() {
		patButtons.clearSelection();
		alPat.actionPerformed(null);
	}

	private JRadioButton getPatButton(JRadioButton button, int x, int y, int w, int h, String tip, Pat pat) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alPat);
		patButtons.add(button);
		patterns.put(pat, button);
		return button;
	}

}
