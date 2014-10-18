// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.modules.fr.datagouvfr.datasets.administration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.plugins.opendata.modules.fr.datagouvfr.datasets.DataGouvDataSetHandler;
import org.openstreetmap.josm.tools.Pair;

public class GeoFlaHandler extends DataGouvDataSetHandler {
    
    public GeoFlaHandler() {
        super();
        setName("GEOFLA®");
        getShpHandler().setPreferMultipolygonToSimpleWay(true);
        try {
            setLocalPortalURL("http://professionnels.ign.fr/geofla#tab-3");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getLocalPortalIconName() {
        return ICON_IGN_24;
    }

    @Override
    public boolean acceptsFilename(String filename) {
        return isDepartementFile(filename) || isCommuneFile(filename) || isCantonFile(filename) || isArrondissementFile(filename);
    }
        
    protected boolean isDepartementFile(String filename) {
        return acceptsShpMifFilename(filename, "DEPARTEMENT") || acceptsShpMifFilename(filename, "LIMITE_DEPARTEMENT");
    }

    protected boolean isCommuneFile(String filename) {
        return acceptsShpFilename(filename, "COMMUNE") || acceptsShpFilename(filename, "LIMITE_COMMUNE");
    }

    protected boolean isCantonFile(String filename) {
        return acceptsShpFilename(filename, "CANTON") || acceptsShpFilename(filename, "LIMITE_CANTON");
    }

    protected boolean isArrondissementFile(String filename) {
        return acceptsShpFilename(filename, "ARRONDISSEMENT") || acceptsShpFilename(filename, "LIMITE_ARRONDISSEMENT");
    }

    @Override
    public void updateDataSet(DataSet ds) {
        final String filename = getAssociatedFile().getName();
        if (isDepartementFile(filename)) {
            setNationalPortalPath("GEOFLA®-Départements-30383060");
        } else if (isCommuneFile(filename)) {
            setNationalPortalPath("GEOFLA®-Communes-30383083");
        }
        for (OsmPrimitive p : ds.allPrimitives()) {
            if (hasKeyIgnoreCase(p, "Id_geofla", "Id_GéoFLA")) {
                String deptName = WordUtils.capitalizeFully(getAndRemoveIgnoreCase(p, "Nom_dept", "Nom_Département"));
                if ("Reunion".equals(deptName)) {
                    deptName = "La Réunion";
                }
                if (isDepartementFile(filename)) {
                    p.put("name", deptName);
                } else if (isCommuneFile(filename)) {
                    p.put("name", WordUtils.capitalizeFully(getAndRemoveIgnoreCase(p, "NOM_COMM")));
                    replace(p, "INSEE_COM", "ref:INSEE");
                }
                p.put("boundary", "administrative");
                String nature = getIgnoreCase(p, "Nature");
                if ("Frontière internationale".equalsIgnoreCase(nature) || "Limite côtière".equalsIgnoreCase(nature)) {
                    p.put("admin_level", "2");
                } else if ("Limite de région".equalsIgnoreCase(nature)) {
                    p.put("admin_level", "4");
                } else if (isDepartementFile(filename) || "Limite de département".equalsIgnoreCase(nature)) {
                    p.put("admin_level", "6");
                } else if(isArrondissementFile(filename) || "Limite d'arrondissement".equalsIgnoreCase(nature)) {
                    p.put("admin_level", "7");
                } else if(isCommuneFile(filename)) {
                    p.put("admin_level", "8");
                }
                if (p instanceof Relation) {
                    p.put("type", "boundary");
                }
                LatLon llCentroid = getLatLon(p, deptName, "centroid", "Centroïde");
                if (llCentroid != null) {
                    Node centroid = new Node(llCentroid);
                    ds.addPrimitive(centroid);
                    //centroid.put("name", p.get("name"));
                    if (p instanceof Relation) {
                        ((Relation) p).addMember(new RelationMember("centroid", centroid));
                    }
                }
                LatLon llChefLieu = getLatLon(p, deptName, "chf_lieu", "Chef_Lieu");
                if (llChefLieu != null) {
                    Node chefLieu = new Node(llChefLieu);
                    ds.addPrimitive(chefLieu);
                    //chefLieu.put("Code_chf", getAndRemoveIgnoreCase(p, "Code_chf", "Code_Chef_Lieu"));
                    String name = WordUtils.capitalizeFully(getAndRemoveIgnoreCase(p, "Nom_chf", "Nom_Chef_lieu"));
                    if (isArrondissementFile(filename)) {
                        p.put("name", name);
                    }
                    chefLieu.put("name", name);
                    if (p instanceof Relation) {
                        ((Relation) p).addMember(new RelationMember("admin_centre", chefLieu));
                    }
                }
            }
        }
    }
    
    protected static boolean hasKeyIgnoreCase(OsmPrimitive p, String ... strings) {
        return getIgnoreCase(p, strings) != null;
    }

    protected static String getIgnoreCase(OsmPrimitive p, String ... strings) {
        String result = null;
        for (String s : strings) {
            if (result == null) result = p.get(s);
            if (result == null) result = p.get(s.toUpperCase());
            if (result == null) result = p.get(s.toLowerCase());
        }
        return result;
    }

    protected static void removeIgnoreCase(OsmPrimitive p, String ... strings) {
        for (String s : strings) {
            p.remove(s);
            p.remove(s.toUpperCase());
            p.remove(s.toLowerCase());
        }
    }
    
    protected static String getAndRemoveIgnoreCase(OsmPrimitive p, String ... strings) {
        String result = getIgnoreCase(p, strings);
        removeIgnoreCase(p, strings);
        return result;
    }

    protected static LatLon getLatLon(OsmPrimitive p, String dptName, String shortAttribute, String longAttribute) {
        String x = getAndRemoveIgnoreCase(p, "X_"+shortAttribute, "Abscisse_"+longAttribute);
        String y = getAndRemoveIgnoreCase(p, "Y_"+shortAttribute, "Ordonnée_"+longAttribute);
        if (x != null && y != null) {
            try {
                String dptCode = getIgnoreCase(p, "Code_dept", "Code_Département");
                if (dptCode != null && dptCode.equals("97") && dptName != null) {
                    if (dptName.equals("Guadeloupe")) {
                        dptCode = "971";
                    } else if (dptName.equals("Martinique")) {
                        dptCode = "972";
                    } else if (dptName.equals("Guyane")) {
                        dptCode = "973";
                    } else if (dptName.equals("La Réunion")) {
                        dptCode = "974";
                    } else if (dptName.equals("Mayotte")) {
                        dptCode = "976";
                    } else {
                        System.err.println("Unknown French department: "+dptName);
                    }
                }
                return getLatLonByDptCode(new EastNorth(Double.parseDouble(x)*100.0, Double.parseDouble(y)*100.0), dptCode, false);
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
            }
        }
        return null;
    }
    
    private Pair<String, URL> getGeoflaURL(String name, String urlSuffix) throws MalformedURLException {
        return new Pair<>(name, new URL("http://professionnels.ign.fr/sites/default/files/"+urlSuffix));
    }

    @Override
    public List<Pair<String, URL>> getDataURLs() {
        List<Pair<String, URL>> result = new ArrayList<>();
        try {
            result.add(getGeoflaURL("Départements France métropolitaine et Corse", "GEOFLADept_FR_Corse_AV_L93.zip"));
            result.add(getGeoflaURL("Départements France entière",                 "FR_DOM_Mayotte_shp_WGS84.zip"));
            // FIXME: tar.gz files
            /*result.add(getGeoflaURL("Communes France métropolitaine", "531/266/5312664/GEOFLA_1-1_SHP_LAMB93_FR-ED111.tar.gz"));
            result.add(getGeoflaURL("Communes Guadeloupe",            "531/265/5312650/GEOFLA_1-1_SHP_UTM20W84_GP-ED111.tar.gz"));
            result.add(getGeoflaURL("Communes Martinique",            "531/265/5312653/GEOFLA_1-1_SHP_UTM20W84_MQ-ED111.tar.gz"));
            result.add(getGeoflaURL("Communes Guyane",                "531/265/5312657/GEOFLA_1-1_SHP_UTM22RGFG95_GF-ED111.tar.gz"));
            result.add(getGeoflaURL("Communes Réunion",               "531/266/5312660/GEOFLA_1-1_SHP_RGR92UTM40S_RE-ED111.tar.gz"));
            result.add(getGeoflaURL("Communes Mayotte",               "531/275/5312753/GEOFLA_1-1_SHP_RGM04UTM38S_YT-ED111.tar.gz"));*/
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return result;
    }
}
