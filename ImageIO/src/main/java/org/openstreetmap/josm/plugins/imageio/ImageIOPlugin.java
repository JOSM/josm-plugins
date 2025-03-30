// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.imageio;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginHandler;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

import javax.imageio.ImageIO;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeSet;

/**
 * The entry point for the plugin
 */
public final class ImageIOPlugin extends Plugin {
    /** Used to check and see if we have ensured that the ImageIO plugins have been initialized */
    private static boolean initialized;

    /**
     * Creates the plugin
     *
     * @param info the plugin information describing the plugin.
     */
    public ImageIOPlugin(PluginInformation info) {
        super(info);
        initialize(false);
        MainApplication.worker.submit(() -> NexusDownloader.setDownloadDirectory(this.getPluginDirs().getCacheDirectory(true)));
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        initialize(false);
        return new ImageIOPreferenceSetting(() -> this.initialize(true));
    }

    @Override
    public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
        initialize(false);
        super.mapFrameInitialized(oldFrame, newFrame);
    }

    synchronized void initialize(boolean force) {
        // This is why we cannot do the initialization in the constructor; it currently returns null until
        // after the constructor is finished.
        final var pluginLoader = PluginHandler.getPluginClassLoader(getPluginInformation().getName());
        if ((!initialized || force) && pluginLoader != null && !Config.getPref().getList("imageio.classpath").isEmpty()) {
            final var urlList = Arrays.asList(pluginLoader.getURLs());
            final var loaded = new TreeSet<>(Config.getPref().getList("imageio.classpath"));
            final var missing = new TreeSet<String>();
            for (String classPath : loaded) {
                final var path = Path.of(classPath);
                if (!Files.isRegularFile(path)) {
                    missing.add(classPath);
                    continue;
                }
                try {
                    final var url = path.toUri().toURL();
                    if (!urlList.contains(url)) {
                        pluginLoader.addURL(url);
                    }
                } catch (MalformedURLException e) {
                    Config.getPref().put("imageio.classpath", null); // Reset since someone probably borked their config
                    throw new UncheckedIOException(e);
                }
            }
            if (!missing.isEmpty()) {
                loaded.removeAll(missing);
                Config.getPref().putList("imageio.classpath", new ArrayList<>(loaded));
            }
            final var currentLoader = Thread.currentThread().getContextClassLoader();

            try {
                Thread.currentThread().setContextClassLoader(pluginLoader);
                ImageIO.scanForPlugins();
            } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
            }
            initialized = true;
        }
    }
}
