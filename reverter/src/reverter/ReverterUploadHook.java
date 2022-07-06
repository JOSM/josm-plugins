// License: GPL. For details, see LICENSE file.
package reverter;

import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.PluginInformation;

/**
 * Upload hook to add tag to new changeset.
 *
 */
public class ReverterUploadHook implements UploadHook {
    final String pluginString;

    /**
     * Create new {@link ReverterUploadHook}
     * @param info plugin information
     */
    public ReverterUploadHook(PluginInformation info) {
        pluginString = "reverter_plugin/" + info.version;
    }

    @Override
    public void modifyChangesetTags(Map<String, String> tags) {
        if (ReverterPlugin.reverterUsed.get()) {
            for (Command cmd : UndoRedoHandler.getInstance().getUndoCommands()) {
                if (Objects.equals(MainApplication.getLayerManager().getEditDataSet(), cmd.getAffectedDataSet()) && isReverterCmd(cmd)) {
                    tags.merge("created_by", pluginString, (oldValue, value) -> String.join(";", value, oldValue));
                    break;
                }
            }
        }
    }

    private static boolean isReverterCmd(Command cmd) {
        if (cmd instanceof RevertChangesetCommand)
            return true;
        if (cmd instanceof SequenceCommand) {
            return ((SequenceCommand) cmd).getLastCommand() instanceof RevertChangesetCommand;
        }
        return false;
    }
}
