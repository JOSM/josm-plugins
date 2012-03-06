package com.vividsolutions.jts.operation.predicate;

import java.util.*;
import com.vividsolutions.jts.algorithm.*;
import com.vividsolutions.jts.geom.*;

/**
 * Tests if any of the segments in a
 * {@link LineString} intersect any segment in a set of {@link LineString}s.
 * <p>
 * The algorithm is optimized for use when the first input has smaller extent
 * than the set of test lines.
 * The code is short-circuited to return as soon an intersection is found.
 *
 * @version 1.7
 */
public class SegmentIntersectionTester 
{
  // for intersection testing, don't need to set precision model
  private LineIntersector li = new RobustLineIntersector();

  private boolean hasIntersection = false;
  private Coordinate pt00 = new Coordinate();
  private Coordinate pt01 = new Coordinate();
  private Coordinate pt10 = new Coordinate();
  private Coordinate pt11 = new Coordinate();

  public SegmentIntersectionTester() {
  }

  public boolean hasIntersectionWithLineStrings(LineString line, List lines)
  {
    for (Iterator i = lines.iterator(); i.hasNext(); ) {
      LineString testLine = (LineString) i.next();
      //hasIntersection(line, testLine);
      hasIntersectionWithEnvelopeFilter(line, testLine);
      if (hasIntersection)
        break;
    }
    return hasIntersection;
  }

  /**
   * Tests the segments of a LineString against the segs in 
   * another LineString for intersection.
   * Uses the envelope of the query LineString
   * to filter before testing segments directly.
   * This is optimized for the case when the query
   * LineString is a rectangle.
   * 
   * Testing shows this is somewhat faster than not checking the envelope.
   * 
   * @param line
   * @param testLine
   * @return
   */
  private boolean hasIntersectionWithEnvelopeFilter(LineString line, LineString testLine) {
    CoordinateSequence seq0 = line.getCoordinateSequence();
    CoordinateSequence seq1 = testLine.getCoordinateSequence();
    Envelope lineEnv = line.getEnvelopeInternal();
    
    for (int i = 1; i < seq1.size() && ! hasIntersection; i++) {
      seq1.getCoordinate(i - 1, pt10);
      seq1.getCoordinate(i, pt11);
      
      // skip test if segment does not intersect query envelope
      if (! lineEnv.intersects(new Envelope(pt10, pt11)))
        continue;
      
      for (int j = 1; j < seq0.size() && ! hasIntersection; j++) {
        seq0.getCoordinate(j - 1, pt00);
        seq0.getCoordinate(j, pt01);

        li.computeIntersection(pt00, pt01, pt10, pt11);
        if (li.hasIntersection())
          hasIntersection = true;
      }
    }
    return hasIntersection;
  }
}