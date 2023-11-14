// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class TileDownloadingTaskTest {

    @Test
    final void testCall() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(1);
        List<Callable<List<String>>> tasks = new ArrayList<>(1);
        tasks.add(new TileDownloadingTask("2202112030033001233"));
        List<Future<List<String>>> results = pool.invokeAll(tasks);
        assertEquals(results.get(0), "2202112030033001233");
    }
}
