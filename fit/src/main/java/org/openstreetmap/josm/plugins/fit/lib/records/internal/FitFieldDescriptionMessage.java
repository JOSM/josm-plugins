// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * Define the meaning of data in a dev field
 *
 * @param developerDataIndex    The index of the developer for this message
 * @param fieldDefinitionNumber The field number for this message
 * @param baseTypeId            The base type of this field
 * @param fieldName             The name of the field
 * @param units                 The units of the field
 * @param nativeFieldNumber     The equivalent native field number
 */
record FitFieldDescriptionMessage(short developerDataIndex, short fieldDefinitionNumber, short baseTypeId,
                                  String fieldName,
                                  String units, short nativeFieldNumber) {
}
