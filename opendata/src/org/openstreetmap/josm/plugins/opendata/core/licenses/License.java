// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.licenses;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;

import org.openstreetmap.josm.plugins.opendata.core.util.OdUtils;

public abstract class License {

    public static final ODbL odbl = new ODbL();
    public static final LOOL lool = new LOOL();

    private final Map<String, URL> urls = new HashMap<>();
    private final Map<String, URL> summaryURLs = new HashMap<>();

    private Icon icon;

    private static URL getURL(Map<String, URL> map) {
        // Find URL for current language
        String lang = OdUtils.getJosmLanguage();
        for (Map.Entry<String, URL> entry : map.entrySet()) {
            if (lang.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        // If not found, return english URL
        URL url = map.get("en");
        if (url != null) {
            return url;
        }
        // If not found, return first non-null url
        if (!map.keySet().isEmpty()) {
            for (URL entryUrl : map.values()) {
                return entryUrl;
            }
        }
        // If empty, return null
        return null;
    }

    public URL getURL() {
        return getURL(urls);
    }

    public URL getSummaryURL() {
        return getURL(summaryURLs);
    }

    public final void setURL(URL url) {
        setURL(url, "en");
    }

    public final void setURL(String url, String lang) throws MalformedURLException {
        setURL(new URL(url), lang);
    }

    public final void setURL(String url) throws MalformedURLException {
        setURL(new URL(url), "en");
    }

    public final void setURL(URL url, String lang) {
        if (url != null) {
            urls.put(lang, url);
        }
    }

    public final void setSummaryURL(URL url) {
        setSummaryURL(url, "en");
    }

    public final void setSummaryURL(String url, String lang) throws MalformedURLException {
        setSummaryURL(new URL(url), lang);
    }

    public final void setSummaryURL(String url) throws MalformedURLException {
        setSummaryURL(new URL(url), "en");
    }

    public final void setSummaryURL(URL url, String lang) {
        if (url != null) {
            summaryURLs.put(lang, url);
        }
    }

    public final Icon getIcon() {
        return icon;
    }

    public final void setIcon(Icon icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "License [" + (urls != null ? "urls=" + urls + ", " : "")
                + (summaryURLs != null ? "summaryURLs=" + summaryURLs : "")
                + "]";
    }
}
