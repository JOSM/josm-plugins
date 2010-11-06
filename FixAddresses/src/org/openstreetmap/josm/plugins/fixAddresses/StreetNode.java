/*
 * This program is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the 
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with this program. 
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.openstreetmap.josm.plugins.fixAddresses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * This class is the container for all street segments with the same name. Every street
 * consists at least of one segment.
 * 
 * @author Oliver Wieland <oliver.wieland@online.de>
 */
public class StreetNode extends OSMEntityBase {
	private List<IOSMEntity> children;
	private List<OSMAddress> addresses;
			
	/**
	 * @param osmPrimitive
	 */
	public StreetNode(OsmPrimitive osmPrimitive) {
		super(osmPrimitive);
	}

	public List<IOSMEntity> getChildren() {
		return children;
	}
	
	/**
	 * Adds a street segment to the street node.
	 * @param segment
	 */
	public void addStreetSegment(StreetSegmentNode segment) {
		lazyCreateChildren();
		
		children.add(segment);
		Collections.sort(children);
	}
	
	private void lazyCreateChildren() {
		if (children == null) {
			children = new ArrayList<IOSMEntity>();
		}
	}
	
	public void addAddress(OSMAddress aNode) {
		lazyCreateAddresses();
		addresses.add(aNode);		
	}

	private void lazyCreateAddresses() {
		if (addresses == null) {
			addresses = new ArrayList<OSMAddress>();
		}
	}
	
	public boolean hasAddresses() {
		return addresses != null && addresses.size() > 0;
	}
	
	public List<OSMAddress> getAddresses() {
		return addresses;
	}
	
	public void setAddresses(List<OSMAddress> addresses) {
		this.addresses = addresses;
	}
	
	/**
	 * Gets the number of addresses associated with this street.
	 * @return
	 */
	public int getNumberOfAddresses() {
		if (addresses == null) return 0;
		
		return addresses.size();
	}
	
	/**
	 * Gets the number of street segments of this street.
	 * @return
	 */
	public int getNumberOfSegments() {
		if (children == null) return 0;
		
		int sc = 0;
		for (IOSMEntity node : children) {
			if (node instanceof StreetSegmentNode) {
				sc++;
			}
		}
		return sc;
	}
	
	/**
	 * Gets the road type(s) of this street. If the street has different types,
	 * they are separated by comma. 
	 * @return
	 */
	public String getType() {
		List<String> types = new ArrayList<String>();
		
		for (IOSMEntity seg : getChildren()) {
			OsmPrimitive osmPrim = seg.getOsmObject();
			if (TagUtils.hasHighwayTag(osmPrim)) {
				String val = osmPrim.get(TagUtils.HIGHWAY_TAG);
				if (!types.contains(val)) {
					types.add(val);
				}
			}
		}
		
		StringBuffer sb = new StringBuffer(20);
		for (String string : types) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			sb.append(string);
			
		}
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.openstreetmap.josm.plugins.addressEdit.NodeEntityBase#toString()
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer(getName());
		
		if (children != null) {
			sb.append(String.format(", %d segments", children.size()));
		}
		
		if (addresses != null) {
			sb.append(String.format(", %d address entries", addresses.size()));
		}
		
		return sb.toString();
	}

}
