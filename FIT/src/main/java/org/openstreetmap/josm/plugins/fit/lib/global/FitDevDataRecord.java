// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.global;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.openstreetmap.josm.plugins.fit.lib.records.IFitDevData;

public record FitDevDataRecord(IFitDevData<?>... fitDevData) {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof FitDevDataRecord r && Arrays.equals(this.fitDevData, r.fitDevData);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fitDevData);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '[' +
                Arrays.stream(this.fitDevData).map(Object::toString).collect(Collectors.joining(", "))
                + ']';
    }
}
