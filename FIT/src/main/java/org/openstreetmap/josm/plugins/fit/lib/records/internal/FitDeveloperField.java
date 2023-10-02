// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.fit.lib.records.internal;

/**
 * Define the meaning of data (e.g. field name, units, and base type)
 *
 * @param fieldNumber        Maps to {@code field_definition_number} of a {@code field_description} message
 * @param size               The size (in bytes) of the FIT message field
 * @param developerDataIndex Maps to the {@code developer_data_index} of a {@code developer_data_id} message.
 */
public record FitDeveloperField(short fieldNumber, short size, short developerDataIndex) implements IField {
}
