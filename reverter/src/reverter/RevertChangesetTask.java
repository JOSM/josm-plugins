package reverter;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.conflict.ConflictAddCommand;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.io.OsmTransferException;

import reverter.ChangesetReverter.RevertType;

public class RevertChangesetTask extends PleaseWaitRunnable {
    private final int changesetId;
    private final RevertType revertType;
    private final boolean newLayer;

    private ChangesetReverter rev;
    private boolean downloadConfirmed;

    public RevertChangesetTask(int changesetId, RevertType revertType) {
        this(changesetId, revertType, false);
    }
    
    public RevertChangesetTask(int changesetId, RevertType revertType, boolean autoConfirmDownload) {
        this(changesetId, revertType, autoConfirmDownload, false);
    }
    
    public RevertChangesetTask(int changesetId, RevertType revertType, boolean autoConfirmDownload, boolean newLayer) {
        super(tr("Reverting..."));
        this.changesetId = changesetId;
        this.revertType = revertType;
        this.downloadConfirmed = autoConfirmDownload;
        this.newLayer = newLayer;
    }

    private boolean checkAndDownloadMissing() throws OsmTransferException {
        if (!rev.hasMissingObjects()) return true;
        if (!downloadConfirmed) {
            downloadConfirmed = JOptionPane.showConfirmDialog(Main.parent,
                    tr("This changeset has objects that are not present in current dataset.\n" +
                            "It is needed to download them before reverting. Do you want to continue?"),
                    tr("Confirm"), JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
            if (!downloadConfirmed) return false;
        }
        final PleaseWaitProgressMonitor monitor =
            new PleaseWaitProgressMonitor(tr("Fetching missing primitives"));
        try {
            rev.downloadMissingPrimitives(monitor);
        } finally {
            monitor.close();
        }
        return !monitor.isCanceled();
    }

    @Override
    protected void realRun() throws OsmTransferException {
        progressMonitor.indeterminateSubTask(tr("Downloading changeset"));
        try {
            rev = new ChangesetReverter(changesetId, revertType, newLayer,
                    progressMonitor.createSubTaskMonitor(0, true));
        } catch (final RevertRedactedChangesetException e) {
            GuiHelper.runInEDT(new Runnable() {
                @Override
                public void run() {
                    new Notification(
                            e.getMessage()+"<br>"+
                            tr("See {0}", "<a href=\"http://www.openstreetmap.org/redactions\">http://www.openstreetmap.org/redactions</a>"))
                    .setIcon(JOptionPane.ERROR_MESSAGE)
                    .setDuration(Notification.TIME_LONG)
                    .show();
                }
            });
            progressMonitor.cancel();
        }
        if (progressMonitor.isCanceled()) return;

        // Check missing objects
        rev.checkMissingCreated();
        rev.checkMissingUpdated();
        if (rev.hasMissingObjects()) {
            // If missing created or updated objects, ask user
            rev.checkMissingDeleted();
            if (!checkAndDownloadMissing()) return;
        } else {
            // Don't ask user to download primitives going to be undeleted
            rev.checkMissingDeleted();
            rev.downloadMissingPrimitives(progressMonitor.createSubTaskMonitor(0, false));
        }

        if (progressMonitor.isCanceled()) return;
        rev.downloadObjectsHistory(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
        if (progressMonitor.isCanceled()) return;
        if (!checkAndDownloadMissing()) return;
        rev.fixNodesWithoutCoordinates(progressMonitor);
        List<Command> cmds = rev.getCommands();
        final Command cmd = new RevertChangesetCommand(tr(revertType == RevertType.FULL ? "Revert changeset #{0}" :
                "Partially revert changeset #{0}",changesetId),cmds);
        int n = 0;
        for (Command c : cmds) {
            if (c instanceof ConflictAddCommand) {
                n++;
            }
        }
        final int newConflicts = n;
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                Main.main.undoRedo.add(cmd);
                if (newConflicts > 0) {
                    Main.map.conflictDialog.warnNumNewConflicts(newConflicts);
                }
            }
        });
    }

    @Override
    protected void cancel() {
    }

    @Override
    protected void finish() {
    }
}
