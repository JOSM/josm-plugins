// License: GPL. For details, see LICENSE file.
package relcontext.relationfix;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Utils;

public class AssociatedStreetFixer extends RelationFixer {

    private static final String ADDR_HOUSENUMBER = "addr:housenumber";
    private static final String BUILDING = "building";
    private static final String HOUSE = "house";
    private static final String STREET = "street";

    public AssociatedStreetFixer() {
        super("associatedStreet");
    }

    @Override
    public boolean isRelationGood(Relation rel) {
        for (RelationMember m : rel.getMembers()) {
            if (m.getType() == OsmPrimitiveType.NODE && !HOUSE.equals(m.getRole())) {
                setWarningMessage(tr("Node without ''house'' role found"));
                return false;
            }
            if (m.getType() == OsmPrimitiveType.WAY && !(HOUSE.equals(m.getRole()) || STREET.equals(m.getRole()))) {
                setWarningMessage(tr("Way without ''house'' or ''street'' role found"));
                return false;
            }
            if (m.getType() == OsmPrimitiveType.RELATION && !HOUSE.equals(m.getRole())) {
                setWarningMessage(tr("Relation without ''house'' role found"));
                return false;
            }
        }
        // relation should have name
        if (!rel.hasKey("name")) {
            setWarningMessage(tr("Relation does not have name"));
            return false;
        }
        // check that all street members have same name as relation (???)
        String streetName = rel.get("name");
        if (streetName == null) {
            streetName = "";
        }
        for (RelationMember m : rel.getMembers()) {
            if (STREET.equals(m.getRole()) && !streetName.equals(m.getWay().get("name"))) {
                String anotherName = m.getWay().get("name");
                if (anotherName != null && !anotherName.isEmpty()) {
                    setWarningMessage(tr("Relation has streets with different names"));
                    return false;
                }
            }
        }
        clearWarningMessage();
        return true;
    }

    @Override
    public Command fixRelation(Relation source) {
        // any way with highway tag -> street
        // any way/point/relation with addr:housenumber=* or building=* or type=multipolygon -> house
        // name - check which name is most used in street members and add to relation
        // copy this name to the other street members (???)
        List<RelationMember> members = source.getMembers();
        boolean fixed = false;

        for (int i = 0; i < members.size(); i++) {
            RelationMember m = members.get(i);
            if (m.isNode()) {
                Node node = m.getNode();
                if (!HOUSE.equals(m.getRole()) &&
                        (node.hasKey(BUILDING) || node.hasKey(ADDR_HOUSENUMBER))) {
                    fixed = true;
                    members.set(i, new RelationMember(HOUSE, node));
                }
            } else if (m.isWay()) {
                Way way = m.getWay();
                if (!STREET.equals(m.getRole()) && way.hasKey("highway")) {
                    fixed = true;
                    members.set(i, new RelationMember(STREET, way));
                } else if (!HOUSE.equals(m.getRole()) &&
                        (way.hasKey(BUILDING) || way.hasKey(ADDR_HOUSENUMBER))) {
                    fixed = true;
                    members.set(i, new RelationMember(HOUSE, way));
                }
            } else if (m.isRelation()) {
                Relation relation = m.getRelation();
                if (!HOUSE.equals(m.getRole()) &&
                        (relation.hasKey(BUILDING) || relation.hasKey(ADDR_HOUSENUMBER) || "multipolygon".equals(relation.get("type")))) {
                    fixed = true;
                    members.set(i, new RelationMember(HOUSE, relation));
                }
            }
        }

        // fill relation name
        Map<String, Integer> streetNames = new HashMap<>();
        for (RelationMember m : members) {
            if (STREET.equals(m.getRole()) && m.isWay()) {
                String name = m.getWay().get("name");
                if (name == null || name.isEmpty()) {
                    continue;
                }
                Integer count = streetNames.get(name);
                streetNames.put(name, count != null ? count + 1 : 1);
            }
        }
        String commonName = "";
        Integer commonCount = 0;
        for (Map.Entry<String, Integer> entry : streetNames.entrySet()) {
            if (entry.getValue() > commonCount) {
                commonCount = entry.getValue();
                commonName = entry.getKey();
            }
        }

        Map<String, String> nameTag = new HashMap<>();
        if (!source.hasKey("name") && !commonName.isEmpty()) {
            nameTag.put("name", commonName);
        }

        List<Command> commandList = new ArrayList<>();
        final DataSet ds = Utils.firstNonNull(source.getDataSet(), MainApplication.getLayerManager().getEditDataSet());
        if (fixed) {
            commandList.add(new ChangeMembersCommand(ds, source, members));
        }
        if (!nameTag.isEmpty()) {
            commandList.add(new ChangePropertyCommand(ds, Collections.singleton(source), nameTag));
        }

        // return results
        if (commandList.isEmpty())
            return null;
        return SequenceCommand.wrapIfNeeded(tr("fix associatedStreet relation"), commandList);
    }
}
