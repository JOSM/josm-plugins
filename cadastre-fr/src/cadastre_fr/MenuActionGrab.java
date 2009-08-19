package cadastre_fr;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.tools.Shortcut;

public class MenuActionGrab extends JosmAction {

    /**
     * Action calling the wms grabber for cadastre.gouv.fr
     */
    private static final long serialVersionUID = 1L;

    public static String name = "Cadastre grab";

    public MenuActionGrab() {
        super(tr(name), "cadastre_small", tr("Download Image from French Cadastre WMS"),
                Shortcut.registerShortcut("cadastre:grab", tr("Cadastre: {0}", tr("Download Image from French Cadastre WMS")),
                KeyEvent.VK_F11, Shortcut.GROUP_DIRECT), false);
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.map != null) {
            if (CadastrePlugin.isCadastreProjection()) {
                WMSLayer wmsLayer = WMSDownloadAction.getLayer();
                if (wmsLayer != null)
                    DownloadWMSVectorImage.download(wmsLayer);
            } else {
                JOptionPane.showMessageDialog(Main.parent,
                        tr("To enable the cadastre WMS plugin, change\n"
                         + "the current projection to one of the cadastre\n"
                         + "projections and retry"));
            }
        }
    }

}
