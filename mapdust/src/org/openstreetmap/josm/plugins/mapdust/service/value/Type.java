/* Copyright (c) 2010, skobbler GmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.plugins.mapdust.service.value;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;


/**
 * Defines the values for the <code>Type</code>.
 *
 * @author Bea
 *
 */
public class Type implements Serializable {

    /** The serial version UID */
    private static final long serialVersionUID = 1L;

    /** The key of the <code>Type</code> */
    private String key;

    /** The value of the <code>Type</code> */
    private String value;

    /** The hash map containing the valid values */
    private static java.util.HashMap<String, Type> table = null;

    /**
     * Builds a new <code>Type</code> object
     */
    public Type() {}

    /**
     * Builds a new <code>status</code> object
     *
     * @param key The key of the object
     * @param value The value of the object
     */
    public Type(String key, String value) {
        this.key = key;
        this.value = value;
        if (Type.table == null) {
            Type.table = new HashMap<String, Type>();
        }
        Type.table.put(key, this);
    }

    /** The wrong turn <code>Type</code> */
    public static final Type WRONG_TURN = new Type("wrong_turn", "Wrong turn");

    /** The bad routing <code>Type</code> */
    public static final Type BAD_ROUTING = new Type("bad_routing",
            "Bad routing");

    /** The oneway road <code>Type</code> */
    public static final Type ONEWAY_ROAD = new Type("oneway_road",
            "Oneway road");

    /** The blocked street <code>Type</code> */
    public static final Type BLOCKED_STREET = new Type("blocked_street",
            "Blocked streeet");

    /** The missing street <code>Type</code> */
    public static final Type MISSING_STREET = new Type("missing_street",
            "Missing street");

    /** The wrong roundabout <code>Type</code> */
    public static final Type WRONG_ROUNDABOUT = new Type("wrong_roundabout",
            "Wrong roundabout");

    /** The missing speedlimit <code>Type</code> */
    public static final Type MISSING_SPEEDLIMIT = new Type(
            "missing_speedlimit", "Missing speedlimit");

    /** The other <code>Type</code> */
    public static final Type OTHER = new Type("other", "Other");

    /**
     * Returns the <code>Type</code> for the given value.
     *
     * @param value The value
     * @return A <code>Type</code> object
     * @throws java.lang.IllegalStateException If the value is invalid
     */
    public static Type getType(java.lang.String value)
            throws java.lang.IllegalStateException {
        Type type = table.get(value);
        if (type == null) {
            type = Type.OTHER;
        }
        return type;
    }

    /**
     * Returns the <code>Type</code> object for the given value. If there is no
     * Type with the given value, the returned value is null.
     *
     * @param value The value of the type
     * @return A <code>Type</code> object
     */
    public static Type getTypeFromValue(String value) {
        Type type = null;
        for (Type obj : table.values()) {
            if (obj.getValue().equals(value)) {
                type = obj;
                break;
            }
        }
        return type;
    }

    /**
     * Returns all the types.
     *
     * @return An array of <code>Type</code> objects
     */
    public static Type[] getTypes() {
        Collection<Type> collection = table.values();
        return collection.toArray(new Type[0]);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals()
     */
    @Override
    public boolean equals(java.lang.Object obj) {
        return (obj == this);
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns the key
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

}
