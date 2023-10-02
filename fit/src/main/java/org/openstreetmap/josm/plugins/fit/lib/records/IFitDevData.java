// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

/**
 * An interface for custom data types.
 */
public sealed
interface IFitDevData<V>
permits FitDevDoubleData, FitDevFloatData, FitDevIntData, FitDevLongData, FitDevStringData, FitDevUnknown
{

    String fieldName();

    String unit();

    V value();
}
