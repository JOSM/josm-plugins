//    JOSM opendata plugin.
//    Copyright (C) 2011-2012 Don-vip
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.opendata.modules.fr.toulouse;

import org.openstreetmap.josm.plugins.opendata.core.datasets.DataSetCategory;
import org.openstreetmap.josm.plugins.opendata.core.datasets.fr.FrenchConstants;

public interface ToulouseConstants extends FrenchConstants {
    
    /**
     * Sources and Refs
     */
    public static final String SOURCE_GRAND_TOULOUSE = "GrandToulouse";
    public static final String SOURCE_TOULOUSE_METROPOLE = "ToulouseMetropole";
    public static final String SOURCE_TISSEO = "Tisséo SMTC";
    public static final String REF_TOULOUSE_METROPOLE = "ref:FR:ToulouseMetropole";
    
    /**
     * Wiki
     */
    public static final String WIKI = "http://wiki.openstreetmap.org/wiki/Toulouse/ToulouseMetropoleData";

    /**
     * Portal
     */
    public static final String PORTAL = "http://data.toulouse-metropole.fr";

    /**
     * Icons
     */
    public static final String ICON_CROIX_16 = "data.fr.toulouse_16.png";
    public static final String ICON_CROIX_24 = "data.fr.toulouse_24.png";

    /**
     * NEPTUNE XML Schema modified to accept Tisséo files
     */
    public static final String TOULOUSE_NEPTUNE_XSD = "/neptune_toulouse/neptune.xsd";
    
    /**
     * Categories
     */
    public static final DataSetCategory CAT_ASSOCIATIONS = new DataSetCategory("Associations", "styles/standard/people.png");
    public static final DataSetCategory CAT_CITOYENNETE = new DataSetCategory("Citoyenneté", "presets/townhall.png");
    public static final DataSetCategory CAT_CULTURE = new DataSetCategory("Culture", "presets/arts_centre.png");
    public static final DataSetCategory CAT_ENFANCE = new DataSetCategory("Enfance", "presets/kindergarten.png");
    public static final DataSetCategory CAT_ENVIRONNEMENT = new DataSetCategory("Environnement", "presets/recycling.png");
    public static final DataSetCategory CAT_PATRIMOINE = new DataSetCategory("Patrimoine", "presets/ruins.png");
    public static final DataSetCategory CAT_SERVICES = new DataSetCategory("Services", "styles/standard/vehicle/services.png");
    public static final DataSetCategory CAT_SPORT = new DataSetCategory("Sport", "presets/soccer.png");
    public static final DataSetCategory CAT_TOPOGRAPHIE = new DataSetCategory("Topographie", "presets/peak.png");
    public static final DataSetCategory CAT_TRANSPORT = new DataSetCategory("Transport", "presets/bus.png");
    public static final DataSetCategory CAT_URBANISME = new DataSetCategory("Urbanisme", "presets/places.png");
}
