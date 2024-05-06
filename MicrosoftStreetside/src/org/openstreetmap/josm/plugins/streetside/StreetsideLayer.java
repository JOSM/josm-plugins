// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.plugins.streetside.cache.CacheUtils;
import org.openstreetmap.josm.plugins.streetside.gui.StreetsideMainDialog;
import org.openstreetmap.josm.plugins.streetside.io.download.StreetsideDownloader;
import org.openstreetmap.josm.plugins.streetside.io.download.StreetsideDownloader.DOWNLOAD_MODE;
import org.openstreetmap.josm.plugins.streetside.mode.AbstractMode;
import org.openstreetmap.josm.plugins.streetside.mode.SelectMode;
import org.openstreetmap.josm.plugins.streetside.utils.MapViewGeometryUtil;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideColorScheme;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideUtils;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

/**
 * This class represents the layer shown in JOSM. There can only exist one
 * instance of this object.
 *
 * @author nokutu
 */
public final class StreetsideLayer extends AbstractModifiableLayer
        implements ActiveLayerChangeListener, StreetsideDataListener {

    private static final Logger LOGGER = Logger.getLogger(StreetsideLayer.class.getCanonicalName());

    /**
     * The radius of the image marker
     */
    private static final int IMG_MARKER_RADIUS = 7;
    /**
     * The radius of the circular sector that indicates the camera angle
     */
    private static final int CA_INDICATOR_RADIUS = 15;
    /**
     * The angle of the circular sector that indicates the camera angle
     */
    private static final int CA_INDICATOR_ANGLE = 40;
    /**
     * Unique instance of the class.
     */
    private static StreetsideLayer instance;
    private static final DataSetListenerAdapter DATASET_LISTENER = new DataSetListenerAdapter(e -> {
        if (e instanceof DataChangedEvent && StreetsideDownloader.getMode() == DOWNLOAD_MODE.OSM_AREA) {
            // When more data is downloaded, a delayed update is thrown, in order to
            // wait for the data bounds to be set.
            MainApplication.worker.execute(StreetsideDownloader::downloadOSMArea);
        }
    });
    /**
     * {@link StreetsideData} object that stores the database.
     */
    private final StreetsideData data;
    /**
     * Mode of the layer.
     */
    public AbstractMode mode;
    /**
     * The nearest images to the selected image from different sequences sorted by distance from selection.
     */
    private StreetsideImage[] nearestImages = {};
    private volatile TexturePaint hatched;

    private StreetsideLayer() {
        super(I18n.tr("Microsoft Streetside Images"));
        data = new StreetsideData();
        data.addListener(this);
    }

    public static void invalidateInstance() {
        if (hasInstance()) {
            getInstance().invalidate();
        }
    }

    private static synchronized void clearInstance() {
        instance = null;
    }

    /**
     * Returns the unique instance of this class.
     *
     * @return The unique instance of this class.
     */
    public static synchronized StreetsideLayer getInstance() {
        if (instance != null) {
            return instance;
        }
        final var layer = new StreetsideLayer();
        layer.init();
        instance = layer; // Only set instance field after initialization is complete
        return instance;
    }

    /**
     * Check if there is a Microsoft Streetside layer
     * @return if the unique instance of this layer is currently instantiated
     */
    public static boolean hasInstance() {
        return instance != null;
    }

    /**
     * Initializes the Layer.
     */
    private void init() {
        final var ds = MainApplication.getLayerManager().getEditDataSet();
        if (ds != null) {
            ds.addDataSetListener(DATASET_LISTENER);
        }
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
        if (!GraphicsEnvironment.isHeadless()) {
            setMode(new SelectMode());
            if (StreetsideDownloader.getMode() == DOWNLOAD_MODE.OSM_AREA) {
                MainApplication.worker.execute(StreetsideDownloader::downloadOSMArea);
            }
            if (StreetsideDownloader.getMode() == DOWNLOAD_MODE.VISIBLE_AREA) {
                mode.zoomChanged();
            }
        }
        // Does not execute when in headless mode
        if (!StreetsideMainDialog.getInstance().isShowing()) {
            StreetsideMainDialog.getInstance().showDialog();
        }
        if (StreetsidePlugin.getMapView() != null) {
            StreetsideMainDialog.getInstance().streetsideImageDisplay.repaint();
        }
        createHatchTexture();
        invalidate();
    }

    /**
     * Changes the mode the given one.
     *
     * @param mode The mode that is going to be activated.
     */
    public void setMode(AbstractMode mode) {
        final var mv = StreetsidePlugin.getMapView();
        if (this.mode != null && mv != null) {
            mv.removeMouseListener(this.mode);
            mv.removeMouseMotionListener(this.mode);
            NavigatableComponent.removeZoomChangeListener(this.mode);
        }
        this.mode = mode;
        if (mode != null && mv != null) {
            mv.setNewCursor(mode.cursor, this);
            mv.addMouseListener(mode);
            mv.addMouseMotionListener(mode);
            NavigatableComponent.addZoomChangeListener(mode);
            StreetsideUtils.updateHelpText();
        }
    }

    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * Returns the {@link StreetsideData} object, which acts as the database of the
     * Layer.
     *
     * @return The {@link StreetsideData} object that stores the database.
     */
    @Override
    public StreetsideData getData() {
        return data;
    }

    /**
     * Returns the n-nearest image, for n=1 the nearest one is returned, for n=2 the second nearest one and so on.
     * The "n-nearest image" is picked from the list of one image from every sequence that is nearest to the currently
     * selected image, excluding the sequence to which the selected image belongs.
     *
     * @param n the index for picking from the list of "nearest images", beginning from 1
     * @return the n-nearest image to the currently selected image
     */
    public synchronized StreetsideImage getNNearestImage(final int n) {
        return n >= 1 && n <= nearestImages.length ? nearestImages[n - 1] : null;
    }

    @Override
    public synchronized void destroy() {
        clearInstance();
        setMode(null);
        AbstractMode.resetThread();
        StreetsideDownloader.stopAll();
        if (StreetsideMainDialog.hasInstance()) {
            StreetsideMainDialog.getInstance().setImage(null);
            StreetsideMainDialog.getInstance().updateImage();
        }
        final var mv = StreetsidePlugin.getMapView();
        if (mv != null) {
            mv.removeMouseListener(mode);
            mv.removeMouseMotionListener(mode);
        }
        try {
            MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
            if (MainApplication.getLayerManager().getEditDataSet() != null) {
                MainApplication.getLayerManager().getEditDataSet().removeDataSetListener(DATASET_LISTENER);
            }
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            // TODO: It would be ideal, to fix this properly. But for the moment let's catch this, for when a listener has already been removed.
        }
        super.destroy();
    }

    /**
     * Initialize the hatch pattern used to paint the non-downloaded area.
     */
    private void createHatchTexture() {
        final var bufferedImage = new BufferedImage(15, 15, BufferedImage.TYPE_INT_ARGB);
        final var g2d = bufferedImage.createGraphics();
        g2d.setColor(StreetsideProperties.BACKGROUND.get());
        final var composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
        g2d.setComposite(composite);
        g2d.fillRect(0, 0, 15, 15);
        g2d.setColor(StreetsideProperties.OUTSIDE_DOWNLOADED_AREA.get());
        g2d.drawLine(0, 15, 15, 0);
        final var rectangle = new Rectangle(0, 0, 15, 15);
        hatched = new TexturePaint(bufferedImage, rectangle);
    }

    @Override
    public synchronized void paint(final Graphics2D g, final MapView mv, final Bounds box) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (MainApplication.getLayerManager().getActiveLayer() == this) {
            // paint remainder
            g.setPaint(hatched);
            g.fill(MapViewGeometryUtil.getNonDownloadedArea(mv, data.getBounds()));
        }

        // Draw the blue and red line
        synchronized (StreetsideLayer.class) {
            final var selectedImg = data.getSelectedImage();
            for (var i = 0; i < nearestImages.length && selectedImg != null; i++) {
                if (i == 0) {
                    g.setColor(Color.RED);
                } else {
                    g.setColor(Color.BLUE);
                }
                final var selected = mv.getPoint(selectedImg);
                final var point = mv.getPoint(nearestImages[i]);
                g.draw(new Line2D.Double(point.getX(), point.getY(), selected.getX(), selected.getY()));
            }
        }

        if (mv != null && mv.getDist100Pixel() < 100) {
            for (var imageAbs : data.search(box.toBBox())) {
                if (imageAbs.visible() && mv.contains(mv.getPoint(imageAbs))) {
                    drawImageMarker(g, imageAbs);
                }
            }
        } else if (mv != null) {
            // Generate sequence lines
            final var sortedImages = new ArrayList<>(data.search(box.toBBox()));
            sortedImages.sort(Comparator.naturalOrder());
            final var imagesToPaint = new ArrayList<StreetsideImage>(sortedImages.size());
            boolean containsSelected = false;
            for (var image : sortedImages) {
                if (!imagesToPaint.isEmpty() && imagesToPaint.getLast().greatCircleDistance(image) > 20) {
                    paintSequence(g, mv, imagesToPaint, containsSelected);
                    containsSelected = false;
                    imagesToPaint.clear();
                }
                imagesToPaint.add(image);
                if (image.equals(getData().getHighlightedImage())) {
                    containsSelected = true;
                }
            }
            if (!imagesToPaint.isEmpty()) {
                paintSequence(g, mv, imagesToPaint, containsSelected);
            }
        }
    }

    /**
     * Paint an artificial sequence
     * @param g The graphics to paint on
     * @param mv The current mapview
     * @param images The images to use for the sequence
     * @param containsSelected {@code true} if the sequence has a selected or highlighted image
     */
    private void paintSequence(Graphics2D g, MapView mv, List<StreetsideImage> images, boolean containsSelected) {
        final var color = containsSelected ? StreetsideColorScheme.SEQ_HIGHLIGHTED
                : StreetsideColorScheme.SEQ_UNSELECTED;
        final var path = new MapViewPath(mv);
        path.moveTo(images.get(0));
        for (int i = 1; i < images.size(); i++) {
            path.lineTo(images.get(i));
        }
        g.setColor(color);
        g.draw(path);
    }

    /**
     * Draws an image marker onto the given Graphics context.
     *
     * @param g   the Graphics context
     * @param img the image to be drawn onto the Graphics context
     */
    private void drawImageMarker(final Graphics2D g, final StreetsideAbstractImage img) {
        if (img == null || Double.isNaN(img.lat()) || Double.isNaN(img.lon())) {
            LOGGER.warning("An image is not painted, because it is null or has no LatLon!");
            return;
        }
        final var selectedImg = getData().getSelectedImage();
        final var point = MainApplication.getMap().mapView.getPoint(img);

        // Determine colors
        final Color markerC;
        final Color directionC;
        if (img.equals(selectedImg)) {
            markerC = StreetsideColorScheme.SEQ_HIGHLIGHTED;
            directionC = StreetsideColorScheme.SEQ_HIGHLIGHTED_CA;
        } else {
            markerC = StreetsideColorScheme.SEQ_UNSELECTED;
            directionC = StreetsideColorScheme.SEQ_UNSELECTED_CA;
        }

        // Paint direction indicator
        final var alpha = 0.75f;
        final int type = AlphaComposite.SRC_OVER;
        final var composite = AlphaComposite.getInstance(type, alpha);
        g.setComposite(composite);
        g.setColor(directionC);
        g.fillArc(point.x - CA_INDICATOR_RADIUS, point.y - CA_INDICATOR_RADIUS, 2 * CA_INDICATOR_RADIUS,
                2 * CA_INDICATOR_RADIUS, (int) (90 - /*img.getMovingHe()*/img.heading() - CA_INDICATOR_ANGLE / 2d),
                CA_INDICATOR_ANGLE);
        // Paint image marker
        g.setColor(markerC);
        g.fillOval(point.x - IMG_MARKER_RADIUS, point.y - IMG_MARKER_RADIUS, 2 * IMG_MARKER_RADIUS,
                2 * IMG_MARKER_RADIUS);

        // Paint highlight for selected or highlighted images
        if (img.equals(getData().getHighlightedImage())) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawOval(point.x - IMG_MARKER_RADIUS, point.y - IMG_MARKER_RADIUS, 2 * IMG_MARKER_RADIUS,
                    2 * IMG_MARKER_RADIUS);
        }
    }

    @Override
    public Icon getIcon() {
        return StreetsidePlugin.LOGO.setSize(ImageSizes.LAYER).get();
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void mergeFrom(Layer from) {
        throw new UnsupportedOperationException("This layer does not support merging yet");
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[] { LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(), new LayerListPopup.InfoAction(this) };
    }

    @Override
    public Object getInfoComponent() {
        return I18n.tr("Streetside layer") + '\n'
                + I18n.tr("{0} downloaded images", getData().getImages().stream().filter(Objects::nonNull).count())
                + "\n= " + I18n.tr("{0} images in total", getData().getImages().size());
    }

    @Override
    public String getToolTipText() {
        return I18n.tr("{0} images", getData().getImages().size());
    }

    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        if (MainApplication.getLayerManager().getActiveLayer() == this) {
            StreetsideUtils.updateHelpText();
        }

        if (MainApplication.getLayerManager().getEditLayer() != e.getPreviousDataLayer()) {
            if (MainApplication.getLayerManager().getEditLayer() != null) {
                MainApplication.getLayerManager().getEditLayer().getDataSet().addDataSetListener(DATASET_LISTENER);
            }
            if (e.getPreviousDataLayer() != null) {
                e.getPreviousDataLayer().getDataSet().removeDataSetListener(DATASET_LISTENER);
            }
        }
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {
        // Streetside currently doesn't care about this
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.plugins.streetside.StreetsideDataListener#imagesAdded()
     */
    @Override
    public void imagesAdded() {
        updateNearestImages();
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.josm.plugins.streetside.StreetsideDataListener#selectedImageChanged(
     *      org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage,
     *      org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage)
     */
    @Override
    public void selectedImageChanged(StreetsideImage oldImage, StreetsideImage newImage) {
        updateNearestImages();
    }

    /**
     * Returns the closest images belonging to a different sequence and
     * different from the specified target image.
     *
     * @param target the image for which you want to find the nearest other images
     * @param limit  the maximum length of the returned array
     * @return An array containing the closest images belonging to different sequences sorted by distance from target.
     */
    private StreetsideImage[] getNearestImagesFromDifferentSequences(StreetsideImage target, int limit) {
        return data.search(target, 0.01).parallelStream().filter(i -> !target.equals(i))
                // Filters out images too far away from target
                .filter(img -> img != null
                        && img.greatCircleDistance(target) < StreetsideProperties.SEQUENCE_MAX_JUMP_DISTANCE.get())
                .sorted(new NearestImgToTargetComparator(target)).map(StreetsideImage.class::cast).limit(limit)
                .toArray(StreetsideImage[]::new);
    }

    private synchronized void updateNearestImages() {
        final StreetsideImage selected = data.getSelectedImage();
        if (selected != null) {
            nearestImages = getNearestImagesFromDifferentSequences(selected, 2);
        } else {
            nearestImages = new StreetsideImage[0];
        }
        if (MainApplication.isDisplayingMapView()) {
            StreetsideMainDialog.getInstance().redButton.setEnabled(nearestImages.length >= 1);
            StreetsideMainDialog.getInstance().blueButton.setEnabled(nearestImages.length >= 2);
        }
        if (nearestImages.length >= 1) {
            CacheUtils.downloadPicture(nearestImages[0]);
            if (nearestImages.length >= 2) {
                CacheUtils.downloadPicture(nearestImages[1]);
            }
        }
    }

    private record NearestImgToTargetComparator(StreetsideAbstractImage target) implements Comparator<StreetsideAbstractImage> {

    @Override
    public int compare(StreetsideAbstractImage img1, StreetsideAbstractImage img2) {
        return (int) Math.signum(img1.greatCircleDistance(target) - img2.greatCircleDistance(target));
    }
}}
