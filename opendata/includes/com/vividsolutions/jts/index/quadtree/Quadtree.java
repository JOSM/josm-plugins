
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
package com.vividsolutions.jts.index.quadtree;

import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.ArrayListVisitor;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.SpatialIndex;
/**
 * A Quadtree is a spatial index structure for efficient querying
 * of 2D rectangles.  If other kinds of spatial objects
 * need to be indexed they can be represented by their
 * envelopes
 * <p>
 * The quadtree structure is used to provide a primary filter
 * for range rectangle queries.  The query() method returns a list of
 * all objects which <i>may</i> intersect the query rectangle.  Note that
 * it may return objects which do not in fact intersect.
 * A secondary filter is required to test for exact intersection.
 * Of course, this secondary filter may consist of other tests besides
 * intersection, such as testing other kinds of spatial relationships.
 *
 * <p>
 * This implementation does not require specifying the extent of the inserted
 * items beforehand.  It will automatically expand to accomodate any extent
 * of dataset.
 * <p>
 * This data structure is also known as an <i>MX-CIF quadtree</i>
 * following the usage of Samet and others.
 *
 * @version 1.7
 */
public class Quadtree
    implements SpatialIndex
{
  /**
   * Ensure that the envelope for the inserted item has non-zero extents.
   * Use the current minExtent to pad the envelope, if necessary
   */
  public static Envelope ensureExtent(Envelope itemEnv, double minExtent)
  {
    //The names "ensureExtent" and "minExtent" are misleading -- sounds like
    //this method ensures that the extents are greater than minExtent.
    //Perhaps we should rename them to "ensurePositiveExtent" and "defaultExtent".
    //[Jon Aquino]
    double minx = itemEnv.getMinX();
    double maxx = itemEnv.getMaxX();
    double miny = itemEnv.getMinY();
    double maxy = itemEnv.getMaxY();
    // has a non-zero extent
    if (minx != maxx && miny != maxy) return itemEnv;

    // pad one or both extents
    if (minx == maxx) {
      minx = minx - minExtent / 2.0;
      maxx = minx + minExtent / 2.0;
    }
    if (miny == maxy) {
      miny = miny - minExtent / 2.0;
      maxy = miny + minExtent / 2.0;
    }
    return new Envelope(minx, maxx, miny, maxy);
  }

  private Root root;
  /**

  * minExtent is the minimum envelope extent of all items
  * inserted into the tree so far. It is used as a heuristic value
  * to construct non-zero envelopes for features with zero X and/or Y extent.
  * Start with a non-zero extent, in case the first feature inserted has
  * a zero extent in both directions.  This value may be non-optimal, but
  * only one feature will be inserted with this value.
  **/
  private double minExtent = 1.0;

  /**
   * Constructs a Quadtree with zero items.
   */
  public Quadtree()
  {
    root = new Root();
  }


  public void insert(Envelope itemEnv, Object item)
  {
    collectStats(itemEnv);
    Envelope insertEnv = ensureExtent(itemEnv, minExtent);
    root.insert(insertEnv, item);
  }

  /**
   * Removes a single item from the tree.
   *
   * @param itemEnv the Envelope of the item to be removed
   * @param item the item to remove
   * @return <code>true</code> if the item was found (and thus removed)
   */
  public boolean remove(Envelope itemEnv, Object item)
  {
    Envelope posEnv = ensureExtent(itemEnv, minExtent);
    return root.remove(posEnv, item);
  }

/*
  public List OLDquery(Envelope searchEnv)
  {
    /**
     * the items that are matched are the items in quads which
     * overlap the search envelope
     */
    /*
    List foundItems = new ArrayList();
    root.addAllItemsFromOverlapping(searchEnv, foundItems);
    return foundItems;
  }
*/

  /**
   * Queries the tree and returns items which may lie in the given search envelope.
   * Precisely, the items that are returned are all items in the tree 
   * whose envelope <b>may</b> intersect the search Envelope.
   * Note that some items with non-intersecting envelopes may be returned as well;
   * the client is responsible for filtering these out.
   * In most situations there will be many items in the tree which do not
   * intersect the search envelope and which are not returned - thus
   * providing improved performance over a simple linear scan.    
   * 
   * @param searchEnv the envelope of the desired query area.
   * @return a List of items which may intersect the search envelope
   */
  public List query(Envelope searchEnv)
  {
    /**
     * the items that are matched are the items in quads which
     * overlap the search envelope
     */
    ArrayListVisitor visitor = new ArrayListVisitor();
    query(searchEnv, visitor);
    return visitor.getItems();
  }

  /**
   * Queries the tree and visits items which may lie in the given search envelope.
   * Precisely, the items that are visited are all items in the tree 
   * whose envelope <b>may</b> intersect the search Envelope.
   * Note that some items with non-intersecting envelopes may be visited as well;
   * the client is responsible for filtering these out.
   * In most situations there will be many items in the tree which do not
   * intersect the search envelope and which are not visited - thus
   * providing improved performance over a simple linear scan.    
   * 
   * @param searchEnv the envelope of the desired query area.
   * @param visitor a visitor object which is passed the visited items
   */
  public void query(Envelope searchEnv, ItemVisitor visitor)
  {
    /**
     * the items that are matched are the items in quads which
     * overlap the search envelope
     */
    root.visit(searchEnv, visitor);
  }


  private void collectStats(Envelope itemEnv)
  {
    double delX = itemEnv.getWidth();
    if (delX < minExtent && delX > 0.0)
      minExtent = delX;

    double delY = itemEnv.getHeight();
    if (delY < minExtent && delY > 0.0)
      minExtent = delY;
  }

}
