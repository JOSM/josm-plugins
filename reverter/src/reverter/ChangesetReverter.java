// License: GPL. For details, see LICENSE file.
package reverter;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.conflict.ConflictAddCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetDataSetEntry;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveComparator;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmServerChangesetReader;
import org.openstreetmap.josm.io.OsmTransferException;

/**
 * Fetches and stores data for reverting of specific changeset.
 * @author Upliner
 *
 */
public class ChangesetReverter {

    /** The type of reversion */
    public enum RevertType {
        FULL,
        SELECTION,
        SELECTION_WITH_UNDELETE
    }

    public static final Collection<Long> MODERATOR_REDACTION_ACCOUNTS = Collections.unmodifiableCollection(Arrays.asList(
            722_137L, // OSMF Redaction Account
            760_215L  // pnorman redaction revert
            ));

    /** The id of the changeset */
    public final int changesetId;
    /** The changeset to be reverted */
    public final Changeset changeset;
    /** The reversion type */
    public final RevertType revertType;

    /** data layer associated with reverter */
    private final OsmDataLayer layer;
    /** DataSet associated with reverter */
    private final DataSet ds;
    /** Current changeset data */
    private final ChangesetDataSet cds;
    /** original DataSet, used if a new layer is requested */
    private final DataSet ods;
    /** Dataset that contains new objects downloaded by reverter */
    private DataSet nds;

    private final HashSet<PrimitiveId> missing = new HashSet<>();

    private final HashSet<HistoryOsmPrimitive> created = new HashSet<>();
    private final HashSet<HistoryOsmPrimitive> updated = new HashSet<>();
    private final HashSet<HistoryOsmPrimitive> deleted = new HashSet<>();
    private final HashMap<PrimitiveId, Integer> earliestVersions = new HashMap<>();

    //// Handling missing objects
    ////////////////////////////////////////

    /**
     * Add a primitive that is not in {@link #ds} to {@link #missing}
     * @param id The primitive to add if missing
     */
    private void addIfMissing(PrimitiveId id) {
        OsmPrimitive p = ds.getPrimitiveById(id);
        if (p == null || p.isIncomplete()) {
            missing.add(id);
        }
    }

    /**
     * Add missing primitives to {@link #missing}
     * @param primitives The primitives to iterate through
     */
    private void addMissingHistoryIds(Iterable<HistoryOsmPrimitive> primitives) {
        for (HistoryOsmPrimitive p : primitives) {
            addIfMissing(p.getPrimitiveId());
        }
    }

    /**
     * Add missing necessary primitives to {@link #missing}
     * @param p The primitive to look at
     */
    private void addMissingId(OsmPrimitive p) {
        addIfMissing(p);
        if (p.getType() == OsmPrimitiveType.WAY) {
            for (Node nd : ((Way) p).getNodes()) {
                addIfMissing(nd);
            }
        }
    }

    /**
     * Checks if {@link ChangesetDataSetEntry} conforms to current RevertType
     * @param entry entry to be checked
     * @return <code>true</code> if {@link ChangesetDataSetEntry} conforms to current RevertType
     */
    private boolean checkOsmChangeEntry(ChangesetDataSetEntry entry) {
        if (revertType == RevertType.FULL) return true;
        if (revertType == RevertType.SELECTION_WITH_UNDELETE &&
                entry.getModificationType() == ChangesetModificationType.DELETED) {
            return true;
        }
        if (ods == null)
            return false; // should not happen
        OsmPrimitive p = ods.getPrimitiveById(entry.getPrimitive().getPrimitiveId());
        if (p == null) return false;
        return p.isSelected();
    }

    /**
     * creates a reverter for specific changeset and fetches initial data
     * @param changesetId changeset id
     * @param revertType type of revert
     * @param newLayer set to true if a new layer should be created
     * @param ods original dataset to search for selected primitives, can be null
     * @param monitor progress monitor
     * @throws OsmTransferException if data transfer errors occur
     * @throws RevertRedactedChangesetException if a redacted changeset is requested
     */
    public ChangesetReverter(int changesetId, RevertType revertType, boolean newLayer, DataSet ods, ProgressMonitor monitor)
            throws OsmTransferException, RevertRedactedChangesetException {
        this.changesetId = changesetId;
        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        newLayer = newLayer || editLayer == null;
        if (newLayer) {
            this.ds = new DataSet();
            this.layer = new OsmDataLayer(this.ds, tr("Reverted changeset") + tr(" [id: {0}]", String.valueOf(changesetId)), null);
        } else {
            this.layer = editLayer;
            this.ds = editLayer.data;
        }
        this.ods = ods;
        this.revertType = revertType;
        if ((revertType == RevertType.SELECTION || revertType == RevertType.SELECTION_WITH_UNDELETE)
                && (ods == null || ods.getAllSelected().isEmpty())) {
            throw new IllegalArgumentException("No selected elements with revert type " + revertType);
        }

        OsmServerChangesetReader csr = new OsmServerChangesetReader(true);
        monitor.beginTask("", 2);
        changeset = csr.readChangeset(changesetId, false, monitor.createSubTaskMonitor(1, false));
        if (MODERATOR_REDACTION_ACCOUNTS.contains(changeset.getUser().getId())) {
            throw new RevertRedactedChangesetException(tr("It is not allowed to revert changeset from {0}", changeset.getUser().getName()));
        }
        try {
            cds = csr.downloadChangeset(changesetId, monitor.createSubTaskMonitor(1, false));
        } finally {
            monitor.finishTask();
            if (newLayer) {
                GuiHelper.runInEDT(() -> MainApplication.getLayerManager().addLayer(layer));
            }
        }

        buildCreatedUpdatedModifiedObjectLists();
    }

    /**
     * Build our own lists of created/updated/modified objects for better performance
     */
    private void buildCreatedUpdatedModifiedObjectLists() {
        for (PrimitiveId id : cds.getIds()) {
            ChangesetDataSetEntry first = cds.getFirstEntry(id);
            earliestVersions.put(id, (int) first.getPrimitive().getVersion());
            ChangesetDataSetEntry entry = first.getModificationType() == ChangesetModificationType.CREATED ? first
                    : cds.getLastEntry(id);
            if (checkOsmChangeEntry(entry)) {
                if (entry.getModificationType() == ChangesetModificationType.CREATED) {
                    created.add(entry.getPrimitive());
                } else if (entry.getModificationType() == ChangesetModificationType.UPDATED) {
                    updated.add(entry.getPrimitive());
                } else if (entry.getModificationType() == ChangesetModificationType.DELETED) {
                    deleted.add(entry.getPrimitive());
                }
            }
        }
    }

    /**
     * Add missing primitives that have been created
     */
    public void checkMissingCreated() {
        addMissingHistoryIds(created);
    }

    /**
     * Add missing primitives that have been updated
     */
    public void checkMissingUpdated() {
        addMissingHistoryIds(updated);
    }

    /**
     * Add missing primitives that have been deleted
     */
    public void checkMissingDeleted() {
        addMissingHistoryIds(deleted);
    }

    /**
     * fetch objects that were updated or deleted by changeset
     * @param progressMonitor progress monitor
     * @throws OsmTransferException if data transfer errors occur
     */
    public void downloadObjectsHistory(ProgressMonitor progressMonitor) throws OsmTransferException {
        final OsmServerMultiObjectReader rdr = new OsmServerMultiObjectReader();

        int num = updated.size() + deleted.size();
        progressMonitor.beginTask(
                addChangesetIdPrefix(
                        trn("Downloading history for {0} object", "Downloading history for {0} objects", num, num)), num + 1);
        try {
            HashMap<Long, Integer> nodeList = new HashMap<>();
            HashMap<Long, Integer> wayList = new HashMap<>();
            HashMap<Long, Integer> relationList = new HashMap<>();

            for (HashSet<HistoryOsmPrimitive> collection : Arrays.asList(updated, deleted)) {
                for (HistoryOsmPrimitive entry : collection) {
                    PrimitiveId id = entry.getPrimitiveId();
                    Integer earliestVersion = earliestVersions.get(id);
                    if (earliestVersion == null || earliestVersion <= 1)
                        throw new OsmTransferException(tr("Unexpected data in changeset #{1}", String.valueOf(changesetId)));
                    switch (id.getType()) {
                    case NODE: nodeList.put(id.getUniqueId(), earliestVersion - 1); break;
                    case WAY: wayList.put(id.getUniqueId(), earliestVersion - 1); break;
                    case RELATION: relationList.put(id.getUniqueId(), earliestVersion - 1); break;
                    default: throw new AssertionError();
                    }
                }
            }
            rdr.readMultiObjectsOrNextOlder(OsmPrimitiveType.NODE, nodeList, progressMonitor);
            rdr.readMultiObjectsOrNextOlder(OsmPrimitiveType.WAY, wayList, progressMonitor);
            rdr.readMultiObjectsOrNextOlder(OsmPrimitiveType.RELATION, relationList, progressMonitor);
            if (progressMonitor.isCanceled()) return;
            nds = rdr.parseOsm(progressMonitor.createSubTaskMonitor(1, true));
            ds.update(this::addPartialPrimitives);
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Add partial primitives if they are incomplete in {@link #nds}
     */
    private void addPartialPrimitives() {
        for (OsmPrimitive p : nds.allPrimitives()) {
            if (!p.isIncomplete()) {
                addMissingId(p);
            } else {
                if (ds.getPrimitiveById(p.getPrimitiveId()) == null) {
                    switch (p.getType()) {
                        case NODE: ds.addPrimitive(new Node(p.getUniqueId())); break;
                        case CLOSEDWAY:
                        case WAY: ds.addPrimitive(new Way(p.getUniqueId())); break;
                        case MULTIPOLYGON:
                        case RELATION: ds.addPrimitive(new Relation(p.getUniqueId())); break;
                        default: throw new AssertionError();
                    }
                }
            }
        }
    }

    /**
     * Download any missing primitives we have
     * @param monitor The monitor to update with our progress
     * @throws OsmTransferException If there is an issue transferring data from OSM
     */
    public void downloadMissingPrimitives(ProgressMonitor monitor) throws OsmTransferException {
        if (!hasMissingObjects()) return;
        MultiFetchServerObjectReader rdr = MultiFetchServerObjectReader.create(false);
        for (PrimitiveId id : missing) {
            switch (id.getType()) {
            case NODE:
                rdr.append(new Node(id.getUniqueId()));
                break;
            case CLOSEDWAY:
            case WAY:
                rdr.append(new Way(id.getUniqueId()));
                break;
            case MULTIPOLYGON:
            case RELATION:
                rdr.append(new Relation(id.getUniqueId()));
                break;
            default: throw new AssertionError();
            }
        }
        DataSet source = rdr.parseOsm(monitor);
        for (OsmPrimitive p : source.allPrimitives()) {
            if (!p.isVisible() && !p.isDeleted()) {
                p.setDeleted(true);
                p.setModified(false);
            }
        }
        layer.mergeFrom(source);
        missing.clear();
    }

    private static Conflict<? extends OsmPrimitive> createConflict(OsmPrimitive p, boolean isMyDeleted) {
        switch (p.getType()) {
        case NODE:
            return new Conflict<>((Node) p, new Node((Node) p), isMyDeleted);
        case CLOSEDWAY:
        case WAY:
            return new Conflict<>((Way) p, new Way((Way) p), isMyDeleted);
        case MULTIPOLYGON:
        case RELATION:
            return new Conflict<>((Relation) p, new Relation((Relation) p), isMyDeleted);
        default: throw new AssertionError();
        }
    }

    private boolean hasEqualSemanticAttributes(OsmPrimitive current, HistoryOsmPrimitive history) {
        if (!current.getKeys().equals(history.getTags())) return false;
        switch (current.getType()) {
        case NODE:
            return hasEqualSemanticAttributesNode(nds, (HistoryNode) history, (Node) current);
        case CLOSEDWAY:
        case WAY:
            return hasEqualSemanticAttributesWay((HistoryWay) history, (Way) current);
        case MULTIPOLYGON:
        case RELATION:
            return hasEqualSemanticAttributesRelation((HistoryRelation) history, (Relation) current);
        default: throw new AssertionError();
        }
    }

    /**
     * Check if two nodes with the same tags are otherwise semantically similar
     * @param nds The dataset with downloaded data from OSM (see {@link #nds})
     * @param history The node history
     * @param current The current node
     * @return {@code true} if they are semantically similar
     */
    private static boolean hasEqualSemanticAttributesNode(DataSet nds, HistoryNode history, Node current) {
        LatLon currentCoor = current.getCoor();
        LatLon historyCoor = history.getCoords();
        if (Objects.equals(currentCoor, historyCoor))
            return true;
        // Handle case where a deleted note has been restored to avoid false conflicts (fix #josm8660)
        if (currentCoor != null && historyCoor == null) {
            LatLon previousCoor = ((Node) nds.getPrimitiveById(history.getPrimitiveId())).getCoor();
            return previousCoor != null && previousCoor.equals(currentCoor);
        }
        return false;
    }

    /**
     * Check if two ways with the same tags are otherwise semantically similar
     * @param history The way history
     * @param current The current way
     * @return {@code true} if they are semantically similar
     */
    private static boolean hasEqualSemanticAttributesWay(HistoryWay history, Way current) {
        List<Node> currentNodes = current.getNodes();
        List<Long> historyNodes = history.getNodes();
        if (currentNodes.size() != historyNodes.size()) return false;
        for (int i = 0; i < currentNodes.size(); i++) {
            if (currentNodes.get(i).getId() != historyNodes.get(i)) return false;
        }
        return true;
    }

    /**
     * Check if two relations with the same tags are otherwise semantically similar
     * @param history The relation history
     * @param current The current relation
     * @return {@code true} if they are semantically similar
     */
    private static boolean hasEqualSemanticAttributesRelation(HistoryRelation history, Relation current) {
        List<org.openstreetmap.josm.data.osm.RelationMember> currentMembers = current.getMembers();
        List<RelationMemberData> historyMembers = history.getMembers();
        if (currentMembers.size() != historyMembers.size()) return false;
        for (int i = 0; i < currentMembers.size(); i++) {
            org.openstreetmap.josm.data.osm.RelationMember currentMember =
                    currentMembers.get(i);
            RelationMemberData historyMember = historyMembers.get(i);
            if (!currentMember.getRole().equals(historyMember.getRole())) return false;
            if (!currentMember.getMember().getPrimitiveId().equals(new SimplePrimitiveId(
                    historyMember.getMemberId(), historyMember.getMemberType()))) return false;
        }
        return true;
    }

    /**
     * Builds a list of commands that will revert the changeset
     * @return list of commands
     *
     */
    public List<Command> getCommands() {
        List<Command> cmds = new ArrayList<>();
        if (this.nds == null) return cmds;

        //////////////////////////////////////////////////////////////////////////
        // Create commands to restore/update all affected objects
        DataSetCommandMerger merger = new DataSetCommandMerger(nds, ds);
        cmds.addAll(merger.getCommandList());

        //////////////////////////////////////////////////////////////////////////
        // Create a set of objects to be deleted

        HashSet<OsmPrimitive> toDelete = new HashSet<>();
        // Mark objects that has visible=false to be deleted
        for (OsmPrimitive p : nds.allPrimitives()) {
            if (!p.isVisible()) {
                OsmPrimitive dp = ds.getPrimitiveById(p);
                if (dp != null) toDelete.add(dp);
            }
        }
        // Mark all created objects to be deleted
        for (HistoryOsmPrimitive id : created) {
            OsmPrimitive p = ds.getPrimitiveById(id.getPrimitiveId());
            if (p != null) toDelete.add(p);
        }

        //////////////////////////////////////////////////////////////////////////
        // Check reversion against current dataset and create necessary conflicts

        HashSet<OsmPrimitive> conflicted = new HashSet<>();

        for (Conflict<? extends OsmPrimitive> conflict : merger.getConflicts()) {
            cmds.add(new ConflictAddCommand(layer.data, conflict));
        }

        checkObjectVersions(cmds, conflicted, toDelete);
        checkForDeletedReferrers(cmds, conflicted, toDelete);
        return cmds;
    }

    /**
     * Check objects versions
     * @param cmds The command list to add to
     * @param conflicted The primitives with conflicts
     * @param toDelete The primitives that will be deleted
     */
    private void checkObjectVersions(List<Command> cmds, Set<OsmPrimitive> conflicted, Set<OsmPrimitive> toDelete) {
        for (Iterator<ChangesetDataSetEntry> it = cds.iterator(); it.hasNext();) {
            ChangesetDataSetEntry entry = it.next();
            if (checkOsmChangeEntry(entry) && checkModificationType(cds, entry, revertType)) {
                HistoryOsmPrimitive hp = entry.getPrimitive();
                OsmPrimitive dp = ds.getPrimitiveById(hp.getPrimitiveId());
                if (dp == null || dp.isIncomplete()) {
                    throw new IllegalStateException(addChangesetIdPrefix(tr("Missing merge target for {0}", hp.getPrimitiveId())));
                }

                if (checkObjectVersionsNotSemanticallySame(toDelete, hp, dp)) {
                    cmds.add(new ConflictAddCommand(layer.data, createConflict(dp,
                            entry.getModificationType() == ChangesetModificationType.CREATED)));
                    conflicted.add(dp);
                }
            }
        }
    }

    /**
     * Check that the modification type is something that we are looking at
     * @param cds {@link #cds} (current changeset data)
     * @param entry The entry in the changeset data
     * @param revertType The type of revert we are doing
     * @return {@code true} if the entry is something we are interested in for this revert
     */
    private static boolean checkModificationType(ChangesetDataSet cds, ChangesetDataSetEntry entry, RevertType revertType) {
        if (entry.getModificationType() == ChangesetModificationType.DELETED
                && revertType == RevertType.SELECTION_WITH_UNDELETE) {
            // see #22520: missing merge target when object is first created and then
            // deleted in the same changeset
            ChangesetDataSetEntry first = cds.getFirstEntry(entry.getPrimitive().getPrimitiveId());
            return first.getModificationType() != ChangesetModificationType.CREATED;
        }
        return true;
    }

    /**
     * Check that versions are not the same prior to creating a conflict
     * @param toDelete The deleted objects
     * @param hp The history primitive
     * @param dp The current primitive
     * @return {@code true} if the objects are not semantically the same
     */
    private boolean checkObjectVersionsNotSemanticallySame(Set<OsmPrimitive> toDelete, HistoryOsmPrimitive hp, OsmPrimitive dp) {
        return hp.getVersion() != dp.getVersion()
                && (hp.isVisible() || dp.isVisible()) &&
                /* Don't create conflict if changeset object and dataset object
                 * has same semantic attributes (but different versions) */
                !hasEqualSemanticAttributes(dp, hp)
                /* Don't create conflict if the object has to be deleted but has already been deleted */
                && !(toDelete.contains(dp) && dp.isDeleted());
    }

    /**
     * Check referrers for deleted objects: if object is referred by another object that
     * isn't going to be deleted or modified, create a conflict.
     * @param cmds The command list to add to
     * @param conflicted The primitives with conflicts
     * @param toDelete The primitives that will be deleted
     */
    private void checkForDeletedReferrers(List<Command> cmds, Set<OsmPrimitive> conflicted, Set<OsmPrimitive> toDelete) {
        List<OsmPrimitive> delSorted = toDelete.stream()
                .filter(p -> !p.isDeleted())
                .sorted(OsmPrimitiveComparator.orderingRelationsWaysNodes())
                .collect(Collectors.toList());
        boolean restartNeeded;
        do {
            restartNeeded = false;
            for (int i = 0; i < delSorted.size() && !restartNeeded; i++) {
                OsmPrimitive p = delSorted.get(i);
                restartNeeded = checkForDeletedReferrersPrimitive(cmds, conflicted, toDelete, delSorted, p, i);
            }
        } while (restartNeeded);
        // Create a Command to delete all marked objects
        delSorted.retainAll(toDelete);
        if (!delSorted.isEmpty()) {
            cmds.add(new DeleteCommand(delSorted));
        }
    }

    /**
     *
     * @param cmds The command list to add to
     * @param conflicted The primitives with conflicts
     * @param toDelete The primitives that will be deleted
     * @param delSorted The list of sorted deleted objects we need to check against
     * @param p The primitive to check
     * @param i The current index in {@code delSorted}
     * @return {@code true} if we need to restart processing
     */
    private boolean checkForDeletedReferrersPrimitive(List<Command> cmds, Set<OsmPrimitive> conflicted,
                                                      Set<OsmPrimitive> toDelete, List<OsmPrimitive> delSorted,
                                                      OsmPrimitive p, int i) {
        for (OsmPrimitive referrer : p.getReferrers()) {
            if (toDelete.contains(referrer) || // object is going to be deleted
                    nds.getPrimitiveById(referrer) != null)
                continue; /* object is going to be modified so it cannot refer to
                 * objects created in changeset to be reverted
                 */
            if (conflicted.add(p)) {
                cmds.add(new ConflictAddCommand(layer.data, createConflict(p, true)));
                if (p instanceof Relation) {
                    // handle possible special case with nested relations
                    for (int j = 0; j < i; j++) {
                        if (delSorted.get(j).getReferrers().contains(p)) {
                            // we have to create a conflict for a previously processed relation
                            return true;
                        }
                    }
                }
            }
            toDelete.remove(p);
            return false;
        }
        return false;
    }

    /**
     * Check if there are missing objects
     * @return {@code true} if we need to handle missing objects
     */
    public boolean hasMissingObjects() {
        return !missing.isEmpty();
    }

    /**
     * Get coordinates for nodes that have no coordinates
     * @param progressMonitor The monitor to update our progress with
     * @throws OsmTransferException If we have an issue getting data from the API
     */
    public void fixNodesWithoutCoordinates(ProgressMonitor progressMonitor) throws OsmTransferException {
        Collection<Node> nodes = new ArrayList<>(nds.getNodes());
        int num = nodes.size();
        progressMonitor.beginTask(addChangesetIdPrefix(
                trn("Checking coordinates of {0} node", "Checking coordinates of {0} nodes", num, num)),
                // downloads == num ticks, then we get the downloaded data (1 tick), then we process the nodes (num ticks)
                2 * num + 1);

        // Remove primitives where we already know the LatLon.
        nodes.removeIf(n -> {
            if (n.isDeleted() || n.isLatLonKnown())
                return true;
            PrimitiveId id = n.getPrimitiveId();
            OsmPrimitive p = ds.getPrimitiveById(id);
            return !(p instanceof Node) || ((Node) p).isLatLonKnown() || p.getVersion() <= 1;
        });

        progressMonitor.worked(num - nodes.size());

        // Do bulk version fetches first
        // The objects to get next
        final Map<Long, Integer> versionMap = nodes.stream()
                .collect(Collectors.toMap(AbstractPrimitive::getUniqueId,
                        id -> Math.max(1, Optional.ofNullable(ds.getPrimitiveById(id)).orElse(id).getVersion() - 1)));

        try {
            while (!versionMap.isEmpty()) {
                final OsmServerMultiObjectReader rds = new OsmServerMultiObjectReader();
                final ProgressMonitor subMonitor = progressMonitor.createSubTaskMonitor(num, false);
                subMonitor.beginTask(tr("Fetching multi-objects"), versionMap.size());
                rds.readMultiObjectsOrNextOlder(OsmPrimitiveType.NODE, versionMap, subMonitor);
                subMonitor.finishTask();
                final DataSet history = rds.parseOsm(progressMonitor.createSubTaskMonitor(0, false));
                ds.update(() -> updateNodes(progressMonitor, nodes, versionMap, history));
                versionMap.values().removeIf(i -> i <= 1);
                versionMap.replaceAll((key, value) -> value - 1);
                if (progressMonitor.isCanceled()) {
                    break;
                }
            }
        } finally {
            progressMonitor.finishTask();
        }
    }

    /**
     * Update nodes lacking a lat/lon
     * @param progressMonitor The monitor to update
     * @param nodes The nodes to update
     * @param versionMap The version map to update for the next round
     * @param history The history dataset
     */
    private void updateNodes(ProgressMonitor progressMonitor, Collection<Node> nodes, Map<Long, Integer> versionMap, DataSet history) {
        for (Node n : nodes) {
            if (!n.isDeleted() && !n.isLatLonKnown()) {
                final Node historyNode = (Node) history.getPrimitiveById(n);
                if (historyNode != null) {
                    if (historyNode.isLatLonKnown()
                            && changeset.getClosedAt().isAfter(historyNode.getInstant())) {
                        n.load(historyNode.save());
                        progressMonitor.worked(1);
                    } else {
                        versionMap.put(n.getId(), historyNode.getVersion());
                    }
                }
            }
            if (progressMonitor.isCanceled()) {
                break;
            }
        }
    }

    /**
     * Add prefix with properly formatted changeset id to a message.
     * @param msg the message string
     * @return prefixed message
     */
    String addChangesetIdPrefix(String msg) {
        return tr("Changeset {0}: {1}", Long.toString(changesetId), msg);
    }
}
