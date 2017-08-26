// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.turnrestrictions.list;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListSelectionModel;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * This is the list model for the list of turn restrictions related to OSM
 * objects in the current selection.
 */
public class TurnRestrictionsInSelectionListModel extends TurnRestrictionsListModel
    implements ActiveLayerChangeListener, SelectionChangedListener {

    public TurnRestrictionsInSelectionListModel(
            DefaultListSelectionModel selectionModel) {
        super(selectionModel);
    }

    /**
     * Initializes the model with the turn restrictions the primitives in
     * {@code selection} participate.
     *
     * @param selection the collection of selected primitives
     */
    public void initFromSelection(Collection<? extends OsmPrimitive> selection) {
        Set<Relation> turnRestrictions = new HashSet<>();
        if (selection == null) return;
        for (OsmPrimitive p: selection) {
            for (OsmPrimitive parent: p.getReferrers()) {
                if (isTurnRestriction(parent))
                    turnRestrictions.add((Relation) parent);
            }
        }
        setTurnRestrictions(turnRestrictions);
    }

    /* --------------------------------------------------------------------------- */
    /* interface ActiveLayerChangeListener                                         */
    /* --------------------------------------------------------------------------- */
    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        OsmDataLayer newLayer = MainApplication.getLayerManager().getEditLayer();
        if (newLayer == null) {
            setTurnRestrictions(null);
            return;
        }
        initFromSelection(newLayer.data.getSelected());
    }

    /* --------------------------------------------------------------------------- */
    /* interface SelectionChangedListener                                          */
    /* --------------------------------------------------------------------------- */
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        initFromSelection(newSelection);
    }
}
