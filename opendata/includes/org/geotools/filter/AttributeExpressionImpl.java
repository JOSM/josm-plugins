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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.factory.Hints;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.filter.expression.PropertyAccessors;
import org.geotools.util.Converters;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.expression.ExpressionVisitor;
import org.xml.sax.helpers.NamespaceSupport;


/**
 * Defines a complex filter (could also be called logical filter). This filter
 * holds one or more filters together and relates them logically in an
 * internally defined manner.
 *
 * @author Rob Hranac, TOPP
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/main/src/main/java/org/geotools/filter/AttributeExpressionImpl.java $
 * @version $Id: AttributeExpressionImpl.java 37298 2011-05-25 05:16:15Z mbedward $
 */
public class AttributeExpressionImpl extends DefaultExpression
    implements AttributeExpression {
            
    /** The logger for the default core module. */
    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geotools.core");

    /** Holds all sub filters of this filter. */
    protected String attPath;

    /** Used to validate attribute references to ensure they match the provided schema */
    protected SimpleFeatureType schema = null;
    
    /** NamespaceSupport used to defining the prefix information for the xpath expression */
    NamespaceSupport namespaceSupport;
    
    /**
     * Configures whether evaluate should return null if it cannot find a working
     * property accessor, rather than throwing an exception (default behaviour).
     * */
    protected boolean lenient = true;
    
    /**
     * Hints passed to the property accessor gathering up additional context information
     * used during evaluation.
     */
    private Hints hints;

    /**
     * Constructor with schema and path to the attribute.
     * 
     * @param xpath the String xpath to the attribute.
     */
    public AttributeExpressionImpl( String xpath ){
        this.attPath = xpath;
        this.schema = null;
        this.namespaceSupport = null;
        this.hints = null;
        this.expressionType = ATTRIBUTE;
    }
        
    /**
     * Constructor with schema and path to the attribute.
     * 
     * @param xpath the String xpath to the attribute.
     * @param namespaceContext Defining the prefix information for the xpath expression 
     */
    public AttributeExpressionImpl( String xpath, NamespaceSupport namespaceContext ){
        attPath = xpath;
        schema = null;
        this.namespaceSupport = namespaceContext;
        this.expressionType = ATTRIBUTE;
    }   
        
    public NamespaceSupport getNamespaceContext() {
        return namespaceSupport;
    }
    
    /**
     * This method calls {@link #getPropertyName()}.
     * 
     * @deprecated use {@link #getPropertyName()}
     */
    public final String getAttributePath() {
      	return getPropertyName();
    }

    /**
     * Gets the path to the attribute to be evaluated by this expression.
     *
     * {@link org.opengis.filter.expression.PropertyName#getPropertyName()}
     */
   public String getPropertyName() {
		return attPath;
   }	
    
    /**
      * Gets the value of this attribute from the passed feature.
      *
      * @param feature Feature from which to extract attribute value.
      */
    public Object evaluate(SimpleFeature feature) {
       //NC - is exact copy of code anyway, don't need to keep changing both
       //this method can probably be removed all together
        return evaluate(feature, null); 
       
    }
  
    /**
     * Gets the value of this property from the passed object.
     *
     * @param obj Object from which we need to extract a property value.
     */
   public Object evaluate(Object obj) {
        return evaluate(obj, null);
   }
   
   
   /**
    * Gets the value of this attribute from the passed object.
    *
    * @param obj Object from which to extract attribute value.
    * @param target Target Class 
    */
    public Object evaluate(Object obj, Class target) {
        // NC- new method

        PropertyAccessor accessor = getLastPropertyAccessor();
        AtomicReference<Object> value = new AtomicReference<Object>();
        AtomicReference<Exception> e = new AtomicReference<Exception>();

        if (accessor == null || !accessor.canHandle(obj, attPath, target)
                || !tryAccessor(accessor, obj, target, value, e)) {
            boolean success = false;
            if( namespaceSupport != null && hints == null ){
                hints = new Hints(PropertyAccessorFactory.NAMESPACE_CONTEXT, namespaceSupport);
            }
            List<PropertyAccessor> accessors = PropertyAccessors.findPropertyAccessors(obj,
                    attPath, target, hints );

            if (accessors != null) {
                Iterator<PropertyAccessor> it = accessors.iterator();
                while (!success && it.hasNext()) {
                    accessor = it.next();
                    success = tryAccessor(accessor, obj, target, value, e);
                }

            }

            if (!success) {
                if (lenient) return null;
                else throw new IllegalArgumentException(
                        "Could not find working property accessor for attribute (" + attPath
                                + ") in object (" + obj + ")", e.get());
            } else {
                setLastPropertyAccessor(accessor);
            }

        }

        if (target == null) {
            return value.get();
        }

        return Converters.convert(value.get(), target);

    }

    // NC - helper method for evaluation - attempt to use property accessor
    private boolean tryAccessor(PropertyAccessor accessor, Object obj, Class target,
            AtomicReference<Object> value, AtomicReference<Exception> ex) {
        try {
            value.set(accessor.get(obj, attPath, target));
            return true;
        } catch (Exception e) {
            ex.set(e);
            return false;
        }

    }

    // accessor caching, scanning the registry every time is really very expensive
    private PropertyAccessor lastAccessor;

    private synchronized PropertyAccessor getLastPropertyAccessor() {
        return lastAccessor;
    }

    private synchronized void setLastPropertyAccessor(PropertyAccessor accessor) {
        lastAccessor = accessor;
    }
   
     /**
     * Return this expression as a string.
     *
     * @return String representation of this attribute expression.
     */
    public String toString() {
        return attPath;
    }

    /**
     * Compares this filter to the specified object.  Returns true  if the
     * passed in object is the same as this expression.  Checks  to make sure
     * the expression types are the same as well as  the attribute paths and
     * schemas.
     *
     * @param obj - the object to compare this ExpressionAttribute against.
     *
     * @return true if specified object is equal to this filter; else false
     */
    public boolean equals(Object obj) {
        if(obj == null)
            return false;
        
        if (obj.getClass() == this.getClass()) {
            AttributeExpressionImpl expAttr = (AttributeExpressionImpl) obj;

            boolean isEqual = (expAttr.getType() == this.expressionType);
            if(LOGGER.isLoggable(Level.FINEST))
                LOGGER.finest("expression type match:" + isEqual + "; in:"
                + expAttr.getType() + "; out:" + this.expressionType);
            isEqual = (expAttr.attPath != null)
                ? (isEqual && expAttr.attPath.equals(this.attPath))
                : (isEqual && (this.attPath == null));
            if(LOGGER.isLoggable(Level.FINEST))
                LOGGER.finest("attribute match:" + isEqual + "; in:"
                + expAttr.getAttributePath() + "; out:" + this.attPath);
            isEqual = (expAttr.schema != null)
                ? (isEqual && expAttr.schema.equals(this.schema))
                : (isEqual && (this.schema == null));
            if(LOGGER.isLoggable(Level.FINEST))
                LOGGER.finest("schema match:" + isEqual + "; in:" + expAttr.schema
                + "; out:" + this.schema);

            return isEqual;
        } else {
            return false;
        }
    }

    /**
     * Override of hashCode method.
     *
     * @return a code to hash this object by.
     */
    public int hashCode() {
        int result = 17;
        result = (37 * result) + (attPath == null ? 0 : attPath.hashCode());
        result = (37 * result) + (schema == null ? 0 : schema.hashCode());
        return result;
    }

    /**
     * Used by FilterVisitors to perform some action on this filter instance.
     * Typicaly used by Filter decoders, but may also be used by any thing
     * which needs infomration from filter structure. Implementations should
     * always call: visitor.visit(this); It is importatant that this is not
     * left to a parent class unless the  parents API is identical.
     *
     * @param visitor The visitor which requires access to this filter, the
     *        method must call visitor.visit(this);
     */
    public Object accept(ExpressionVisitor visitor, Object extraData) {
    	return visitor.visit(this,extraData);
    }
}
