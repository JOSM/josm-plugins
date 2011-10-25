package oseam.panels;

import java.awt.event.*;
import java.awt.*;

import javax.swing.*;

import java.util.*;

import oseam.Messages;
import oseam.dialogs.OSeaMAction;
import oseam.seamarks.SeaMark.*;

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
			Shp shp = Shp.UNKNOWN;
			if (dlg.mark != null) {
				shp = dlg.mark.getShape();
				dlg.panelMain.panelMore.panelPat.clearSelections();
				dlg.panelMain.panelMore.panelPat.horizButton.doClick();
				if (northButton.isSelected()) {
					dlg.mark.setCategory(Cat.CAM_NORTH);
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.yellowButton.doClick();
					if (shapes.containsKey(shp))
						shapes.get(shp).doClick();
					northButton.setBorderPainted(true);
				} else {
					northButton.setBorderPainted(false);
				}
				if (southButton.isSelected()) {
					dlg.mark.setCategory(Cat.CAM_SOUTH);
					dlg.panelMain.panelMore.panelPat.panelCol.yellowButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					if (shapes.containsKey(shp))
						shapes.get(shp).doClick();
					southButton.setBorderPainted(true);
				} else {
					southButton.setBorderPainted(false);
				}
				if (eastButton.isSelected()) {
					dlg.mark.setCategory(Cat.CAM_EAST);
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.yellowButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					if (shapes.containsKey(shp))
						shapes.get(shp).doClick();
					eastButton.setBorderPainted(true);
				} else {
					eastButton.setBorderPainted(false);
				}
				if (westButton.isSelected()) {
					dlg.mark.setCategory(Cat.CAM_WEST);
					dlg.panelMain.panelMore.panelPat.panelCol.yellowButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.yellowButton.doClick();
					if (shapes.containsKey(shp))
						shapes.get(shp).doClick();
					westButton.setBorderPainted(true);
				} else {
					westButton.setBorderPainted(false);
				}
				if (isolButton.isSelected()) {
					dlg.mark.setCategory(Cat.UNKNOWN);
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.redButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.addButton.doClick();
					dlg.panelMain.panelMore.panelPat.panelCol.blackButton.doClick();
					if (shapes.containsKey(shp))
						shapes.get(shp).doClick();
					isolButton.setBorderPainted(true);
				} else {
					isolButton.setBorderPainted(false);
				}
			}
		}
	};

	private ButtonGroup shapeButtons = new ButtonGroup();
	public JRadioButton pillarButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/PillarButton.png")));
	public JRadioButton sparButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/SparButton.png")));
	public JRadioButton floatButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/FloatButton.png")));
	public JRadioButton canButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/CanButton.png")));
	public JRadioButton coneButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/ConeButton.png")));
	public JRadioButton sphereButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/SphereButton.png")));
	public JRadioButton beaconButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/BeaconButton.png")));
	public JRadioButton towerButton = new JRadioButton(new ImageIcon(getClass().getResource("/images/TowerButton.png")));
	public EnumMap<Shp, JRadioButton> shapes = new EnumMap<Shp, JRadioButton>(Shp.class);
	public EnumMap<Shp, Obj> carObjects = new EnumMap<Shp, Obj>(Shp.class);
	public EnumMap<Shp, Obj> isdObjects = new EnumMap<Shp, Obj>(Shp.class);
	private ActionListener alShape = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			for (Shp shp : shapes.keySet()) {
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
			if (dlg.mark != null) {
				if ((dlg.mark.getObject() != Obj.UNKNOWN) && (dlg.mark.getShape() != Shp.UNKNOWN)) {
					topmarkButton.setVisible(true);
					dlg.panelMain.moreButton.setVisible(true);
					dlg.panelMain.topButton.setEnabled(true);
					dlg.panelMain.fogButton.setEnabled(true);
					dlg.panelMain.radButton.setEnabled(true);
					dlg.panelMain.litButton.setEnabled(true);
				} else {
					topmarkButton.setVisible(false);
					dlg.panelMain.moreButton.setVisible(false);
					dlg.panelMain.topButton.setEnabled(false);
					dlg.panelMain.fogButton.setEnabled(false);
					dlg.panelMain.radButton.setEnabled(false);
					dlg.panelMain.litButton.setEnabled(false);
				}
			}
		}
	};
	public JToggleButton topmarkButton = new JToggleButton(new ImageIcon(getClass().getResource("/images/HazTopButton.png")));
	private ActionListener alTop = new ActionListener() {
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (dlg.mark != null) {
				dlg.panelMain.panelTop.clearSelections();
				if (topmarkButton.isSelected()) {
					dlg.panelMain.panelTop.panelPat.noneButton.doClick();
					dlg.panelMain.panelTop.panelPat.panelCol.blackButton.doClick();
					switch (dlg.mark.getCategory()) {
					case CAM_NORTH:
						dlg.panelMain.panelTop.northTopButton.doClick();
						break;
					case CAM_SOUTH:
						dlg.panelMain.panelTop.southTopButton.doClick();
						break;
					case CAM_EAST:
						dlg.panelMain.panelTop.eastTopButton.doClick();
						break;
					case CAM_WEST:
						dlg.panelMain.panelTop.westTopButton.doClick();
						break;
					default:
						dlg.panelMain.panelTop.spheres2TopButton.doClick();
						break;
					}
					topmarkButton.setBorderPainted(true);
				} else {
					topmarkButton.setBorderPainted(false);
				}
			}
		}
	};

	public PanelHaz(OSeaMAction dia) {
		dlg = dia;
		this.setLayout(null);
		this.add(getCatButton(northButton, 0, 0, 52, 32, "North"), null);
		this.add(getCatButton(southButton, 0, 32, 52, 32, "South"), null);
		this.add(getCatButton(eastButton, 0, 64, 52, 32, "East"), null);
		this.add(getCatButton(westButton, 0, 96, 52, 32, "West"), null);
		this.add(getCatButton(isolButton, 0, 128, 52, 32, "Isol"), null);

		this.add(getShapeButton(pillarButton, 55, 0, 34, 32, "Pillar", Shp.PILLAR, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(sparButton, 55, 32, 34, 32, "Spar", Shp.SPAR, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(canButton, 55, 64, 34, 32, "Can", Shp.CAN, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(coneButton, 55, 96, 34, 32, "Cone", Shp.CONE, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(sphereButton, 55, 128, 34, 32, "Sphere", Shp.SPHERE, Obj.BOYCAR, Obj.BOYISD), null);
		this.add(getShapeButton(floatButton, 90, 0, 34, 32, "Float", Shp.FLOAT, Obj.LITFLT, Obj.LITFLT), null);
		this.add(getShapeButton(beaconButton, 90, 32, 34, 32, "Beacon", Shp.BEACON, Obj.BCNCAR, Obj.BCNISD), null);
		this.add(getShapeButton(towerButton, 90, 64, 34, 32, "Tower", Shp.TOWER, Obj.BCNCAR, Obj.BCNISD), null);

		topmarkButton.setBounds(new Rectangle(130, 0, 34, 32));
		topmarkButton.setBorder(BorderFactory.createLoweredBevelBorder());
		topmarkButton.addActionListener(alTop);
		this.add(topmarkButton);
	}

	public void clearSelections() {
		topmarkButton.setSelected(false);
		topmarkButton.setVisible(false);
		alTop.actionPerformed(null);
		catButtons.clearSelection();
		alCat.actionPerformed(null);
		shapeButtons.clearSelection();
		alShape.actionPerformed(null);
	}

	private JRadioButton getCatButton(JRadioButton button, int x, int y, int w, int h, String tip) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLoweredBevelBorder());
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alCat);
		catButtons.add(button);
		return button;
	}

	private JRadioButton getShapeButton(JRadioButton button, int x, int y, int w, int h, String tip, Shp shp, Obj car, Obj isd) {
		button.setBounds(new Rectangle(x, y, w, h));
		button.setBorder(BorderFactory.createLoweredBevelBorder());
		button.setToolTipText(Messages.getString(tip));
		button.addActionListener(alShape);
		shapeButtons.add(button);
		shapes.put(shp, button);
		carObjects.put(shp, car);
		isdObjects.put(shp, isd);
		return button;
	}

}
