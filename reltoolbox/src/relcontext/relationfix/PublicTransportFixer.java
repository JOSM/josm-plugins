// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Utils;

import relcontext.actions.PublicTransportHelper;

/**
 * Helper function for determinate role in public_transport relation
 * @author freeExec
 * @see <a href="https://wiki.openstreetmap.org/wiki/Key:public_transport">osmwiki:Key:public_transport</a>
 */
public class PublicTransportFixer extends RelationFixer {

    public PublicTransportFixer() {
        super("route", "public_transport");
    }

    @Override
    public boolean isRelationGood(Relation rel) {
        for (RelationMember m : rel.getMembers()) {
            if (m.getType() == OsmPrimitiveType.NODE
                    && !(m.getRole().startsWith(PublicTransportHelper.STOP) || m.getRole().startsWith(PublicTransportHelper.PLATFORM))) {
                setWarningMessage(tr("Node without ''stop'' or ''platform'' role found"));
                return false;
            }
            if (m.getType() == OsmPrimitiveType.WAY
                    && PublicTransportHelper.isWayPlatform(m)
                    && !m.getRole().startsWith(PublicTransportHelper.PLATFORM)) {
                setWarningMessage(tr("Way platform without ''platform'' role found") + " r" + m.getUniqueId());
                return false;
            }
        }
        clearWarningMessage();
        return true;
    }

    @Override
    public Command fixRelation(Relation rel) {
        List<RelationMember> members = fixStopPlatformRole(rel.getMembers());
        if (!members.equals(rel.getMembers())) {
            final DataSet ds = Utils.firstNonNull(rel.getDataSet(), MainApplication.getLayerManager().getEditDataSet());
            return new ChangeMembersCommand(ds, rel, rel.getMembers());
        }
        return null;
    }

    /**
     * Fix roles of members.
     * @param origMembers original list of relation members
     * @return either the original and unmodified list or a new one with at least one new item
     */
    private static List<RelationMember> fixStopPlatformRole(List<RelationMember> origMembers) {
        List<RelationMember> members = origMembers;
        for (int i = 0; i < members.size(); i++) {
            RelationMember m = members.get(i);
            String role = PublicTransportHelper.getRoleByMember(m);

            if (role != null && !m.getRole().startsWith(role)) {
                if (members == origMembers) {
                    members = new ArrayList<>(origMembers);
                }
                members.set(i, new RelationMember(role, m.getMember()));
            }
        }
        return members;
    }
}
