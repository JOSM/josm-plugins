package buildings_tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class BuildingSizeAction extends JosmAction {

	public BuildingSizeAction() {
		super(tr("Set buildings size"), "mapmode/building", tr("Set buildings size"),
				Shortcut.registerShortcut("edit:buildingsdialog",
				tr("Edit: {0}", tr("Set buildings size")),
				KeyEvent.VK_W, Shortcut.GROUP_EDIT,
				Shortcut.SHIFT_DEFAULT),
				true);
	}

	public void actionPerformed(ActionEvent arg0) {
		BuildingSizeDialog dlg = new BuildingSizeDialog();
		if (dlg.getValue() == 1) {
			ToolSettings.setSizes(dlg.width(), dlg.lenstep());
			ToolSettings.setAddrDialog(dlg.useAddr());
		}
	}
}
