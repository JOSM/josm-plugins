//    JOSM opendata plugin.
//    Copyright (C) 2011-2012 Don-vip
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.opendata.core.io.tabular;

import java.util.Map;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.opendata.core.io.tabular.SpreadSheetReader.CoordinateColumns;

public interface SpreadSheetHandler {

	public void setSheetNumber(int n);
	
	public int getSheetNumber();

	public void setHandlesProjection(boolean handle);
	
	public boolean handlesProjection();

	public LatLon getCoor(EastNorth en, String[] fields);

	public void setLineNumber(int n);
	
	public int getLineNumber();

	public void setXCol(int i);

	public void setYCol(int i);
	
	public int getXCol();

	public int getYCol();

    public void nodesAdded(DataSet ds, Map<CoordinateColumns, Node> nodes, String[] fields, int lineNumber);
}
