// License: GPL. For details, see LICENSE file.
package reverter;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Commands to revert a changeset.
 *
 */
public class RevertChangesetCommand extends SequenceCommand {

    /**
     * Create the command by specifying the list of commands to execute.
     * @param name The description text
     * @param sequenz The sequence that was already executed.
     */
    public RevertChangesetCommand(String name, Collection<Command> sequenz) {
        super(name, sequenz);
        ReverterPlugin.reverterUsed.set(true);
        setSequenceComplete(true);
    }

    @Override
    public String getDescriptionText() {
        return getName();
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("revert-changeset");
    }

    @Override
    public void undoCommand() {
        getAffectedDataSet().update(super::undoCommand);
    }

    @Override
    public boolean executeCommand() {
        return getAffectedDataSet().update(super::executeCommand);
    }

}
