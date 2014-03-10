// License: GPL. For details, see LICENSE file.
package reverter.corehacks;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmChangesetParser;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.XmlParsingException;

/**
 * Reads the history of an {@see OsmPrimitive} from the OSM API server.
 *
 */
public class OsmServerChangesetReader extends OsmServerReader {

    /**
     * constructor
     *
     */
    public OsmServerChangesetReader(){
        setDoAuthenticate(false);
    }

    /**
     * don't use - not implemented!
     *
     */
    @Override
    public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
        return null;
    }

    /**
     * Queries a list
     * @param query  the query specification. Must not be null.
     * @param monitor a progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @return the list of changesets read from the server
     * @throws IllegalArgumentException thrown if query is null
     * @throws OsmTransferException thrown if something goes wrong w
     */
    public List<Changeset> queryChangesets(ChangesetQuery query, ProgressMonitor monitor) throws OsmTransferException {
        CheckParameterUtil.ensureParameterNotNull(query, "query");
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Reading changesets..."));
            StringBuffer sb = new StringBuffer();
            sb.append("changesets?").append(query.getQueryString());
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.indeterminateSubTask(tr("Downloading changesets ..."));
            List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
            return changesets;
        } catch(OsmTransferException e) {
            throw e;
        } catch(IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Reads the changeset with id <code>id</code> from the server
     *
     * @param id  the changeset id. id > 0 required.
     * @param monitor the progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @return the changeset read
     * @throws OsmTransferException thrown if something goes wrong
     * @throws IllegalArgumentException if id <= 0
     */
    public Changeset readChangeset(long id, ProgressMonitor monitor) throws OsmTransferException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0 expected. Got ''{1}''.", "id", id));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Reading changeset {0} ...",id));
            StringBuffer sb = new StringBuffer();
            sb.append("changeset/").append(id);
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.indeterminateSubTask(tr("Downloading changeset {0} ...", id));
            List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
            if (changesets == null || changesets.isEmpty())
                return null;
            return changesets.get(0);
        } catch(OsmTransferException e) {
            throw e;
        } catch(IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Reads the changeset with id <code>id</code> from the server
     *
     * @param ids  the list of ids. Ignored if null. Only load changesets for ids > 0.
     * @param monitor the progress monitor. Set to {@see NullProgressMonitor#INSTANCE} if null
     * @return the changeset read
     * @throws OsmTransferException thrown if something goes wrong
     * @throws IllegalArgumentException if id <= 0
     */
    public List<Changeset> readChangesets(Collection<Integer> ids, ProgressMonitor monitor) throws OsmTransferException {
        if (ids == null)
            return Collections.emptyList();
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(trn("Downloading {0} changeset ...", "Downloading {0} changesets ...",ids.size(),ids.size()));
            monitor.setTicksCount(ids.size());
            List<Changeset> ret = new ArrayList<Changeset>();
            int i=0;
            for (Iterator<Integer> it = ids.iterator(); it.hasNext(); ) {
                int id = it.next();
                if (id <= 0) {
                    continue;
                }
                i++;
                StringBuffer sb = new StringBuffer();
                sb.append("changeset/").append(id);
                InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
                if (in == null)
                    return null;
                monitor.indeterminateSubTask(tr("({0}/{1}) Downloading changeset {2} ...", i,ids.size(), id));
                List<Changeset> changesets = OsmChangesetParser.parse(in, monitor.createSubTaskMonitor(1, true));
                if (changesets == null || changesets.isEmpty()) {
                    continue;
                }
                ret.addAll(changesets);
                monitor.worked(1);
            }
            return ret;
        } catch(OsmTransferException e) {
            throw e;
        } catch(IllegalDataException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }

    /**
     * Downloads the content of a changeset
     *
     * @param id the changeset id. >0 required.
     * @param monitor the progress monitor. {@see NullProgressMonitor#INSTANCE} assumed if null.
     * @return the changeset content
     * @throws IllegalArgumentException thrown if id <= 0
     * @throws OsmTransferException thrown if something went wrong
     */
    public ChangesetDataSet downloadChangeset(int id, ProgressMonitor monitor) throws IllegalArgumentException, OsmTransferException {
        if (id <= 0)
            throw new IllegalArgumentException(MessageFormat.format("Expected value of type integer > 0 for parameter ''{0}'', got {1}", "id", id));
        if (monitor == null) {
            monitor = NullProgressMonitor.INSTANCE;
        }
        try {
            monitor.beginTask(tr("Downloading changeset content"));
            StringBuffer sb = new StringBuffer();
            sb.append("changeset/").append(id).append("/download");
            InputStream in = getInputStream(sb.toString(), monitor.createSubTaskMonitor(1, true));
            if (in == null)
                return null;
            monitor.setCustomText(tr("Downloading content for changeset {0} ...", id));
            OsmChangesetContentParser parser = new OsmChangesetContentParser(in);
            ChangesetDataSet ds = parser.parse(monitor.createSubTaskMonitor(1, true));
            return ds;
        } catch(UnsupportedEncodingException e) {
            throw new OsmTransferException(e);
        } catch(XmlParsingException e) {
            throw new OsmTransferException(e);
        } finally {
            monitor.finishTask();
        }
    }
}
