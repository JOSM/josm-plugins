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
package org.geotools.data;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.geotools.factory.Factory;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.util.InternationalString;


/**
 * Constructs a live DataAccess from a set of connection parameters.
 * <p>
 * The following example shows how a user might connect to a PostGIS database,
 * and maintain the resulting datastore in a Registry:
 * </p>
 *
 * <p>
 * <pre><code>
 * HashMap params = new HashMap();
 * params.put("namespace", "leeds");
 * params.put("dbtype", "postgis");
 * params.put("host","feathers.leeds.ac.uk");
 * params.put("port", "5432");
 * params.put("database","postgis_test");
 * params.put("user","postgis_ro");
 * params.put("passwd","postgis_ro");
 *
 * DefaultRegistry registry = new DefaultRegistry();
 * registry.addDataStore("leeds", params);
 *
 * DataStore postgis = registry.getDataStore( "leeds" );
 * SimpleFeatureSource = postgis.getFeatureSource( "table" );
 * </code></pre>
 * </p>
 * The required parameters are described by the getParameterInfo() method. Client
 * 
 * <h2>Implementation Notes</h2>
 * <p>
 * An instance of this interface should exist for all DataAccess implementations
 * that want to advantage of the dynamic plug-in system. In addition to implementing
 * this interface a DataAccess implementation should provide a services file:
 * </p>
 *
 * <p>
 * <code>META-INF/services/org.geotools.data.DataAccessFactory</code>
 * </p>
 *
 * <p>
 * The file should contain a single line which gives the full name of the
 * implementing class.
 * </p>
 *
 * <p>
 * Example:<br/><code>e.g.
 * org.geotools.data.mytype.MyTypeDataSourceFacotry</code>
 * </p>
 *
 * <p>
 * The factories are never called directly by client code, instead the
 * DataStoreFinder class is used.
 * </p>
 * 
 * @author Jody Garnett (Refractions Research)
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/api/src/main/java/org/geotools/data/DataAccessFactory.java $
 */
public interface DataAccessFactory extends Factory {
    /**
     * Construct a live DataAccess using the connection parameters provided.
     * <p>
     * You can think of this class as setting up a connection to the back end data source. The
     * required parameters are described by the getParameterInfo() method.
     * </p>
     *
     * <p>
     * Magic Params: the following params are magic and are honoured by
     * convention by the GeoServer and uDig application.
     *
     * <ul>
     * <li>
     * "user": is taken to be the user name
     * </li>
     * <li>
     * "passwd": is taken to be the password
     * </li>
     * <li>
     * "namespace": is taken to be the namespace prefix (and will be kept in
     * sync with GeoServer namespace management.
     * </li>
     * </ul>
     *
     * When we eventually move over to the use of OpperationalParam we will
     * have to find someway to codify this convention.
     * </p>
     *
     * @param params The full set of information needed to construct a live
     *        data store. Typical key values for the map include: url -
     *        location of a resource, used by file reading datasources. dbtype
     *        - the type of the database to connect to, e.g. postgis, mysql
     *
     * @return The created DataStore, this may be null if the required resource
     *         was not found or if insufficent parameters were given. Note
     *         that canProcess() should have returned false if the problem is
     *         to do with insuficent parameters.
     *
     * @throws IOException if there were any problems setting up (creating or
     *         connecting) the datasource.
     */
    DataAccess<? extends FeatureType, ? extends Feature> createDataStore(Map<String, Serializable> params) throws IOException;

    /**
     * Describe the nature of the datasource constructed by this factory.
     *
     * <p>
     * A non localized description of this data store type.
     * </p>
     *
     * @return A human readable description that is suitable for inclusion in a
     *         list of available datasources.
     */
    String getDescription();

    /**
     * Test to see if the implementation is available for use.
     * This method ensures all the appropriate libraries to construct
     * the DataAccess are available. 
     * <p>
     * Most factories will simply return <code>true</code> as GeoTools will
     * distribute the appropriate libraries. Though it's not a bad idea for
     * DataStoreFactories to check to make sure that the  libraries are there.
     * <p>
     * OracleDataStoreFactory is an example of one that may generally return
     * <code>false</code>, since GeoTools can not distribute the oracle jars.
     * (they must be added by the client.)
     * <p>
     * One may ask how this is different than canProcess, and basically available
     * is used by the DataStoreFinder getAvailableDataStore method, so that
     * DataStores that can not even be used do not show up as options in gui
     * applications.
     *
     * @return <tt>true</tt> if and only if this factory has all the
     *         appropriate jars on the classpath to create DataStores.
     */
    boolean isAvailable();

    /**
     * Data class used to capture Parameter requirements.
     *
     * <p>
     * Subclasses may provide specific setAsText()/getAsText() requirements
     * </p>
     *
     * <p>
     * Warning: We would like to start moving towards a common paraemters
     * framework with GridCoverageExchnage. Param will be maintained as a
     * wrapper for one point release (at which time it will be deprecated).
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static class Param extends Parameter {

        /**
         * Provides support for text representations
         *
         * @param key Key used to file this Param in the Parameter Map for
         *        createDataStore
         * @param type Class type intended for this Param
         * @param description User description of Param (40 chars or less)
         * @param required <code>true</code> is param is required
         */
        public Param(String key, Class<?> type, String description, boolean required) {
            this(key, type, description, required, null);
        }

        /**
         * Provides support for text representations
         *
         * @param key Key used to file this Param in the Parameter Map for
         *        createDataStore
         * @param type Class type intended for this Param
         * @param description User description of Param (40 chars or less)
         * @param required <code>true</code> is param is required
         * @param sample Sample value as an example for user input
         */
        public Param(String key, Class<?> type, String description, boolean required, Object sample) {
            this(key, type, description == null? null : new SimpleInternationalString(description),
                    required, sample, null);
        }

        /**
         * Provides support for text representations
         *
         * @param key Key used to file this Param in the Parameter Map for
         *        createDataStore
         * @param type Class type intended for this Param
         * @param description User description of Param (40 chars or less)
         * @param required <code>true</code> is param is required
         * @param sample Sample value as an example for user input
         * @param extra metadata information, preferably keyed by known identifiers 
         * like {@link Parameter#IS_PASSWORD}
         */
        public Param(String key,
                     Class type,
                     String description,
                     boolean required,
                     Object sample,
                     Map<String, ?> metadata) {
            this(key, type, new SimpleInternationalString(description), required, sample, metadata);
        }

        /**
         * Provides support for text representations
         *
         * @param key Key used to file this Param in the Parameter Map for
         *        createDataStore
         * @param type Class type intended for this Param
         * @param description User description of Param (40 chars or less)
         * @param required <code>true</code> is param is required
         * @param sample Sample value as an example for user input
         * @param extra metadata information, preferably keyed by known identifiers 
         * like {@link Parameter#IS_PASSWORD}
         */
        public Param(String key,
                     Class type,
                     InternationalString description,
                     boolean required,
                     Object sample,
                     Map<String, ?> metadata) {
            super(key, type, new SimpleInternationalString(key), description, required, 1, 1, sample, metadata);
        }
        
        /**
         * Lookup Param in a user supplied map.
         *
         * <p>
         * Type conversion will occur if required, this may result in an
         * IOException. An IOException will be throw in the Param is required
         * and the Map does not contain the Map.
         * </p>
         *
         * <p>
         * The handle method is used to process the user's value.
         * </p>
         *
         * @param map Map of user input
         *
         * @return Parameter as specified in map
         *
         * @throws IOException if parse could not handle value
         */
        public Object lookUp(Map<String, ?> map) throws IOException {
            if (!map.containsKey(key)) {
                if (required) {
                    throw new IOException("Parameter " + key + " is required:" + description);
                } else {
                    return null;
                }
            }

            Object value = map.get(key);

            if (value == null) {
                return null;
            }

            if (value instanceof String && (type != String.class)) {
                value = handle((String) value);
            }

            if (value == null) {
                return null;
            }

            if (!type.isInstance(value)) {
                throw new IOException(type.getName() + " required for parameter " + key + ": not "
                    + value.getClass().getName());
            }

            return value;
        }

        /**
         * Convert value to text representation for this Parameter
         *
         * @param value DOCUMENT ME!
         *
         * @return DOCUMENT ME!
         */
        public String text(Object value) {
            return value.toString();
        }

        /**
         * Handle text in a sensible manner.
         *
         * <p>
         * Performs the most common way of handling text value:
         * </p>
         *
         * <ul>
         * <li>
         * null: If text is null
         * </li>
         * <li>
         * origional text: if type == String.class
         * </li>
         * <li>
         * null: if type != String.class and text.getLength == 0
         * </li>
         * <li>
         * parse( text ): if type != String.class
         * </li>
         * </ul>
         *
         *
         * @param text
         *
         * @return Value as processed by text
         *
         * @throws IOException If text could not be parsed
         */
        public Object handle(String text) throws IOException {
            if (text == null) {
                return null;
            }

            if (type == String.class) {
                return text;
            }

            if (text.length() == 0) {
                return null;
            }

            // if type is an array, tokenize the string and have the reflection
            // parsing be tried on each element, then build the array as a result
            if (type.isArray()) {
                StringTokenizer tokenizer = new StringTokenizer(text, " ");
                List<Object> result = new ArrayList<Object>();

                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    Object element;

                    try {
                        if (type.getComponentType() == String.class) {
                            element = token;
                        } else {
                            element = parse(token);
                        }
                    } catch (IOException ioException) {
                        throw ioException;
                    } catch (Throwable throwable) {
                        throw new DataSourceException("Problem creating " + type.getName()
                            + " from '" + text + "'", throwable);
                    }

                    result.add(element);
                }

                Object array = Array.newInstance(type.getComponentType(), result.size());

                for (int i = 0; i < result.size(); i++) {
                    Array.set(array, i, result.get(i));
                }

                return array;
            }

            try {
                return parse(text);
            } catch (IOException ioException) {
                throw ioException;
            } catch (Throwable throwable) {
                throw new DataSourceException("Problem creating " + type.getName() + " from '"
                    + text + "'", throwable);
            }
        }

        /**
         * Provides support for text representations
         *
         * <p>
         * Provides basic support for common types using reflection.
         * </p>
         *
         * <p>
         * If needed you may extend this class to handle your own custome
         * types.
         * </p>
         *
         * @param text Text representation of type should not be null or empty
         *
         * @return Object converted from text representation
         *
         * @throws Throwable DOCUMENT ME!
         * @throws IOException If text could not be parsed
         * @throws DataSourceException DOCUMENT ME!
         */
        public Object parse(String text) throws Throwable {
            Constructor<?> constructor;

            try {
                constructor = type.getConstructor(new Class[] { String.class });
            } catch (SecurityException e) {
                //  type( String ) constructor is not public
                throw new IOException("Could not create " + type.getName() + " from text");
            } catch (NoSuchMethodException e) {
                // No type( String ) constructor
                throw new IOException("Could not create " + type.getName() + " from text");
            }

            try {
                return constructor.newInstance(new Object[] { text, });
            } catch (IllegalArgumentException illegalArgumentException) {
                throw new DataSourceException("Could not create " + type.getName() + ": from '"
                    + text + "'", illegalArgumentException);
            } catch (InstantiationException instantiaionException) {
                throw new DataSourceException("Could not create " + type.getName() + ": from '"
                    + text + "'", instantiaionException);
            } catch (IllegalAccessException illegalAccessException) {
                throw new DataSourceException("Could not create " + type.getName() + ": from '"
                    + text + "'", illegalAccessException);
            } catch (InvocationTargetException targetException) {
                throw targetException.getCause();
            }
        }

        /**
         * key=Type description
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(key);
            buf.append('=');
            buf.append(type.getName());
            buf.append(' ');

            if (required) {
                buf.append("REQUIRED ");
            }

            buf.append(description);

            return buf.toString();
        }
    }
}
