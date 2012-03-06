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
package org.geotools.filter.visitor;

import java.util.logging.Logger;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.filter.And;
import org.opengis.filter.ExcludeFilter;
import org.opengis.filter.Filter;
import org.opengis.filter.Id;
import org.opengis.filter.IncludeFilter;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.expression.Add;
import org.opengis.filter.expression.Divide;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.Multiply;
import org.opengis.filter.expression.NilExpression;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.expression.Subtract;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Extract a maximal envelope from the provided Filter.
 * <p>
 * The maximal envelope is generated from:
 * <ul>
 * <li>all the literal geometry instances involved if spatial operations - using
 * geom.getEnvelopeInternal().
 * <li>Filter.EXCLUDES will result in an empty envelope
 * <li>Filter.INCLUDES will result in a "world" envelope with range Double.NEGATIVE_INFINITY to
 * Double.POSITIVE_INFINITY for each axis.
 * <li>all other non spatial filters will result in a world envelope
 * <li>combinations in and will return the intersection of the envelopes, or an empty envelope
 *     if an exclude is in the mix, or null if the and is mixing non spatial filters</li>
 * <li>combinations in or will return the intersection of 
 * </ul>
 * Since geometry literals do not contains CRS information we can only produce a ReferencedEnvelope
 * without CRS information. You can call this function with an existing ReferencedEnvelope 
 * or with your data CRS to correct for this limitation.
 * ReferencedEnvelope example:<pre><code>
 * ReferencedEnvelope bbox = (ReferencedEnvelope)
 *     filter.accepts(new ExtractBoundsFilterVisitor(), dataCRS );
 * </code></pre>
 * You can also call this function with an existing Envelope; if you are building up bounds based on
 * several filters.
 * <p>
 * This is a replacement for FilterConsumer.
 * 
 * @author Jody Garnett
 *
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/main/src/main/java/org/geotools/filter/visitor/ExtractBoundsFilterVisitor.java $
 */
public class ExtractBoundsFilterVisitor extends NullFilterVisitor {
    static public NullFilterVisitor BOUNDS_VISITOR = new ExtractBoundsFilterVisitor();
    
    private static Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger("org.geotools.index.rtree");

    /**
     * This FilterVisitor is stateless - use ExtractBoundsFilterVisitor.BOUNDS_VISITOR.
     * <p>
     * You may also subclass in order to reuse this functionality in your own
     * FilterVisitor implementation.
     */
    protected ExtractBoundsFilterVisitor(){        
    }
    
    /**
     * Produce an ReferencedEnvelope from the provided data parameter.
     * 
     * @param data
     * @return ReferencedEnvelope
     */
    private ReferencedEnvelope bbox( Object data ) {
        if( data == null ){
            return null;
        }
        else if (data instanceof ReferencedEnvelope) {
            return (ReferencedEnvelope) data;
        }
        else if (data instanceof Envelope){
            return new ReferencedEnvelope( (Envelope) data, null );
        }
        else if (data instanceof CoordinateReferenceSystem){
            return new ReferencedEnvelope( (CoordinateReferenceSystem) data );
        }
        throw new ClassCastException("Could not cast data to ReferencedEnvelope");        
    }

    public Object visit( ExcludeFilter filter, Object data ) {
        return new Envelope();
    }

    public Object visit( IncludeFilter filter, Object data ) {
        return infinity();
    }

	Envelope infinity() {
		return new Envelope(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

    /**
     * Please note we are only visiting literals involved in spatial operations.
     * @param literal, hopefully a Geometry or Envelope
     * @param data Incoming BoundingBox (or Envelope or CRS)
     * 
     * @return ReferencedEnvelope updated to reflect literal
     */
    public Object visit( Literal expression, Object data ) {        
        ReferencedEnvelope bbox = bbox( data );

        Object value = expression.getValue();
        if (value instanceof Geometry) {
                        
            Geometry geometry = (Geometry) value;
            Envelope bounds = geometry.getEnvelopeInternal();
            
            if(bbox != null) {
            	bbox.expandToInclude(bounds);
            	return bbox;
            } else {
            	return bbox(bounds);
            }
        } else {
            LOGGER.finer("LiteralExpression ignored!");
        }
        return bbox;
    }
    
	@Override
	public Object visit(And filter, Object data) {
		Envelope mixed = infinity();
		for (Filter f : filter.getChildren()) {
			Envelope env = (Envelope) f.accept(this, data);
    		mixed = mixed.intersection(env);
		}
		return mixed;
	}
	
	public Object visit(Not filter, Object data) {
		// no matter what we have to return an infinite envelope
		// rationale
		// !(finite envelope) -> an unbounded area -> infinite
		// !(non spatial filter) -> infinite (no spatial concern)
		// !(infinite) -> ... infinite, as the first infinite could be the result 
		// of !(finite envelope) 
		
		return infinity();
	}
	
	@Override
	public Object visit(Or filter, Object data) {
		Envelope mixed = new Envelope();
		for (Filter f : filter.getChildren()) {
			Envelope env = (Envelope) f.accept(this, data);
		    mixed.expandToInclude(env);
		}
		return mixed;
	}

	@Override
	public Object visit(Add expression, Object data) {
		return infinity();
	}

	@Override
	public Object visit(Divide expression, Object data) {
		return infinity();
	}

	@Override
	public Object visit(Function expression, Object data) {
		return infinity();
	}

	@Override
	public Object visit(Id filter, Object data) {
		return infinity();
	}

	@Override
	public Object visit(Multiply expression, Object data) {
		return infinity();
	}

	@Override
	public Object visit(NilExpression expression, Object data) {
		return infinity();
	}

	@Override
	public Object visit(PropertyName expression, Object data) {
		return null;
	}

	@Override
	public Object visit(Subtract expression, Object data) {
		return infinity();
	}

	@Override
	public Object visitNullFilter(Object data) {
		return infinity();
	}
}
