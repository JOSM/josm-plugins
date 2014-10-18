// License: GPL. v2 and later. Copyright 2011 by Don-vip
package org.openstreetmap.josm.plugins.fr.epci;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.gui.NameFormatterHook;

/**
 * @author Don-vip
 */
public class EpciNameFormatter implements NameFormatterHook {

    @Override
    public String checkRelationTypeName(IRelation relation, String defaultName) {
        if (relation == null) return null;
        String localAuthorityFR = relation.get("local_authority:FR");
        if (localAuthorityFR != null) {
            return (defaultName != null ? defaultName : "") +
                    "["+("metropole".equals(localAuthorityFR) ? "MP" : localAuthorityFR)+"]";
        } else {
            return null;
        }
    }

    @Override
    public String checkFormat(INode node, String defaultName) {
        return null;
    }

    @Override
    public String checkFormat(IWay node, String defaultName) {
        return null;
    }

    @Override
    public String checkFormat(IRelation node, String defaultName) {
        return null;
    }
}
