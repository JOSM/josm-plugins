// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.imageryxmlbounds.io;

import java.io.IOException;

import javax.xml.transform.stream.StreamSource;

import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.imagery.ImageryReader;
import org.openstreetmap.josm.plugins.imageryxmlbounds.XmlBoundsConstants;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.SAXException;

/**
 * An extended Imagery Reader able to validate input against the JOSM Maps XSD.
 * @author Don-vip
 *
 */
public class ValidatingImageryReader extends ImageryReader implements XmlBoundsConstants {

    /**
     * Constructs a new {@code ValidatingImageryReader}.
     * @param source input source URL
     * @throws SAXException if a SAX error occurs during parsing
     * @throws IOException if any I/O error occurs
     */
    public ValidatingImageryReader(String source) throws SAXException, IOException {
        this(source, true);
    }

    /**
     * Constructs a new {@code ValidatingImageryReader}.
     * @param source input source URL
     * @param validateFirst if {@code true}, the source if validated first
     * @throws SAXException if a SAX error occurs during parsing
     * @throws IOException if any I/O error occurs
     */
    public ValidatingImageryReader(String source, boolean validateFirst) throws SAXException, IOException {
        super(source);
        if (validateFirst) {
            validate(source);
        }
    }

    /**
     * Validates input against the JOSM Maps XSD.
     * @param source input source URL
     * @throws SAXException if a SAX error occurs during parsing
     * @throws IOException if any I/O error occurs
     */
    public static void validate(String source) throws SAXException, IOException {
        try (CachedFile xmlSchema = new CachedFile(XML_SCHEMA)) {
            XmlUtils.newXmlSchemaFactory()
                .newSchema(new StreamSource(xmlSchema.getInputStream()))
                .newValidator().validate(new StreamSource(source));
        }
    }
}
