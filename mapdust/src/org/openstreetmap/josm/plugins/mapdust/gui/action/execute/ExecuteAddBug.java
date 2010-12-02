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
package org.openstreetmap.josm.plugins.mapdust.gui.action.execute;


import static org.openstreetmap.josm.tools.I18n.tr;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JOptionPane;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.mapdust.gui.MapdustGUI;
import org.openstreetmap.josm.plugins.mapdust.gui.component.dialog.CreateIssueDialog;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustActionListObservable;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustActionListObserver;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustBugObservable;
import org.openstreetmap.josm.plugins.mapdust.gui.observer.MapdustBugObserver;
import org.openstreetmap.josm.plugins.mapdust.gui.value.MapdustAction;
import org.openstreetmap.josm.plugins.mapdust.gui.value.MapdustPluginState;
import org.openstreetmap.josm.plugins.mapdust.gui.value.MapdustServiceCommand;
import org.openstreetmap.josm.plugins.mapdust.service.MapdustServiceHandler;
import org.openstreetmap.josm.plugins.mapdust.service.MapdustServiceHandlerException;
import org.openstreetmap.josm.plugins.mapdust.service.value.MapdustBug;
import org.openstreetmap.josm.plugins.mapdust.service.value.Status;
import org.openstreetmap.josm.plugins.mapdust.service.value.Type;


/**
 * Creates a new <code>MapdustBug</code> object. Validates the input data, and
 * based on the plugin state executes the corresponding action. If the plugin is
 * in the "ONLINE" state then executes the Mapdust Service 'addBug' method with
 * the given input parameters. If the plugin is in the "OFFLINE" state then adds
 * the given action to the actions list.
 *
 * @author Bea
 *
 */
public class ExecuteAddBug extends MapdustExecuteAction implements
        MapdustBugObservable, MapdustActionListObservable {

    /** The serial version UID */
    private static final long serialVersionUID = 1L;

    /** The list of Mapdust bug observers */
    private final ArrayList<MapdustBugObserver> bugObservers =
            new ArrayList<MapdustBugObserver>();

    /** The list of Mapdust action observers */
    private final ArrayList<MapdustActionListObserver> actionObservers =
            new ArrayList<MapdustActionListObserver>();

    /**
     * Builds a <code>ExecuteAddBug</code> object
     */
    public ExecuteAddBug() {}

    /***
     * Builds a <code>ExecuteAddBug</code> object based on the given arguments.
     *
     * @param dialog The <code>CreateIssueDialog</code> object
     * @param mapdustGUI The <code>MapdustGUI</code> object
     */
    public ExecuteAddBug(CreateIssueDialog dialog, MapdustGUI mapdustGUI) {
        setDialog(dialog);
        setMapdustGUI(mapdustGUI);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event != null) {
            CreateIssueDialog createDialog = (CreateIssueDialog) getDialog();
            Type type = (Type) (createDialog).getCbbType().getSelectedItem();
            String nickname = createDialog.getTxtNickname().getText();
            String commentText = createDialog.getTxtDescription().getText();
            /* validates the input */
            String errorMessage = validate(nickname, commentText);
            if (errorMessage != null) {
                /* invalid data */
                JOptionPane.showMessageDialog(Main.parent, tr(errorMessage),
                        tr("Missing input data"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            /* valid data */
            Main.pref.put("mapdust.nickname", nickname);
            Point p = createDialog.getPoint();
            LatLon latlon = null;
            if (p != null) {
                latlon = Main.map.mapView.getLatLon(p.x, p.y);
            }
            MapdustBug bug =
                    new MapdustBug(latlon, type, commentText, nickname);
            String pluginState = Main.pref.get("mapdust.pluginState");
            if (pluginState.equals(MapdustPluginState.OFFLINE.getValue())) {
                /* offline state */
                int index = getSelectedBugIndex();
                bug.setStatus(Status.OPEN);
                String iconPath = getIconPath(bug);
                MapdustAction mapdustAction =
                        new MapdustAction(MapdustServiceCommand.ADD_BUG,
                                iconPath, bug);
                if (getMapdustGUI().getQueuePanel() != null) {
                    notifyObservers(mapdustAction);
                }
                resetSelectedBug(index);
            } else {
                /* online state */
                MapdustServiceHandler handler = new MapdustServiceHandler();
                Long id = null;
                try {
                    id = handler.addBug(bug);
                } catch (MapdustServiceHandlerException e) {
                    errorMessage = "There was a Mapdust service error.";
                    JOptionPane.showMessageDialog(Main.parent,
                            tr(errorMessage), tr("Error"),
                            JOptionPane.ERROR_MESSAGE);
                }
                if (id != null) {
                    /* success */
                    MapdustBug newMapdustBug = null;
                    try {
                        newMapdustBug = handler.getBug(id, null);
                    } catch (MapdustServiceHandlerException e) {
                        errorMessage = "There was a Mapdust service error.";
                        JOptionPane.showMessageDialog(Main.parent,
                                tr(errorMessage), tr("Error"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                    if (newMapdustBug != null) {
                        notifyObservers(newMapdustBug);
                    }
                }
                resetSelectedBug(0);
            }
            Main.pref.put("mapdust.modify", false);
            /* destroy dialog */
            getDialog().dispose();
        }
    }

    /**
     * Adds a new MapDust bug observer to the list of observers.
     *
     * @param observer The <code>MapdustBugObserver</code> object
     */
    @Override
    public void addObserver(MapdustBugObserver observer) {
        if (!this.bugObservers.contains(observer)) {
            this.bugObservers.add(observer);
        }
    }

    /**
     * Removes the MapDust bug observer object from the list of observers.
     *
     * @param observer The <code>MapdustBugObserver</code> object
     */
    @Override
    public void removeObserver(MapdustBugObserver observer) {
        this.bugObservers.remove(observer);
    }

    /**
     * Notifies the observers observing this action.
     *
     * @param mapdustBug The <code>MapdustBug</code> object
     */
    @Override
    public void notifyObservers(MapdustBug mapdustBug) {
        Iterator<MapdustBugObserver> elements = this.bugObservers.iterator();
        while (elements.hasNext()) {
            (elements.next()).changedData(mapdustBug);
        }
    }

    /**
     * Adds a new MapDust action list observer to the list of observers.
     *
     * @param observer The <code>MapdustActionListObserver</code> object
     */
    @Override
    public void addObserver(MapdustActionListObserver observer) {
        if (!this.actionObservers.contains(observer)) {
            this.actionObservers.add(observer);
        }
    }

    /**
     * Removes the MapDust action list observer object from the list of
     * observers.
     *
     * @param observer The <code>MapdustActionListObserver</code> object
     */
    @Override
    public void removeObserver(MapdustActionListObserver observer) {
        this.actionObservers.remove(observer);

    }

    /**
     * Notifies the observers observing this action.
     *
     * @param mapdustAction The <code>MapdustAction</code> object
     */
    @Override
    public void notifyObservers(MapdustAction mapdustAction) {
        Iterator<MapdustActionListObserver> elements =
                this.actionObservers.iterator();
        while (elements.hasNext()) {
            (elements.next()).addAction(mapdustAction);
        }
    }

}
