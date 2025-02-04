// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.tools.GBC;

/**
 * A dialog for setting building sizes and other information
 */
public class BuildingSizeDialog extends MyDialog {
    private final JFormattedTextField twidth = new JFormattedTextField(NumberFormat.getInstance());
    private final JFormattedTextField tlenstep = new JFormattedTextField(NumberFormat.getInstance());
    private final JCheckBox caddr = new JCheckBox(tr("Use Address dialog"));
    private final JCheckBox cAutoSelect = new JCheckBox(tr("Auto-select building"));
    private final JCheckBox cAutoSelectReplaceSelection = new JCheckBox(tr("Auto-select replaces existing selection"));
    private final JCheckBox cAddrNode = new JCheckBox(tr("Use address nodes under buildings"));
    private final JRadioButton circleRadio = new JRadioButton(tr("Circle"));
    private final JRadioButton rectangleRadio = new JRadioButton(tr("Rectangle"));

    /**
     * Create a new dialog for building sizes
     */
    public BuildingSizeDialog() {
        super(tr("Set buildings size and shape"));

        ButtonGroup shapeGroup = new ButtonGroup();
        shapeGroup.add(circleRadio);
        shapeGroup.add(rectangleRadio);
        circleRadio.setSelected(ToolSettings.Shape.CIRCLE == ToolSettings.getShape());
        rectangleRadio.setSelected(ToolSettings.Shape.RECTANGLE == ToolSettings.getShape());

        panel.add(rectangleRadio, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        panel.add(circleRadio, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

        addLabelled(tr("Buildings width/diameter:"), twidth);
        addLabelled(tr("Length step:"), tlenstep);
        panel.add(caddr, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        panel.add(cAutoSelect, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        panel.add(cAutoSelectReplaceSelection, GBC.eol().fill(GridBagConstraints.HORIZONTAL));
        panel.add(cAddrNode, GBC.eol().fill(GridBagConstraints.HORIZONTAL));

        twidth.setValue(ToolSettings.getWidth());
        tlenstep.setValue(ToolSettings.getLenStep());
        caddr.setSelected(ToolSettings.isUsingAddr());
        cAutoSelect.setSelected(ToolSettings.isAutoSelect());
        cAutoSelectReplaceSelection.setSelected(ToolSettings.isAutoSelectReplaceSelection());
        cAddrNode.setSelected(ToolSettings.PROP_USE_ADDR_NODE.get());

        JButton bAdv = new JButton(tr("Advanced..."));
        bAdv.addActionListener(ignored -> {
            AdvancedSettingsDialog dlg = new AdvancedSettingsDialog();
            if (dlg.getValue() == 1) {
                dlg.saveSettings();
            }
        });
        panel.add(bAdv, GBC.eol().insets(0, 5, 0, 0).anchor(GridBagConstraints.EAST));

        setupDialog();
        showDialog();
    }

    /**
     * Get the specified max width/diameter
     * @return The maximum width/diameter for rectangles/circles
     */
    public final double width() {
        try {
            return NumberFormat.getInstance().parse(twidth.getText()).doubleValue();
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * Get the step length for drawing rectangular buildings
     * @return The discrete steps to increase rectanglular building sizes
     */
    public double lenstep() {
        try {
            return NumberFormat.getInstance().parse(tlenstep.getText()).doubleValue();
        } catch (ParseException e) {
            return 0;
        }
    }

    /**
     * Check if the user wants to use addresses from underlying nodes
     * @return {@code true} if the user wants to use addresses
     */
    public final boolean useAddr() {
        return caddr.isSelected();
    }

    /**
     * Save the settings for this dialog
     */
    public final void saveSettings() {
        if (circleRadio.isSelected()) {
            ToolSettings.saveShape(ToolSettings.Shape.CIRCLE);
        } else if (rectangleRadio.isSelected()) {
            ToolSettings.saveShape(ToolSettings.Shape.RECTANGLE);
        }
        ToolSettings.setSizes(width(), lenstep());
        ToolSettings.setAddrDialog(useAddr());
        ToolSettings.setAutoSelect(cAutoSelect.isSelected());
        ToolSettings.setAutoSelectReplaceSelection(cAutoSelectReplaceSelection.isSelected());
        ToolSettings.PROP_USE_ADDR_NODE.put(cAddrNode.isSelected());
    }
}
