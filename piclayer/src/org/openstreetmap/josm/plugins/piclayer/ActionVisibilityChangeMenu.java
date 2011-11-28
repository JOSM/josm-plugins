package org.openstreetmap.josm.plugins.piclayer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;

enum PicActions {MOVE_PICTURE, MOVE_POINT, TRANSFORM_POINT, SCALEX, SCALEY, SCALEXY, SHEAR, ROTATE}

@SuppressWarnings("serial")
public class ActionVisibilityChangeMenu extends JMenu {
    public ActionVisibilityChangeMenu() {
        super(tr("Change visibility of controls"));

        for (int i = 0;i < PicLayerPlugin.buttonList.size(); i++) {
            add(new SwitchVisibilityMenuItem(PicLayerPlugin.buttonList.get(i)));
        }
    }
}

@SuppressWarnings("serial")
class SwitchVisibilityMenuItem extends JCheckBoxMenuItem {
    public SwitchVisibilityMenuItem(final PicToggleButton button) {
        super();
        setSelected(Main.pref.getBoolean(button.getVisibilityKey(), button.getDefVisibility()));
        setAction(new ButtonAction(button));
    }
    class ButtonAction extends JosmAction {
        private PicToggleButton button;

        public ButtonAction(PicToggleButton button) {
            super(button.getBtnName(), null, button.getBtnName(), null, false, false);
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean val = !Main.pref.getBoolean(button.getVisibilityKey(), button.getDefVisibility());
            Main.pref.put(button.getVisibilityKey(), val);
            SwitchVisibilityMenuItem.this.setSelected(val);
            button.setVisible(val);
        }

    }
}