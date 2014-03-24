// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse.datasets.transport;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi;
import org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse.datasets.ToulouseDataSetHandler;

import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaQueryType.*;
import static org.openstreetmap.josm.plugins.opendata.core.io.OverpassApi.OaRecurseType.*;

public class PistesCyclablesHandler extends ToulouseDataSetHandler {

    protected final Map<String, Collection<String>> map = new HashMap<String, Collection<String>>();
    
    private String streetField;
    
    public PistesCyclablesHandler() {
        this("Nom_voie");
        setCategory(CAT_TRANSPORT);
    }
    
    public PistesCyclablesHandler(String streetField) {
        super(12544, true, "cycleway", "cycleway:right", "cycleway:left", "cycleway:both", "highway=cycleway", "ramp:bicycle=yes", "bicycle=yes", "barrier=cycle_barrier");
        setWikiPage("Réseau cyclable et vert");
        addForbiddenTag("cycleway=no");
        this.streetField = streetField;
        map.put("secondary", Arrays.asList(new String[] {"AVENUE", "Av ", "av ", "Avenue ", "avenue ", "BOULEVARD ", "bd ", "ALLEE", "Allee", "allee", 
                "PONT ", "Pont ", "PORT ", "ROUTE ", "Rte ", "BOULINGRIN", "boulingrin"}));
        map.put("residential", Arrays.asList(new String[] {"CHEMIN ", "Chemin ", "chemin ", "IMPASSE ", "imp ", "PLACE ", "Place ", "place ", 
                "RUE ", "Rue ", "rue ", "QUAI", "VOIE ", "grand rue"}));
    }
    
    @Override
    public boolean acceptsFilename(String filename) {
        return acceptsKmzTabFilename(filename, "Pistes_Cyclables");
    }
    
    @Override
    protected String getOverpassApiQueries(String bbox, String... conditions) {
        return OverpassApi.query(bbox, NODE, conditions) + "\n" + 
                OverpassApi.recurse(NODE_RELATION, RELATION_WAY, WAY_NODE) + "\n" +
                OverpassApi.query(bbox, WAY, conditions) + "\n" +
                OverpassApi.recurse(WAY_NODE, "nodes") + "\n" +
                OverpassApi.recurse(WAY_RELATION);
    }

    private String applyHighwayTag(String name, IPrimitive p) {
        if (name != null && p != null) {
            for (String key : map.keySet()) {
                for (String value : map.get(key)) {
                    if (name.startsWith(value)) {
                        p.put("highway", key);
                        return key;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public void updateDataSet(DataSet ds) {
        for (Way w : ds.getWays()) {

            w.remove("name");
            
            String obs_type = w.get("obs_type");
            if (obs_type.equals("bande")) {
                w.put("cycleway", "lane");
            } else if (obs_type.equals("bande a contresens") || obs_type.equals("contre allee")) {
                w.put("cycleway", "opposite_lane");
            } else if (obs_type.equals("couloir bus")) {
                w.put("cycleway", "share_busway");
            } else if (obs_type.equals("trottoir")) {
                w.put("cycleway", "track");
            } else if (obs_type.equalsIgnoreCase("cheminement mixte") || obs_type.equals("reseau vert")) {
                w.put("highway", "cycleway");
                w.put("foot", "yes");
            } else if (obs_type.equals("piste") || obs_type.equals("voie verte")) {
                w.put("highway", "cycleway");
            } else if (obs_type.equals("zone 30")) {
                w.put("zone:maxspeed", "FR:30");
            } else {
                System.out.println(obs_type);
            }
                
            String name = w.get(streetField);
            if (name != null) {
                w.remove(streetField);
                    
                if (w.get("highway") == null && applyHighwayTag(name, w) == null) {
                    w.put("highway", "road");
                }

                w.put("name", name);
            }
        }
    }
}
