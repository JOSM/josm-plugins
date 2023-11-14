// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideData;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideSequence;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideSequenceIdGenerator;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL.APIv3;
import org.openstreetmap.josm.tools.Logging;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;

public final class SequenceDownloadRunnable extends BoundsDownloadRunnable {
  private static final Logger LOG = Logger.getLogger(BoundsDownloadRunnable.class.getCanonicalName());
  private static final Function<Bounds, URL> URL_GEN = APIv3::searchStreetsideImages;
  private final StreetsideData data;

  public SequenceDownloadRunnable(final StreetsideData data, final Bounds bounds) {
    super(bounds);
    this.data = data;
  }

  @Override
  public void run(final URLConnection con) throws IOException {
    if (Thread.interrupted()) {
      return;
    }

    StreetsideSequence seq = new StreetsideSequence(StreetsideSequenceIdGenerator.generateId());

    List<StreetsideImage> bubbleImages = new ArrayList<>();

    final long startTime = System.currentTimeMillis();

    try (JsonParser parser = Json.createParser(con.getInputStream())) {
      if (!parser.hasNext() || parser.next() != JsonParser.Event.START_ARRAY) {
        throw new IllegalStateException("Expected an array");
      }

      StreetsideImage previous = null;

      while (parser.hasNext() && parser.next() == JsonParser.Event.START_OBJECT) {
        // read everything from this START_OBJECT to the matching END_OBJECT
        // and return it as a tree model ObjectNode
        JsonObject node = parser.getObject();
        // Discard the first sequence ('enabled') - it does not contain bubble data
        if (node.get("id") != null && node.get("la") != null && node.get("lo") != null) {
          StreetsideImage image = new StreetsideImage(
              CubemapUtils.convertDecimal2Quaternary(node.getJsonNumber("id").longValue()),
              new LatLon(node.getJsonNumber("la").doubleValue(), node.getJsonNumber("lo").doubleValue()),
              node.getJsonNumber("he").doubleValue());
          if (previous != null) {
            image.setPr(Long.parseLong(previous.getId()));
            previous.setNe(Long.parseLong(image.getId()));

          }
          previous = image;
          if (node.containsKey("ad"))
            image.setAd(node.getJsonNumber("ad").intValue());
          if (node.containsKey("al"))
            image.setAl(node.getJsonNumber("al").doubleValue());
          if (node.containsKey("bl"))
            image.setBl(node.getString("bl"));
          if (node.containsKey("ml"))
            image.setMl(node.getJsonNumber("ml").intValue());
          if (node.containsKey("ne"))
            image.setNe(node.getJsonNumber("ne").longValue());
          if (node.containsKey("pi"))
            image.setPi(node.getJsonNumber("pi").doubleValue());
          if (node.containsKey("pr"))
            image.setPr(node.getJsonNumber("pr").longValue());
          if (node.containsKey("ro"))
            image.setRo(node.getJsonNumber("ro").doubleValue());
          if (node.containsKey("nbn"))
              image.setNbn(node.getJsonArray("nbn").getValuesAs(JsonValue::toString));
          if (node.containsKey("pbn"))
              image.setPbn(node.getJsonArray("pbn").getValuesAs(JsonValue::toString));

          // Add list of cubemap tile images to images
          List<StreetsideImage> tiles = new ArrayList<>();

          EnumSet.allOf(CubemapUtils.CubemapFaces.class).forEach(face -> {

            for (int i = 0; i < 4; i++) {
              // Initialize four-tiled cubemap faces (four images per cube side with 18-length
              // Quadkey)
              if (Boolean.FALSE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
                StreetsideImage tile = new StreetsideImage(image.getId() + i);
                tiles.add(tile);
              }
              // Initialize four-tiled cubemap faces (four images per cub eside with 20-length
              // Quadkey)
              if (Boolean.TRUE.equals(StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get())) {
                for (int j = 0; j < 4; j++) {
                  StreetsideImage tile = new StreetsideImage(image.getId() + face.getValue()
                      + CubemapUtils.rowCol2StreetsideCellAddressMap
                          .get(i + Integer.toString(j)));
                  tiles.add(tile);
                }
              }
            }
          });

          bubbleImages.add(image);
          LOG.info("Added image with id <" + image.getId() + ">");
          if (Boolean.TRUE.equals(StreetsideProperties.PREDOWNLOAD_CUBEMAPS.get())) {
            StreetsideData.downloadSurroundingCubemaps(image);
          }
        } else {
          LOG.info(MessageFormat.format("Unparsable JSON node object: {0}", node));
        }
      }
    } catch (ClassCastException | JsonException e) {
      LOG.log(Logging.LEVEL_ERROR, e, () -> MessageFormat
          .format("JSON parsing error occurred during Streetside sequence download {0}", e.getMessage()));
    } catch (IOException e) {
      LOG.log(Logging.LEVEL_ERROR, e, () -> MessageFormat
          .format("Input/output error occurred during Streetside sequence download {0}", e.getMessage()));
    }

    /*
     * Top Level Bubble Metadata in Streetside are bubble (aka images) not Sequences
     *  so a sequence needs to be created and have images added to it. If the distribution
     *  of Streetside images is non-sequential, the Mapillary "Walking Action" may behave
     *  unpredictably.
     */
    seq.add(bubbleImages);

    if (Boolean.TRUE.equals(StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.get())) {
      for (StreetsideAbstractImage img : seq.getImages()) {
        if (bounds.contains(img.getLatLon())) {
          data.add(img);
        } else {
          seq.remove(img);
        }
      }
    } else {
      boolean sequenceCrossesThroughBounds = false;
      for (int i = 0; i < seq.getImages().size() && !sequenceCrossesThroughBounds; i++) {
        sequenceCrossesThroughBounds = bounds.contains(seq.getImages().get(i).getLatLon());
      }
      if (sequenceCrossesThroughBounds) {
        data.addAll(seq.getImages(), true);
      }
    }

    final long endTime = System.currentTimeMillis();
    LOG.info(MessageFormat.format("Successfully loaded {0} Microsoft Streetside images in {1} seconds.",
        seq.getImages().size(), (endTime - startTime) / 1000));
  }

  @Override
  protected Function<Bounds, URL> getUrlGenerator() {
    return URL_GEN;
  }
}
