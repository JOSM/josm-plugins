// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Utils;

/**
 * Fix multipolygon boundaries
 * @see <a href="https://wiki.openstreetmap.org/wiki/Relation:boundary">osmwiki:Relation:boundary</a>
 */
public class BoundaryFixer extends MultipolygonFixer {

    public BoundaryFixer() {
        super("boundary", "multipolygon");
    }

    /**
     * For boundary relations both "boundary" and "multipolygon" types are applicable, but
     * it should also have key boundary=administrative to be fully boundary.
     * @see <a href="https://wiki.openstreetmap.org/wiki/Relation:boundary">osmwiki:Relation:boundary</a>
     */
    @Override
    public boolean isFixerApplicable(Relation rel) {
        return super.isFixerApplicable(rel) && "administrative".equals(rel.get("boundary"));
    }

    @Override
    public boolean isRelationGood(Relation rel) {
        for (RelationMember m : rel.getMembers()) {
            if (m.getType() == OsmPrimitiveType.RELATION && !"subarea".equals(m.getRole())) {
                setWarningMessage(tr("Relation without ''subarea'' role found"));
                return false;
            }
            if (m.getType() == OsmPrimitiveType.NODE && !("label".equals(m.getRole()) || "admin_centre".equals(m.getRole()))) {
                setWarningMessage(tr("Node without ''label'' or ''admin_centre'' role found"));
                return false;
            }
            if (m.getType() == OsmPrimitiveType.WAY && !("outer".equals(m.getRole()) || "inner".equals(m.getRole()))) {
                setWarningMessage(tr("Way without ''inner'' or ''outer'' role found"));
                return false;
            }
        }
        clearWarningMessage();
        return true;
    }

    @Override
    public Command fixRelation(Relation rel) {
        List<RelationMember> members = fixMultipolygonRoles(rel.getMembers());
        if (members.isEmpty()) {
            members = rel.getMembers();
        }
        members = fixBoundaryRoles(members);
        if (!members.equals(rel.getMembers())) {
            final DataSet ds = Utils.firstNonNull(rel.getDataSet(), MainApplication.getLayerManager().getEditDataSet());
            return new ChangeMembersCommand(ds, rel, members);
        }
        return null;
    }

    /**
     * Possibly change roles of non-way members.
     * @param origMembers original list of relation members
     * @return either the original and unmodified list or a new one with at least one new item
     */
    private static List<RelationMember> fixBoundaryRoles(List<RelationMember> origMembers) {
        List<RelationMember> members = origMembers;
        for (int i = 0; i < members.size(); i++) {
            RelationMember m = members.get(i);
            String role = null;
            if (m.isRelation()) {
                role = "subarea";
            } else if (m.isNode() && !m.getMember().isIncomplete()) {
                Node n = (Node) m.getMember();
                if (n.hasKey("place")) {
                    String place = n.get("place");
                    if ("state".equals(place) || "country".equals(place) ||
                            "county".equals(place) || "region".equals(place)) {
                        role = "label";
                    } else {
                        role = "admin_centre";
                    }
                } else {
                    role = "label";
                }
            }
            if (role != null && !role.equals(m.getRole())) {
                if (members == origMembers) {
                    members = new ArrayList<>(origMembers); // don't modify original list
                }
                members.set(i, new RelationMember(role, m.getMember()));
            }
        }
        return members;
    }
}
