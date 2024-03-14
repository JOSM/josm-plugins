// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.cubemap;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.plugins.streetside.CubeMapTileXY;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideDataListener;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.cache.StreetsideCache;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideViewerDialog;
import org.openstreetmap.josm.plugins.streetside.gui.imageinfo.StreetsideViewerPanel;
import org.openstreetmap.josm.plugins.streetside.utils.GraphicsUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.tools.Logging;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Build a cubemap
 */
// JavaFX access in Java 8
public final class CubemapBuilder implements ITileDownloadingTaskListener, StreetsideDataListener {

    private static final Logger LOGGER = Logger.getLogger(CubemapBuilder.class.getCanonicalName());

    private static CubemapBuilder instance;
    boolean isBuilding;
    private StreetsideAbstractImage cubemap;
    private long startTime;

    private final Map<CubeMapTileXY, BufferedImage> tileImages = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    private final List<Future<?>> lastFutures = new ArrayList<>();

    private final AtomicInteger currentTileCount = new AtomicInteger();

    private CubemapBuilder() {
        // private constructor to avoid instantiation
    }

    public static synchronized CubemapBuilder getInstance() {
        if (instance == null) {
            instance = new CubemapBuilder();
        }
        return instance;
    }

    /**
     * Destroys the unique instance of the class.
     */
    public static synchronized void destroyInstance() {
        CubemapBuilder.instance = null;
    }

    /**
     * Get the current tile images
     * @return the tileImages
     */
    public Map<CubeMapTileXY, BufferedImage> getTileImages() {
        return tileImages;
    }

    /**
     * Set the tile images to show
     * @param tileImages the tileImages to set
     */
    public void setTileImages(Map<CubeMapTileXY, BufferedImage> tileImages) {
        synchronized (this.tileImages) {
            this.tileImages.clear();
            this.tileImages.putAll(tileImages);
        }
    }

    /**
     * Add an entry to the tile images
     * @param key The key to use
     * @param value The value to use
     */
    private void addTileImage(CubeMapTileXY key, BufferedImage value) {
        synchronized (this.tileImages) {
            this.tileImages.put(key, value);
        }
    }

    /**
     * Fired when any image is added to the database.
     */
    @Override
    public void imagesAdded() {
        // Not implemented by the CubemapBuilder
    }

    /**
     * Fired when the selected image is changed by something different from
     * manually clicking on the icon.
     *
     * @param oldImage Old selected {@link StreetsideImage}
     * @param newImage New selected {@link StreetsideImage}
     * @see StreetsideDataListener
     */
    @Override
    public void selectedImageChanged(StreetsideImage oldImage, StreetsideImage newImage) {

        startTime = System.currentTimeMillis();

        if (newImage != null) {

            cubemap = newImage;
            currentTileCount.set(0);
            resetTileImages();

            // download cubemap images in different threads and then subsequently
            // set the cubeface images in JavaFX
            downloadCubemapImages(cubemap);

            long runTime = (System.currentTimeMillis() - startTime) / 1000;
            if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
                LOGGER.log(Logging.LEVEL_DEBUG, "Completed downloading tiles for {0} in {1} seconds.",
                        new Object[] { newImage.id(), runTime });
            }
        }
    }

    /**
     * Reload an image
     * @param image The image to reload -- nothing happens if it is not the current image
     */
    public void reload(StreetsideAbstractImage image) {
        if (cubemap != null && image.id().equals(cubemap.id())) {
            this.tileImages.clear();
            downloadCubemapImages(image);
        }
    }

    /**
     * Download the cubemap images for the specified image
     * @param image The streetside image to get the cubemap for
     * @return The images (FIXME: Currently returns an empty map)
     */
    public Map<CubeMapTileXY, StreetsideCache> downloadCubemapImages(StreetsideAbstractImage image) {
        final var panel360 = StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel();
        if (panel360 != null && panel360.getScene() != panel360.getLoadingScene()) {
            panel360.setScene(panel360.getLoadingScene());
        }

        final int maxThreadCount = CubemapUtils.NUM_SIDES * CubemapUtils.getMaxCols(image)
                * CubemapUtils.getMaxRows(image);

        // TODO: message for progress bar
        // final var message = new String[2];
        // message[0] = MessageFormat.format("Downloading Streetside imagery for {0}", image.id());
        // message[1] = "Wait for completion…….";

        final long startTimeDownloadCubemapImages = System.currentTimeMillis();

        if (!CubemapBuilder.getInstance().getTileImages().keySet().isEmpty()) {
            CubemapBuilder.getInstance().resetTileImages();
        }

        List<Callable<List<String>>> tasks = new ArrayList<>(maxThreadCount);

        if (Boolean.TRUE.equals(StreetsideProperties.DOWNLOAD_CUBEFACE_TILES_TOGETHER.get())) {
            EnumSet.allOf(CubemapUtils.CubemapFaces.class)
                    .forEach(face -> tasks.add(new TileDownloadingTask(image, face, new CubeMapTileXY(face, 0, 0))));
        } else {
            final var zoom = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())
                    // launch 16-tiled (high-res) downloading tasks
                    ? image.zoomMax()
                    // launch 4-tiled (low-res) downloading tasks . . .
                    : image.zoomMin();
            // download all imagery for each cubeface at once
            for (var face : CubemapUtils.CubemapFaces.values()) {
                tasks.addAll(
                        image.getFaceTiles(face, zoom).map(f -> new TileDownloadingTask(image, face, f.a)).toList());
            }
        }
        // finish preparing tasks for invocation

        // execute tasks
        MainApplication.worker.submit(() -> {
            try {
                final List<Future<List<String>>> results;
                synchronized (lastFutures) {
                    lastFutures.forEach(f -> f.cancel(true));
                    lastFutures.clear();
                    // Timeout after a minute to avoid blocking the worker thread.
                    results = pool.invokeAll(tasks, 1, TimeUnit.MINUTES);
                    lastFutures.addAll(results);
                }

                if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
                    waitForCompletedTasks(results, startTimeDownloadCubemapImages);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
            }
        });

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTimeDownloadCubemapImages;

        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Tile imagery downloading tasks completed in {0} seconds.", runTime / 1000);
        }

        return Collections.emptyMap(); // FIXME: Actually return something for cancelling
    }

    /**
     * Wait for completed tasks and log errors
     * @param results The list of tasks to wait for
     * @param startTimeDownloadCubemapImages The time that downloads started
     * @throws InterruptedException If this thread was interrupted (see {@link Future#get()})
     */
    private static void waitForCompletedTasks(List<Future<List<String>>> results, long startTimeDownloadCubemapImages)
            throws InterruptedException {
        for (Future<List<String>> ff : results) {
            try {
                LOGGER.log(Logging.LEVEL_DEBUG, "Completed tile downloading task {0} in {1} seconds.", new Object[] {
                        ff.get(), (System.currentTimeMillis() - startTimeDownloadCubemapImages) / 1000 });
            } catch (ExecutionException e) {
                LOGGER.log(Logging.LEVEL_ERROR, e.getMessage(), e);
            }
        }
    }

    /**
     * Fired when a TileDownloadingTask has completed downloading an image tile. When all the tiles for the Cubemap
     * have been downloaded, the CubemapBuilder assembles the cubemap.
     *
     * @param image The image that the task has finished downloading for
     * @param tileXY
     *      the complete quadKey of the imagery tile, including cubeface and row/column in quaternary.
     * @param bufferedImage The image for the tile
     * @see TileDownloadingTask
     */
    @Override
    public void tileAdded(StreetsideAbstractImage image, CubeMapTileXY tileXY, BufferedImage bufferedImage) {
        // determine whether four tiles have been set for each of the
        // six cubemap faces. If so, build the images for the faces
        // and set the views in the cubemap box.

        if (!cubemap.id().equals(image.id())) {
            return;
        }
        this.addTileImage(tileXY, bufferedImage);

        if (currentTileCount.incrementAndGet() == (CubemapUtils.NUM_SIDES * CubemapUtils.getMaxCols(image)
                * CubemapUtils.getMaxRows(image))) {
            if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
                long endTime = System.currentTimeMillis();
                long runTime = (endTime - startTime) / 1000;
                LOGGER.log(Logging.LEVEL_DEBUG,
                        "{0} tile images ready for building cumbemap faces for cubemap {1} in {2} seconds.",
                        new Object[] { currentTileCount.get(), CubemapBuilder.getInstance().getCubemap().id(),
                                Long.toString(runTime) });
            }

            buildCubemapFaces();
        }
    }

    /**
     * Assembles the cubemap once all the tiles have been downloaded.
     * <p>
     * The tiles for each cubemap face are cropped and stitched together
     * then the ImageViews of the cubemap are set with the new imagery.
     *
     * @see StreetsideAbstractImage
     */
    private void buildCubemapFaces() {
        StreetsideViewerDialog.getInstance();
        final var cubemapBox = StreetsideViewerPanel.getCubemapBox();
        final var views = cubemapBox.getViews();

        final var finalImages = new Image[CubemapUtils.NUM_SIDES];

        // build 4-tiled cubemap faces and crop buffers
        final int zoom = Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())
                ? this.cubemap.zoomMax()
                : this.cubemap.zoomMin();
        for (var i = 0; i < CubemapUtils.NUM_SIDES; i++) {
            final var face = CubemapUtils.CubemapFaces.values()[i];
            final Map<CubeMapTileXY, BufferedImage> tiles;
            synchronized (this.tileImages) {
                tiles = this.cubemap.getFaceTiles(face, zoom).filter(p -> tileImages.get(p.a) != null)
                        .collect(Collectors.toMap(p -> p.a, p -> tileImages.get(p.a)));
            }
            if (this.cubemap.getFaceTiles(face, zoom).count() != tiles.size()) {
                return; // We don't have all the sides yet. FIXME
            }

            final var tImage = GraphicsUtils.buildMultiTiledCubemapFaceImage(tiles, zoom);
            // We need to flip the image horizontally (at least with JavaFX).
            final var finalImg = new BufferedImage(tImage.getWidth(), tImage.getHeight(), tImage.getType());
            final var g2d = finalImg.createGraphics();
            final var translate = AffineTransform.getScaleInstance(-1, 1);
            translate.concatenate(AffineTransform.getTranslateInstance(-finalImg.getWidth(), 0));
            // rotate top/down cubeface 180 degrees - misalignment workaround (this could probably be worked around in
            // CubemapBox).
            g2d.drawImage(face == CubemapUtils.CubemapFaces.DOWN || face == CubemapUtils.CubemapFaces.UP
                    ? GraphicsUtils.rotateImage(tImage)
                    : tImage, new AffineTransformOp(translate, AffineTransformOp.TYPE_BILINEAR), 0, 0);
            g2d.dispose();
            finalImages[i] = SwingFXUtils.toFXImage(finalImg, null);
        }

        for (var i = 0; i < CubemapUtils.NUM_SIDES; i++) {
            views[i].setImage(finalImages[i]);
        }

        StreetsideViewerDialog.getInstance().getStreetsideViewerPanel().revalidate();
        StreetsideViewerDialog.getInstance().getStreetsideViewerPanel().repaint();

        StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel()
                .setScene(StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().getCubemapScene());

        StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().revalidate();
        StreetsideViewerPanel.getThreeSixtyDegreeViewerPanel().repaint();

        long endTime = System.currentTimeMillis();
        long runTime = (endTime - startTime) / 1000;

        String message = MessageFormat.format(
                "Completed downloading, assembling and setting cubemap imagery for cubemap {0} in  {1} seconds.",
                cubemap.id(), runTime);

        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, message);
        }

        // reset count and image map after assembly
        resetTileImages();
        currentTileCount.set(0);
        isBuilding = false;
    }

    private void resetTileImages() {
        tileImages.clear();
    }

    /**
     * Get the current image that is providing the cubemap data
     * @return the cubemap
     */
    public synchronized StreetsideAbstractImage getCubemap() {
        return cubemap;
    }
}
