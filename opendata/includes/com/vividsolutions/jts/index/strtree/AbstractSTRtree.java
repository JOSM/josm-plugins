
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
package com.vividsolutions.jts.index.strtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.util.Assert;

/**
 * Base class for STRtree and SIRtree. STR-packed R-trees are described in:
 * P. Rigaux, Michel Scholl and Agnes Voisard. Spatial Databases With
 * Application To GIS. Morgan Kaufmann, San Francisco, 2002.
 * <p>
 * This implementation is based on Boundables rather than just AbstractNodes,
 * because the STR algorithm operates on both nodes and
 * data, both of which are treated here as Boundables.
 *
 * @see STRtree
 * @see SIRtree
 *
 * @version 1.7
 */
public abstract class AbstractSTRtree {

  /**
   * A test for intersection between two bounds, necessary because subclasses
   * of AbstractSTRtree have different implementations of bounds.
   */
  protected static interface IntersectsOp {
    /**
     * For STRtrees, the bounds will be Envelopes; for SIRtrees, Intervals;
     * for other subclasses of AbstractSTRtree, some other class.
     * @param aBounds the bounds of one spatial object
     * @param bBounds the bounds of another spatial object
     * @return whether the two bounds intersect
     */
    boolean intersects(Object aBounds, Object bBounds);
  }

  protected AbstractNode root;

  private boolean built = false;
  private ArrayList itemBoundables = new ArrayList();
  private int nodeCapacity;

  /**
   * Constructs an AbstractSTRtree with the specified maximum number of child
   * nodes that a node may have
   */
  public AbstractSTRtree(int nodeCapacity) {
    Assert.isTrue(nodeCapacity > 1, "Node capacity must be greater than 1");
    this.nodeCapacity = nodeCapacity;
  }

  /**
   * Creates parent nodes, grandparent nodes, and so forth up to the root
   * node, for the data that has been inserted into the tree. Can only be
   * called once, and thus can be called only after all of the data has been
   * inserted into the tree.
   */
  public void build() {
    Assert.isTrue(!built);
    root = itemBoundables.isEmpty()
           ?createNode(0)
           :createHigherLevels(itemBoundables, -1);
    built = true;
  }

  protected abstract AbstractNode createNode(int level);

  /**
   * Sorts the childBoundables then divides them into groups of size M, where
   * M is the node capacity.
   */
  protected List createParentBoundables(List childBoundables, int newLevel) {
    Assert.isTrue(!childBoundables.isEmpty());
    ArrayList parentBoundables = new ArrayList();
    parentBoundables.add(createNode(newLevel));
    ArrayList sortedChildBoundables = new ArrayList(childBoundables);
    Collections.sort(sortedChildBoundables, getComparator());
    for (Iterator i = sortedChildBoundables.iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (lastNode(parentBoundables).getChildBoundables().size() == getNodeCapacity()) {
        parentBoundables.add(createNode(newLevel));
      }
      lastNode(parentBoundables).addChildBoundable(childBoundable);
    }
    return parentBoundables;
  }

  protected AbstractNode lastNode(List nodes) {
    return (AbstractNode) nodes.get(nodes.size() - 1);
  }

  protected int compareDoubles(double a, double b) {
    return a > b ? 1
         : a < b ? -1
         : 0;
  }

  /**
   * Creates the levels higher than the given level
   *
   * @param boundablesOfALevel
   *            the level to build on
   * @param level
   *            the level of the Boundables, or -1 if the boundables are item
   *            boundables (that is, below level 0)
   * @return the root, which may be a ParentNode or a LeafNode
   */
  private AbstractNode createHigherLevels(List boundablesOfALevel, int level) {
    Assert.isTrue(!boundablesOfALevel.isEmpty());
    List parentBoundables = createParentBoundables(boundablesOfALevel, level + 1);
    if (parentBoundables.size() == 1) {
      return (AbstractNode) parentBoundables.get(0);
    }
    return createHigherLevels(parentBoundables, level + 1);
  }

  /**
   * Returns the maximum number of child nodes that a node may have
   */
  public int getNodeCapacity() { return nodeCapacity; }

  protected int size() {
    if (!built) { build(); }
    if (itemBoundables.isEmpty()) {
      return 0;
    }
    return size(root);
  }

  protected int size(AbstractNode node)
  {
    int size = 0;
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (childBoundable instanceof AbstractNode) {
        size += size((AbstractNode) childBoundable);
      }
      else if (childBoundable instanceof ItemBoundable) {
        size += 1;
      }
    }
    return size;
  }

  protected int depth() {
    if (!built) { build(); }
    if (itemBoundables.isEmpty()) {
      return 0;
    }
    return depth(root);
  }

  protected int depth(AbstractNode node)
  {
    int maxChildDepth = 0;
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (childBoundable instanceof AbstractNode) {
        int childDepth = depth((AbstractNode) childBoundable);
        if (childDepth > maxChildDepth)
          maxChildDepth = childDepth;
      }
    }
    return maxChildDepth + 1;
  }


  protected void insert(Object bounds, Object item) {
    Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.");
    itemBoundables.add(new ItemBoundable(bounds, item));
  }

  /**
   *  Also builds the tree, if necessary.
   */
  protected List query(Object searchBounds) {
    if (!built) { build(); }
    ArrayList matches = new ArrayList();
    if (itemBoundables.isEmpty()) {
      Assert.isTrue(root.getBounds() == null);
      return matches;
    }
    if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {
      query(searchBounds, root, matches);
    }
    return matches;
  }

  /**
   *  Also builds the tree, if necessary.
   */
  protected void query(Object searchBounds, ItemVisitor visitor) {
    if (!built) { build(); }
    if (itemBoundables.isEmpty()) {
      Assert.isTrue(root.getBounds() == null);
    }
    if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {
      query(searchBounds, root, visitor);
    }
  }

  /**
   * @return a test for intersection between two bounds, necessary because subclasses
   * of AbstractSTRtree have different implementations of bounds.
   * @see IntersectsOp
   */
  protected abstract IntersectsOp getIntersectsOp();

  private void query(Object searchBounds, AbstractNode node, List matches) {
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue;
      }
      if (childBoundable instanceof AbstractNode) {
        query(searchBounds, (AbstractNode) childBoundable, matches);
      }
      else if (childBoundable instanceof ItemBoundable) {
        matches.add(((ItemBoundable)childBoundable).getItem());
      }
      else {
        Assert.shouldNeverReachHere();
      }
    }
  }

  private void query(Object searchBounds, AbstractNode node, ItemVisitor visitor) {
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue;
      }
      if (childBoundable instanceof AbstractNode) {
        query(searchBounds, (AbstractNode) childBoundable, visitor);
      }
      else if (childBoundable instanceof ItemBoundable) {
        visitor.visitItem(((ItemBoundable)childBoundable).getItem());
      }
      else {
        Assert.shouldNeverReachHere();
      }
    }
  }

  /**
   * Removes an item from the tree.
   * (Builds the tree, if necessary.)
   */
  protected boolean remove(Object searchBounds, Object item) {
    if (!built) { build(); }
    if (itemBoundables.isEmpty()) {
      Assert.isTrue(root.getBounds() == null);
    }
    if (getIntersectsOp().intersects(root.getBounds(), searchBounds)) {
      return remove(searchBounds, root, item);
    }
    return false;
  }

  private boolean removeItem(AbstractNode node, Object item)
  {
    Boundable childToRemove = null;
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (childBoundable instanceof ItemBoundable) {
        if ( ((ItemBoundable) childBoundable).getItem() == item)
          childToRemove = childBoundable;
      }
    }
    if (childToRemove != null) {
      node.getChildBoundables().remove(childToRemove);
      return true;
    }
    return false;
  }

  private boolean remove(Object searchBounds, AbstractNode node, Object item) {
    // first try removing item from this node
    boolean found = removeItem(node, item);
    if (found)
      return true;

    AbstractNode childToPrune = null;
    // next try removing item from lower nodes
    for (Iterator i = node.getChildBoundables().iterator(); i.hasNext(); ) {
      Boundable childBoundable = (Boundable) i.next();
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue;
      }
      if (childBoundable instanceof AbstractNode) {
        found = remove(searchBounds, (AbstractNode) childBoundable, item);
        // if found, record child for pruning and exit
        if (found) {
          childToPrune = (AbstractNode) childBoundable;
          break;
        }
      }
    }
    // prune child if possible
    if (childToPrune != null) {
      if (childToPrune.getChildBoundables().isEmpty()) {
        node.getChildBoundables().remove(childToPrune);
      }
    }
    return found;
  }

  protected abstract Comparator getComparator();

}
