package org.openstreetmap.josm.plugins.imagery;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Class that stores info about a WMS server.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class ImageryInfo implements Comparable<ImageryInfo> {
    public enum ImageryType {
        WMS("wms"),
        TMS("tms"),
        HTML("html"),
        BING("bing");

        private String urlString;
        ImageryType(String urlString) {
            this.urlString = urlString;
        }
        public String getUrlString() {
            return urlString;
        }
    }

    String name;
    String url=null;
    String cookies = null;
    String eulaAcceptanceRequired = null;
    ImageryType imageryType = ImageryType.WMS;
    double pixelPerDegree = 0.0;
    int maxZoom = 0;

    public ImageryInfo(String name) {
        this.name=name;
    }

    public ImageryInfo(String name, String url) {
        this.name=name;
        setURL(url);
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired) {
        this.name=name;
        setURL(url);
        this.eulaAcceptanceRequired = eulaAcceptanceRequired;
    }

    public ImageryInfo(String name, String url, String eulaAcceptanceRequired, String cookies) {
        this.name=name;
        setURL(url);
        this.cookies=cookies;
    }

    public ImageryInfo(String name, String url, String cookies, double pixelPerDegree) {
        this.name=name;
        setURL(url);
        this.cookies=cookies;
        this.pixelPerDegree=pixelPerDegree;
    }

    public ArrayList<String> getInfoArray() {
        String e2 = null;
        String e3 = null;
        String e4 = null;
        if(url != null && !url.isEmpty()) e2 = getFullURL();
        if(cookies != null && !cookies.isEmpty()) e3 = cookies;
        if(imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
            if(pixelPerDegree != 0.0) e4 = String.valueOf(pixelPerDegree);
        } else {
            if(maxZoom != 0) e4 = String.valueOf(maxZoom);
        }
        if(e4 != null && e3 == null) e3 = "";
        if(e3 != null && e2 == null) e2 = "";

        ArrayList<String> res = new ArrayList<String>();
        res.add(name);
        if(e2 != null) res.add(e2);
        if(e3 != null) res.add(e3);
        if(e4 != null) res.add(e4);
        return res;
    }

    public ImageryInfo(Collection<String> list) {
        ArrayList<String> array = new ArrayList<String>(list);
        this.name=array.get(0);
        if(array.size() >= 2) setURL(array.get(1));
        if(array.size() >= 3) this.cookies=array.get(2);
        if(array.size() >= 4) {
            if (imageryType == ImageryType.WMS || imageryType == ImageryType.HTML) {
                this.pixelPerDegree=Double.valueOf(array.get(3));
            } else {
                this.maxZoom=Integer.valueOf(array.get(3));
            }
        }
    }

    public ImageryInfo(ImageryInfo i) {
        this.name=i.name;
        this.url=i.url;
        this.cookies=i.cookies;
        this.imageryType=i.imageryType;
        this.pixelPerDegree=i.pixelPerDegree;
    }

    @Override
    public int compareTo(ImageryInfo in)
    {
        int i = name.compareTo(in.name);
        if(i == 0)
            i = url.compareTo(in.url);
        if(i == 0)
            i = Double.compare(pixelPerDegree, in.pixelPerDegree);
        return i;
    }

    public boolean equalsBaseValues(ImageryInfo in)
    {
        return url.equals(in.url);
    }

    public void setPixelPerDegree(double ppd) {
        this.pixelPerDegree = ppd;
    }

    public void setURL(String url) {
        for (ImageryType type : ImageryType.values()) {
            if (url.startsWith(type.getUrlString() + ":")) {
                this.url = url.substring(type.getUrlString().length() + 1);
                this.imageryType = type;
                return;
            }
        }

        // Default imagery type is WMS
        this.url = url;
        this.imageryType = ImageryType.WMS;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getURL() {
        return this.url;
    }

    public String getCookies() {
        return this.cookies;
    }

    public double getPixelPerDegree() {
        return this.pixelPerDegree;
    }

    public int getMaxZoom() {
        return this.maxZoom;
    }

    public String getFullURL() {
        return imageryType.getUrlString() + ":" + url;
    }

    public String getToolbarName()
    {
        String res = name;
        if(pixelPerDegree != 0.0)
            res += "#PPD="+pixelPerDegree;
        return res;
    }

    public String getMenuName()
    {
        String res = name;
        if(pixelPerDegree != 0.0)
            res += " ("+pixelPerDegree+")";
        else if(maxZoom != 0)
            res += " (z"+maxZoom+")";
        return res;
    }

    public ImageryType getImageryType() {
        return imageryType;
    }
}
