// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.datasets.fr;

import static org.openstreetmap.josm.plugins.opendata.core.io.LambertCC9ZonesProjectionPatterns.lambertCC9Zones;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.datasets.SimpleDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.tabular.DefaultCsvHandler;
import org.openstreetmap.josm.tools.Logging;

public abstract class FrenchDataSetHandler extends SimpleDataSetHandler implements FrenchConstants {

    private Projection singleProjection;

    private String nationalPortalPath;

    protected static final Projection lambert93 = OdConstants.PRJ_LAMBERT_93.getProjection(); // France metropolitaine
    protected static final Projection utm20 = Projections.getProjectionByCode("EPSG:32620"); // UTM 20 N - Guadeloupe, Martinique
    protected static final Projection utm22 = Projections.getProjectionByCode("EPSG:32622"); // UTM 22 N - Guyane
    protected static final Projection utm38 = Projections.getProjectionByCode("EPSG:32738"); // UTM 38 S - Mayotte
    protected static final Projection utm40 = Projections.getProjectionByCode("EPSG:32740"); // UTM 40 S - Reunion

    protected static final Projection[] lambert4Zones = new Projection[4];
    static {
        for (int i = 0; i < lambert4Zones.length; i++) {
            lambert4Zones[i] = Projections.getProjectionByCode("EPSG:"+Integer.toString(27561+i));
        }
    }

    protected static final Projection[] projections = new Projection[]{
            lambert93, // France metropolitaine
            utm20, // Guadeloupe, Martinique
            utm22, // Guyane
            utm38, // Mayotte
            utm40, // Reunion
    };

    protected class InternalCsvHandler extends DefaultCsvHandler {
        @Override
        public LatLon getCoor(EastNorth en, String[] fields) {
            if (singleProjection != null) {
                return singleProjection.eastNorth2latlon(en);
            } else {
                return super.getCoor(en, fields);
            }
        }

        @Override
        public boolean handlesProjection() {
            return singleProjection != null;
        }
    }

    protected FrenchDataSetHandler() {
        init();
    }

    protected FrenchDataSetHandler(String relevantTag) {
        super(relevantTag);
        init();
    }

    protected FrenchDataSetHandler(boolean relevantUnion, String[] relevantTags) {
        super(relevantUnion, relevantTags);
        init();
    }

    protected FrenchDataSetHandler(boolean relevantUnion, Tag[] relevantTags) {
        super(relevantUnion, relevantTags);
        init();
    }

    private void init() {
        setShpHandler(new FrenchShpHandler());
        setCsvHandler(new InternalCsvHandler());
    }

    protected final void setNationalPortalPath(String nationalPortalPath) {
        this.nationalPortalPath = nationalPortalPath;
    }

    protected final void setSingleProjection(Projection singleProjection) {
        this.singleProjection = singleProjection;
    }

    @Override
    public URL getNationalPortalURL() {
        try {
            if (nationalPortalPath != null && !nationalPortalPath.isEmpty()) {
                return new URL(FRENCH_PORTAL + "donnees/view/" + nationalPortalPath);
            }
        } catch (MalformedURLException e) {
            Logging.error(e);
        }
        return null;
    }

    @Override
    public String getLocalPortalIconName() {
        return ICON_FR_24;
    }

    @Override
    public String getNationalPortalIconName() {
        return ICON_FR_24;
    }

    protected static LatLon getLatLonByDptCode(EastNorth en, String dpt, boolean useCC9) {
        // CHECKSTYLE.OFF: BooleanExpressionComplexity
        // CHECKSTYLE.OFF: LineLength
        if (dpt.equals("971") || dpt.equals("972") || dpt.equals("977") || dpt.equals("978")) {    // Antilles
            return utm20.eastNorth2latlon(en);
        } else if (dpt.equals("973")) {    // Guyane
            return utm22.eastNorth2latlon(en);
        } else if (dpt.equals("974")) {    // La Réunion
            return utm40.eastNorth2latlon(en);
        } else if (dpt.equals("976") || dpt.equals("985")) { // 985 = ancien code de Mayotte ? (présent dans geofla)
            return utm38.eastNorth2latlon(en);
        } else if (!useCC9) {
            return lambert93.eastNorth2latlon(en);
        } else if (dpt.endsWith("2A") || dpt.endsWith("2B") || dpt.endsWith("20")) {
            return lambertCC9Zones[0].eastNorth2latlon(en);
        } else if (dpt.endsWith("64") || dpt.endsWith("65") || dpt.endsWith("31") || dpt.endsWith("09") || dpt.endsWith("11") || dpt.endsWith("66") || dpt.endsWith("34") || dpt.endsWith("83")) {
            return lambertCC9Zones[1].eastNorth2latlon(en);
        } else if (dpt.endsWith("40") || dpt.endsWith("32") || dpt.endsWith("47") || dpt.endsWith("82") || dpt.endsWith("81") || dpt.endsWith("12") || dpt.endsWith("48") || dpt.endsWith("30") || dpt.endsWith("13") || dpt.endsWith("84") || dpt.endsWith("04") || dpt.endsWith("06")) {
            return lambertCC9Zones[2].eastNorth2latlon(en);
        } else if (dpt.endsWith("33") || dpt.endsWith("24") || dpt.endsWith("46") || dpt.endsWith("19") || dpt.endsWith("15") || dpt.endsWith("43") || dpt.endsWith("07") || dpt.endsWith("26") || dpt.endsWith("05") || dpt.endsWith("38") || dpt.endsWith("73")) {
            return lambertCC9Zones[3].eastNorth2latlon(en);
        } else if (dpt.endsWith("17") || dpt.endsWith("16") || dpt.endsWith("87") || dpt.endsWith("23") || dpt.endsWith("63") || dpt.endsWith("03") || dpt.endsWith("42") || dpt.endsWith("69") || dpt.endsWith("01") || dpt.endsWith("74")) {
            return lambertCC9Zones[4].eastNorth2latlon(en);
        } else if (dpt.endsWith("44") || dpt.endsWith("85") || dpt.endsWith("49") || dpt.endsWith("79") || dpt.endsWith("37") || dpt.endsWith("86") || dpt.endsWith("36") || dpt.endsWith("18") || dpt.endsWith("58") || dpt.endsWith("71") || dpt.endsWith("21") || dpt.endsWith("39") || dpt.endsWith("25")) {
            return lambertCC9Zones[5].eastNorth2latlon(en);
        } else if (dpt.endsWith("29") || dpt.endsWith("56") || dpt.endsWith("22") || dpt.endsWith("35") || dpt.endsWith("53") || dpt.endsWith("72") || dpt.endsWith("41") || dpt.endsWith("28") || dpt.endsWith("45") || dpt.endsWith("89") || dpt.endsWith("10") || dpt.endsWith("52") || dpt.endsWith("70") || dpt.endsWith("88") || dpt.endsWith("68") || dpt.endsWith("90")) {
            return lambertCC9Zones[6].eastNorth2latlon(en);
        } else if (dpt.endsWith("50") || dpt.endsWith("14") || dpt.endsWith("61") || dpt.endsWith("27") || dpt.endsWith("60") || dpt.endsWith("95") || dpt.endsWith("78") || dpt.endsWith("91") || dpt.endsWith("92") || dpt.endsWith("93") || dpt.endsWith("94") || dpt.endsWith("75") || dpt.endsWith("77") || dpt.endsWith("60") || dpt.endsWith("02") || dpt.endsWith("51") || dpt.endsWith("55") || dpt.endsWith("54") || dpt.endsWith("57") || dpt.endsWith("67")) {
            return lambertCC9Zones[7].eastNorth2latlon(en);
        } else if (dpt.endsWith("76") || dpt.endsWith("80") || dpt.endsWith("62") || dpt.endsWith("59") || dpt.endsWith("08")) {
            return lambertCC9Zones[8].eastNorth2latlon(en);
        } else {
            throw new IllegalArgumentException("Unsupported department code: "+dpt);
        }
        // CHECKSTYLE.ON: LineLength
        // CHECKSTYLE.ON: BooleanExpressionComplexity
    }

    private static void replaceFaxPhone(OsmPrimitive p, String dataKey, String osmKey) {
        String phone = p.get(dataKey);
        if (phone != null) {
            p.put(osmKey, phone.replace(" ", "").replace(".", "").replaceFirst("0", "+33"));
            p.remove(dataKey);
        }
    }

    protected void replaceFax(OsmPrimitive p, String dataKey) {
        replaceFaxPhone(p, dataKey, "contact:fax");
    }

    protected void replacePhone(OsmPrimitive p, String dataKey) {
        replaceFaxPhone(p, dataKey, "contact:phone");
    }

    private static final String dayFr = "L|Lu|M|Ma|Me|J|Je|V|Ve|S|Sa|D|Di";
    private static final String dayEn = "Mo|Mo|Tu|Tu|We|Th|Th|Fr|Fr|Sa|Sa|Su|Su";

    private static final String[] dayFrSplit = dayFr.split("\\|");
    private static final String[] dayEnSplit = dayEn.split("\\|");

    protected void replaceOpeningHours(OsmPrimitive p, String dataKey) {
        String hours = p.get(dataKey);
        if (hours != null) {
            hours = hours.replace("h", ":").replace(": ", ":00 ");
            hours = hours.replace(" - ", "-").replace(" au ", "-").replace(" à ", "-");
            hours = hours.replace(" et ", ",").replace("/", ",");
            hours = hours.replace(" (sf vacances)", "; PH off");
            if (hours.endsWith(":")) {
                hours += "00";
            }
            String dayGroup = "("+dayFr+")";
            String dayNcGroup = "(?:"+dayFr+")";
            // FIXME: this doesn't work yet
            for (String sep : new String[]{"-", ","}) {
                boolean finished = false;
                while (!finished) {
                    Matcher m = Pattern.compile(".*("+dayNcGroup+"(?:"+sep+dayNcGroup+")+).*").matcher(hours);
                    if (m.matches()) {
                        String range = m.group(1);
                        Matcher m2 = Pattern.compile(dayGroup+"(?:"+sep+dayGroup+")+").matcher(range);
                        if (m2.matches()) {
                            StringBuilder replacement = new StringBuilder();
                            for (int i = 0; i < m2.groupCount(); i++) {
                                if (i > 0) {
                                    replacement.append(sep);
                                }
                                replacement.append(getEnDay(m2.group(i + 1)));
                            }
                            hours = hours.replace(range, replacement.toString());
                        }
                    } else {
                        finished = true;
                    }
                }
            }
            p.put("opening_hours", hours);
            p.remove(dataKey);
        }
    }

    private static String getEnDay(String frDay) {
        for (int i = 0; i < dayFrSplit.length; i++) {
            if (dayFrSplit[i].equals(frDay)) {
                return dayEnSplit[i];
            }
        }
        return "";
    }
}
