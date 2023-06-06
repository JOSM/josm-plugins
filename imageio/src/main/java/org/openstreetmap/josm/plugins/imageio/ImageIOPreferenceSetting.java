// License: GPL. For details, see LICENSE file.
// SPDX-License-Identifier: GPL-2.0-or-later
package org.openstreetmap.josm.plugins.imageio;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * The preferences class for the {@link ImageIOPlugin}
 */
final class ImageIOPreferenceSetting implements SubPreferenceSetting {
    private final Set<Artifact> imagePlugins = new TreeSet<>();
    private final Runnable callback;

    ImageIOPreferenceSetting(Runnable callback) {
        this.callback = callback;
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        final var panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel("<html><h4>" +
                        tr("Enable or disable additional image formats. Disabling an image format requires a restart.")
                + "</h4></html>"),
                GBC.eol().anchor(GBC.PAGE_START).insets(5, 5, 5, 5).fill(GBC.HORIZONTAL));
        panel.add(new JSeparator(JSeparator.HORIZONTAL), GBC.eol());
        final var checkboxes = new JPanel(new GridBagLayout());
        panel.add(new JScrollPane(checkboxes), GBC.eol().fill(GBC.BOTH));
        addWebPPanel(checkboxes);
        // Used to force the panel to put the checkboxes at the top of the screen.
        // There is probably a better way, but I don't know it off the top of my head.
        checkboxes.add(new JLabel(""), GBC.eol().fill(GBC.BOTH));

        getTabPreferenceSetting(gui).addSubTab(this, tr("ImageIO"), panel);
    }

    private void addWebPPanel(JPanel panel) {
        final var webp = new Artifact("com.twelvemonkeys.imageio", "imageio-webp", null);
        final var webpCheck = new JCheckBox(tr("webp (recommended, implementation provided by TwelveMonkeys, BSD-3-Clause)"));
        final var webpIterator = ImageIO.getImageReadersByFormatName("WEBP");
        webpCheck.setSelected(webpIterator.hasNext()
                && "com.twelvemonkeys.imageio.plugins.webp.WebPImageReader"
                    .equals(webpIterator.next().getClass().getCanonicalName()));
        if (webpCheck.isSelected()) {
            imagePlugins.add(webp);
        }
        webpCheck.addActionListener(l -> {
            if (webpCheck.isSelected()) {
                imagePlugins.add(webp);
            } else {
                imagePlugins.remove(webp);
            }
        });
        panel.add(webpCheck, GBC.eol().anchor(GBC.PAGE_START));
    }

    @Override
    public boolean ok() {
        final var classPath = new TreeSet<String>();
        for (var artifact : imagePlugins) {
            classPath.addAll(NexusDownloader.download(artifact));
        }
        final var oldConfig = new TreeSet<>(Config.getPref().getList("imageio.classpath"));
        if (Config.getPref().putList("imageio.classpath", new ArrayList<>(classPath))) {
            MainApplication.worker.execute(this.callback);
        }
        // If the user removed an ImageIO plugin, they need to restart to actually remove it.
        return !classPath.containsAll(oldConfig);
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getImageryPreference();
    }
}
