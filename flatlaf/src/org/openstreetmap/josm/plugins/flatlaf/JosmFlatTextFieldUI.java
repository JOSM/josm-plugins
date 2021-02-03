// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.flatlaf;

import java.awt.Color;
import java.beans.PropertyChangeEvent;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;

import com.formdev.flatlaf.ui.FlatTextFieldUI;

/**
 * Special JOSM UI delegate for JTextField that changes a {@link LineBorder} into a FlatLaf outline.
 * Line borders are used in several dialogs/components to indicate warnings/errors/etc.
 * E.g. used by {@link AbstractTextComponentValidator} or {@link DownloadDialog}.
 */
public class JosmFlatTextFieldUI
    extends FlatTextFieldUI
{
    public static ComponentUI createUI(JComponent c) {
        return new JosmFlatTextFieldUI();
    }

    @Override
    protected void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);

        if ("border".equals(e.getPropertyName())) {
            Object newBorder = e.getNewValue();
            JTextComponent textField = getComponent();
            if (!(textField.getParent() instanceof JComboBox)) {
                if (newBorder instanceof LineBorder) {
                    // change LineBorder to FlatLaf outline
                    Color borderColor = ((LineBorder)newBorder).getLineColor();
                    textField.putClientProperty("JComponent.outline", borderColor);
                    textField.setBorder(UIManager.getBorder("TextField.border"));
                } else if (newBorder == null) {
                    // clear FlatLaf outline
                    textField.putClientProperty("JComponent.outline", null);
                }
            }
        }
    }
}
