package panels;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.util.EnumMap;

import messages.Messages;
import smed.SmedAction;
import seamarks.SeaMark.*;

public class PanelFog extends JPanel {

	private SmedAction dlg;
	private ButtonGroup fogButtons = new ButtonGroup();
	public JRadioButton noFogButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/OffButton.png")));
	public JRadioButton yesFogButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogYesButton.png")));
	public JRadioButton hornButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogHornButton.png")));
	public JRadioButton sirenButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogSirenButton.png")));
	public JRadioButton diaButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogDiaButton.png")));
	public JRadioButton bellButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogBellButton.png")));
	public JRadioButton whisButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogWhisButton.png")));
	public JRadioButton gongButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogGongButton.png")));
	public JRadioButton explosButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FogExplosButton.png")));
	private EnumMap<Fog, JRadioButton> fogs = new EnumMap<>(Fog.class);
	private ActionListener alFog = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (Fog fog : fogs.keySet()) {
				JRadioButton button = fogs.get(fog);
				if (button.isSelected()) {
					SmedAction.panelMain.mark.setFogSound(fog);
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
		}
	};
	public JLabel groupLabel;
	public JTextField groupBox;
	private FocusListener flGroup = new FocusAdapter() {
		public void focusLost(java.awt.event.FocusEvent e) {
			SmedAction.panelMain.mark.setFogGroup(groupBox.getText());
		}
	};
	public JLabel periodLabel;
	public JTextField periodBox;
	private FocusListener flPeriod = new FocusAdapter() {
		public void focusLost(java.awt.event.FocusEvent e) {
			SmedAction.panelMain.mark.setFogPeriod(periodBox.getText());
		}
	};
	public JLabel seqLabel;
	public JTextField seqBox;
	private FocusListener flSeq = new FocusAdapter() {
		public void focusLost(java.awt.event.FocusEvent e) {
			SmedAction.panelMain.mark.setFogSequence(seqBox.getText());
		}
	};
	public JLabel rangeLabel;
	public JTextField rangeBox;
	private FocusListener flRange = new FocusAdapter() {
		public void focusLost(java.awt.event.FocusEvent e) {
			SmedAction.panelMain.mark.setFogRange(rangeBox.getText());
		}
	};

	public PanelFog(SmedAction dia) {
		dlg = dia;
		setLayout(null);
		add(getFogButton(noFogButton, 0, 2, 27, 27, "NoFog", Fog.NOFOG));
		add(getFogButton(yesFogButton, 0, 32, 27, 27, "FogSignal", Fog.FOGSIG));
		add(getFogButton(hornButton, 0, 62, 27, 27, "Horn", Fog.HORN));
		add(getFogButton(sirenButton, 0, 92, 27, 27, "Siren", Fog.SIREN));
		add(getFogButton(gongButton, 0, 122, 27, 27, "Gong", Fog.GONG));
		add(getFogButton(diaButton, 30, 2, 27, 27, "Diaphone", Fog.DIA));
		add(getFogButton(bellButton, 30, 32, 27, 27, "Bell", Fog.BELL));
		add(getFogButton(whisButton, 30, 62, 27, 27, "Whistle", Fog.WHIS));
		add(getFogButton(explosButton, 30, 92, 27, 27, "Explosion", Fog.EXPLOS));

		groupLabel = new JLabel(Messages.getString("Group"), SwingConstants.CENTER);
		groupLabel.setBounds(new Rectangle(75, 0, 100, 20));
		add(groupLabel);
		groupBox = new JTextField();
		groupBox.setBounds(new Rectangle(100, 20, 50, 20));
		groupBox.setHorizontalAlignment(SwingConstants.CENTER);
		add(groupBox);
		groupBox.addFocusListener(flGroup);

		periodLabel = new JLabel(Messages.getString("Period"), SwingConstants.CENTER);
		periodLabel.setBounds(new Rectangle(75, 40, 100, 20));
		add(periodLabel);
		periodBox = new JTextField();
		periodBox.setBounds(new Rectangle(100, 60, 50, 20));
		periodBox.setHorizontalAlignment(SwingConstants.CENTER);
		add(periodBox);
		periodBox.addFocusListener(flPeriod);

		seqLabel = new JLabel(Messages.getString("Sequence"), SwingConstants.CENTER);
		seqLabel.setBounds(new Rectangle(75, 80, 100, 20));
		add(seqLabel);
		seqBox = new JTextField();
		seqBox.setBounds(new Rectangle(100, 100, 50, 20));
		seqBox.setHorizontalAlignment(SwingConstants.CENTER);
		add(seqBox);
		seqBox.addFocusListener(flSeq);

		rangeLabel = new JLabel(Messages.getString("Range"), SwingConstants.CENTER);
		rangeLabel.setBounds(new Rectangle(75, 120, 100, 20));
		add(rangeLabel);
		rangeBox = new JTextField();
		rangeBox.setBounds(new Rectangle(100, 140, 50, 20));
		rangeBox.setHorizontalAlignment(SwingConstants.CENTER);
		add(rangeBox);
		rangeBox.addFocusListener(flRange);

	}

	public void syncPanel() {
		for (Fog fog : fogs.keySet()) {
			JRadioButton button = fogs.get(fog);
			button.setBorderPainted(SmedAction.panelMain.mark.getFogSound() == fog);
		}
		groupBox.setText(SmedAction.panelMain.mark.getFogGroup());
		seqBox.setText(SmedAction.panelMain.mark.getFogSequence());
		periodBox.setText(SmedAction.panelMain.mark.getFogPeriod());
		rangeBox.setText(SmedAction.panelMain.mark.getFogRange());
	}

	private JRadioButton getFogButton(JRadioButton button, int x, int y, int w, int h, String tip, Fog fog) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLoweredBevelBorder());
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alFog);
		fogButtons.add(button);
		fogs.put(fog, button);
		return button;
	}

}
