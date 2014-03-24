// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.licenses;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.Icon;

import org.openstreetmap.josm.plugins.opendata.core.util.OdUtils;

public abstract class License {
	
	public static final ODbL ODbL = new ODbL();
	public static final LOOL LOOL = new LOOL();
	
	private final Map<String, URL> urls = new HashMap<String, URL>();
	private final Map<String, URL> summaryURLs = new HashMap<String, URL>();
	
	private Icon icon;
	
	private static final URL getURL(Map<String, URL> map) {
		// Find URL for current language
		String lang = OdUtils.getJosmLanguage();
		for (String l : map.keySet()) {
			if (lang.startsWith(l)) {
				return map.get(l);
			}
		}
		// If not found, return english URL
		URL url = map.get("en");
		if (url != null) {
			return url;
		}
		// If not found, return first non-null url
		if (map.keySet().size() > 0) {
			for (Iterator<String> it=map.keySet().iterator(); it.hasNext(); ) {
				url = map.get(it.next());
				if (url != null) {
					return url;
				}
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
