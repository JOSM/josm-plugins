// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.utils.TestUtil;

@Disabled
class TileDownloadingTaskTest {

    @Test
    final void testCall() throws InterruptedException, ExecutionException {
        try (ExecutorService pool = Executors.newFixedThreadPool(1)) {
            List<Callable<List<String>>> tasks = new ArrayList<>(1);
            tasks.add(new TileDownloadingTask(TestUtil.generateImage("2202112030033001233", 0, 0),
                    CubemapUtils.CubemapFaces.FRONT, new CubeMapTileXY(CubemapUtils.CubemapFaces.FRONT, 0, 0)));
            List<Future<List<String>>> results = pool.invokeAll(tasks);
            assertEquals("2202112030033001233", results.get(0).get().get(0));
        }
    }
}
