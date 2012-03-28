

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
 * Filters out all shapes except the one with the top score. Not needed
 * if you are using OneToOneFCMatchFinder.
 */
public class TopScoreFilter implements FeatureMatcher {

  public TopScoreFilter() {
  }

  /**
   * Finds the candidate with the highest score. If several candidates share
   * the top score, only one of them will be returned.
   * @param target ignored
   * @param candidates a Matches object created by another FeatureMatcher
   * @return the candidate having the greatest score. The scores is
   * preserved from the original Matches object.
   */
    @Override
  public Matches match(Feature target, FeatureCollection candidates) {
    Matches survivors = new Matches(candidates.getFeatureSchema());
    Feature survivor = null;
    double topScore = 0;
    Matches matches = (Matches) candidates;
    for (int i = 0; i < matches.size(); i++) {
      if (matches.getScore(i) >= topScore) {
        topScore = matches.getScore(i);
        survivor = matches.getFeature(i);
      }
    }
    if (survivor != null) {
      survivors.add(survivor, topScore);
    }
    return survivors;
  }
}
