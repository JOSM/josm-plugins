// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

public record FitDevDoubleData(String fieldName, String unit, Double value) implements IFitDevData<Double> {
}
