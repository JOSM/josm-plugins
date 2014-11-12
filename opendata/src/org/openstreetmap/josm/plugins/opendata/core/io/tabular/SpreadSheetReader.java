// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.tabular;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.plugins.opendata.core.OdConstants;
import org.openstreetmap.josm.plugins.opendata.core.gui.ChooserLauncher;
import org.openstreetmap.josm.plugins.opendata.core.io.ProjectionPatterns;

public abstract class SpreadSheetReader extends AbstractReader {
    
    private static final NumberFormat formatFrance = NumberFormat.getInstance(Locale.FRANCE);
    private static final NumberFormat formatUK = NumberFormat.getInstance(Locale.UK);
    
    protected final SpreadSheetHandler handler;

    public SpreadSheetReader(SpreadSheetHandler handler) {
        this.handler = handler;
    }

    protected static double parseDouble(String value) throws ParseException {
        if (value.contains(",")) { 
            return formatFrance.parse(value.replace(" ", "")).doubleValue();
        } else {
            return formatUK.parse(value.replace(" ", "")).doubleValue();
        }
    }
    
    protected abstract void initResources(InputStream in, ProgressMonitor progressMonitor) throws IOException;
    
    protected abstract String[] readLine(ProgressMonitor progressMonitor) throws IOException;
    
    protected final int getSheetNumber() {
        return handler != null && handler.getSheetNumber() > -1 ? handler.getSheetNumber() : 0;
    }
    
    protected final int getLineNumber() {
        return handler != null ? handler.getLineNumber() : -1;
    }
    
    public static class CoordinateColumns {
        public Projection proj = null;
        public int xCol = -1;
        public int yCol = -1;
        public final boolean isOk() {
            return xCol > -1 && yCol > -1;
        }
        @Override
        public String toString() {
            return "CoordinateColumns [proj=" + proj + ", xCol=" + xCol + ", yCol=" + yCol + "]";
        }
    }
    
    private final CoordinateColumns addCoorColIfNeeded(List<CoordinateColumns> columns, CoordinateColumns col) {
        if (col == null || col.isOk()) {
            columns.add(col = new CoordinateColumns());
        }
        return col;
    }
    
    public DataSet doParse(String[] header, ProgressMonitor progressMonitor) throws IOException {
        Main.info("Header: "+Arrays.toString(header));
        
        Map<ProjectionPatterns, List<CoordinateColumns>> projColumns = new HashMap<>();
        
        for (int i = 0; i<header.length; i++) {
            for (ProjectionPatterns pp : OdConstants.PROJECTIONS) {
                List<CoordinateColumns> columns = projColumns.get(pp);
                if (columns == null) {
                    projColumns.put(pp, columns = new ArrayList<>());
                }
                CoordinateColumns col = columns.isEmpty() ? null : columns.get(columns.size()-1);
                if (pp.getXPattern().matcher(header[i]).matches()) {
                    addCoorColIfNeeded(columns, col).xCol = i;
                    break;
                } else if (pp.getYPattern().matcher(header[i]).matches()) {
                    addCoorColIfNeeded(columns, col).yCol = i;
                    break;
                }
            }
        }

        final List<CoordinateColumns> columns = new ArrayList<>();
        
        for (ProjectionPatterns pp : projColumns.keySet()) {
            for (CoordinateColumns col : projColumns.get(pp)) {
                if (col.isOk()) {
                    columns.add(col);
                    if (col.proj == null) {
                        col.proj = pp.getProjection(header[col.xCol], header[col.yCol]);
                    }
                }
            }
        }

        final boolean handlerOK = handler != null && handler.handlesProjection();

        boolean projFound = false;
        
        for (CoordinateColumns c : columns) {
            if (c.proj != null) {
                projFound = true;
                break;
            }
        }
        
        if (projFound) {
            // projection identified, do nothing
        } else if (!columns.isEmpty()) {
            if (!handlerOK) {
                if (GraphicsEnvironment.isHeadless()) {
                    throw new IllegalArgumentException("No valid coordinates have been found and cannot prompt user in headless mode.");
                }
                // TODO: filter proposed projections with min/max values ?
                Projection p = ChooserLauncher.askForProjection(progressMonitor);
                if (p == null) {
                    return null; // User clicked Cancel
                }
                for (CoordinateColumns c : columns) {
                    c.proj = p;
                }
            }
            
        } else {
            throw new IllegalArgumentException(tr("No valid coordinates have been found."));
        }

        String message = "";
        for (CoordinateColumns c : columns) {
            if (!message.isEmpty()) {
                message += "; ";
            }
            message += c.proj + "("+header[c.xCol]+", "+header[c.yCol]+")";
        }
        
        Main.info("Loading data using projections "+message);
        
        final DataSet ds = new DataSet();
        int lineNumber = 1;
        
        String[] fields;
        while ((fields = readLine(progressMonitor)) != null) {
            lineNumber++;
            if (handler != null) {
                handler.setXCol(-1);
                handler.setYCol(-1);
            }
            
            final Map<CoordinateColumns, EastNorth> ens = new HashMap<>();
            final Map<CoordinateColumns, Node> nodes = new HashMap<>();
            for (CoordinateColumns c : columns) {
                nodes.put(c, new Node());
                ens.put(c, new EastNorth(Double.NaN, Double.NaN));
            }
            
            if (fields.length > header.length) {
                Main.warn(tr("Invalid file. Bad length on line {0}. Expected {1} columns, got {2}.", lineNumber, header.length, fields.length));
                Main.warn(Arrays.toString(fields));
            }
            
            for (int i = 0; i<Math.min(fields.length, header.length); i++) {
                try {
                    boolean coordinate = false;
                    for (CoordinateColumns c : columns) {
                        EastNorth en = ens.get(c);
                        if (i == c.xCol) {
                            coordinate = true;
                            ens.put(c, new EastNorth(parseDouble(fields[i]), en.north()));
                            if (handler != null) {
                                handler.setXCol(i);
                            }
                        } else if (i == c.yCol) {
                            coordinate = true;
                            ens.put(c, new EastNorth(en.east(), parseDouble(fields[i])));
                            if (handler != null) {
                                handler.setYCol(i);
                            }
                        }                            
                    }
                    if (!coordinate) {
                        if (!fields[i].isEmpty()) {
                            nodes.values().iterator().next().put(header[i], fields[i]);
                        }
                    }
                } catch (ParseException e) {
                    Main.warn("Parsing error on line "+lineNumber+": "+e.getMessage());
                }
            }
            Node firstNode = null;
            for (CoordinateColumns c : columns) {
                Node n = nodes.get(c);
                EastNorth en = ens.get(c);
                if (en.isValid()) {
                    n.setCoor(c.proj != null && !handlerOK ? c.proj.eastNorth2latlon(en) : handler.getCoor(en, fields));
                } else {
                    Main.warn("Skipping line "+lineNumber+" because no valid coordinates have been found at columns "+c);
                }
                if (n.getCoor() != null) {
                    if (firstNode == null) {
                        firstNode = n;
                    }
                    if (n == firstNode || n.getCoor().greatCircleDistance(firstNode.getCoor()) > Main.pref.getDouble(OdConstants.PREF_TOLERANCE, OdConstants.DEFAULT_TOLERANCE)) {
                        ds.addPrimitive(n);
                    } else {
                        nodes.remove(c);
                    }
                }
            }
            if (handler != null && !Main.pref.getBoolean(OdConstants.PREF_RAWDATA)) {
                handler.nodesAdded(ds, nodes, header, lineNumber);
            }
        }
        
        return ds;
    }
    
    public final DataSet parse(InputStream in, ProgressMonitor progressMonitor) throws IOException {
        
        initResources(in, progressMonitor);
        
        String[] header = null;
        int length = 0;
        int n = 0;
        
        while (header == null || length == 0) {
            n++;
            header = readLine(progressMonitor);
            length = 0;
            if (header == null && n > getLineNumber()) {
                return null;
            } else if (header != null && (getLineNumber() == -1 || getLineNumber() == n)) {
                for (String field : header) {
                    length += field.length();
                }
            }
        }
        
        return doParse(header, progressMonitor);
    }
}
