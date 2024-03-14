// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.Arrays;

import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.tools.JosmRuntimeException;

/**
 * Utilities for tests.
 */
public final class TestUtil {

    private TestUtil() {
        // Prevent instantiation
    }

    public static Field getAccessibleField(Class<?> clazz, String fieldName) {
        try {
            Field result = clazz.getDeclaredField(fieldName);
            result.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(result, modifiers.getInt(result) & ~Modifier.FINAL);
            return result;
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Helper method for obtaining the value of a private field
     * @param object the object of which you want the private field
     * @param name the name of the private field
     * @return the current value that field has
     */
    public static Object getPrivateFieldValue(Object object, String name) {
        try {
            return getAccessibleField(object.getClass(), name).get(object);
        } catch (IllegalAccessException | SecurityException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * This method tests utility classes for common coding standards (exactly one constructor that's private,
     * only static methods, …) and fails the current test if one of those standards is not met.
     * This is inspired by <a href="https://stackoverflow.com/a/10872497">an answer on StackOverflow.com</a> .
     * @param c the class under test
     */
    public static void testUtilityClass(final Class<?> c) {
        try {
            // class must be final
            assertTrue(Modifier.isFinal(c.getModifiers()));
            // with exactly one constructor
            assertEquals(1, c.getDeclaredConstructors().length);
            final Constructor<?> constructor = c.getDeclaredConstructors()[0];
            // constructor has to be private
            assertTrue(!constructor.isAccessible() && Modifier.isPrivate(constructor.getModifiers()));
            constructor.setAccessible(true);
            // Call private constructor for code coverage
            constructor.newInstance();
            for (Method m : c.getMethods()) {
                // Check if all methods are static
                assertTrue(m.getDeclaringClass() != c || Modifier.isStatic(m.getModifiers()));
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new JosmRuntimeException(e);
        }
    }

    /**
     * Generate a valid image
     * @param id The id of the image
     * @param lat The latitude of the image
     * @param lon The longitude of the image
     * @return The new image
     */
    public static StreetsideImage generateImage(String id, double lat, double lon) {
        return new StreetsideImage("https://ecn.{subdomain}.tiles.virtualearth.net/tiles/hs" + id
                + "{faceId}{tileId}?g=14336&key=Arzdiw4nlOJzRwOz__qailc8NiR31Tt51dN2D7cm57NrnceZnCpgOkmJhNpGoppU", lat,
                lon, 268.811, 1.395, -4.875, Instant.ofEpochMilli(1614556800000L), Instant.ofEpochMilli(1614643199999L),
                "https://dev.virtualearth.net/Branding/logo_powered_by.png",
                "Copyright © 2024 Microsoft and its suppliers. All rights reserved. This API cannot be accessed and the content and any results may not be used, reproduced or transmitted in any manner without express written permission from Microsoft Corporation.",
                1, 3, 256, 256, Arrays.asList("t0", "t1", "t2", "t3"));
    }
}
