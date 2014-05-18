package org.openstreetmap.josm.plugins.turnrestrictions.list;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.data.osm.Relation;

/**
 * The abstract base class for two views of turn restriction lists.
 * 
 * @see TurnRestrictionsInSelectionView
 * @see TurnRestrictionsInDatasetView
 *
 */
abstract class AbstractTurnRestrictionsListView extends JPanel {
    protected TurnRestrictionsListModel model;
    protected JList<Relation> lstTurnRestrictions;
    
    public TurnRestrictionsListModel getModel(){
        return model;
    }
    
    public JList<Relation> getList() {
        return lstTurnRestrictions;
    }
    
    public void addListSelectionListener(ListSelectionListener listener) {
        lstTurnRestrictions.addListSelectionListener(listener);
    }
     
    public void removeListSelectionListener(ListSelectionListener listener) {
        lstTurnRestrictions.addListSelectionListener(listener);
    }
    
    public void initIconSetFromPreferences(Preferences prefs){
        TurnRestrictionCellRenderer renderer = (TurnRestrictionCellRenderer)lstTurnRestrictions.getCellRenderer();
        renderer.initIconSetFromPreferences(prefs);
    }
}
