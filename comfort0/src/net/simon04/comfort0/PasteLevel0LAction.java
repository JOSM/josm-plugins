package net.simon04.comfort0;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Logging;

import net.simon04.comfort0.level0l.parsergen.Level0LParser;
import net.simon04.comfort0.level0l.parsergen.ParseException;

/**
 * Pastes Level0L format and updates objects in the dataset.
 */
public class PasteLevel0LAction extends JosmAction {

    /**
     * Constructs a new {@link PasteLevel0LAction}.
     */
    public PasteLevel0LAction() {
        super(tr("Paste Level0L"),
                "theta",
                tr("Pastes Level0L format and updates objects in the dataset"),
                null, false, "PasteLevel0LAction", true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            final String clipboard = ClipboardUtils.getClipboardStringContent();
            if (clipboard == null) {
                return;
            }
            try (StringReader reader = new StringReader(clipboard)) {
                EditLevel0LAction.readLevel0(reader, getLayerManager().getEditDataSet());
            }
        } catch (Exception ex) {
            Logging.error(ex);
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getLayerManager().getEditDataSet() != null);
        updateEnabledStateOnCurrentSelection(true);
    }
}
