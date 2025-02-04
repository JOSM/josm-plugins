// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.buildings_tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * Address dialog for buildings
 */
public class AddressDialog extends MyDialog {
    private static String lhousenum;
    private static String lstreetname;
    private static int inc;
    private final JTextField housenum = new JTextField();
    private final JTextField streetname = new JTextField();
    private final JSpinner incSpinner;

    /**
     * Create a new dialog object
     */
    public AddressDialog() {
        super(tr("Building address"));

        addLabelled(tr("House number:"), housenum);
        addLabelled(tr("Street Name:"), streetname);
        housenum.setText(nextHouseNum());
        streetname.setText(lstreetname);

        SpinnerNumberModel incModel = new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        incSpinner = new JSpinner(incModel);
        incSpinner.setValue(inc);
        addLabelled(tr("House number increment:"), incSpinner);

        setContent(panel);
        setupDialog();
    }

    private static String nextHouseNum() {
        if (lhousenum == null)
            return "";
        try {
            int num = NumberFormat.getInstance().parse(lhousenum).intValue() + inc;
            return Integer.toString(num);
        } catch (ParseException e) {
            return lhousenum;
        }
    }

    /**
     * Save values for future use in the current running JOSM instance
     */
    public final void saveValues() {
        saveValues(this);
    }

    private static void saveValues(AddressDialog dialog) {
        lhousenum = dialog.getHouseNum();
        lstreetname = dialog.getStreetName();
        inc = (Integer) dialog.incSpinner.getValue();
    }

    public final String getHouseNum() {
        return housenum.getText();
    }

    public final String getStreetName() {
        return streetname.getText();
    }
}
