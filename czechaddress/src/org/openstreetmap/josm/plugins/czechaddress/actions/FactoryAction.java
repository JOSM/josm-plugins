package org.openstreetmap.josm.plugins.czechaddress.actions;

import org.openstreetmap.josm.plugins.czechaddress.gui.FactoryDialog;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.mapmode.MapMode;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.plugins.czechaddress.CzechAddressPlugin;
import org.openstreetmap.josm.plugins.czechaddress.addressdatabase.House;
import org.openstreetmap.josm.plugins.czechaddress.proposal.ProposalContainer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Mapmode, which creates address elements with "one click".
 *
 * <p>This action takes the currently selected {@link House} from the
 * {@link FactoryDialog} and creates a node for, whenever user clicks
 * on the map.</p>
 *
 * @see FactoryDialog
 *
 * @author Radomír Černoch, radomir.cernoch@gmail.com
 */
public class FactoryAction extends MapMode {

    /**
     * Default constructor, which sets the title, shortcut, ...
     */
    public FactoryAction(MapFrame frame) {
        super("Sputit továrnu na adresy",
              "envelope-cursor.png",
              "Vytváří adresní body jedním kliknutím",
              Shortcut.registerShortcut( "mapmode:clickaddress",
                  "Sputit továrnu na adresy",
                  KeyEvent.VK_F, Shortcut.GROUP_EDIT),
              frame,
              ImageProvider.getCursor("crosshair", "envelope-star-small"));
    }

    /**
     * Method called from JOSM when the user selects this mapmode.
     *
     * <p>It registers itself into the {@link MapFrame}'s list of
     * {@link MouseListener}s.</p>
     */
    @Override
    public void enterMode() {
        super.enterMode();
        Main.map.mapView.addMouseListener(this);

        if (CzechAddressPlugin.reasoner == null) {
            exitMode();
            return;
        }

        // Switch to next unassigned house.
        FactoryDialog d = CzechAddressPlugin.factoryDialog;
        if (CzechAddressPlugin.reasoner.translate(d.getSelectedHouse()) != null)
            d.selectNextUnmatchedHouseByCheckBox();
    }

    /**
     * Method called from JOSM when the user deselects this mapmode.
     *
     * <p>It unregisters itself from the {@link MapFrame}'s list of
     * {@link MouseListener}s.</p>
     */
    @Override
    public void exitMode() {
        Main.map.mapView.removeMouseListener(this);
        super.exitMode();
    }

    /**
     * Core methos of this action, which actually creates the nodes.
     *
     * @param e event encapsulating the click-position on the {@link MapFrame}.
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);

        // Get the currently selected House in the FactoryDialog.
        House house = CzechAddressPlugin.factoryDialog.getSelectedHouse();
        if (house == null)
            return; // TODO: Some meaningful messageBox would be useful.

        // Create a new Node and add it into the map.
        Node newNode = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));

        ProposalContainer container = new ProposalContainer(newNode);
        container.setProposals(house.getDiff(newNode));
        container.applyAll();

        // And make the new node selected.
        Main.ds.addPrimitive(newNode);
        Main.ds.setSelected(newNode);

        CzechAddressPlugin.reasoner.overwriteMatch(house, newNode);
        CzechAddressPlugin.factoryDialog.selectNextUnmatchedHouseByCheckBox();
    }
}
