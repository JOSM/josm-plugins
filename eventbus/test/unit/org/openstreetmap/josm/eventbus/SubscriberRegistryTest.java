/*
 * Copyright (C) 2014 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openstreetmap.josm.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


/**
 * Tests for {@link SubscriberRegistry}.
 *
 * @author Colin Decker
 */
public class SubscriberRegistryTest {

  private final SubscriberRegistry registry = new SubscriberRegistry(new EventBus());

  @Test
  void testRegister() {
    assertEquals(0, registry.getSubscribersForTesting(String.class).size());

    registry.register(new StringSubscriber());
    assertEquals(1, registry.getSubscribersForTesting(String.class).size());

    registry.register(new StringSubscriber());
    assertEquals(2, registry.getSubscribersForTesting(String.class).size());

    registry.register(new ObjectSubscriber());
    assertEquals(2, registry.getSubscribersForTesting(String.class).size());
    assertEquals(1, registry.getSubscribersForTesting(Object.class).size());
  }

  @Test
  void testUnregister() {
    StringSubscriber s1 = new StringSubscriber();
    StringSubscriber s2 = new StringSubscriber();

    registry.register(s1);
    registry.register(s2);

    registry.unregister(s1);
    assertEquals(1, registry.getSubscribersForTesting(String.class).size());

    registry.unregister(s2);
    assertTrue(registry.getSubscribersForTesting(String.class).isEmpty());
  }

  @Test
  void testUnregisterNotRegistered() {
    StringSubscriber temp = new StringSubscriber();
    assertThrows(IllegalArgumentException.class, () -> registry.unregister(temp));

    StringSubscriber s1 = new StringSubscriber();
    registry.register(s1);
    // a StringSubscriber was registered, but not the same one we tried to unregister
    assertThrows(IllegalArgumentException.class, () -> registry.unregister(temp));

    registry.unregister(s1);
    assertThrows(IllegalArgumentException.class, () -> registry.unregister(s1));
  }

  @Test
  void testGetSubscribers() {
    assertEquals(0, size(registry.getSubscribers("")));

    registry.register(new StringSubscriber());
    assertEquals(1, size(registry.getSubscribers("")));

    registry.register(new StringSubscriber());
    assertEquals(2, size(registry.getSubscribers("")));

    registry.register(new ObjectSubscriber());
    assertEquals(3, size(registry.getSubscribers("")));
    assertEquals(1, size(registry.getSubscribers(new Object())));
    assertEquals(1, size(registry.getSubscribers(1)));

    registry.register(new IntegerSubscriber());
    assertEquals(3, size(registry.getSubscribers("")));
    assertEquals(1, size(registry.getSubscribers(new Object())));
    assertEquals(2, size(registry.getSubscribers(1)));
  }

  @Test
  @Disabled("FIXME")
  void testGetSubscribersReturnsImmutableSnapshot() {
    StringSubscriber s1 = new StringSubscriber();
    StringSubscriber s2 = new StringSubscriber();
    ObjectSubscriber o1 = new ObjectSubscriber();

    Iterator<Subscriber> empty = registry.getSubscribers("");
    assertFalse(empty.hasNext());

    empty = registry.getSubscribers("");

    registry.register(s1);
    assertFalse(empty.hasNext());

    Iterator<Subscriber> one = registry.getSubscribers("");
    assertEquals(s1, one.next().target);
    assertFalse(one.hasNext());

    one = registry.getSubscribers("");

    registry.register(s2);
    registry.register(o1);

    Iterator<Subscriber> three = registry.getSubscribers("");
    assertEquals(s1, one.next().target);
    assertFalse(one.hasNext());

    assertEquals(s1, three.next().target);
    assertEquals(s2, three.next().target);
    assertEquals(o1, three.next().target);
    assertFalse(three.hasNext());

    three = registry.getSubscribers("");

    registry.unregister(s2);

    assertEquals(s1, three.next().target);
    assertEquals(s2, three.next().target);
    assertEquals(o1, three.next().target);
    assertFalse(three.hasNext());

    Iterator<Subscriber> two = registry.getSubscribers("");
    assertEquals(s1, two.next().target);
    assertEquals(o1, two.next().target);
    assertFalse(two.hasNext());
  }

  public static class StringSubscriber {

    @Subscribe
    public void handle(String s) {}
  }

  public static class IntegerSubscriber {

    @Subscribe
    public void handle(Integer i) {}
  }

  public static class ObjectSubscriber {

    @Subscribe
    public void handle(Object o) {}
  }

  @Test
  void testFlattenHierarchy() {
    assertEquals(new HashSet<>(Arrays.asList(
        Object.class,
        HierarchyFixtureInterface.class,
        HierarchyFixtureSubinterface.class,
        HierarchyFixtureParent.class,
        HierarchyFixture.class)), SubscriberRegistry.flattenHierarchy(HierarchyFixture.class));
  }

  private interface HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  private interface HierarchyFixtureSubinterface extends HierarchyFixtureInterface {
    // Exists only for hierarchy mapping; no members.
  }

  private static class HierarchyFixtureParent implements HierarchyFixtureSubinterface {
    // Exists only for hierarchy mapping; no members.
  }

  private static class HierarchyFixture extends HierarchyFixtureParent {
    // Exists only for hierarchy mapping; no members.
  }
  
  /**
   * Returns the number of elements remaining in {@code iterator}. The iterator will be left
   * exhausted: its {@code hasNext()} method will return {@code false}.
   */
  public static int size(Iterator<?> iterator) {
    int count = 0;
    while (iterator.hasNext()) {
      iterator.next();
      count++;
    }
    return count;
  }
}
