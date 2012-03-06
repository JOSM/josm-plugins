/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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

import java.util.Iterator;


/**
 * Provides iterators over factories of specified categories. Users shall
 * {@linkplain Factories#addFactoryIteratorProvider register} an implementation
 * of this interface when the default lookup mechanism (namely scanning the content of the
 * <code>META-INF/services/</code><var>category</var> file in every JARs found on the classpath)
 * can not work. Such need may appear in the context of {@linkplain ClassLoader class loaders}
 * restricting access to non-package directories as {@code META-INF}. This constraint occurs on
 * the Eclipse platform for instance.
 *
 * @since 2.4
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/FactoryIteratorProvider.java $
 * @version $Id: FactoryIteratorProvider.java 37298 2011-05-25 05:16:15Z mbedward $
 * @author Martin Desruisseaux
 *
 * @see FactoryRegistry#addFactoryIteratorProvider
 * @see CommonFactory#addFactoryIteratorProvider
 */
public interface FactoryIteratorProvider {
    /**
     * Returns an iterator over all {@linkplain Factory factories} of the specified category.
     * The {@code category} argument should be the interface class to be implemented, not the
     * actual implementation.
     *
     * @param  category The category for the factories to be returned.
     * @return Factories that implement the specified category.
     */
    <T> Iterator<T> iterator(final Class<T> category);
}
