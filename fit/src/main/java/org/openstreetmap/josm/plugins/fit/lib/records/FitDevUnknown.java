// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records;

public record FitDevUnknown(String fieldName, String unit, byte[] value) implements IFitDevData<byte[]> {
}
