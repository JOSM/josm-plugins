/* LakewalkerReader.java
 * 
 * Read and process data from a Lakwalker python module
 * 
 */
package org.openstreetmap.josm.plugins.lakewalker;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

public class LakewalkerReader {
  
  protected Collection<Command> commands = new LinkedList<Command>();
  protected Collection<Way> ways = new ArrayList<Way>();
  protected boolean cancel;
  
  /*
   * Read the data
   */
  public void read(BufferedReader input) {
    /*
     * Lakewalker will output data it stdout. Each line has a code in
     * character 1 indicating the type of data on the line:
     * 
     * m text - Status message l name [size] - Access landsat image name. size
     * is returned if it needs to be downloaded. e text - Error message s nnn -
     * Start node data stream, nnn seperate tracings to follow t nnn - Start
     * tracing, nnn nodes to follow x [o] - End of Tracing. o indicates do not
     * connect last node to first n lat lon [o] - Node. o indicates it is an
     * open node (not connected to the previous node) z - End of data stream
     */

    Way way = new Way();
    String line;
    setStatus("Initializing");
    double eastOffset = 0.0;
    double northOffset = 0.0;
    try {
      eastOffset = Double.parseDouble(Main.pref.get(LakewalkerPreferences.PREF_EAST_OFFSET, "0.0"));
      northOffset = Double.parseDouble(Main.pref.get(LakewalkerPreferences.PREF_NORTH_OFFSET, "0.0"));
    }
    catch (Exception e) {
      
    }
    char option = ' ';
    
    try {
    	
      Node fn = null; //new Node(new LatLon(0,0));
    	
      while ((line = input.readLine()) != null) {
        if (cancel) {
          return;
        }
        System.out.println(line);
        option = line.charAt(0);
        switch (option) {
        case 'n':
          String[] tokens = line.split(" ");
          try {
            LatLon ll = new LatLon(Double.parseDouble(tokens[1])+northOffset, Double.parseDouble(tokens[2])+eastOffset);
            Node n = new Node(ll);
            commands.add(new AddCommand(n));
            way.nodes.add(n);
            if(fn==null){
            	fn = n;
            }
          }
          catch (Exception ex) {

          }
          break;

        case 's':
          setStatus(line.substring(2));
          break;
          
        case 'x':
          String waytype = Main.pref.get(LakewalkerPreferences.PREF_WAYTYPE, "water");
          
          if(!waytype.equals("none")){
        	  way.put("natural",waytype);
          }
          
          way.put("created_by", "Dshpak_landsat_lakes");
          commands.add(new AddCommand(way));
          
          break;
          
        case 'e':
          String error = line.substring(2);
          cancel = true;
          break;
        }
      } 
      input.close();
      way.nodes.add(fn);
    }

    catch (Exception ex) {
    }
    
    if (!commands.isEmpty()) {
      Main.main.undoRedo.add(new SequenceCommand(tr("Lakewalker trace"), commands));
      Main.ds.setSelected(ways);
    }
  }
  
  /*
   * User has hit the cancel button
   */
  public void cancel() {
    cancel = true;
  }
  
  protected void setStatus(String s) {
    Main.pleaseWaitDlg.currentAction.setText(s);
    Main.pleaseWaitDlg.repaint();
  }
  
}