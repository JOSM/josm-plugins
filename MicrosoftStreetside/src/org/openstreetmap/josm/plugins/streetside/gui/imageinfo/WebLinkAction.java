// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.gui.imageinfo;

import java.awt.event.ActionEvent;
import java.io.Serial;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * Open an image on Microsoft's Streetside website
 */
public class WebLinkAction extends AbstractAction {

    @Serial
    private static final long serialVersionUID = 6157320554869780625L;

    private static final Logger LOGGER = Logger.getLogger(WebLinkAction.class.getCanonicalName());

    private URL url;

    /**
     * Create a new web link
     * @param name The name to show the user
     * @param url The URL to open
     */
    public WebLinkAction(final String name, final URL url) {
        super(name, ImageProvider.get("link", ImageSizes.SMALLICON));
        setURL(url);
    }

    /**
     * Set the URL for this action
     * @param url the url to set
     */
    public final void setURL(URL url) {
        this.url = url;
        setEnabled(url != null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (this.url != null) {
                OpenBrowser.displayUrl(this.url.toURI());
            }
        } catch (URISyntaxException e1) {
            String msg = url + " in a browser";
            LOGGER.log(Logging.LEVEL_WARN, msg, e1);
            new Notification(msg).setIcon(JOptionPane.WARNING_MESSAGE).show();
        }
    }
}
