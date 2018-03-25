// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.gui;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.layers.OdDataLayer;
import org.openstreetmap.josm.plugins.opendata.core.layers.OdLayer;
import org.openstreetmap.josm.tools.Shortcut;

@SuppressWarnings("serial")
public class OdDialog extends ToggleDialog implements ActiveLayerChangeListener {

    //private final SideButton selectButton;
    private final SideButton downloadButton;
    private final SideButton diffButton;
    private final SideButton toolsButton;

    private final List<SideButton> buttons;

    private final TreeModel treeModel;

    private OdDataLayer dataLayer;

    private class DownloadAction extends JosmAction {
        DownloadAction() {
            super(marktr("Download"), "download", tr("Download OSM data corresponding to the current data set."), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (MainApplication.getLayerManager().getEditLayer() instanceof OdLayer) {
                dataLayer.downloadOsmData();
                diffButton.setEnabled(dataLayer.osmLayer != null);
            }
        }
    }

    private class SelectAction extends JosmAction {
        SelectAction() {
            super(marktr("Select"), "dialogs/select",
                    tr("Set the selected elements on the map to the selected items in the list above."), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO
        }
    }

    private class DiffAction extends JosmAction {
        DiffAction() {
            super(marktr("Diff"), "dialogs/diff", tr("Perform diff between current data set and existing OSM data."), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO
        }
    }

    private class ToolsAction extends JosmAction {
        ToolsAction() {
            super(marktr("Tools"), "dialogs/utils", tr("Open tools menu for this data."), null, false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Done via mouseListener
        }
    }

    public OdDialog() {
        super("OpenData", OdConstants.ICON_CORE_24, tr("Open the OpenData window."),
                Shortcut.registerShortcut("subwindow:opendata", tr("Toggle: {0}", "OpenData"),
                        KeyEvent.VK_A, Shortcut.ALT_CTRL_SHIFT), 150, false, OdPreferenceSetting.class);

        this.buttons = Arrays.asList(new SideButton[] {
                /*selectButton =*/ new SideButton(new SelectAction()),
                downloadButton = new SideButton(new DownloadAction()),
                diffButton = new SideButton(new DiffAction()),
                toolsButton = new SideButton(new ToolsAction())
        });

        this.toolsButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (MainApplication.getLayerManager().getEditLayer() instanceof OdLayer) {
                    JPopupMenu popup = new JPopupMenu();
                    for (JosmAction tool : dataLayer.handler.getTools()) {
                        popup.add(tool);
                    }
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        disableAllButtons();

        this.treeModel = new DefaultTreeModel(null); // TODO: treeNode
        this.dataLayer = null;

        createLayout(new JTree(treeModel), true, buttons);

        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
    }

    private void disableAllButtons() {
        for (SideButton button : buttons) {
            button.setEnabled(false);
        }
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        Layer newLayer = MainApplication.getLayerManager().getActiveLayer();
        if (newLayer instanceof OdLayer) {
            dataLayer = ((OdLayer) newLayer).getDataLayer();
        } else {
            dataLayer = null;
        }

        if (dataLayer != null) {
            if (dataLayer.osmLayer == null) {
                downloadButton.setEnabled(true);
            }
            toolsButton.setEnabled(dataLayer.handler != null && !dataLayer.handler.getTools().isEmpty());
        } else {
            disableAllButtons();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
    }

    public OdDataLayer getDataLayer() {
        return dataLayer;
    }
}
