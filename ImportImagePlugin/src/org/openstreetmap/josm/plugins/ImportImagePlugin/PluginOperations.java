// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.ImportImagePlugin;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.imageio.ImageIO;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CRSAuthorityFactory;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.WorldFileReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class provides methods for resampling operations, IO and stores important data.
 *
 * @author Christoph Beekmans, Fabian Kowitz, Anna Robaszkiewicz, Oliver Kuhn, Martin Ulitzny
 *
 */
public final class PluginOperations {

    // contains descriptions of all available CRS
    static List<String> crsDescriptions;

    // the standard native CRS of user images
    static CoordinateReferenceSystem defaultSourceCRS;
    // description of 'defaultSourceCRS'
    static String defaultSourceCRSDescription;

    public enum SUPPORTEDIMAGETYPES {
        tiff, tif, jpg, jpeg, bmp, png
    }

    public enum POSTFIXES_WORLDFILE {
        wld, jgw, jpgw, pgw, pngw, tfw, tifw, bpw, bmpw,
    }
    
    private PluginOperations() {
        // Hide default constructor for utilities classes
    }

    /**
     * Reprojects a GridCoverage to a given CRS.
     */
    public static GridCoverage2D reprojectCoverage(GridCoverage2D coverage,
            CoordinateReferenceSystem targetCrs) {

        // TODO: add category for NO_DATA values in coverage (transparency in image)

        GridCoverage2D destination;

        CoverageProcessor processor = new CoverageProcessor();
        ParameterValueGroup resampleParams = processor.getOperation("Resample").getParameters();

        // set parameters
        resampleParams.parameter("Source").setValue(coverage);
        resampleParams.parameter("CoordinateReferenceSystem").setValue(targetCrs);

        // resample coverage with given parameters
        destination = (GridCoverage2D) processor.doOperation(resampleParams);

        return destination;
    }

    /**
     * Creates a {@link GridCoverage2D} from a given file.
     * @param file The file to read from
     * @param refSys The reference system to use
     * @param failIfNoPrjFile {@code true} if we need to fail if no projection file is found
     * @throws IOException if the file could not be read
     * @return The 2d grid coverage of the file
     */
    public static GridCoverage2D createGridFromFile(File file, CoordinateReferenceSystem refSys, boolean failIfNoPrjFile) throws IOException {

        GridCoverage2D coverage;

        if (!file.exists()) throw new FileNotFoundException("File not found.");

        String extension;
        String fileNameWithoutExt;
        int dotPos = file.getAbsolutePath().lastIndexOf(".");
        extension = file.getAbsolutePath().substring(dotPos);
        fileNameWithoutExt = file.getAbsolutePath().substring(0, dotPos);

        /*------- switch for file type -----------*/
        if (".tif".equalsIgnoreCase(extension) || ".tiff".equalsIgnoreCase(extension)) {

            // try to read GeoTIFF:
            try {
                coverage = readGeoTiff(file, refSys);
                return coverage;
            } catch (DataSourceException dse) {
                if (!dse.getMessage().contains("Coordinate Reference System is not available")) {
                    Logging.error(dse);
                } else {
                    Logging.trace(dse);
                }
            }

            // file is no GeoTiff, searching for Worldfile and projection file:
            String[] postfixes = {"wld", "tfw", "tifw"};
            // try to read Worldfile:
            WorldFileReader tfwReader = null;
            for (String postfix : postfixes) {
                File prjFile = new File(fileNameWithoutExt + "." + postfix);
                if (prjFile.exists()) {
                    tfwReader = new WorldFileReader(prjFile);
                }
            }

            if (tfwReader == null) {
                throw new IOException("No Worldfile found.");
            }

            if (refSys == null) {
                // if no crs is delivered try to read projection file:
                refSys = readPrjFile(file);
                if (refSys == null) {
                    if (failIfNoPrjFile) throw new IOException("No projection file found.");
                    Logging.debug("no projection given, no projection file found; using unprojected file.");
                }
            }

            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new IOException("Cannot read image file " + file.getAbsolutePath());
            }

            // create Envelope
            double width = img.getWidth() * tfwReader.getXPixelSize();
            double height = img.getHeight() * (-tfwReader.getYPixelSize());
            double lowerLeft_x = tfwReader.getXULC();
            double lowerLeft_y = tfwReader.getYULC() - height;
            ReferencedEnvelope bbox = ReferencedEnvelope.rect(lowerLeft_x, lowerLeft_y, width, height, null);
            coverage = createGridCoverage(img, bbox, refSys);

        } else if (".jpg".equalsIgnoreCase(extension)
                || ".jpeg".equalsIgnoreCase(extension)) {
            String[] postfixes = {"wld", "jgw", "jpgw"};
            // try to read Worldfile:
            WorldFileReader tfwReader = null;
            for (String postfix : postfixes) {
                File prjFile = new File(fileNameWithoutExt + "." + postfix);
                if (prjFile.exists()) {
                    tfwReader = new WorldFileReader(prjFile);
                }
            }
            if (tfwReader == null) throw new IOException("No Worldfile found.");

            if (refSys == null) {
                // if no crs is delivered try to read projection file:
                refSys = readPrjFile(file);
                if (refSys == null) {
                    if (failIfNoPrjFile) throw new IOException("No projection file found.");
                    Logging.debug("no projection given, no projection file found; using unprojected file.");
                }
            }

            BufferedImage img = ImageIO.read(file);

            // create Envelope
            double width = img.getWidth() * tfwReader.getXPixelSize();
            double height = img.getHeight() * (-tfwReader.getYPixelSize());
            double lowerLeft_x = tfwReader.getXULC();
            double lowerLeft_y = tfwReader.getYULC() - height;
            ReferencedEnvelope bbox = ReferencedEnvelope.rect(lowerLeft_x, lowerLeft_y, width, height, null);
            coverage = createGridCoverage(img, bbox, refSys);

        } else if (".bmp".equalsIgnoreCase(extension)) {
            String[] postfixes = {"wld", "bmpw", "bpw"};
            // try to read Worldfile:
            WorldFileReader tfwReader = null;
            for (String postfix : postfixes) {
                File prjFile = new File(fileNameWithoutExt + "." + postfix);
                if (prjFile.exists()) {
                    tfwReader = new WorldFileReader(prjFile);
                }
            }
            if (tfwReader == null) throw new IOException("No Worldfile found.");

            if (refSys == null) {
                // if no crs is delivered try to read projection file:
                refSys = readPrjFile(file);
                if (refSys == null) {
                    if (failIfNoPrjFile) throw new IOException("No projection file found.");
                    Logging.debug("no projection given, no projection file found; using unprojected file.");
                }
            }

            BufferedImage img = ImageIO.read(file);

            // create Envelope
            double width = img.getWidth() * tfwReader.getXPixelSize();
            double height = img.getHeight() * (-tfwReader.getYPixelSize());
            double lowerLeft_x = tfwReader.getXULC();
            double lowerLeft_y = tfwReader.getYULC() - height;
            ReferencedEnvelope bbox = ReferencedEnvelope.rect(lowerLeft_x, lowerLeft_y, width, height, null);
            coverage = createGridCoverage(img, bbox, refSys);

        } else if (".png".equalsIgnoreCase(extension)) {

            String[] postfixes = {"wld", "pgw", "pngw"};
            // try to read Worldfile:
            WorldFileReader tfwReader = null;
            for (String postfix : postfixes) {
                File prjFile = new File(fileNameWithoutExt + "." + postfix);
                if (prjFile.exists()) {
                    tfwReader = new WorldFileReader(prjFile);
                }
            }
            if (tfwReader == null) throw new IOException("No Worldfile found.");

            if (refSys == null) {
                // if no crs is delivered try to read projection file:
                refSys = readPrjFile(file);
                if (refSys == null) {
                    if (failIfNoPrjFile) throw new IOException("No projection file found.");
                    Logging.debug("no projection given, no projection file found; using unprojected file.");
                }
            }

            BufferedImage img = ImageIO.read(file);

            // create Envelope
            double width = img.getWidth() * tfwReader.getXPixelSize();
            double height = img.getHeight() * (-tfwReader.getYPixelSize());
            double lowerLeft_x = tfwReader.getXULC();
            double lowerLeft_y = tfwReader.getYULC() - height;
            ReferencedEnvelope bbox = ReferencedEnvelope.rect(lowerLeft_x, lowerLeft_y, width, height, null);
            coverage = createGridCoverage(img, bbox, refSys);

        } else {
            throw new IOException("Image type not supported. Supported formats are: \n" +
                    Arrays.toString(SUPPORTEDIMAGETYPES.values()));
        }

        return coverage;
    }

    /**
     * Searches for a projection file (.prj) with the same name of 'file'
     * tries to parse it.
     *
     * @param file image file, not the real world file (will be searched)
     */
    public static CoordinateReferenceSystem readPrjFile(File file) throws IOException {
        CoordinateReferenceSystem refSys;

        String prjFilename;
        int dotPos = file.getAbsolutePath().lastIndexOf(".");
        prjFilename = file.getAbsolutePath().substring(0, dotPos) + ".prj";

        File prjFile = new File(prjFilename);
        if (!prjFile.exists()) return null;
        Logging.debug("Loading .prj file: " + prjFile.getAbsolutePath());

        try (BufferedReader br = Files.newBufferedReader(prjFile.toPath())) {
            StringBuilder sb = new StringBuilder();
            String content;
            while ((content = br.readLine()) != null) {
                sb.append(content);
            }
            refSys = CRS.parseWKT(sb.toString().trim());
        } catch (FactoryException e) {
            throw new IOException("Unable to parse prj-file: '" + prjFile.getName() + "'", e);
        }

        return refSys;
    }

    /**
     * Method for external use.
     */
    public static GridCoverage2D createGridCoverage(BufferedImage img, ReferencedEnvelope bbox, CoordinateReferenceSystem crs) {
        bbox.setCoordinateReferenceSystem(crs);
        return new GridCoverageFactory().create("", img, bbox);
    }

    /**
     * Method for reading a GeoTIFF file.
     *
     * @param refSys if delivered, the coverage will be forced to use this crs
     */
    public static GridCoverage2D readGeoTiff(File file, CoordinateReferenceSystem refSys) throws IOException {
        GridCoverage2D coverage;
        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
        if (refSys != null) {
            hints.put(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, refSys);
        }
        // don't use the EPSG-Factory because of wrong behaviour
        //hints.put(Hints.CRS_AUTHORITY_FACTORY, CRS.getAuthorityFactory(true));

        GeoTiffReader reader = new GeoTiffReader(file, hints);

        coverage = reader.read(null);

        return coverage;
    }

    /**
     * Loads CRS data from an EPSG database and creates descriptions for each one.
     */
    public static void loadCRSData(Properties pluginProps) {
        String defaultcrsString = pluginProps.getProperty("default_crs_srid");

        crsDescriptions = new Vector<>();
        Set<String> supportedCodes = CRS.getSupportedCodes("EPSG");
        CRSAuthorityFactory fac = CRS.getAuthorityFactory(false);

        for (String string : supportedCodes) {
            try {
                if ("WGS84(DD)".equals(string)) {
                    continue;
                }
                InternationalString desc = fac.getDescriptionText("EPSG:" + string);
                String description = desc.toString() + " [-EPSG:" + string + "-]";
                crsDescriptions.add(description);
                if (defaultcrsString != null && defaultcrsString.equalsIgnoreCase("EPSG:" + string)) {
                    boolean isEastingFirst = Boolean.parseBoolean(pluginProps.getProperty("default_crs_eastingfirst"));
                    defaultSourceCRS = CRS.decode("EPSG:" + string, isEastingFirst);
                    defaultSourceCRSDescription = description;
                }

            } catch (FactoryException e) {
                Logging.error("Error while loading EPSG data: " + e.getMessage());
                Logging.error(e);
            }
        }
    }
}
