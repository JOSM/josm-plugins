// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;

import org.junit.runners.model.InitializationError;

import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

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
      fail(e.getLocalizedMessage());
    }
    return null;
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
      fail(e.getLocalizedMessage());
    }
    return null;
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
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      fail(e.getLocalizedMessage());
    }
  }

  public static class StreetsideTestRules extends JOSMTestRules {
    @Override
    protected void before() throws InitializationError, ReflectiveOperationException {
      Logging.getLogger().setFilter(record -> record.getLevel().intValue() >= Level.WARNING.intValue() || record.getSourceClassName().startsWith("org.openstreetmap.josm.plugins.streetside"));
      Utils.updateSystemProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT.%1$tL %2$s %4$s: %5$s%6$s%n");
      final String isHeadless = Boolean.toString(GraphicsEnvironment.isHeadless());
      super.before();
      System.setProperty("java.awt.headless", isHeadless);
    }
  }
}
