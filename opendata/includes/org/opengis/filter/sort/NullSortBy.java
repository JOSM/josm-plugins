/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2011, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2003-2005, Open Geospatial Consortium Inc.
 *    
 *    All Rights Reserved. http://www.opengis.org/legal/
 */
package org.opengis.filter.sort;

import java.io.Serializable;

import org.opengis.filter.expression.PropertyName;


/**
 * Default implementation of {@link SortBy} as a "null object". Used for {@link SortBy} constants.
 *
 * @author Jody Garnett (Refractions Research)
 * @author Martin Desruisseaux (Geomatys)
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/opengis/src/main/java/org/opengis/filter/sort/NullSortBy.java $
 */
final class NullSortBy implements SortBy, Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -4846119001746135007L;

    /**
     * The sort order.
     */
    private final SortOrder order;

    /**
     * Creates a new Null object for the specified sort order.
     */
    NullSortBy(final SortOrder order) {
        this.order = order;
    }

    /**
     * Natural order usually associated with FID, or Key Attribtues.
     */
    public PropertyName getPropertyName() {
        return null;
    }

    /**
     * Returns the sort order.
     */
    public SortOrder getSortOrder() {
        return order;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return (int) serialVersionUID ^ order.hashCode();
    }

    /**
     * Compares this object with the specified one for equality.
     */
    @Override
    public boolean equals(final Object object) {
        if (object instanceof NullSortBy) {
            return order.equals(((NullSortBy) object).order);
        }
        return false;
    }
}
