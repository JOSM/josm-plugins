// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.flatlaf;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxEditor;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ComponentUI;

import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatTextBorder;

/**
 * Special JOSM UI delegate for JComboBox that changes a {@link LineBorder},
 * set on the editor text field, into a FlatLaf outline.
 * Also assigns background color set on editor text field to combo box.
 * E.g. used by {@link AbstractTextComponentValidator}.
 */
public class JosmFlatComboBoxUI
    extends FlatComboBoxUI
{
    public static ComponentUI createUI(JComponent c) {
        return new JosmFlatComboBoxUI();
    }

    @Override
    protected ComboBoxEditor createEditor() {
        ComboBoxEditor comboBoxEditor = super.createEditor();

        Component editor = comboBoxEditor.getEditorComponent();
        if (editor instanceof JTextField) {
            JTextField textField = (JTextField) editor;
            editor.addPropertyChangeListener(e -> {
                String propertyName = e.getPropertyName();
                if ("border".equals(propertyName)) {
                    Object newBorder = e.getNewValue();
                    if (newBorder instanceof LineBorder) {
                        // change LineBorder to FlatLaf outline
                        Color borderColor = ((LineBorder)newBorder).getLineColor();
                        comboBox.putClientProperty("JComponent.outline", borderColor);
                        textField.setBorder(BorderFactory.createEmptyBorder());
                    } else if (newBorder instanceof FlatTextBorder) {
                        // change FlatTextBorder to empty border
                        textField.setBorder(BorderFactory.createEmptyBorder());
                    } else if (newBorder == null) {
                        // clear FlatLaf outline
                        comboBox.putClientProperty("JComponent.outline", null);
                    }
                } else if ("background".equals(propertyName)) {
                    // assign background color set on editor text field to combo box
                    comboBox.setBackground((Color) e.getNewValue());
                }
            });
        }

        return comboBoxEditor;
    }
}
