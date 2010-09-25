/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.jhlabs.map;

import java.io.*;

/**
 * A class representing a geographic ellipsoid.
 * Changes: Added Serializable interface by Bernhard Jenny, 18 May 2010
 */
public class Ellipsoid implements Cloneable, Serializable {

    public String name;
    public String shortName;
    public double equatorRadius = 1.0;
    public double poleRadius = 1.0;
    public double eccentricity = 1.0;
    public double eccentricity2 = 1.0;
    // From: USGS PROJ package.
    public final static Ellipsoid SPHERE = new Ellipsoid("sphere", 6371008.7714, 6371008.7714, 0.0, "Sphere");
    public final static Ellipsoid BESSEL = new Ellipsoid("bessel", 6377397.155, 0.0, 299.1528128, "Bessel 1841");
    public final static Ellipsoid CLARKE_1866 = new Ellipsoid("clrk66", 6378206.4, 6356583.8, 0.0, "Clarke 1866");
    public final static Ellipsoid CLARKE_1880 = new Ellipsoid("clrk80", 6378249.145, 0.0, 293.4663, "Clarke 1880 mod.");
    public final static Ellipsoid AIRY = new Ellipsoid("airy", 6377563.396, 6356256.910, 0.0, "Airy 1830");
    public final static Ellipsoid WGS_1960 = new Ellipsoid("WGS60", 6378165.0, 0.0, 298.3, "WGS 60");
    public final static Ellipsoid WGS_1966 = new Ellipsoid("WGS66", 6378145.0, 0.0, 298.25, "WGS 66");
    public final static Ellipsoid WGS_1972 = new Ellipsoid("WGS72", 6378135.0, 0.0, 298.26, "WGS 72");
    public final static Ellipsoid WGS_1984 = new Ellipsoid("WGS84", 6378137.0, 0.0, 298.257223563, "WGS 84");
    public final static Ellipsoid KRASOVSKY = new Ellipsoid("krass", 6378245.0, 298.3, 0.0, "Krassovsky, 1942");
    public final static Ellipsoid EVEREST = new Ellipsoid("evrst30", 6377276.345, 0.0, 300.8017, "Everest 1830");
    public final static Ellipsoid INTERNATIONAL_1967 = new Ellipsoid("new_intl", 6378157.5, 6356772.2, 0.0, "New International 1967");
    public final static Ellipsoid GRS_1980 = new Ellipsoid("GRS80", 6378137.0, 0.0, 298.257222101, "GRS 1980 (IUGG, 1980)");
    public final static Ellipsoid AUSTRALIAN = new Ellipsoid("australian", 6378160.0, 6356774.7, 298.25, "Australian");
    public final static Ellipsoid[] ellipsoids = {
        SPHERE,
        new Ellipsoid("MERIT", 6378137.0, 0.0, 298.257, "MERIT 1983"),
        new Ellipsoid("SGS85", 6378136.0, 0.0, 298.257, "Soviet Geodetic System 85"),
        GRS_1980,
        new Ellipsoid("IAU76", 6378140.0, 0.0, 298.257, "IAU 1976"),
        AIRY,
        new Ellipsoid("APL4.9", 6378137.0, 0.0, 298.25, "Appl. Physics. 1965"),
        new Ellipsoid("NWL9D", 6378145.0, 298.25, 0.0, "Naval Weapons Lab., 1965"),
        new Ellipsoid("mod_airy", 6377340.189, 6356034.446, 0.0, "Modified Airy"),
        new Ellipsoid("andrae", 6377104.43, 300.0, 0.0, "Andrae 1876 (Den., Iclnd.)"),
        new Ellipsoid("aust_SA", 6378160.0, 0.0, 298.25, "Australian Natl & S. Amer. 1969"),
        new Ellipsoid("GRS67", 6378160.0, 0.0, 298.2471674270, "GRS 67 (IUGG 1967)"),
        BESSEL,
        new Ellipsoid("bess_nam", 6377483.865, 0.0, 299.1528128, "Bessel 1841 (Namibia)"),
        CLARKE_1866,
        CLARKE_1880,
        new Ellipsoid("CPM", 6375738.7, 0.0, 334.29, "Comm. des Poids et Mesures 1799"),
        new Ellipsoid("delmbr", 6376428.0, 0.0, 311.5, "Delambre 1810 (Belgium)"),
        new Ellipsoid("engelis", 6378136.05, 0.0, 298.2566, "Engelis 1985"),
        EVEREST,
        new Ellipsoid("evrst48", 6377304.063, 0.0, 300.8017, "Everest 1948"),
        new Ellipsoid("evrst56", 6377301.243, 0.0, 300.8017, "Everest 1956"),
        new Ellipsoid("evrst69", 6377295.664, 0.0, 300.8017, "Everest 1969"),
        new Ellipsoid("evrstSS", 6377298.556, 0.0, 300.8017, "Everest (Sabah & Sarawak)"),
        new Ellipsoid("fschr60", 6378166.0, 0.0, 298.3, "Fischer (Mercury Datum) 1960"),
        new Ellipsoid("fschr60m", 6378155.0, 0.0, 298.3, "Modified Fischer 1960"),
        new Ellipsoid("fschr68", 6378150.0, 0.0, 298.3, "Fischer 1968"),
        new Ellipsoid("helmert", 6378200.0, 0.0, 298.3, "Helmert 1906"),
        new Ellipsoid("hough", 6378270.0, 0.0, 297.0, "Hough"),
        new Ellipsoid("intl", 6378388.0, 0.0, 297.0, "International 1909 (Hayford)"),
        KRASOVSKY,
        new Ellipsoid("kaula", 6378163.0, 0.0, 298.24, "Kaula 1961"),
        new Ellipsoid("lerch", 6378139.0, 0.0, 298.257, "Lerch 1979"),
        new Ellipsoid("mprts", 6397300.0, 0.0, 191.0, "Maupertius 1738"),
        INTERNATIONAL_1967,
        new Ellipsoid("plessis", 6376523.0, 6355863.0, 0.0, "Plessis 1817 France)"),
        new Ellipsoid("SEasia", 6378155.0, 6356773.3205, 0.0, "Southeast Asia"),
        new Ellipsoid("walbeck", 6376896.0, 6355834.8467, 0.0, "Walbeck"),
        WGS_1960,
        WGS_1966,
        WGS_1972,
        WGS_1984,
        new Ellipsoid("NAD27", 6378249.145, 0.0, 293.4663, "NAD27: Clarke 1880 mod."),
        new Ellipsoid("NAD83", 6378137.0, 0.0, 298.257222101, "NAD83: GRS 1980 (IUGG, 1980)"),};

    public Ellipsoid() {
    }

    // One of of poleRadius or reciprocalFlattening must be specified, the other zero
    public Ellipsoid(String shortName, double equatorRadius, double poleRadius, double reciprocalFlattening, String name) {
        this.shortName = shortName;
        this.name = name;
        this.equatorRadius = equatorRadius;
        this.poleRadius = poleRadius;
        if (reciprocalFlattening != 0) {
            double flattening = 1.0 / reciprocalFlattening;
            double f = flattening;
            eccentricity2 = 2 * f - f * f;
            poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricity2);
        } else {
            eccentricity2 = 1.0 - (poleRadius * poleRadius) / (equatorRadius * equatorRadius);
        }
        eccentricity = Math.sqrt(eccentricity2);
    }

    public Ellipsoid(String shortName, double equatorRadius, double eccentricity2, String name) {
        this.shortName = shortName;
        this.name = name;
        this.equatorRadius = equatorRadius;
        setEccentricitySquared(eccentricity2);
    }

    public Object clone() {
        try {
            Ellipsoid e = (Ellipsoid) super.clone();
            return e;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setEquatorRadius(double equatorRadius) {
        this.equatorRadius = equatorRadius;
    }

    public double getEquatorRadius() {
        return equatorRadius;
    }

    public void setEccentricitySquared(double eccentricity2) {
        this.eccentricity2 = eccentricity2;
        poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricity2);
        eccentricity = Math.sqrt(eccentricity2);
    }

    public double getEccentricitySquared() {
        return eccentricity2;
    }

    public String toString() {
        return name;
    }
}
