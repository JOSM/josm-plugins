// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import org.openstreetmap.josm.plugins.fit.lib.records.IFitDevData;
import org.openstreetmap.josm.plugins.fit.lib.records.internal.IField;

public record FitUnknownRecord(int globalMessageNumber, FieldData[] data, FitDevDataRecord devData) implements FitData {

    public record FieldData(IField field, IFitDevData<?> data) {}
}
