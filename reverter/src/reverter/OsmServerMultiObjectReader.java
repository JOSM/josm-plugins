// License: GPL. For details, see LICENSE file.
package reverter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;

public class OsmServerMultiObjectReader extends OsmServerReader {
    private final MultiOsmReader rdr = new MultiOsmReader();

    public void readObject(PrimitiveId id, int version, ProgressMonitor progressMonitor) throws OsmTransferException {
        readObject(id.getUniqueId(), version, id.getType(), progressMonitor);
    }

    public void readObject(long id, int version, OsmPrimitiveType type, ProgressMonitor progressMonitor) throws OsmTransferException {
        StringBuilder sb = new StringBuilder();
        sb.append(type.getAPIName());
        sb.append("/");
        sb.append(id);
        sb.append("/");
        sb.append(version);
        progressMonitor.beginTask("", 1);
        try (InputStream in = getInputStream(sb.toString(), progressMonitor.createSubTaskMonitor(1, true))) {
            rdr.addData(in);
        } catch (IOException | IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Generate query strings
     * @param type The type of the object to get
     * @param list The map of ids to versions
     * @return The queries to make
     */
    private static List<String> makeQueryStrings(OsmPrimitiveType type, Map<Long, Integer> list) {
        // This is a "worst-case" calculation. Keep it fast (and err higher rather than lower), not accurate.
        final int expectedSize = (int) (list.entrySet().stream().mapToLong(entry ->
                // Keep in mind that 0-3 is 0, 3-32 is 1, 32-316 is 2, and so on when rounding log10.
                // First the key.
                Math.round(Math.log10(entry.getKey())) + 1 +
                // Then the value
                Math.round(Math.log10(entry.getValue())) + 1 +
                // And finally the "static" size (',' + 'v')
                2
                ).sum() / MAX_QUERY_LENGTH);
        List<String> result = new ArrayList<>(expectedSize + 1);
        StringBuilder sb = new StringBuilder();
        int cnt = 0;
        for (Map.Entry<Long, Integer> entry : list.entrySet()) {
            if (cnt == 0) {
                sb.append(type.getAPIName());
                sb.append("s?");
                sb.append(type.getAPIName());
                sb.append("s=");
            } else {
                sb.append(",");
            }
            sb.append(entry.getKey());
            sb.append("v");
            sb.append(entry.getValue());
            cnt++;
            if (cnt >= MAX_QUERY_IDS || sb.length() > MAX_QUERY_LENGTH) {
                result.add(sb.toString());
                sb.setLength(0);
                cnt = 0;
            }
        }
        if (cnt > 0) {
            result.add(sb.toString());
        }
        return result;
    }

    /**
     * The maximum ids we want to query. API docs indicate 725 is "safe" for non-versioned objects with 10-digit ids.
     * Since we use {@link #MAX_QUERY_LENGTH} to avoid issues where we go over the permitted length, we can have a
     * bigger number here.
     * @see <a href="https://wiki.openstreetmap.org/wiki/API_v0.6#Multi_fetch:_GET_/api/0.6/[nodes|ways|relations]?#parameters">
     *     API_v0.6#Multi_fetch
     * </a>
     */
    protected static final int MAX_QUERY_IDS = 2000;
    /**
     * The maximum query length. Docs indicate 8213 characters in the URI is safe. The maximum base length before
     * query parameters is for relations at 59 characters for 8154 characters in the query parameters. We round down to
     * 8000 characters.
     * @see <a href="https://wiki.openstreetmap.org/wiki/API_v0.6#Multi_fetch:_GET_/api/0.6/[nodes|ways|relations]?#parameters">
     *     API_v0.6#Multi_fetch
     * </a>
     */
    protected static final int MAX_QUERY_LENGTH = 8000;

    /**
     * Parse many objects.
     * @param type The object type (<i>must</i> be common between all objects)
     * @param list The map of object id to object version. Successfully retrieved objects are removed.
     * @param progressMonitor The progress monitor to update
     * @throws OsmTransferException If there is an issue getting the data
     */
    public void readMultiObjects(OsmPrimitiveType type, Map<Long, Integer> list, ProgressMonitor progressMonitor) throws OsmTransferException {
        for (String query : makeQueryStrings(type, list)) {
            if (progressMonitor.isCanceled()) {
                return;
            }
            rdr.callback = id -> {
                if (id.getType() == type && list.remove(id.getUniqueId()) != null) {
                    progressMonitor.worked(1);
                }
            };
            try (InputStream in = getInputStream(query, NullProgressMonitor.INSTANCE)) {
                rdr.addData(in);
            } catch (IOException | IllegalDataException e) {
                Logging.warn(e);
                throw new OsmTransferException(e);
            } catch (OsmApiException e) {
                Logging.warn(e);
                // allow to continue further bulk requests
                if (e.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN
                        && e.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
                    throw e;
                }
            } finally {
                rdr.callback = null;
            }
        }
    }

    /**
     * Parse many objects. If redacted elements are requested the method tries to retrieve the next older version.
     * @param type The object type (<i>must</i> be common between all objects)
     * @param list The map of object id to object version. Successfully retrieved objects are removed.
     * @param progressMonitor The progress monitor to update
     * @throws OsmTransferException If there is an issue getting the data
     */
    public void readMultiObjectsOrNextOlder(OsmPrimitiveType type, Map<Long, Integer> list,
            ProgressMonitor progressMonitor) throws OsmTransferException {
        readMultiObjects(type, list, progressMonitor);
        // If multi-read failed, retry with regular read
        for (Map.Entry<Long, Integer> entry : list.entrySet()) {
            if (progressMonitor.isCanceled()) return;
            readObjectVersion(type, entry.getKey(), entry.getValue(), progressMonitor);
        }
    }

    private void readObjectVersion(OsmPrimitiveType type, long id, int version, ProgressMonitor progressMonitor)
            throws OsmTransferException {
        boolean readOK = false;
        while (!readOK && version >= 1) {
            try {
                readObject(id, version, type, progressMonitor.createSubTaskMonitor(1, true));
                readOK = true;
            } catch (OsmApiException e) {
                if (e.getResponseCode() != HttpURLConnection.HTTP_FORBIDDEN) {
                    throw e;
                }
                String message = "Version " + version + " of " + id + " is unauthorized";
                Logging.info(version <= 1 ? message : message + ", requesting previous one");
                version--;
            }
        }
        if (!readOK) {
            Logging.warn("Cannot retrieve any previous version of {1} {2}", type, id);
        }
    }


    /**
     * Method to parse downloaded objects
     * @return the data requested
     * @throws OsmTransferException in case of error
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        progressMonitor.beginTask("", 1);
        progressMonitor.subTask(tr("Preparing history data..."));
        try {
            rdr.processData();
            return rdr.getDataSet();
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
            activeConnection = null;
        }
    }
}
