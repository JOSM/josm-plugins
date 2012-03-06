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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.expression.PropertyName;



/**
 * A simple visitor that extracts every attribute used by a filter or an expression
 * <p>
 * Access to this class is available via:
 * <ul>
 * <li>DataUtilities.attributeNames( Filter )</li>
 * <li>DataUtilities.attributeNames( Filter, FeatureType )</li>
 * <li>DataUtilities.attributeNames( Expression )</li>
 * <li>DataUtilities.attributeNames( Expression, FeatureType )</li>
 * </ul>
 * 
 * The class can also be used to determine if an expression is "static", that is, 
 * despite a complex structure does not use attribute or volatile functions, and can
 * be thus replaced by a constant: for this use case refer to the 
 * {@link #isConstantExpression()} method
 * 
 * @author Andrea Aime - GeoSolutions
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/main/src/main/java/org/geotools/filter/FilterAttributeExtractor.java $
 */
public class FilterAttributeExtractor extends DefaultFilterVisitor {
    /** Last set visited */
    protected Set<String> attributeNames = new HashSet<String>();

    /** feature type to evaluate against */
    protected SimpleFeatureType featureType;

    /**
     * Just extract the property names; don't check against a feature type.
     */
    public FilterAttributeExtractor() {
        this(null);
    }
    /**
     * Use the provided feature type as a sanity check when extracting
     * property names.
     * 
     * @param featureType
     */
    public FilterAttributeExtractor(SimpleFeatureType featureType) {
        this.featureType = featureType;
    }
    /**
     * DOCUMENT ME!
     *
     * @return an unmofiable set of the attribute names found so far during the visit
     */
    public Set<String> getAttributeNameSet() {
        return Collections.unmodifiableSet(attributeNames);
    }

    /**
     * DOCUMENT ME!
     *
     * @return an array of the attribute names found so far during the visit
     */
    public String[] getAttributeNames() {
        return attributeNames.toArray(new String[attributeNames.size()]);
    }

    public Object visit( PropertyName expression, Object data ) {
        if( data != null && data != attributeNames ){
            attributeNames = (Set<String>) data;
        }
        if (featureType != null) {
            //evaluate against the feature type instead of using straight name
            // since the path from the property name may be an xpath or a 
            // namespace prefixed string
            AttributeDescriptor type = (AttributeDescriptor) expression.evaluate( featureType );
            if ( type != null ) {
               attributeNames.add( type.getLocalName() );
            }
            else {
               attributeNames.add( expression.getPropertyName() );
            }
        }
        else {
            attributeNames.add( expression.getPropertyName() );
        }

        return attributeNames;
    }
}
