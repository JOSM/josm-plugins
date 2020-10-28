package net.simon04.comfort0;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.simon04.comfort0.level0l.parsergen.Level0LParser;
import net.simon04.comfort0.level0l.parsergen.ParseException;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

/**
 * Edit the selected objects in an external editor in the Level0L format.
 */
public class EditLevel0LAction extends JosmAction {

    private static final Charset CHARSET = Charset.defaultCharset();

    /**
     * Constructs a new {@link EditLevel0LAction}.
     */
    public EditLevel0LAction() {
        super(tr("Edit as Level0L"),
                "theta",
                tr("Edit the selected objects in an external editor in the Level0L format"),
                null, false, "EditLevel0LAction", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            editLevel0();
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection(true);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    private void editLevel0() throws IOException {
        final DataSet dataSet = MainApplication.getLayerManager().getEditDataSet();
        final Path path = writeLevel0(dataSet);
        final Process editor = new EditorLauncher(path).launch();
        new Notification(tr("Comfort0: Launching editor on file {0}", path)).show();
        new Thread(() -> awaitEditing(dataSet, path, editor), path.getFileName().toString()).start();
    }

    private Path writeLevel0(DataSet dataSet) throws IOException {
        final byte[] level0 = new OsmToLevel0L().visit(dataSet.getSelected())
                .toString()
                .getBytes(CHARSET);
        final Path path = Files.createTempFile("josm_level0_", ".txt");
        Files.write(path, level0);
        return path;
    }

    private void awaitEditing(DataSet dataSet, Path path, Process editor) {
        try {
            editor.waitFor();
            Logging.info("Comfort0: Editing of file {0} done", path);
            readLevel0(path, dataSet);
            Files.delete(path);
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    private void readLevel0(Path path, final DataSet dataSet) throws IOException, ParseException {
        try (BufferedReader reader = Files.newBufferedReader(path, CHARSET)) {
            final List<PrimitiveData> primitives = readLevel0(reader, dataSet);
            Logging.info("Comfort0: Reading file {0} yielded {1} primitives", path, primitives.size());
        }
    }

    static List<PrimitiveData> readLevel0(Reader reader, final DataSet dataSet) throws ParseException {
        final List<PrimitiveData> primitives = new Level0LParser(reader).primitives();
        final SequenceCommand command = buildChangeCommands(dataSet, primitives);
        GuiHelper.runInEDT(() -> {
            if (command == null || command.getChildren().isEmpty()) {
                return;
            }
            new Notification(trn(
                    "Comfort0: Changing {0} primitive",
                    "Comfort0: Changing {0} primitives", command.getChildren().size(), command.getChildren().size())).show();
            UndoRedoHandler.getInstance().add(command);
        });
        return primitives;
    }

    static SequenceCommand buildChangeCommands(DataSet dataSet, List<PrimitiveData> primitives) {
        final List<Command> commands = new ArrayList<>();
        for (PrimitiveData fromLevel0L : primitives) {
            final OsmPrimitive fromDataSet = dataSet.getPrimitiveById(fromLevel0L);

            // TODO handle way nodes, relation members
            if (fromLevel0L instanceof WayData) {
                ((WayData) fromLevel0L).setNodeIds(Collections.emptyList());
            } else if (fromLevel0L instanceof RelationData) {
                ((RelationData) fromLevel0L).setMembers(Collections.emptyList());
            }

            final OsmPrimitive newInstance = fromLevel0L.getType().newVersionedInstance(fromDataSet.getUniqueId(), fromDataSet.getVersion());
            newInstance.load(fromLevel0L);

            final TagMap newKeys = newInstance.getKeys();
            if (!Objects.equals(newKeys, fromDataSet.getKeys())) {
                fromDataSet.getKeys().keySet().forEach(key ->
                                newKeys.computeIfAbsent(key, k -> ""));
                final ChangePropertyCommand command = new ChangePropertyCommand(Collections.singleton(fromDataSet), newKeys);
                commands.add(command);
            }

            if (fromDataSet instanceof Node && !Objects.equals(((Node) fromDataSet).getCoor(), ((Node) newInstance).getCoor())) {
                final MoveCommand command = new MoveCommand(((Node) fromDataSet), ((Node) newInstance).getCoor());
                commands.add(command);
            }
        }
        if (commands.isEmpty()) {
            return null;
        }
        return new SequenceCommand("Comfort0", commands);
    }
}
