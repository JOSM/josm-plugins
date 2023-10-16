// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.datasets.be;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.datasets.SimpleDataSetHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.tabular.CsvHandler;
import org.openstreetmap.josm.plugins.opendata.core.io.tabular.DefaultCsvHandler;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;

public abstract class BelgianDataSetHandler extends SimpleDataSetHandler implements BelgianConstants {

    private Projection singleProjection;

    private String nationalPortalPathDe;
    private String nationalPortalPathEn;
    private String nationalPortalPathFr;
    private String nationalPortalPathNl;

    protected static final Projection lambert1972 = OdConstants.PRJ_LAMBERT_1972.getProjection();
    protected static final Projection lambert2008 = OdConstants.PRJ_LAMBERT_2008.getProjection();

    protected static final Projection[] projections = new Projection[]{
            lambert1972,
            lambert2008
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
    }

    protected BelgianDataSetHandler() {
        init();
    }

    protected BelgianDataSetHandler(String relevantTag) {
        super(relevantTag);
        init();
    }

    protected BelgianDataSetHandler(boolean relevantUnion, String[] relevantTags) {
        super(relevantUnion, relevantTags);
        init();
    }

    protected BelgianDataSetHandler(boolean relevantUnion, Tag[] relevantTags) {
        super(relevantUnion, relevantTags);
        init();
    }

    private void init() {
        setCsvHandler(new InternalCsvHandler());
    }

    protected final void setNationalPortalPath(String nationalPortalPathDe, String nationalPortalPathEn,
            String nationalPortalPathFr, String nationalPortalPathNl) {
        this.nationalPortalPathDe = nationalPortalPathDe;
        this.nationalPortalPathEn = nationalPortalPathEn;
        this.nationalPortalPathFr = nationalPortalPathFr;
        this.nationalPortalPathNl = nationalPortalPathNl;
    }

    protected final void setSingleProjection(Projection singleProjection) {
        this.singleProjection = singleProjection;
        final CsvHandler handler = getCsvHandler();
        if (handler != null) {
            handler.setHandlesProjection(singleProjection != null);
        }
    }

    @Override
    public URL getNationalPortalURL() {
        try {
            String nationalPortalPath;
            String lang = Config.getPref().get("language");
            if (lang == null || lang.isEmpty()) {
                lang = Locale.getDefault().toString();
            }

            if (lang.startsWith("de") && nationalPortalPathDe != null) {
                nationalPortalPath = nationalPortalPathDe;
            } else if (lang.startsWith("fr") && nationalPortalPathFr != null) {
                nationalPortalPath = nationalPortalPathFr;
            } else if (lang.startsWith("nl") && nationalPortalPathNl != null) {
                nationalPortalPath = nationalPortalPathNl;
            } else {
                nationalPortalPath = nationalPortalPathEn;
            }
            return new URL(BELGIAN_PORTAL.replace(OdConstants.PATTERN_LANG, lang.substring(0, 2))+nationalPortalPath); //FIXME
        } catch (MalformedURLException e) {
            Logging.error(e);
        }
        return null;
    }

    @Override
    public String getLocalPortalIconName() {
        return ICON_BE_24;
    }

    @Override
    public String getNationalPortalIconName() {
        return ICON_BE_24;
    }
}
