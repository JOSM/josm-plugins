/*
 * Copyright (C) 2007 The Guava Authors
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link EventBus}.
 *
 * @author Cliff Biffle
 */
class EventBusTest {
  private static final String EVENT = "Hello";
  private static final String BUS_IDENTIFIER = "test-bus";

  private EventBus bus;

  @BeforeEach
  void setUp() {
    bus = new EventBus(BUS_IDENTIFIER);
  }

  @Test
  void testBasicCatcherDistribution() {
    StringCatcher catcher = new StringCatcher();
    bus.register(catcher);
    bus.post(EVENT);

    List<String> events = catcher.getEvents();
    assertEquals(1, events.size(), "Only one event should be delivered.");
    assertEquals(EVENT, events.get(0), "Correct string should be delivered.");
  }

  /**
   * Tests that events are distributed to any subscribers to their type or any supertype, including
   * interfaces and superclasses.
   *
   * <p>Also checks delivery ordering in such cases.
   */
  @Test
  void testPolymorphicDistribution() {
    // Three catchers for related types String, Object, and Comparable<?>.
    // String isa Object
    // String isa Comparable<?>
    // Comparable<?> isa Object
    StringCatcher stringCatcher = new StringCatcher();

    final List<Object> objectEvents = new ArrayList<>();
    Object objCatcher =
        new Object() {
          @Subscribe
          public void eat(Object food) {
            objectEvents.add(food);
          }
        };

    final List<Comparable<?>> compEvents = new ArrayList<>();
    Object compCatcher =
        new Object() {
          @Subscribe
          public void eat(Comparable<?> food) {
            compEvents.add(food);
          }
        };
    bus.register(stringCatcher);
    bus.register(objCatcher);
    bus.register(compCatcher);

    // Two additional event types: Object and Comparable<?> (played by Integer)
    Object objEvent = new Object();
    Object compEvent = 6;

    bus.post(EVENT);
    bus.post(objEvent);
    bus.post(compEvent);

    // Check the StringCatcher...
    List<String> stringEvents = stringCatcher.getEvents();
    assertEquals(1, stringEvents.size(), "Only one String should be delivered.");
    assertEquals(EVENT, stringEvents.get(0), "Correct string should be delivered.");

    // Check the Catcher<Object>...
    assertEquals(3, objectEvents.size(), "Three Objects should be delivered.");
    assertEquals(EVENT, objectEvents.get(0), "String fixture must be first object delivered.");
    assertEquals(objEvent, objectEvents.get(1), "Object fixture must be second object delivered.");
    assertEquals(compEvent, objectEvents.get(2), "Comparable fixture must be thirdobject delivered.");

    // Check the Catcher<Comparable<?>>...
    assertEquals(2, compEvents.size(), "Two Comparable<?>s should be delivered.");
    assertEquals(EVENT, compEvents.get(0), "String fixture must be first comparable delivered.");
    assertEquals(compEvent, compEvents.get(1), "Comparable fixture must be second comparable delivered.");
  }

  @Test
  void testSubscriberThrowsException() throws Exception {
    final RecordingSubscriberExceptionHandler handler = new RecordingSubscriberExceptionHandler();
    final EventBus eventBus = new EventBus(handler);
    final RuntimeException exception =
        new RuntimeException("but culottes have a tendancy to ride up!");
    final Object subscriber =
        new Object() {
          @Subscribe
          public void throwExceptionOn(String message) {
            throw exception;
          }
        };
    eventBus.register(subscriber);
    eventBus.post(EVENT);

    assertEquals(exception, handler.exception, "Cause should be available.");
    assertEquals(eventBus, handler.context.getEventBus(), "EventBus should be available.");
    assertEquals(EVENT, handler.context.getEvent(), "Event should be available.");
    assertEquals(subscriber, handler.context.getSubscriber(), "Subscriber should be available.");
    assertEquals(subscriber.getClass().getMethod("throwExceptionOn", String.class), handler.context.getSubscriberMethod(), "Method should be available.");
  }

  @Test
  void testSubscriberThrowsExceptionHandlerThrowsException() throws Exception {
    final EventBus eventBus =
        new EventBus(
                (exception, context) -> {
                  throw new RuntimeException("testSubscriberThrowsExceptionHandlerThrowsException_1. This is a normal exception");
                });
    final Object subscriber =
        new Object() {
          @Subscribe
          public void throwExceptionOn(String message) {
            throw new RuntimeException("testSubscriberThrowsExceptionHandlerThrowsException_2. This is a normal exception");
          }
        };
    eventBus.register(subscriber);
    assertDoesNotThrow(() -> eventBus.post(EVENT));
  }

  @Test
  void testDeadEventForwarding() {
    GhostCatcher catcher = new GhostCatcher();
    bus.register(catcher);

    // A String -- an event for which noone has registered.
    bus.post(EVENT);

    List<DeadEvent> events = catcher.getEvents();
    assertEquals(1, events.size(), "One dead event should be delivered.");
    assertEquals(EVENT, events.get(0).getEvent(), "The dead event should wrap the original event.");
  }

  @Test
  void testDeadEventPosting() {
    GhostCatcher catcher = new GhostCatcher();
    bus.register(catcher);

    bus.post(new DeadEvent(this, EVENT));

    List<DeadEvent> events = catcher.getEvents();
    assertEquals(1, events.size(), "The explicit DeadEvent should be delivered.");
    assertEquals(EVENT, events.get(0).getEvent(), "The dead event must not be re-wrapped.");
  }

  @Test
  void testMissingSubscribe() {
    bus.register(new Object());
  }

  @Test
  void testUnregister() {
    StringCatcher catcher1 = new StringCatcher();
    StringCatcher catcher2 = new StringCatcher();
    assertThrows(IllegalArgumentException.class, () -> bus.unregister(catcher1), "Attempting to unregister an unregistered object succeeded");

    bus.register(catcher1);
    bus.post(EVENT);
    bus.register(catcher2);
    bus.post(EVENT);

    List<String> expectedEvents = new ArrayList<>();
    expectedEvents.add(EVENT);
    expectedEvents.add(EVENT);

    assertEquals(expectedEvents, catcher1.getEvents(), "Two correct events should be delivered.");

    assertEquals(Collections.singletonList(EVENT), catcher2.getEvents(), "One correct event should be delivered.");

    bus.unregister(catcher1);
    bus.post(EVENT);

    assertEquals(expectedEvents, catcher1.getEvents(), "Shouldn't catch any more events when unregistered.");
    assertEquals(expectedEvents, catcher2.getEvents(), "Two correct events should be delivered.");

    assertThrows(IllegalArgumentException.class, () -> bus.unregister(catcher1), "Attempting to unregister an unregistered object succeeded");

    bus.unregister(catcher2);
    bus.post(EVENT);
    assertEquals(expectedEvents, catcher1.getEvents(), "Shouldn't catch any more events when unregistered.");
    assertEquals(expectedEvents, catcher2.getEvents(), "Shouldn't catch any more events when unregistered.");
  }

  // NOTE: This test will always pass if register() is thread-safe but may also
  // pass if it isn't, though this is unlikely.

  @Test
  void testRegisterThreadSafety() throws Exception {
    List<StringCatcher> catchers = new CopyOnWriteArrayList<>();
    List<Future<?>> futures = new ArrayList<>();
    ExecutorService executor = Executors.newFixedThreadPool(10);
    int numberOfCatchers = 10000;
    for (int i = 0; i < numberOfCatchers; i++) {
      futures.add(executor.submit(new Registrator(bus, catchers)));
    }
    for (int i = 0; i < numberOfCatchers; i++) {
      futures.get(i).get();
    }
    assertEquals(numberOfCatchers, catchers.size(), "Unexpected number of catchers in the list");
    bus.post(EVENT);
    List<String> expectedEvents = Collections.singletonList(EVENT);
    for (StringCatcher catcher : catchers) {
      assertEquals(expectedEvents, catcher.getEvents(), "One of the registered catchers did not receive an event.");
    }
  }

  @Test
  void testToString() {
    EventBus eventBus = new EventBus("a b ; - \" < > / \\ €");
    assertEquals("EventBus [a b ; - \" < > / \\ €]", eventBus.toString());
  }

  /**
   * Tests that bridge methods are not subscribed to events. In Java 8, annotations are included on
   * the bridge method in addition to the original method, which causes both the original and bridge
   * methods to be subscribed (since both are annotated @Subscribe) without specifically checking
   * for bridge methods.
   */
  @Test
  void testRegistrationWithBridgeMethod() {
    final AtomicInteger calls = new AtomicInteger();
    bus.register(
        new Callback<String>() {
          @Subscribe
          @Override
          public void call(String s) {
            calls.incrementAndGet();
          }
        });

    bus.post("hello");

    assertEquals(1, calls.get());
  }

  /** Records thrown exception information. */
  private static final class RecordingSubscriberExceptionHandler
      implements SubscriberExceptionHandler {

    public SubscriberExceptionContext context;
    public Throwable exception;

    @Override
    public void handleException(Throwable exception, SubscriberExceptionContext context) {
      this.exception = exception;
      this.context = context;
    }
  }

  /** Runnable which registers a StringCatcher on an event bus and adds it to a list. */
  private static class Registrator implements Runnable {
    private final EventBus bus;
    private final List<StringCatcher> catchers;

    Registrator(EventBus bus, List<StringCatcher> catchers) {
      this.bus = bus;
      this.catchers = catchers;
    }

    @Override
    public void run() {
      StringCatcher catcher = new StringCatcher();
      bus.register(catcher);
      catchers.add(catcher);
    }
  }

  /**
   * A collector for DeadEvents.
   *
   * @author cbiffle
   */
  public static class GhostCatcher {
    private final List<DeadEvent> events = new ArrayList<>();

    @Subscribe
    public void ohNoesIHaveDied(DeadEvent event) {
      events.add(event);
    }

    public List<DeadEvent> getEvents() {
      return events;
    }
  }

  private interface Callback<T> {
    void call(T t);
  }
}
