package org.openstreetmap.josm.plugins.czechaddress.gui.utils;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * Class for shorter and faster implementations of {@link ListModel}s.
 *
 * <p>This creates a list of {@link ListDataListener}s and implements
 * method for adding and removing them. Moreover it allows to notify all
 * listeners with the generic message {@code CONTENT_CHANGED}.</p>
 *
 * @author Radomír Černoch, radomir.cernoch@gmail.com
 */
public abstract class HalfCookedListModel implements ListModel {

    List<ListDataListener> listeners = new ArrayList<ListDataListener>();

    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    public void notifyAllListeners() {
        ListDataEvent evt = new ListDataEvent(this,
                ListDataEvent.CONTENTS_CHANGED, 0, getSize()-1);

        for (ListDataListener l : listeners)
            l.contentsChanged(evt);
    }
}
