package org.openstreetmap.josm.plugins.remotecontrol.handler;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.remotecontrol.PermissionPref;
import org.openstreetmap.josm.plugins.remotecontrol.RequestHandler;
import org.openstreetmap.josm.plugins.remotecontrol.RequestHandlerBadRequestException;

/**
 * Handler for add_node request.
 */
public class AddNodeHandler extends RequestHandler {

	public static final String command = "/add_node";

	@Override
	protected void handleRequest() {
        addNode(args);
	}

	@Override
	protected String[] getMandatoryParams()
	{
		return new String[] { "lat", "lon" };
	}
	
	@Override
	public String getPermissionMessage() {
		return tr("Remote Control has been asked to create a new node.");
	}

	@Override
	public PermissionPref getPermissionPref()
	{
		return new PermissionPref("remotecontrol.permission.create-objects",
				"RemoteControl: creating objects forbidden by preferences");
	}
	
    /**
     * Adds a node, implements the GET /add_node?lon=...&amp;lat=... request.
     * @param args
     */
    private void addNode(HashMap<String, String> args){

        // Parse the arguments
        double lat = Double.parseDouble(args.get("lat"));
        double lon = Double.parseDouble(args.get("lon"));
        System.out.println("Adding node at (" + lat + ", " + lon + ")");

        // Create a new node
        LatLon ll = new LatLon(lat, lon);
        Node nnew = new Node(ll);

        // Now execute the commands to add this node.
        Main.main.undoRedo.add(new AddCommand(nnew));
        Main.main.getCurrentDataSet().setSelected(nnew);
        Main.map.mapView.repaint();

    }

}
