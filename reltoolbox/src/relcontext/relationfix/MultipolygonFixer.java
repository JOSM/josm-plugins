// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.MultipolygonBuilder;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Utils;

/**
 * @see <a href="https://wiki.openstreetmap.org/wiki/Relation:multipolygon">osmwiki:Relation:multipolygon</a>
 */
public class MultipolygonFixer extends RelationFixer {

    public MultipolygonFixer() {
        super("multipolygon");
    }

    protected MultipolygonFixer(String... types) {
        super(types);
    }

    @Override
    public boolean isRelationGood(Relation rel) {
        for (RelationMember m : rel.getMembers()) {
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
        if (!members.equals(rel.getMembers())) {
            final DataSet ds = Utils.firstNonNull(rel.getDataSet(), MainApplication.getLayerManager().getEditDataSet());
            return new ChangeMembersCommand(ds, rel, members);
        }
        return null;
    }

    /**
     * Basically, create member list of a multipolygon from scratch, and if
     * successful, replace roles with new ones.
     *
     * @param origMembers original list of relation members
     * @return either the original and unmodified list or a new one with at least
     *         one new item or the empty list if the way members don't build a
     *         correct multipolygon
     */
    protected List<RelationMember> fixMultipolygonRoles(final List<RelationMember> origMembers) {
        List<Way> ways = origMembers.stream().filter(m -> m.isWay()).map(m -> m.getWay()).collect(Collectors.toList());

        MultipolygonBuilder mpc = new MultipolygonBuilder();
        String error = mpc.makeFromWays(ways);
        if (error != null)
            return Collections.emptyList();

        Set<Way> outerWays = new HashSet<>();
        for (MultipolygonBuilder.JoinedPolygon poly : mpc.outerWays) {
            outerWays.addAll(poly.ways);
        }
        Set<Way> innerWays = new HashSet<>();
        for (MultipolygonBuilder.JoinedPolygon poly : mpc.innerWays) {
            innerWays.addAll(poly.ways);
        }
        List<RelationMember> members = origMembers;
        for (int i = 0; i < members.size(); i++) {
            RelationMember m = members.get(i);
            if (m.isWay()) {
                final Way way = m.getWay();
                String role = null;
                if (outerWays.contains(way)) {
                    role = "outer";
                } else if (innerWays.contains(way)) {
                    role = "inner";
                }
                if (role != null && !role.equals(m.getRole())) {
                    if (members == origMembers) {
                        members = new ArrayList<>(origMembers); // don't modify original list
                    }
                    members.set(i, new RelationMember(role, way));
                }
            }
        }
        return members;
    }
}
