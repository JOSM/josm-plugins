package public_transport;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

import java.util.Collection;
import java.util.Vector;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

public class WaypointsNameCommand extends Command
{
  private int workingLine = 0;
  private WaypointTableModel waypointTM = null;
  private String oldName = null;
  private String name = null;
  private String oldShelter = null;
  private String shelter = null;
  
  public WaypointsNameCommand
      (WaypointTableModel waypointTM, int workingLine, String name, String shelter)
  {
    this.waypointTM = waypointTM;
    this.workingLine = workingLine;
    if (waypointTM.nodes.elementAt(workingLine) != null)
    {
      oldName = waypointTM.nodes.elementAt(workingLine).get("name");
      oldShelter = waypointTM.nodes.elementAt(workingLine).get("shelter");
    }
    this.name = name;
    this.shelter = shelter;
    if ("".equals(shelter))
      shelter = null;
  }
  
  public boolean executeCommand()
  {
    if (waypointTM.nodes.elementAt(workingLine) != null)
    {
      waypointTM.nodes.elementAt(workingLine).put("name", name);
      waypointTM.nodes.elementAt(workingLine).put("shelter", shelter);
    }
    waypointTM.inEvent = true;
    if (name == null)
      waypointTM.setValueAt("", workingLine, 1);
    else
      waypointTM.setValueAt(name, workingLine, 1);
    if (shelter == null)
      waypointTM.setValueAt("", workingLine, 2);
    else
      waypointTM.setValueAt(shelter, workingLine, 2);
    waypointTM.inEvent = false;
    return true;
  }
  
  public void undoCommand()
  {
    if (waypointTM.nodes.elementAt(workingLine) != null)
    {
      waypointTM.nodes.elementAt(workingLine).put("name", oldName);
      waypointTM.nodes.elementAt(workingLine).put("shelter", oldShelter);
    }
    waypointTM.inEvent = true;
    if (oldName == null)
      waypointTM.setValueAt("", workingLine, 1);
    else
      waypointTM.setValueAt(oldName, workingLine, 1);
    if (oldShelter == null)
      waypointTM.setValueAt("", workingLine, 2);
    else
      waypointTM.setValueAt(oldShelter, workingLine, 2);
    waypointTM.inEvent = false;
  }
  
  public void fillModifiedData
    (Collection< OsmPrimitive > modified, Collection< OsmPrimitive > deleted,
     Collection< OsmPrimitive > added)
  {
  }
  
  public MutableTreeNode description()
  {
    return new DefaultMutableTreeNode("public_transport.Waypoints.EditName");
  }
};
