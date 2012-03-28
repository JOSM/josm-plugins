

/*
 * The JCS Conflation Suite (JCS) is a library of Java classes that
 * can be used to build automated or semi-automated conflation solutions.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jcs.conflate.polygonmatch;

import com.vividsolutions.jump.feature.Feature;
import com.vividsolutions.jump.feature.FeatureCollection;

/**
 * An algorithm for finding potential matches for a feature from a collection
 * of candidate features. To facilitate specification using an XML file in the
 * future:
 * <UL>
 *   <LI>there should be a constructor that takes no parameters
 *   <LI>when the 0-parameter constructor is used, initialization should be
 *       done using setter methods
 *   <LI>composite FeatureMatchers should have an #add(FeatureMatcher) method
 * </UL>
 */
public interface FeatureMatcher {

  /**
   * Searches a collection of candidate features for those that match the given
   * target feature.
   * @param target the feature to match
   * @param candidates the features to search for matches
   * @return the matching features, and a score for each. (Implementors should
   * document how they do their scoring).
   */
  public Matches match(Feature target, FeatureCollection candidates);

}
