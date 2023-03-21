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

//import com.google.common.testing.EqualsTester;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Subscriber}.
 *
 * @author Cliff Biffle
 * @author Colin Decker
 */
class SubscriberTest {

  private static final Object FIXTURE_ARGUMENT = new Object();

  private EventBus bus;
  private boolean methodCalled;
  private Object methodArgument;

  @BeforeEach
  protected void setUp() {
    bus = new EventBus();
    methodCalled = false;
    methodArgument = null;
  }

  @Test
  void testCreate() {
    Subscriber s1 = Subscriber.create(bus, this, getTestSubscriberMethod("recordingMethod"));
    assertTrue(s1 instanceof Subscriber.SynchronizedSubscriber);

    // a thread-safe method should not create a synchronized subscriber
    Subscriber s2 = Subscriber.create(bus, this, getTestSubscriberMethod("threadSafeMethod"));
    Assertions.assertFalse(s2 instanceof Subscriber.SynchronizedSubscriber);
  }

  @Test
  void testInvokeSubscriberMethodBasicMethodCall() throws Throwable {
    Method method = getTestSubscriberMethod("recordingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT);

    assertTrue(methodCalled, "Subscriber must call provided method");
    assertSame(methodArgument, FIXTURE_ARGUMENT, "Subscriber argument must be exactly the provided object.");
  }

  @Test
  void testInvokeSubscriberMethodExceptionWrapping() {
    Method method = getTestSubscriberMethod("exceptionThrowingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT),
            "Subscribers whose methods throw must throw InvocationTargetException");
    assertTrue(ite.getCause() instanceof IntentionalException);
  }

  @Test
  void testInvokeSubscriberMethodErrorPassthrough() {
    Method method = getTestSubscriberMethod("errorThrowingMethod");
    Subscriber subscriber = Subscriber.create(bus, this, method);

    assertThrows(JudgmentError.class, () -> subscriber.invokeSubscriberMethod(FIXTURE_ARGUMENT),
      "Subscribers whose methods throw Errors must rethrow them");
  }

  @Test
  @Disabled("FIXME")
  void testEquals() {
    /*Method charAt = String.class.getMethod("charAt", int.class);
    Method concat = String.class.getMethod("concat", String.class);
    new EqualsTester()
        .addEqualityGroup(
            Subscriber.create(bus, "foo", charAt), Subscriber.create(bus, "foo", charAt))
        .addEqualityGroup(Subscriber.create(bus, "bar", charAt))
        .addEqualityGroup(Subscriber.create(bus, "foo", concat))
        .testEquals();*/
  }

  private Method getTestSubscriberMethod(String name) {
    try {
      return getClass().getDeclaredMethod(name, Object.class);
    } catch (NoSuchMethodException e) {
      throw new AssertionError();
    }
  }

  /**
   * Records the provided object in {@link #methodArgument} and sets {@link #methodCalled}. This
   * method is called reflectively by Subscriber during tests, and must remain public.
   *
   * @param arg argument to record.
   */
  @Subscribe
  public void recordingMethod(Object arg) {
    Assertions.assertFalse(methodCalled);
    methodCalled = true;
    methodArgument = arg;
  }

  @Subscribe
  public void exceptionThrowingMethod(Object arg) throws Exception {
    throw new IntentionalException();
  }

  /** Local exception subclass to check variety of exception thrown. */
  static class IntentionalException extends Exception {

    private static final long serialVersionUID = -2500191180248181379L;
  }

  @Subscribe
  public void errorThrowingMethod(Object arg) {
    throw new JudgmentError();
  }

  @Subscribe
  @AllowConcurrentEvents
  public void threadSafeMethod(Object arg) {}

  /** Local Error subclass to check variety of error thrown. */
  class JudgmentError extends Error {

    private static final long serialVersionUID = 634248373797713373L;
  }
}
