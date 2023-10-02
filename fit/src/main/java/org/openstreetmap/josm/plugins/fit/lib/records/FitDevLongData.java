// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

public record FitDevLongData(String fieldName, String unit, Long value) implements IFitDevData<Long> {
}
