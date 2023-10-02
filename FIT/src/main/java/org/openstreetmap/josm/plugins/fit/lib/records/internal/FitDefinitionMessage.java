// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

import java.util.List;

public record FitDefinitionMessage(short reserved, boolean littleEndian, int globalMessageNumber,
                                   List<FitField> fitFields, List<FitDeveloperField> fitDeveloperFields) {
}
