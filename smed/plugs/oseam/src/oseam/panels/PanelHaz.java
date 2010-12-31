package oseam.panels;

import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JRadioButton;

import java.util.EnumMap;
import java.util.Iterator;

import oseam.Messages;
import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.Cat;
import oseam.seamarks.SeaMark.Col;
import oseam.seamarks.SeaMark.Ent;
import oseam.seamarks.SeaMark.Shp;
import oseam.seamarks.SeaMark.Obj;

public class PanelHaz extends JPanel {

	private OSeaMAction dlg;
	private ButtonGroup catButtons = new ButtonGroup();
	public JRadioButton northButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/CardNButton.png")));
	public JRadioButton southButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/CardSButton.png")));
	public JRadioButton eastButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/CardEButton.png")));
	public JRadioButton westButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/CardWButton.png")));
	public JRadioButton isolButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/IsolButton.png")));
	private ActionListener alCat = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (catButtons.getSelection() != null) {
//				if (!(dlg.mark instanceof MarkCard) && !isolButton.isSelected()) {
//					dlg.mark = new MarkCard(dlg);
//					alShape.actionPerformed(null);
//				}
				dlg.panelMain.topButton.setEnabled(true);
				dlg.panelMain.fogButton.setEnabled(true);
				dlg.panelMain.radButton.setEnabled(true);
				dlg.panelMain.litButton.setEnabled(true);
				dlg.panelMain.panelTop.enableAll(false);
				dlg.panelMain.panelTop.panelCol.enableAll(false);
				dlg.panelMain.panelTop.panelCol.blackButton.setEnabled(true);
				dlg.panelMain.panelTop.panelCol.blackButton.doClick();
			}
			if (northButton.isSelected()) {
				dlg.mark.setCategory(Cat.CARD_NORTH);
				dlg.mark.setColour(Ent.BODY, Col.BLACK_YELLOW);
				dlg.panelMain.panelTop.northTopButton.setEnabled(true);
				dlg.panelMain.panelTop.northTopButton.doClick();
				northButton.setBorderPainted(true);
			} else {
				northButton.setBorderPainted(false);
			}
			if (southButton.isSelected()) {
				dlg.mark.setCategory(Cat.CARD_SOUTH);
				dlg.mark.setColour(Ent.BODY, Col.YELLOW_BLACK);
				dlg.panelMain.panelTop.southTopButton.setEnabled(true);
				dlg.panelMain.panelTop.southTopButton.doClick();
				southButton.setBorderPainted(true);
			} else {
				southButton.setBorderPainted(false);
			}
			if (eastButton.isSelected()) {
				dlg.mark.setCategory(Cat.CARD_EAST);
				dlg.mark.setColour(Ent.BODY, Col.BLACK_YELLOW_BLACK);
				dlg.panelMain.panelTop.eastTopButton.setEnabled(true);
				dlg.panelMain.panelTop.eastTopButton.doClick();
				eastButton.setBorderPainted(true);
			} else {
				eastButton.setBorderPainted(false);
			}
			if (westButton.isSelected()) {
				dlg.mark.setCategory(Cat.CARD_WEST);
				dlg.mark.setColour(Ent.BODY, Col.YELLOW_BLACK_YELLOW);
				dlg.panelMain.panelTop.westTopButton.setEnabled(true);
				dlg.panelMain.panelTop.westTopButton.doClick();
				westButton.setBorderPainted(true);
			} else {
				westButton.setBorderPainted(false);
			}
			if (isolButton.isSelected()) {
//				if (!(dlg.mark instanceof MarkIsol)) {
//					dlg.mark = new MarkIsol(dlg);
//					alShape.actionPerformed(null);
//				}
				dlg.mark.setColour(Ent.BODY, Col.BLACK_RED_BLACK);
				dlg.panelMain.panelTop.spheres2TopButton.setEnabled(true);
				dlg.panelMain.panelTop.spheres2TopButton.doClick();
				isolButton.setBorderPainted(true);
			} else {
				isolButton.setBorderPainted(false);
			}
			if (dlg.mark != null)
				dlg.mark.paintSign();
		}
	};

	private ButtonGroup shapeButtons = new ButtonGroup();
	public JRadioButton pillarButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/PillarButton.png")));
	public JRadioButton sparButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/SparButton.png")));
	public JRadioButton floatButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FloatButton.png")));
	public JRadioButton beaconButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BeaconButton.png")));
	public JRadioButton towerButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/TowerButton.png")));
	private EnumMap<Shp, JRadioButton> shapes = new EnumMap<Shp, JRadioButton>(Shp.class);
	private EnumMap<Shp, Obj> carObjects = new EnumMap<Shp, Obj>(Shp.class);
	private EnumMap<Shp, Obj> isdObjects = new EnumMap<Shp, Obj>(Shp.class);
	private ActionListener alShape = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			Iterator<Shp> it = shapes.keySet().iterator();
			while (it.hasNext()) {
				Shp shp = it.next();
				JRadioButton button = shapes.get(shp);
				if (button.isSelected()) {
					dlg.mark.setShape(shp);
					if (isolButton.isSelected())
						dlg.mark.setObject(isdObjects.get(shp));
					else
						dlg.mark.setObject(carObjects.get(shp));
					button.setBorderPainted(true);
				} else
					button.setBorderPainted(false);
			}
			if (dlg.mark != null)
				dlg.mark.paintSign();
		}
	};

	public PanelHaz(OSeaMAction dia) {
		dlg = dia;
		this.setLayout(null);
		this.add(getCatButton(northButton, 0, 0, 52, 32, "NorthTip"), null);
		this.add(getCatButton(southButton, 0, 32, 52, 32, "SouthTip"), null);
		this.add(getCatButton(eastButton, 0, 64, 52, 32, "EastTip"), null);
		this.add(getCatButton(westButton, 0, 96, 52, 32, "WestTip"), null);
		this.add(getCatButton(isolButton, 0, 128, 52, 32, "IsolTip"), null);

		this.add(getShapeButton(pillarButton, 55, 0, 34, 32, "PillarTip", Shp.PILLAR, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(sparButton, 55, 32, 34, 32, "SparTip", Shp.SPAR, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(floatButton, 55, 64, 34, 32, "FloatTip", Shp.FLOAT, Obj.LITFLT, Obj.LITFLT), null);
		this.add(getShapeButton(beaconButton, 55, 96, 34, 32, "BeaconTip", Shp.BEACON, Obj.BCNCAR, Obj.BCNISD), null);
		this.add(getShapeButton(towerButton, 55, 128, 34, 32, "TowerTip", Shp.TOWER, Obj.BCNCAR, Obj.BCNISD), null);
	}

	public void clearSelections() {
		catButtons.clearSelection();
		alCat.actionPerformed(null);
		shapeButtons.clearSelection();
		alShape.actionPerformed(null);
	}

	private JRadioButton getCatButton(JRadioButton button, int x, int y, int w, int h, String tip) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alCat);
		catButtons.add(button);
		return button;
	}

	private JRadioButton getShapeButton(JRadioButton button, int x, int y, int w, int h, String tip, Shp shp, Obj car, Obj isd) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLineBorder(Color.magenta, 2));
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alShape);
		shapeButtons.add(button);
		shapes.put(shp, button);
		carObjects.put(shp, car);
		isdObjects.put(shp, isd);
		return button;
	}

}
