package org.openstreetmap.josm.plugins.imagery.tms;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.gui.jmapviewer.OsmTileSource;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.ImageProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class BingAerialTileSource extends OsmTileSource.AbstractOsmTileSource {
    private static String API_KEY = "Arzdiw4nlOJzRwOz__qailc8NiR31Tt51dN2D7cm57NrnceZnCpgOkmJhNpGoppU";
    private static List<Attribution> attributions;

    public BingAerialTileSource() {
        super("Bing Aerial Maps", "http://ecn.t2.tiles.virtualearth.net/tiles/");

        if (attributions == null) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    attributions = loadAttributionText();
                }
            });
            t.setDaemon(true);
            t.start();
        }
    }

    class Attribution {
        String attribution;
        int minZoom;
        int maxZoom;
        Bounds bounds;
    }

    class AttrHandler extends DefaultHandler {

        private String string;
        private Attribution curr;
        private List<Attribution> attributions = new ArrayList<Attribution>();
        private double southLat;
        private double northLat;
        private double eastLon;
        private double westLon;
        private boolean inCoverage = false;

        @Override
        public void startElement(String uri, String stripped, String tagName,
                Attributes attrs) throws SAXException {
            if("ImageryProvider".equals(tagName)) {
                curr = new Attribution();
            } else if("CoverageArea".equals(tagName)) {
                inCoverage = true;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            string = new String(ch, start, length);
        }

        @Override
        public void endElement(String uri, String stripped, String tagName)
                throws SAXException {
            if("ImageryProvider".equals(tagName)) {
                attributions.add(curr);
            } else if("Attribution".equals(tagName)) {
                curr.attribution = string;
            } else if(inCoverage && "ZoomMin".equals(tagName)) {
                curr.minZoom = Integer.parseInt(string);
            } else if(inCoverage && "ZoomMax".equals(tagName)) {
                curr.maxZoom = Integer.parseInt(string);
            } else if(inCoverage && "SouthLatitude".equals(tagName)) {
                southLat = Double.parseDouble(string);
            } else if(inCoverage && "NorthLatitude".equals(tagName)) {
                northLat = Double.parseDouble(string);
            } else if(inCoverage && "EastLongitude".equals(tagName)) {
                eastLon = Double.parseDouble(string);
            } else if(inCoverage && "WestLongitude".equals(tagName)) {
                westLon = Double.parseDouble(string);
            } else if("BoundingBox".equals(tagName)) {
                curr.bounds = new Bounds(northLat, westLon, southLat, eastLon);
            } else if("CoverageArea".equals(tagName)) {
                inCoverage = false;
            }
            string = "";
        }
    }

    private List<Attribution> loadAttributionText() {
        try {
            URL u = new URL("http://dev.virtualearth.net/REST/v1/Imagery/Metadata/Aerial/0,0?zl=1&mapVersion=v1&key="+API_KEY+"&include=ImageryProviders&output=xml");
            InputStream stream = u.openStream();
            XMLReader parser = XMLReaderFactory.createXMLReader();
            AttrHandler handler = new AttrHandler();
            parser.setContentHandler(handler);
            parser.parse(new InputSource(stream));
            System.err.println("Added " + handler.attributions.size() + " attributions.");
            return handler.attributions;
        } catch (IOException e) {
            System.err.println("Could not open Bing aerials attribution metadata.");
        } catch (SAXException e) {
            System.err.println("Could not parse Bing aerials attribution metadata.");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getMaxZoom() {
        return 22;
    }

    @Override
    public String getExtension() {
        return("jpeg");
    }

    @Override
    public String getTilePath(int zoom, int tilex, int tiley) {
        String quadtree = computeQuadTree(zoom, tilex, tiley);
        return "/tiles/a" + quadtree + "." + getExtension() + "?g=587";
    }

    @Override
    public TileUpdate getTileUpdate() {
        return TileUpdate.IfNoneMatch;
    }

    @Override
    public boolean requiresAttribution() {
        return true;
    }

    @Override
    public Image getAttributionImage() {
        return ImageProvider.get("bing_maps").getImage();
    }

    @Override
    public String getAttributionLinkURL() {
        //return "http://bing.com/maps"
        // FIXME: I've set attributionLinkURL temporarily to ToU URL to comply with bing ToU
        // (the requirement is that we have such a link at the bottom of the window)
        return "http://go.microsoft.com/?linkid=9710837";
    }

    @Override
    public String getTermsOfUseURL() {
        return "http://opengeodata.org/microsoft-imagery-details";
    }

    @Override
    public String getAttributionText(int zoom, LatLon topLeft, LatLon botRight) {
        if (attributions == null) {
            // TODO: don't show Bing tiles until attribution data is loaded
            return "";
        }
        Bounds windowBounds = new Bounds(topLeft, botRight);
        StringBuilder a = new StringBuilder();
        for (Attribution attr : attributions) {
            Bounds attrBounds = attr.bounds;
            if(zoom <= attr.maxZoom && zoom >= attr.minZoom) {
                if(windowBounds.getMin().lon() < attrBounds.getMax().lon()
                        && windowBounds.getMax().lon() > attrBounds.getMin().lon()
                        && windowBounds.getMax().lat() < attrBounds.getMin().lat()
                        && windowBounds.getMin().lat() > attrBounds.getMax().lat()) {
                    a.append(attr.attribution);
                    a.append(" ");
                }
            }
        }
        return a.toString();
    }

    static String computeQuadTree(int zoom, int tilex, int tiley) {
        StringBuilder k = new StringBuilder();
        for(int i = zoom; i > 0; i--) {
            char digit = 48;
            int mask = 1 << (i - 1);
            if ((tilex & mask) != 0) {
                digit += 1;
            }
            if ((tiley & mask) != 0) {
                digit += 2;
            }
            k.append(digit);
        }
        return k.toString();
    }
}