
/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.vividsolutions.jts.index.bintree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * The base class for nodes in a {@link Bintree}.
 *
 * @version 1.7
 */
public abstract class NodeBase {

  /**
   * Returns the index of the subnode that wholely contains the given interval.
   * If none does, returns -1.
   */
  public static int getSubnodeIndex(Interval interval, double centre)
  {
    int subnodeIndex = -1;
    if (interval.min >= centre) subnodeIndex = 1;
    if (interval.max <= centre) subnodeIndex = 0;
    return subnodeIndex;
  }

  protected List items = new ArrayList();

  /**
   * subnodes are numbered as follows:
   *
   *  0 | 1
   */
  protected Node[] subnode = new Node[2];

  public NodeBase() {
  }

  public void add(Object item)
  {
    items.add(item);
  }
  protected abstract boolean isSearchMatch(Interval interval);

  /**
   * Adds items in the tree which potentially overlap the query interval
   * to the given collection.
   * If the query interval is <tt>null</tt>, add all items in the tree.
   * 
   * @param interval a query nterval, or null
   * @param resultItems the candidate items found
   */
  public void addAllItemsFromOverlapping(Interval interval, Collection resultItems)
  {
    if (interval != null && ! isSearchMatch(interval))
      return;

    // some of these may not actually overlap - this is allowed by the bintree contract
    resultItems.addAll(items);

    if (subnode[0] != null) subnode[0].addAllItemsFromOverlapping(interval, resultItems);
    if (subnode[1] != null) subnode[1].addAllItemsFromOverlapping(interval, resultItems);
  }
}
