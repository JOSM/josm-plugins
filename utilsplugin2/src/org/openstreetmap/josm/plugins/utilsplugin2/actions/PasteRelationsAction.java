package org.openstreetmap.josm.plugins.utilsplugin2.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Pastes relation membership from objects in the paste buffer onto selected object(s).
 *
 * @author Zverik
 */
public class PasteRelationsAction extends JosmAction {
    private static final String TITLE = tr("Paste Relations");

    public PasteRelationsAction() {
        super(TITLE, "dumbutils/pasterelations", tr("Paste relation membership from objects in the buffer onto selected object(s)"),
                Shortcut.registerShortcut("tools:pasterelations", tr("Tool: {0}",  tr("Paste Relations")), KeyEvent.VK_V, Shortcut.ALT_CTRL), true);
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        if( selection.isEmpty() )
            return;

        Map<Relation, String> relations = new HashMap<>();
        for( PrimitiveData pdata : Main.pasteBuffer.getDirectlyAdded() ) {
            OsmPrimitive p = getCurrentDataSet().getPrimitiveById(pdata.getUniqueId(), pdata.getType());
            if (p != null) {
	            for( Relation r : OsmPrimitive.getFilteredList(p.getReferrers(), Relation.class)) {
	                String role = relations.get(r);
	                for( RelationMember m : r.getMembers() ) {
	                    if( m.getMember().equals(p) ) {
	                        String newRole = m.getRole();
	                        if( newRole != null && role == null )
	                            role = newRole;
	                        else if( newRole != null ? !newRole.equals(role) : role != null ) {
	                            role = "";
	                            break;
	                        }
	                    }
	                }
	                relations.put(r, role);
	            }
            }
        }

        List<Command> commands = new ArrayList<>();
        for( Relation rel : relations.keySet() ) {
            Relation r = new Relation(rel);
            boolean changed = false;
            for( OsmPrimitive p : selection ) {
                if( !r.getMemberPrimitives().contains(p) && !r.equals(p) ) {
                    String role = relations.get(rel);
                    if ("associatedStreet".equals(r.get("type"))) {
                        if (p.get("highway") != null) {
                            role="street";
                        } else if (p.get("addr:housenumber") != null) {
                            role="house";
                        }
                    }
                    r.addMember(new RelationMember(role, p));
                    changed = true;
                }
            }
            if( changed )
                commands.add(new ChangeCommand(rel, r));
        }

        if( !commands.isEmpty() )
            Main.main.undoRedo.add(new SequenceCommand(TITLE, commands));
    }

    @Override
    protected void updateEnabledState() {
        if( getCurrentDataSet() == null ) {
            setEnabled(false);
        }  else
            updateEnabledState(getCurrentDataSet().getSelected());
    }

    @Override
    protected void updateEnabledState( Collection<? extends OsmPrimitive> selection ) {
        setEnabled(selection != null && !selection.isEmpty() && !Main.pasteBuffer.isEmpty() );
    }
}
