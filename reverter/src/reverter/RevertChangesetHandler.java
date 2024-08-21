// License: GPL. For details, see LICENSE file.
package reverter;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.remotecontrol.PermissionPrefWithDefault;
import org.openstreetmap.josm.io.remotecontrol.handler.RequestHandler;
import org.openstreetmap.josm.tools.Logging;

import java.util.List;
import java.util.ArrayList;

/**
 * The handler for {@link org.openstreetmap.josm.io.remotecontrol.RemoteControl} revert commands
 */
public class RevertChangesetHandler extends RequestHandler {
    public static final String COMMAND = "revert_changeset";
    private static final String PERMISSION_KEY = "remotecontrol.permission.revert_changeset";
    private static final boolean PERMISSION_DEFAULT = true;

    private final List<Integer> changesetIds = new ArrayList<>();

    @Override
    protected void handleRequest() throws RequestHandlerErrorException {
        try {
            MainApplication.worker.submit(new RevertChangesetTask(changesetIds, ChangesetReverter.RevertType.FULL, true, false));
        } catch (Exception ex) {
            Logging.debug("RemoteControl: Error parsing revert_changeset remote control request:");
            Logging.debug(ex);
            throw new RequestHandlerErrorException(ex);
        }
    }

    @Override
    public String[] getMandatoryParams() {
        return new String[] {"id"};
    }

    @Override
    public PermissionPrefWithDefault getPermissionPref() {
        return new PermissionPrefWithDefault(PERMISSION_KEY, PERMISSION_DEFAULT, tr("Revert changeset(s)"));
    }

    @Override
    public String getPermissionMessage() {
        return tr("Remote Control has been asked to revert a changeset.");
    }

    @Override
    protected void validateRequest() throws RequestHandlerBadRequestException {
        if (args.get("id") != null) {
            try {
                for (String id : args.get("id").split(",", -1)) {
                    changesetIds.add(Integer.parseInt(id));
                }
            } catch (NumberFormatException e) {
                throw new RequestHandlerBadRequestException("NumberFormatException: " + e.getMessage());
            }
        } else {
            throw new RequestHandlerBadRequestException("The required id argument must be specified");
        }
    }
}
