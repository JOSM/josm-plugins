package org.openstreetmap.josm.plugins.measurement;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.Main;

/**
 * A small tool dialog for displaying the current measurement data.
 *
 * @author ramack
 */
public class MeasurementDialog extends ToggleDialog implements ActionListener
{
	private static final long serialVersionUID = 4708541586297950021L;

	/** 
     * The reset button
     */
    private JButton resetButton;
    
    /**
     * The measurement label for the path length
     */
    protected JLabel pathLengthLabel;
    
    /**
     * The measurement label for the currently selected segments
     */
    protected JLabel selectLengthLabel;
    
    /**
     * The measurement label for area of the currently selected loop
     */
    protected JLabel selectAreaLabel;
    
    /**
     * The measurement label for the segment angle, actually updated, if 2 nodes are selected
     */
    protected JLabel segAngleLabel;
    
    /**
     * Constructor
     */
    public MeasurementDialog() 
    {
        super(tr("Measured values"), "measure", tr("Open the measurement window."), KeyEvent.VK_M, KeyEvent.ALT_DOWN_MASK);
        
        JPanel buttonPanel = new JPanel(new GridLayout(1,2));
        
		resetButton = new JButton(tr("Reset"), ImageProvider.get("mapmode/selection/select"));
		resetButton.setActionCommand("Reset");
		resetButton.addActionListener(this);
		resetButton.setToolTipText(tr("Reset current measurement results and delete measurement path."));
//		resetButton.putClientProperty("help", "Dialog/SelectionList/Reset");
		
        buttonPanel.add(resetButton);
        add(buttonPanel, BorderLayout.SOUTH);

        JPanel valuePanel = new JPanel(new GridLayout(0,2));
        
        valuePanel.add(new JLabel("Path Length"));
        
        pathLengthLabel = new JLabel("0 m");
        valuePanel.add(pathLengthLabel);
        
        valuePanel.add(new JLabel("Selection Length"));
        
        selectLengthLabel = new JLabel("0 m");
        valuePanel.add(selectLengthLabel);

        valuePanel.add(new JLabel("Selection Area"));
        
        selectAreaLabel = new JLabel("0 m²");
        valuePanel.add(selectAreaLabel);
        
        JLabel angle = new JLabel("Angle");
        angle.setToolTipText(tr("Angle between two selected Nodes"));
        valuePanel.add(angle);
        
        segAngleLabel = new JLabel("- °");
        valuePanel.add(segAngleLabel);
        
        add(valuePanel, BorderLayout.CENTER);

        this.setPreferredSize(new Dimension(0, 92));
        final MeasurementDialog dlg = this;
       //TODO: is this enough? 

        Main.ds.selListeners.add(new SelectionChangedListener(){

			public void selectionChanged(Collection<? extends OsmPrimitive> arg0) {
				double length = 0.0;
				double segAngle = 0.0;
                                double area = 0.0;
                                Node lastNode = null;
				for(OsmPrimitive p:arg0){
                                    if(p instanceof Node){
                                        Node n =(Node)p;
                                        if(lastNode == null){
                                            lastNode = n;
                                        }else{
                                            length += MeasurementLayer.calcDistance(lastNode.coor, n.coor);
                                            segAngle = MeasurementLayer.angleBetween(lastNode.coor, n.coor);
                                            lastNode = n;
                                        }
                                    } else if(p instanceof Way){
                                        Way w = (Way)p;
                                        Node lastN = null;
                                        for(Node n: w.nodes){
                                            if(lastN != null){
                                                length += MeasurementLayer.calcDistance(lastN.coor, n.coor);
                                                //http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
                                                area += (MeasurementLayer.calcX(n.coor) * MeasurementLayer.calcY(lastN.coor))
						      - (MeasurementLayer.calcY(n.coor) * MeasurementLayer.calcX(lastN.coor));
                                            }
                                            lastN = n;
                                        }
                                        if (lastN != null && lastN == w.nodes.iterator().next()){
                                            area = Math.abs(area / 2);
                                        }else{
                                            area = 0;
                                        }
                                    }
				}
				dlg.selectLengthLabel.setText(new DecimalFormat("#0.00").format(length) + " m");

				dlg.segAngleLabel.setText(new DecimalFormat("#0.0").format(segAngle) + " °");		
 				dlg.selectAreaLabel.setText(new DecimalFormat("#0.00").format(area) + " m²");

			}
        	
        });
    }

	public void actionPerformed(ActionEvent e) 
	{
		String actionCommand = e.getActionCommand();
		if( actionCommand.equals("Reset")){
			resetValues();
		}
	}
    
	/**
	 * Cleans the active Meausurement Layer
	 */
	public void resetValues(){
		MeasurementPlugin.getCurrentLayer().reset();
	}
	
}
