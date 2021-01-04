// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.flatlaf;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

import org.openstreetmap.josm.gui.widgets.HideableTabbedPane;
import com.formdev.flatlaf.ui.FlatTabbedPaneUI;

/**
 * Special JOSM UI delegate for JTabbedPane that supports hiding tab area for {@link HideableTabbedPane}.
 */
public class JosmFlatTabbedPaneUI extends FlatTabbedPaneUI {
    public static ComponentUI createUI(JComponent c) {
        return (c instanceof HideableTabbedPane)
            ? new JosmFlatTabbedPaneUI()
            : FlatTabbedPaneUI.createUI(c);
    }

    @Override
    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);

        // HideableTabbedPane replaces FlatTabbedPaneUI with its own implementation
        // based on BasicTabbedPaneUI. This listener detects this, sets a FlatLaf client
        // property to enabed tab area hiding, and then resets the UI delegate to FlatTabbedPaneUI.
        PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                c.removePropertyChangeListener("UI", this);

                if (e.getOldValue() instanceof FlatTabbedPaneUI &&
                    e.getNewValue() != null &&
                    e.getNewValue().getClass().getName().startsWith(HideableTabbedPane.class.getName()+'$'))
                {
                    c.putClientProperty("JTabbedPane.hideTabAreaWithOneTab", true);

                    // reset to FlatTabbedPaneUI
                    c.updateUI();
                }
            }
        };
        c.addPropertyChangeListener("UI", l);
    }
}
