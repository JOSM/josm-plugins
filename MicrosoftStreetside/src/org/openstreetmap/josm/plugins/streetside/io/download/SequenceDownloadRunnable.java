// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.io.download;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.streetside.StreetsideAbstractImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideData;
import org.openstreetmap.josm.plugins.streetside.StreetsideImage;
import org.openstreetmap.josm.plugins.streetside.StreetsideSequence;
import org.openstreetmap.josm.plugins.streetside.cubemap.CubemapUtils;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideProperties;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideSequenceIdGenerator;
import org.openstreetmap.josm.plugins.streetside.utils.StreetsideURL.APIv3;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.Logging;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class SequenceDownloadRunnable extends BoundsDownloadRunnable {

	private final StreetsideData data;

  private static final Function<Bounds, URL> URL_GEN = APIv3::searchStreetsideSequences;

  public SequenceDownloadRunnable(final StreetsideData data, final Bounds bounds) {
    super(bounds);
    this.data = data;
  }

  @Override
  public void run(final URLConnection con) throws IOException {
    if (Thread.interrupted()) {
      return;
    }

    StreetsideSequence seq = new StreetsideSequence(null);
    // TODO: re-add sequenceID
    //StreetsideSequence seq = new StreetsideSequence(StreetsideSequenceIdGenerator.generateId());

    // TODO: how can LatLon and heading / camera angles (he attribute) be set for a sequence?
    // and does it make sense? @rrh

    List<StreetsideImage> bubbleImages = new ArrayList<>();

    final long startTime = System.currentTimeMillis();

    ObjectMapper mapper = new ObjectMapper();
    // Creation of Jackson Object Mapper necessary for Silverlight 2.0 JSON Syntax parsing:
    // (no double quotes in JSON on attribute names)
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    // Allow unrecognized properties - won't break with addition of new attributes
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    try {

    JsonParser parser = mapper.getFactory().createParser(new BufferedInputStream(con.getInputStream()));
    if(parser.nextToken() != JsonToken.START_ARRAY) {
      parser.close();
      throw new IllegalStateException("Expected an array");
    }


    StreetsideImage previous = null;

    while (parser.nextToken() == JsonToken.START_OBJECT) {
        // read everything from this START_OBJECT to the matching END_OBJECT
        // and return it as a tree model ObjectNode
        ObjectNode node = mapper.readTree(parser);
        // Discard the first sequence ('enabled') - it does not contain bubble data
        if (node.get("id") != null && node.get("la") != null && node.get("lo") != null) {
          StreetsideImage image = new StreetsideImage(CubemapUtils.convertDecimal2Quaternary(node.path("id").asLong()), node.path("la").asDouble(), node.get("lo").asDouble());
          if(previous!=null) {
        	  // Analyze sequence behaviour
        	  //previous.setNext(image.)
          }
          image.setAd(node.path("ad").asInt());
          image.setAl(node.path("al").asDouble());
          image.setBl(node.path("bl").asText());
          image.setCd(node.path("cd").asLong());
          image.setHe(node.path("he").asDouble());
          image.setMl(node.path("ml").asInt());
          image.setNbn(node.findValuesAsText("nbn"));
          image.setNe(node.path("ne").asLong());
          image.setPbn(node.findValuesAsText("pbn"));
          image.setPi(node.path("pi").asDouble());
          image.setPr(node.path("pr").asLong());
          // TODO: inner class @rrh
          // image.setRn(node.path("rn").asText());
          image.setRo(node.path("ro").asDouble());

          // Add list of cubemap tile images to images
          List<StreetsideImage> tiles = new ArrayList<StreetsideImage>();

        // TODO: set previous and next @rrh

					EnumSet.allOf(CubemapUtils.CubemapFaces.class).forEach(face -> {

							for (int i = 0; i < 4; i++) {
								// Initialize four-tiled cubemap faces (four images per cube side with 18-length
								// Quadkey)
								//if (StreetsideProperties.CUBEFACE_SIZE.get().intValue() == 4) {
								if (!StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) {
									StreetsideImage tile = new StreetsideImage(
											String.valueOf(image.getId() + Integer.valueOf(i)));
									tiles.add(tile);
								}
								// Initialize four-tiled cubemap faces (four images per cub eside with 20-length
								// Quadkey)
								//if (StreetsideProperties.CUBEFACE_SIZE.get().intValue() == 16) {
								if (StreetsideProperties.SHOW_HIGH_RES_STREETSIDE_IMAGERY.get()) {
									for (int j = 0; j < 4; j++) {
										StreetsideImage tile = new StreetsideImage(String.valueOf(image.getId()
												+ face.getValue() + CubemapUtils.rowCol2StreetsideCellAddressMap
														.get(String.valueOf(Integer.valueOf(i).toString() + Integer.valueOf(j).toString()))));
										tiles.add(tile);
									}
								}
							}
					});

      	  bubbleImages.add(image);
          Logging.info("Added image with id <" + image.getId() + ">");
          // TODO: double check whether this pre-caches successfullly @rrh
          //StreetsideData.downloadSurroundingCubemaps(image);

        }
      }

    parser.close();

    //StreetsideImage[] images;

      // First load all of the 'bubbles' from the request as Streetside Images
      /*List<StreetsideImage> images = mapper
        .readValue(new BufferedInputStream(con.getInputStream()), new TypeReference<List<StreetsideImage>>() {});
      */


      //images = mapper.readValue(new BufferedInputStream(con.getInputStream()), StreetsideImage[].class);

      /*for (StreetsideImage image : bubbleImages) {
        image = JsonStreetsideSequencesDecoder.decodeBubbleData(image);
        if(image != null) bubbleImages.add(image);
      }*/

    } catch (JsonParseException e) {
      e.printStackTrace();
    } catch (JsonMappingException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    /** Top Level Bubble Metadata in Streetside are bubble (aka images) not Sequences
     *  so a sequence needs to be created and have images added to it. If the distribution
     *  of Streetside images is non-sequential, the Mapillary "Walking Action" may behave
     *  unpredictably.
     **/
    seq.add(bubbleImages);

    if (StreetsideProperties.CUT_OFF_SEQUENCES_AT_BOUNDS.get()) {
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
    Logging.info(I18n.tr("Sucessfully loaded {0} Microsoft Streetside images in {0} ",seq.getImages().size(),endTime-startTime%60));
  }

  @Override
  protected Function<Bounds, URL> getUrlGenerator() {
    return URL_GEN;
  }
}