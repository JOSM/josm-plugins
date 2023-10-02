// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

import static org.openstreetmap.josm.plugins.fit.lib.utils.NumberUtils.checkShort;

import org.openstreetmap.josm.plugins.fit.lib.FitBaseType;

/**
 * A FIT field
 *
 * @param fieldDefinitionNumber The field definition number
 * @param size                  The size (in bytes) of the fit message field
 * @param baseType              The base type of the FIT message. See {@link FitBaseType} and {@link #fitBaseType()}.
 * @param fitBaseType           The parsed base type of the FIT message.
 */
public record FitField(short fieldDefinitionNumber, short size, short baseType, FitBaseType fitBaseType) implements IField {
    public FitField(int fieldDefinitionNumber, int size, int baseType) {
        this(checkShort(fieldDefinitionNumber), checkShort(size), checkShort(baseType), FitBaseType.fromBaseTypeField(baseType));
    }

}
