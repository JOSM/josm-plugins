package org.wikipedia.tools;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Debouncer {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Object, Future<?>> delayedMap = new ConcurrentHashMap<>();

    public Debouncer(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Debounces {@code callable} by {@code delay}, i.e., schedules it to be executed after {@code delay},
     * or cancels its execution if the method is called with the same key within the {@code delay} again.
     */
    public void debounce(final Object key, final Runnable runnable, long delay, TimeUnit unit) {
        final Future<?> prev = delayedMap.put(key, scheduler.schedule(() -> {
            try {
                runnable.run();
            } finally {
                delayedMap.remove(key);
            }
        }, delay, unit));
        if (prev != null) {
            prev.cancel(true);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}