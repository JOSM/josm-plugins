// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui;

import java.io.Serial;

import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.streetside.gui.imageinfo.StreetsideViewerPanel;

/**
 * Toggle dialog that shows an image and some buttons.
 *
 * @author nokutu
 */

public final class StreetsideViewerDialog extends ToggleDialog {

    @Serial
    private static final long serialVersionUID = 6424974077669812562L;

    private static final String BASE_TITLE = "360° Streetside Viewer";

    private static StreetsideViewerDialog instance;

    /**
     * Object containing the shown image and that handles zoom and drag
     */
    private final StreetsideViewerPanel streetsideViewerPanel;

    private StreetsideViewerDialog() {
        super(StreetsideViewerDialog.BASE_TITLE, "streetside-viewer", "Open Streetside Viewer window", null, 200, true,
                StreetsidePreferenceSetting.class);
        streetsideViewerPanel = new StreetsideViewerPanel();
        createLayout(streetsideViewerPanel, true, null);
    }

    /**
     * Returns the unique instance of the class.
     *
     * @return The unique instance of the class.
     */
    public static synchronized StreetsideViewerDialog getInstance() {
        if (StreetsideViewerDialog.instance == null) {
            StreetsideViewerDialog.instance = new StreetsideViewerDialog();
        }
        return StreetsideViewerDialog.instance;
    }

    /**
     * Destroys the unique instance of the class.
     */
    public static synchronized void destroyInstance() {
        StreetsideViewerDialog.instance = null;
    }

    public StreetsideViewerPanel getStreetsideViewerPanel() {
        return streetsideViewerPanel;
    }
}
