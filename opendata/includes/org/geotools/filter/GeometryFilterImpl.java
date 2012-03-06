/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
 *        
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.filter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;


/**
 * Implements a geometry filter.
 * 
 * <p>
 * This filter implements a relationship - of some sort -  between two geometry
 * expressions. Note that this comparison does not attempt to restict its
 * expressions to be meaningful.  This means that it considers itself a valid
 * filter as long as it contains two <b>geometry</b> sub-expressions. It is
 * also slightly less  restrictive than the OGC Filter specification because
 * it does not require that one sub-expression be an geometry attribute and
 * the other be a geometry literal.
 * </p>
 * 
 * <p>
 * In other words, you may use this filter to compare two geometries in the
 * same feature, such as: attributeA inside attributeB?  You may also compare
 * two literal geometries, although this is fairly meaningless, since it could
 * be reduced (ie. it is always either true or false).  This approach is very
 * similar to that taken in the FilterCompare class.
 * </p>
 *
 * @author Rob Hranac, TOPP
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/main/src/main/java/org/geotools/filter/GeometryFilterImpl.java $
 * @version $Id: GeometryFilterImpl.java 37298 2011-05-25 05:16:15Z mbedward $
 *
 * @task REVISIT: make this class (and all filters) immutable, implement
 *       cloneable and return new filters when calling addLeftGeometry and
 *       addRightG Issues to think through: would be cleaner immutability to
 *       have constructor called with left and right Geometries, but this does
 *       not jive with SAX parsing, which is one of the biggest uses of
 *       filters.  But the alternative is not incredibly efficient either, as
 *       there will be two filters that  are just thrown away every time we
 *       make a full geometry filter.  These issues extend to most filters, as
 *       just about all of them are mutable when creating them.  Other issue
 *       is that lots of code will need to  be changed for immutability.
 *       (comments by cholmes) - MUTABLE FACTORIES!  Sax and immutability.
 */
public abstract class GeometryFilterImpl extends BinaryComparisonAbstract
    implements GeometryFilter {
    /** Class logger */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.filter");
    
    protected GeometryFilterImpl(org.opengis.filter.FilterFactory factory,org.opengis.filter.expression.Expression e1,org.opengis.filter.expression.Expression e2) {
        super(factory,e1,e2);
    }
    
    public void setExpression1(org.opengis.filter.expression.Expression expression) {
        if (expression instanceof Expression) {
            Expression leftGeometry = (Expression) expression;

            // Checks if this is geometry filter or not and handles appropriately
            if (DefaultExpression.isGeometryExpression(leftGeometry.getType())
                    || permissiveConstruction) {
                super.setExpression1(leftGeometry);
            } else {
                throw new IllegalFilterException("Attempted to add (left)"
                        + " non-geometry expression" + " to geometry filter.");
            }
        } else {
            // I guess we assume it is a good expression...
            super.setExpression1(expression);
        }

    }
  
    public void setExpression2(org.opengis.filter.expression.Expression expression) {
        if (expression instanceof Expression) {
            Expression rightGeometry = (Expression) expression;

            // Checks if this is math filter or not and handles appropriately
            if (DefaultExpression.isGeometryExpression(rightGeometry.getType())
                    || permissiveConstruction) {
                super.setExpression2(rightGeometry);
            } else {
                throw new IllegalFilterException("Attempted to add (right)" + " non-geometry"
                        + "expression to geometry filter.");
            }
        } else {
            // I guess we assume it is a good expression...
            super.setExpression2(expression);
        }
    }

    /**
     * Subclass convenience method for returning left expression as a 
     * JTS geometry.
     */
    protected Geometry getLeftGeometry(Object feature) {
        org.opengis.filter.expression.Expression leftGeometry = getExpression1();
        
         if (leftGeometry != null) {
             Object obj = leftGeometry.evaluate(feature,Geometry.class);

             //LOGGER.finer("leftGeom = " + o.toString()); 
             return (Geometry) obj;
         } else if (feature instanceof SimpleFeature) {
             return (Geometry) ((SimpleFeature)feature).getDefaultGeometry();
         }
         return null;
    }
    
    /**
     * Subclass convenience method for returning right expression as a 
     * JTS geometry.
     */
    protected Geometry getRightGeometry(Object feature) {
        org.opengis.filter.expression.Expression rightGeometry = getExpression2();
        
         if (rightGeometry != null) {
             return rightGeometry.evaluate(feature,Geometry.class);
         } else if(feature instanceof SimpleFeature){
             return (Geometry) ((SimpleFeature)feature).getDefaultGeometry();
         }
         return null;
    }
    
    /**
     * Determines whether or not a given feature is 'inside' this filter.
     *
     * @param feature Specified feature to examine.
     *
     * @return Flag confirming whether or not this feature is inside filter.
     */
    public boolean evaluate(SimpleFeature feature) {
        return evaluate((Object)feature);
    }

    /**
     * Return this filter as a string.
     *
     * @return String representation of this geometry filter.
     */
    public String toString() {
        String operator = null;

        // Handles all normal geometry cases
        if (filterType == GEOMETRY_EQUALS) {
            operator = " equals ";
        } else if (filterType == GEOMETRY_DISJOINT) {
            operator = " disjoint ";
        } else if (filterType == GEOMETRY_INTERSECTS) {
            operator = " intersects ";
        } else if (filterType == GEOMETRY_CROSSES) {
            operator = " crosses ";
        } else if (filterType == GEOMETRY_WITHIN) {
            operator = " within ";
        } else if (filterType == GEOMETRY_CONTAINS) {
            operator = " contains ";
        } else if (filterType == GEOMETRY_OVERLAPS) {
            operator = " overlaps ";
        } else if (filterType == GEOMETRY_BEYOND) {
            operator = " beyond ";
        } else if (filterType == GEOMETRY_BBOX) {
            operator = " bbox ";
        }

        org.opengis.filter.expression.Expression leftGeometry = getExpression1();
        org.opengis.filter.expression.Expression rightGeometry = getExpression2();
        
        if ((expression1 == null) && (rightGeometry == null)) {
            return "[ " + "null" + operator + "null" + " ]";
        } else if (leftGeometry == null) {
            return "[ " + "null" + operator + rightGeometry.toString() + " ]";
        } else if (rightGeometry == null) {
            return "[ " + leftGeometry.toString() + operator + "null" + " ]";
        }

        return "[ " + leftGeometry.toString() + operator
        + rightGeometry.toString() + " ]";
    }

    /**
     * Compares this filter to the specified object.  Returns true  if the
     * passed in object is the same as this filter.  Checks  to make sure the
     * filter types are the same as well as the left and right geometries.
     *
     * @param obj - the object to compare this GeometryFilter against.
     *
     * @return true if specified object is equal to this filter; else false
     */
    public boolean equals(Object obj) {
        if (obj instanceof GeometryFilterImpl) {
            GeometryFilterImpl geomFilter = (GeometryFilterImpl) obj;
            boolean isEqual = true;

            isEqual = geomFilter.getFilterType() == this.filterType;
            if( LOGGER.isLoggable(Level.FINEST) ) {
                LOGGER.finest("filter type match:" + isEqual + "; in:"
                        + geomFilter.getFilterType() + "; out:" + this.filterType);
            }
            isEqual = (geomFilter.expression1 != null)
                ? (isEqual && geomFilter.expression1.equals(this.expression1))
                : (isEqual && (this.expression1 == null));
            if( LOGGER.isLoggable(Level.FINEST) ) {
                LOGGER.finest("left geom match:" + isEqual + "; in:"
                        + geomFilter.expression1 + "; out:" + this.expression1);
            }
            isEqual = (geomFilter.expression2 != null)
                ? (isEqual
                && geomFilter.expression2.equals(this.expression2))
                : (isEqual && (this.expression2 == null));
            if( LOGGER.isLoggable(Level.FINEST) ) {
                LOGGER.finest("right geom match:" + isEqual + "; in:"
                        + geomFilter.expression2 + "; out:" + this.expression2);
            }
            return isEqual;
        } else {
            return false;
        }
    }

    /**
     * Override of hashCode method.
     *
     * @return a hash code value for this geometry filter.
     */
    public int hashCode() {
        org.opengis.filter.expression.Expression leftGeometry = getExpression1();
        org.opengis.filter.expression.Expression rightGeometry = getExpression2();
         
        int result = 17;
        result = (37 * result) + filterType;
        result = (37 * result)
            + ((leftGeometry == null) ? 0 : leftGeometry.hashCode());
        result = (37 * result)
            + ((rightGeometry == null) ? 0 : rightGeometry.hashCode());

        return result;
    }

}
