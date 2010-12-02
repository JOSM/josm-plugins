/* Copyright (c) 2010, skobbler GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.plugins.mapdust.gui;


import static org.openstreetmap.josm.tools.I18n.tr;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.plugins.mapdust.MapdustPlugin;
import org.openstreetmap.josm.plugins.mapdust.gui.component.panel.MapdustActionPanel;
import org.openstreetmap.josm.plugins.mapdust.gui.component.panel.MapdustBugPropertiesPanel;
import org.openstreetmap.josm.plugins.mapdust.gui.component.panel.MapdustPanel;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustActionListObserver;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustBugDetailsObservable;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustBugDetailsObserver;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustInitialUpdateObservable;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustInitialUpdateObserver;
import org.openstreetmap.josm.plugins.mapdust.gui.value.MapdustAction;
import org.openstreetmap.josm.plugins.mapdust.gui.value.MapdustPluginState;
import org.openstreetmap.josm.plugins.mapdust.service.value.MapdustBug;
import org.openstreetmap.josm.tools.Shortcut;


/**
 * This class is the main graphical user interface class.
 *
 * @author Bea
 */
public class MapdustGUI extends ToggleDialog implements
        MapdustActionListObserver, MapdustBugDetailsObservable,
        MapdustInitialUpdateObservable {

    /** The serial version UID */
    private static final long serialVersionUID = 1L;

    /** The list of MapDust bug details observers */
    private final ArrayList<MapdustBugDetailsObserver> bugDetailsObservers =
            new ArrayList<MapdustBugDetailsObserver>();

    /** The list of MapDust initial update observers */
    private final ArrayList<MapdustInitialUpdateObserver> initialUpdateObservers =
            new ArrayList<MapdustInitialUpdateObserver>();

    /** The <code>MapdustPanel</code> object */
    private MapdustPanel panel;

    /** The <code>MapdustActionPanel</code> object */
    private MapdustActionPanel queuePanel;

    /** The <code>JTabbedPanel</code> object */
    private JTabbedPane tabbedPane;

    /** The <code>MapdustPlugin</code> plugin */
    private MapdustPlugin mapdustPlugin;

    /** The <code>MapdustBugPropertiesPanel</code> */
    private final MapdustBugPropertiesPanel detailsPanel;

    /** The <code>JPanel</code> */
    private JPanel mainPanel;

    /**
     * Flag indicating if the MapDust data was downloaded and the view was
     * updated
     */
    private boolean initialUpdate = false;

    /**
     * Builds a <code>MapdustGUi</code> based on the given parameters.
     *
     * @param name The name of the GUI
     * @param iconName The name of the icon
     * @param tooltip The tool tip
     * @param shortcut The shortcut
     * @param preferredHeight The height
     * @param mapdustPlugin The <code>MapdustPlugin</code> object
     */
    public MapdustGUI(String name, String iconName, String tooltip,
            Shortcut shortcut, int preferredHeight, MapdustPlugin mapdustPlugin) {
        super(tr(name), iconName, tr(tooltip), shortcut, preferredHeight);
        this.mapdustPlugin = mapdustPlugin;
        panel = new MapdustPanel(mapdustPlugin.getMapdustBugList(),
                "Bug reports", mapdustPlugin);
        MapdustBug bug = null;
        if (mapdustPlugin.getMapdustBugList() != null
                && mapdustPlugin.getMapdustBugList().size() > 0) {
            bug = mapdustPlugin.getMapdustBugList().get(0);
        }
        detailsPanel = new MapdustBugPropertiesPanel(bug);
        panel.addObserver(detailsPanel);
        addObserver(detailsPanel);
        String pluginState = Main.pref.get("mapdust.pluginState");
        if (pluginState.equals(MapdustPluginState.OFFLINE.getValue())) {
            /* offline mode, need to add also queue panel */
            tabbedPane = new JTabbedPane();
            List<MapdustAction> list = new LinkedList<MapdustAction>();
            mainPanel = new JPanel();
            mainPanel.setAutoscrolls(true);
            mainPanel.setLayout(new BorderLayout(5, 10));
            mainPanel.add(detailsPanel, BorderLayout.NORTH);
            mainPanel.add(panel, BorderLayout.CENTER);
            queuePanel = new MapdustActionPanel(list, "Work offline", mapdustPlugin);
            tabbedPane.add(mainPanel, 0);
            tabbedPane.add(queuePanel);
            add(tabbedPane, BorderLayout.CENTER);
        } else {
            /* online mode */
            mainPanel = new JPanel();
            mainPanel.setIgnoreRepaint(true);
            mainPanel.setAutoscrolls(true);
            mainPanel.setLayout(new BorderLayout(5, 10));
            mainPanel.add(detailsPanel, BorderLayout.NORTH);
            mainPanel.add(panel, BorderLayout.CENTER);
            add(mainPanel, BorderLayout.CENTER);
            tabbedPane = null;
            queuePanel = null;
        }
    }

    /**
     * Updates the MapDust GUI with the given list of <code>MapdustBug</code>
     * objects.
     *
     * @param mapdustBugs The list of <code>MapdustBug</code> objects
     * @param mapdustPlugin The <code>MapdustPlugin</code> object
     */
    public synchronized void update(List<MapdustBug> mapdustBugs,
            MapdustPlugin mapdustPlugin) {
        this.mapdustPlugin = mapdustPlugin;
        String pluginState = Main.pref.get("mapdust.pluginState");
        if (pluginState.equals(MapdustPluginState.ONLINE.getValue())) {
            /* remove the panels */
            if (tabbedPane != null) {
                /* offline to online */
                tabbedPane.remove(panel);
                tabbedPane.remove(queuePanel);
                mainPanel.remove(tabbedPane);
                remove(mainPanel);
                queuePanel = null;
            } else {
                /* online to online */
                mainPanel.remove(detailsPanel);
                mainPanel.remove(panel);
                remove(mainPanel);
            }
            /* add panels with updated data */
            panel = new MapdustPanel(mapdustBugs, "Bug reports", mapdustPlugin);
            panel.addObserver(detailsPanel);
            MapdustBug selectedBug =
                    (mapdustBugs != null && mapdustBugs.size() > 0) ? mapdustBugs
                            .get(0) : null;
            notifyObservers(selectedBug);
            mainPanel = new JPanel();
            mainPanel.setAutoscrolls(true);
            mainPanel.setLayout(new BorderLayout());
            if (mapdustBugs != null) {
                mainPanel.add(detailsPanel, BorderLayout.NORTH);
            }
            mainPanel.add(panel, BorderLayout.CENTER);
            add(mainPanel, BorderLayout.CENTER);
        } else {
            List<MapdustAction> list = new ArrayList<MapdustAction>();
            /* remove panels */
            if (queuePanel == null) {
                /* from online to offline */
                mainPanel.remove(detailsPanel);
                mainPanel.remove(panel);
                remove(mainPanel);
            } else {
                list = queuePanel.getActionList();
                mainPanel.remove(detailsPanel);
                tabbedPane.remove(panel);
                tabbedPane.remove(queuePanel);
                mainPanel.remove(tabbedPane);
                remove(mainPanel);
            }
            /* add panels with updated data */
            tabbedPane = new JTabbedPane();
            queuePanel = new MapdustActionPanel(list, "Offline Contribution",
                            mapdustPlugin);
            panel = new MapdustPanel(mapdustBugs, "Bug reports (offline)",
                            mapdustPlugin);
            panel.addObserver(detailsPanel);
            mainPanel = new JPanel();
            mainPanel.setAutoscrolls(true);
            mainPanel.setLayout(new BorderLayout());
            if (mapdustBugs != null && mapdustBugs.size() > 0) {
                mainPanel.add(detailsPanel, BorderLayout.NORTH);
            }
            tabbedPane.add(panel, 0);
            tabbedPane.add(queuePanel);
            mainPanel.add(tabbedPane, BorderLayout.CENTER);
            add(mainPanel, BorderLayout.CENTER);
        }
        /* invalidate and repaint */
        invalidate();
        repaint();
    }

    /**
     * Adds the given <code>MapdustAction</code> object to the list of actions.
     *
     * @param action The <code>MapdustAction</code> object
     */
    @Override
    public synchronized void addAction(MapdustAction action) {
        /* add the action */
        List<MapdustAction> list = queuePanel.getActionList();
        List<MapdustBug> mapdustBugs = panel.getMapdustBugsList();
        mapdustBugs = modifyBug(mapdustBugs, action.getMapdustBug());

        /* remove panels */
        mainPanel.remove(detailsPanel);
        tabbedPane.remove(panel);
        tabbedPane.remove(queuePanel);
        mainPanel.remove(tabbedPane);
        remove(mainPanel);
        /* create new tabbed pane */
        tabbedPane = new JTabbedPane();
        list.add(action);
        queuePanel = new MapdustActionPanel(list, "Offline Contribution",
                mapdustPlugin);
        panel = new MapdustPanel(mapdustBugs, "Bug reports (offline)",
                mapdustPlugin);
        mainPanel = new JPanel();
        mainPanel.setAutoscrolls(true);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.add(detailsPanel, BorderLayout.NORTH);
        tabbedPane.add(panel, 0);
        tabbedPane.add(queuePanel);
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        /* invalidate and repaint */
        invalidate();
        repaint();
    }

    /**
     * Shows the MapDust main dialog. In the case if the plugin was not updated
     * with the new MapDust data, or the data was not downloaded; it will also
     * download the MapDust data from the current view, and update the plugin
     * with the new data.
     *
     */
    @Override
    public void showDialog() {
        super.showDialog();
        /* was not updated */
        if (!initialUpdate && isShowing) {
            notifyObservers();
            initialUpdate = true;
        }
    }

    /**
     * Modifies the given <code>MapdustBug</code> in the given list of
     * <code>MapdustBug</code> objects. Returns the list of bugs containing the
     * modified bug.
     *
     * @param mapdustBugs The list of <code>MapdustBug</code> objects
     * @param modifiedBug The <code>MapdustBug</code> object
     * @return the modified list
     */
    private List<MapdustBug> modifyBug(List<MapdustBug> mapdustBugs,
            MapdustBug modifiedBug) {
        int index = -1;
        for (int i = 0; i < mapdustBugs.size(); i++) {
            if (modifiedBug.getId() != null) {
                if (mapdustBugs.get(i).getId().equals(modifiedBug.getId())) {
                    index = i;
                }
            }
        }
        if (index != -1) {
            /* remove, and add to the top of the list */
            mapdustBugs.remove(index);
            mapdustBugs.add(0, modifiedBug);
        }
        return mapdustBugs;
    }

    /**
     * Adds a new MapDust bug details observer to the list of observers.
     *
     * @param observer The <code>MapdustBugDetailsObserver</code> object
     */
    @Override
    public void addObserver(MapdustBugDetailsObserver observer) {
        if (!this.bugDetailsObservers.contains(observer)) {
            this.bugDetailsObservers.add(observer);
        }
    }

    /**
     * Adds a new MapDust initial update observer to the list of observers.
     *
     * @param observer The <code>MapdustInitialUpdateObserver</code> object
     */
    @Override
    public void addObserver(MapdustInitialUpdateObserver observer) {
        if (!this.initialUpdateObservers.contains(observer)) {
            this.initialUpdateObservers.add(observer);
        }
    }

    /**
     * Removes the given MapDust bug details observer from the list of
     * observers.
     *
     * @param observer The <code>MapdustBugDetailsObserver</code> object
     */
    @Override
    public void removeObserver(MapdustBugDetailsObserver observer) {
        this.bugDetailsObservers.remove(observer);

    }

    /**
     * Removes the given MapDust initial update observer from the list of
     * observers.
     *
     * @param observer The <code>MapdustInitialUpdateObserver</code> object
     */
    @Override
    public void removeObserver(MapdustInitialUpdateObserver observer) {
        this.initialUpdateObservers.remove(observer);

    }

    /**
     * Notifies the <code>MapdustBugDetailsObserver</code> objects observing the
     * given <code>MapdustBug</code> object.
     */
    @Override
    public void notifyObservers(MapdustBug mapdustBug) {
        Iterator<MapdustBugDetailsObserver> elements =
                this.bugDetailsObservers.iterator();
        while (elements.hasNext()) {
            (elements.next()).showDetails(mapdustBug);
        }
    }

    /**
     * Notifies the <code>MapdustInitialUpdateObserver</code> objects waiting
     * for the initial download, and update of plugin.
     */
    @Override
    public void notifyObservers() {
        Iterator<MapdustInitialUpdateObserver> elements =
                this.initialUpdateObservers.iterator();
        while (elements.hasNext()) {
            (elements.next()).initialUpdate();
        }
    }

    /**
     * Returns the <code>MapdustPanel</code> object
     *
     * @return the panel
     */
    public MapdustPanel getPanel() {
        return panel;
    }

    /**
     * Returns the <code>MapdustActionPanel</code> object
     *
     * @return the queuePanel
     */
    public MapdustActionPanel getQueuePanel() {
        return queuePanel;
    }

}
