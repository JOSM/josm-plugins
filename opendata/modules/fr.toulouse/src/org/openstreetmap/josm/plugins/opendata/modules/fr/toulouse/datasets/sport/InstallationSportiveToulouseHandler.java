//    JOSM opendata plugin.
//    Copyright (C) 2011-2012 Don-vip
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse.datasets.sport;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse.datasets.ToulouseDataSetHandler;

public class InstallationSportiveToulouseHandler extends ToulouseDataSetHandler {

    public InstallationSportiveToulouseHandler() {
        super(14413);
        setWikiPage("Installations sportives (Toulouse)");
        setCategory(CAT_SPORT);
    }

    @Override
    public boolean acceptsFilename(String filename) {
        return acceptsKmzFilename(filename, "Sports");
    }

    @Override
    public void updateDataSet(DataSet ds) {
        for (Node n : ds.getNodes()) {
            /*if (n.get("name").startsWith("Gymnase")) {
                n.put("leisure", "sports_centre");
            } else if (n.get("name").startsWith("Piscine")) {
                n.put("leisure", "swimming_pool");
                n.put("sport", "swimming");
            } else if (n.get("name").startsWith("Skate")) {
                n.put("leisure", "skate_park");
                n.put("sport", "skateboard");
            } else if (n.get("name").startsWith("Tennis")) {
                n.put("leisure", "pitch");
                n.put("sport", "tennis");
            } else if (n.get("name").startsWith("Stade")) {
                n.put("leisure", "pitch");
            } else if (n.get("name").startsWith("Dojo")) {
                n.put("amenity", "dojo");
            } else if (n.get("name").startsWith("Boulodrome")) {
                n.put("sport", "boules");
            }*/
        }
    }
}
