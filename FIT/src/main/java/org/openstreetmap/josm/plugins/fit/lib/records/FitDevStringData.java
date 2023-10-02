// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;


public record FitDevStringData(String fieldName, String unit, String value) implements IFitDevData<String> {
}
