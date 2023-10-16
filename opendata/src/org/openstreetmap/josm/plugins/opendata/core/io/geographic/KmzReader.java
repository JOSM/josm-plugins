// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.core.io.geographic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.AbstractReader;
import org.openstreetmap.josm.tools.Logging;

/**
 * Read kmz files
 */
public final class KmzReader extends AbstractReader {

    private final ZipInputStream zis;

    /**
     * Create a new {@link KmzReader}
     * @param zis The stream to read
     */
    KmzReader(ZipInputStream zis) {
        this.zis = zis;
    }

    /**
     * Parse a dataset from a stream
     * @param in The stream to read
     * @param instance The progress monitor to update
     * @return The dataset
     * @throws IOException If there was an issue reading the stream
     * @throws XMLStreamException If there was an issue with the XML
     * @throws FactoryConfigurationError If there was an issue with creating the xml factory
     */
    public static DataSet parseDataSet(InputStream in, ProgressMonitor instance)
            throws IOException, XMLStreamException, FactoryConfigurationError {
        return new KmzReader(new ZipInputStream(in)).parseDoc(instance);
    }

    @Override
    protected DataSet doParseDataSet(InputStream source,
            ProgressMonitor progressMonitor) {
        return null;
    }

    private DataSet parseDoc(ProgressMonitor instance) throws IOException, XMLStreamException, FactoryConfigurationError {
        ZipEntry entry;
        do {
            entry = zis.getNextEntry();
            if (entry == null) {
                Logging.warn("No KML file found");
                return null;
            }
        } while (!entry.getName().toLowerCase(Locale.ROOT).endsWith(".kml"));
        long size = entry.getSize();
        byte[] buffer;
        if (size > 0) {
            buffer = new byte[(int) size];
            int off = 0;
            int count;
            while ((count = zis.read(buffer, off, (int) size)) > 0) {
                off += count;
                size -= count;
            }
        } else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b;
            do {
                b = zis.read();
                if (b != -1) {
                    out.write(b);
                }
            } while (b != -1);
            buffer = out.toByteArray();
        }

        return KmlReader.parseDataSet(new ByteArrayInputStream(buffer), instance);
    }
}
