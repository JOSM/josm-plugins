// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

public record FitDevIntData(String fieldName, String unit, Integer value) implements IFitDevData<Integer> {
}
