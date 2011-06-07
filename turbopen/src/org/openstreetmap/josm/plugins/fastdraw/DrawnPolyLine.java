package org.openstreetmap.josm.plugins.fastdraw;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView;

public class DrawnPolyLine {
    MapView mv;
    private LinkedList<LatLon> points = new LinkedList<LatLon>();
    private LinkedList<LatLon> simplePoints = new LinkedList<LatLon>();
    private Set<LatLon> used;
    private Set<LatLon> fixed = new HashSet<LatLon>();
    
    private int lastIdx;

    public void setMv(MapView mv) {
        this.mv = mv;
    }

    boolean isFixed(LatLon pp2) {
        return fixed.contains(pp2);
    }

    LinkedList<LatLon> getPoints() {
        if (simplePoints!=null) return simplePoints; else return points;
    }

    boolean wasSimplified() {
        return (simplePoints!=null && simplePoints.size()>0);
    }
    
    int findClosestPoint(Point p, double d) {
        double x=p.x, y=p.y;
        int n=points.size();
        int idx=-1;
        double dist,minD=1e10;
        for (int i=0;i<n;i++) {
            dist = Math.sqrt(getPoint(points.get(i)).distanceSq(x,y));
            if (dist<d && dist<minD) {
                idx=i;
                minD=dist;
            };
        }
        return idx;
    }

    void clear() {
        points.clear();
        used=null;
        lastIdx=0;
        fixed.clear();
        simplePoints=null;
    }

    void undo() {
        //if (points.size() > 0) points.removeLast();
        if (lastIdx>0 && lastIdx<points.size()){
        points.remove(lastIdx);
        lastIdx--;
        }
        
    }
    
    void fixPoint(LatLon p) {
        fixed.add(p);
    }

    void addFixed(LatLon coor) {
         addLast(coor);
        fixed.add(coor);
    }
    void addLast(LatLon coor) {
        if (lastIdx>=points.size()-1) {
            //System.out.println("add last "+points.size());
            points.addLast(coor);if (points.size()>1) lastIdx++;
            //System.out.println("lastIdx="+lastIdx);
            }
        else {points.add(lastIdx+1, coor); lastIdx++; 
            //System.out.println("add at "+lastIdx+"/"+points.size());
        }
    }
  
    Point getLastPoint() {
        return getPoint(points.get(lastIdx));
    }

    Point getPoint(LatLon p) {
        return mv.getPoint(p);
    }
    
    /**
     * Simplified drawn line, not touching the nodes includes in "fixed" set.
     */
    void simplify(double epsilon) {
        //System.out.println("Simplify polyline...");
        int n = points.size();
        if (n < 3) return;
        used = new HashSet<LatLon>(n);
        int start = 0;
        for (int i = 0; i < n; i++) {
            LatLon p = points.get(i);
            if (fixed.contains(p) || i == n - 1) {
                if (start < 0) {
                    start = i;
                } else {
                    douglasPeucker(start, i, epsilon, 0);
                }
            }
        }
        simplePoints = new LinkedList<LatLon>();
        simplePoints.addAll(points);
        simplePoints.retainAll(used);
        //Main.map.mapView.repaint();
        used = null;
    }

    /**
     * Simplification of the line specified by "points" field.
     * Remainin points are included to "used" set.
     * @param start - starting index
     * @param end - ending index
     * @param epsilon - min point-to line distance in pixels (tolerance)
     * @param depth - recursion level
     */
    private void douglasPeucker(int start, int end, double epsilon, int depth) {
        if (depth > 500) return;
        if (end - start < 1) return; // incorrect invocation
        LatLon first = points.get(start);
        LatLon last = points.get(end);
        Point firstp = getPoint(first);
        Point lastp = getPoint(last);
        used.add(first);
        used.add(last);

        if (end - start < 2) return;
        
        int farthest_node = -1;
        double farthest_dist = 0;

        double d = 0;

        for (int i = start + 1; i < end; i++) {
            d = pointLineDistance(getPoint(points.get(i)), firstp, lastp);
            if (d > farthest_dist) {
                farthest_dist = d;
                farthest_node = i;
            }
        }

        if (farthest_dist > epsilon) {
            douglasPeucker(start, farthest_node, epsilon, depth + 1);
            douglasPeucker(farthest_node, end, epsilon, depth + 1);
        }
    }

    /** Modfified funclion from LakeWalker
     * Gets distance from point p1 to line p2-p3
     */
    public double pointLineDistance(Point p1, Point p2, Point p3) {
        double x0 = p1.x;        double y0 = p1.y;
        double x1 = p2.x;        double y1 = p2.y;
        double x2 = p3.x;        double y2 = p3.y;
        if (x2 == x1 && y2 == y1) {
            return Math.hypot(x1 - x0, y1 - y0);
        } else {
            return Math.abs((x2-x1)*(y1-y0)-(x1-x0)*(y2-y1))/Math.hypot(x2 - x1,y2 - y1);
        }
    }

    void closeLine() {
        points.add(points.getFirst());
    }
    
    void deleteNode(int idx) {
        if (idx<=lastIdx) lastIdx--;
        fixed.remove(points.get(idx));
        points.remove(idx); 
    }
    void tryToDeleteSegment(Point p) {
        if (points.size()<3) return;
        
        LatLon start;
        start = findBigSegment(p);
        ListIterator<LatLon> it= points.listIterator();
        LatLon pp;
        boolean f=false;
        int i=0,idx=-1;
        while (it.hasNext()) {
            pp=it.next();
            if (f &&(fixed.contains(pp))) {
                // if end of line fragment reached
                lastIdx=idx;
                return;
            }if (f &&(!it.hasNext())) {
                // if end of whole line reached
                it.remove();
                lastIdx=points.size()-1;
                return;
            }
            
            // if we are deleting this segment
            if (f) it.remove(); 
            if (pp == start) {f=true;idx=i;} // next node should be removed
            i++;
        }
        lastIdx=points.size()-1;
        }

    /** find starting point of the polyline line fragment close to p
     *  line fragment = segments between two fixed (green) nodes
     * @param p
     * @return 
     */
    LatLon findBigSegment(Point p) {
        if (points.size()<2) return null;
        Iterator<LatLon> it1 = points.listIterator(0);
        Iterator<LatLon> it2 = points.listIterator(1);
        Point p1,p2;
        LatLon pp1,pp2,start=null;
        start=points.getFirst();
        do {
        pp1=it1.next();
        pp2=it2.next();
        p1 = getPoint(pp1);
        p2 = getPoint(pp2);
        // maintain segment start end end
        if (fixed.contains(pp1) ) { start=pp1; }
        if (pointSegmentDistance(p,p1,p2) < 5) {
            return start;
        } 
        } while (it2.hasNext());
        return null;
        
    }

    private double pointSegmentDistance(Point p, Point p1, Point p2) {
        double a,b,x,y,l,h,kt,kn,dist;
        x=p.x-p1.x; y=p.y-p1.y; 
        a=p2.x-p1.x; b=p2.y-p1.y;
        l=Math.hypot(a,b);
        if (l==0) return Math.hypot(x, y); // p1 = p2
        kt=(x*a+y*b)/l;
        kn=Math.abs((-x*b+y*a)/l);
        if (kt>=0 && kt<l) dist=kn; else {
            dist=Math.min(Math.hypot(x, y), Math.hypot(x-a, y-b));
        }
        return dist;
    }

    void clearSimplifiedVersion() {
        simplePoints=null;
    }

    boolean isLastPoint(int i) {
        return (lastIdx==i);
    }

    void moveToTheEnd() {
        lastIdx=points.size()-1;
    }

    void toggleFixed(int idx) {
        LatLon p = points.get(idx);
        if (fixed.contains(p)) fixed.remove(p);
        else fixed.add(p);
    }

}
