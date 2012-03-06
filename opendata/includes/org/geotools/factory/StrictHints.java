/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.factory;



/**
 * Hints which should not be merged with global hints, usually because the global hints have
 * already been merged.
 *
 * @since 2.4
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/StrictHints.java $
 * @version $Id: StrictHints.java 30640 2008-06-12 17:34:32Z acuster $
 * @author Martin Desruisseaux
 */
class StrictHints extends Hints {
    /**
     * Creates a set of strict hints which is a copy of the specified hints.
     */
    public StrictHints(final Hints hints) {
        super(hints);
    }
}
