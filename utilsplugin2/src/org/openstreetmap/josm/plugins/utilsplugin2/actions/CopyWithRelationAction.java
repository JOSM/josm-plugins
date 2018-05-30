// License: GPL. For details, see LICENSE file.
// Author: David Earl
package org.openstreetmap.josm.plugins.utilsplugin2.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.datatransfer.PrimitiveTransferable;
import org.openstreetmap.josm.gui.datatransfer.data.PrimitiveTransferData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Copy OSM primitives and relation to clipboard in order to paste them, or their tags, somewhere else.
 * @since 404
 */
public class CopyWithRelationAction extends JosmAction {
    /**
     * Constructs a new {@code CopyAction}.
     */
    public CopyWithRelationAction() {
        super(tr("Copy Relation"), "copy",
                tr("Copy selected objects and relations to paste buffer."),
                Shortcut.registerShortcut("system:copy:relation", tr("Edit: {0}", tr("Copy Relation")), KeyEvent.VK_C, Shortcut.ALT_SHIFT), true);
        putValue("help", ht("/Action/Copy"));
        // CUA shortcut for copy (https://en.wikipedia.org/wiki/IBM_Common_User_Access#Description)
        MainApplication.registerActionShortcut(this,
                Shortcut.registerShortcut("system:copy:relation:cua", tr("Edit: {0}", tr("Copy Relation")), KeyEvent.VK_INSERT, Shortcut.CTRL_SHIFT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet set = getLayerManager().getEditDataSet();
        Collection<Relation> relations = set.getRelations();
        Collection<OsmPrimitive> addRelations = new ArrayList<>();

        Collection<OsmPrimitive> selection = set == null ? Collections.<OsmPrimitive>emptySet() : set.getSelected();
        if (selection.isEmpty()) {
            showEmptySelectionWarning();
            return;
        }

        selection.forEach(s -> {
            relations.forEach(r ->{
                r.getMembers().forEach(m -> {
                    if (s.getUniqueId() == m.getUniqueId())
                        addRelations.add(r);
                });
            });
        });

        addRelations.addAll(selection);
        selection = addRelations;

        copy(getLayerManager().getEditLayer(), selection);
    }

    /**
     * Copies the given primitive ids to the clipboard. The output by this function
     * looks similar to: node 1089302677,node 1089303458,way 93793372
     * @param source The OSM data layer source
     * @param primitives The OSM primitives to copy
     */
    public static void copy(OsmDataLayer source, Collection<OsmPrimitive> primitives) {
        // copy ids to the clipboard
        ClipboardUtils.copy(new PrimitiveTransferable(PrimitiveTransferData.getDataWithReferences(primitives), source));
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    protected void showEmptySelectionWarning() {
        JOptionPane.showMessageDialog(
                Main.parent,
                tr("Please select something to copy."),
                tr("Information"),
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}