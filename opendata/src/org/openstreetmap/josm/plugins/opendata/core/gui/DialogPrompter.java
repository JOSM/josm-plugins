// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.gui;

import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;

public class DialogPrompter<T extends ExtendedDialog> implements Runnable {

    private T dialog;
    private int value = -1;

    protected T buildDialog() {
        // To be overriden if needed
        return null;
    }

    public DialogPrompter() {
        this(null);
    }

    public DialogPrompter(T dialog) {
        this.dialog = dialog;
    }

    public final T getDialog() {
        return dialog;
    }

    @Override
    public final void run() {
        if (dialog == null) {
            dialog = buildDialog();
        }
        if (dialog != null) {
            value = dialog.showDialog().getValue();
        }
    }

    public final DialogPrompter<T> promptInEdt() {
        GuiHelper.runInEDTAndWait(this);
        return this;
    }

    public final int getValue() {
        return value;
    }
}
