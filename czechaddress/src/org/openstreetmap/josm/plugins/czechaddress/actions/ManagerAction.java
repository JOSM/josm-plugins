package org.openstreetmap.josm.plugins.czechaddress.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.plugins.czechaddress.gui.ManagerDialog;
import org.openstreetmap.josm.tools.Shortcut;

/**
 *
 * @author Radomír Černoch, radomir.cernoch@gmail.com
 */
public class ManagerAction extends JosmAction {

    public ManagerAction() {
        super("Upravit databázi",
              null,//"envelope-closed-big.png",
              "Upravit jména elemntů dle mapy",
              Shortcut.registerShortcut("address:assignaddress",
                        "Adresy: Přiřadit adresy",
                        KeyEvent.VK_P, Shortcut.GROUP_DIRECT, Shortcut.SHIFT_DEFAULT),
              true);
    }

    public void actionPerformed(ActionEvent e) {
        (new ManagerDialog()).setVisible(true);
    }
}
