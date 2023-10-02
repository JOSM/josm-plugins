// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

public record FitDevFloatData(String fieldName, String unit, Float value) implements IFitDevData<Float> {
}
