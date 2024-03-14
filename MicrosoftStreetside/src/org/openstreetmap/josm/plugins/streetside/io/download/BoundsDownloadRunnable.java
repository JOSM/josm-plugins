// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.Function;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.plugins.streetside.StreetsidePlugin;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL.APIv3;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

public abstract class BoundsDownloadRunnable implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(BoundsDownloadRunnable.class.getCanonicalName());

    protected final Bounds bounds;

    protected BoundsDownloadRunnable(final Bounds bounds) {
        this.bounds = bounds;
    }

    protected abstract Function<Bounds, URL> getUrlGenerator();

    @Override
    public void run() {
        URL nextURL = getUrlGenerator().apply(bounds);
        if (Boolean.TRUE.equals(StreetsideProperties.DEBUGING_ENABLED.get())) {
            LOGGER.log(Logging.LEVEL_DEBUG, "Downloading bounds: URL: {0}", nextURL);
        }
        try {
            while (nextURL != null) {
                if (Thread.interrupted()) {
                    LOGGER.log(Logging.LEVEL_ERROR, "{0} for {1} interrupted!",
                            new Object[] { getClass().getSimpleName(), bounds });
                    return;
                }
                final URLConnection con = nextURL.openConnection();
                run(con);
                nextURL = APIv3.parseNextFromLinkHeaderValue(con.getHeaderField("Link"));
            }
        } catch (IOException e) {
            String message = "Could not read from URL " + nextURL + "!";
            LOGGER.log(Logging.LEVEL_WARN, message, e);
            if (!GraphicsEnvironment.isHeadless()) {
                new Notification(message).setIcon(StreetsidePlugin.LOGO.setSize(ImageSizes.LARGEICON).get())
                        .setDuration(Notification.TIME_LONG).show();
            }
        }
    }

    public abstract void run(final URLConnection connection) throws IOException;
}
