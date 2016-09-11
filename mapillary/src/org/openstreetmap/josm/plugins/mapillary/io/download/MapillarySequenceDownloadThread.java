// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.io.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryData;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.MapillarySequence;
import org.openstreetmap.josm.plugins.mapillary.utils.MapillaryURL;

/**
 * This thread downloads all the pictures in a given sequence and creates the
 * needed MapillaryImage and MapillarySequence objects. It just stores the ones
 * in the given area.
 *
 * @author nokutu
 * @see MapillarySquareDownloadManagerThread
 */
public class MapillarySequenceDownloadThread extends Thread {
  private final Bounds bounds;
  private final int page;
  private final ExecutorService ex;

  /**
   * Main constructor.
   *
   * @param ex {@link ExecutorService} executing this thread.
   * @param bounds The bounds inside which the sequences should be downloaded
   * @param page the pagenumber of the results that should be retrieved
   */
  public MapillarySequenceDownloadThread(ExecutorService ex, Bounds bounds, int page) {
    this.bounds = bounds;
    this.page = page;
    this.ex = ex;
  }

  @Override
  public void run() {
    try (
      BufferedReader br = new BufferedReader(new InputStreamReader(
        MapillaryURL.searchSequenceURL(bounds, page).openStream(), "UTF-8"
      ))
    ) {
      JsonObject jsonall = Json.createReader(br).readObject();

      if (!jsonall.getBoolean("more") && !this.ex.isShutdown())
        this.ex.shutdown();
      JsonArray jsonseq = jsonall.getJsonArray("ss");
      boolean isSequenceWrong = false;
      for (int i = 0; i < jsonseq.size(); i++) {
        JsonObject jsonobj = jsonseq.getJsonObject(i);
        JsonArray cas = jsonobj.getJsonArray("cas");
        JsonArray coords = jsonobj.getJsonArray("coords");
        JsonArray keys = jsonobj.getJsonArray("keys");
        ArrayList<MapillaryImage> images = new ArrayList<>();
        for (int j = 0; j < cas.size(); j++) {
          try {
            images.add(new MapillaryImage(
              keys.getString(j),
              new LatLon(
                coords.getJsonArray(j).getJsonNumber(1).doubleValue(),
                coords.getJsonArray(j).getJsonNumber(0).doubleValue()
              ),
              cas.getJsonNumber(j).doubleValue()));
          } catch (IndexOutOfBoundsException e) {
            Main.warn("Mapillary bug at " + MapillaryURL.searchSequenceURL(bounds, page));
            isSequenceWrong = true;
          }
        }
        if (isSequenceWrong)
          break;
        MapillarySequence sequence = new MapillarySequence(
          jsonobj.getString("key"), jsonobj.getJsonNumber("captured_at")
          .longValue());
        List<MapillaryImage> finalImages = new ArrayList<>(images);
        // Here it gets only those images which are in the downloaded
        // area.
        finalImages = images.parallelStream().filter(MapillarySequenceDownloadThread::isInside).collect(Collectors.toList());

        synchronized (MapillarySequenceDownloadThread.class) {
          synchronized (MapillaryAbstractImage.class) {
            for (MapillaryImage img : finalImages) {
              if (MapillaryLayer.getInstance().getData().getImages().contains(img)) {
                // The image in finalImages is substituted by the one in the
                // database, as they represent the same picture.

                final MapillaryImage lambdaImg = img;
                //noinspection OptionalGetWithoutIsPresent
                img = (MapillaryImage) MapillaryLayer.getInstance().getData().getImages().parallelStream().filter(source -> source.equals(lambdaImg)).findAny().get();

                sequence.add(img);
                img.setSequence(sequence);
                finalImages.set(finalImages.indexOf(img), img);
              } else {
                img.setSequence(sequence);
                sequence.add(img);
              }
            }
          }
        }

        MapillaryLayer.getInstance().getData().add(new ConcurrentSkipListSet<>(finalImages), false);
      }
    } catch (IOException e) {
      Main.error(String.format(
        "Error reading the url %s, this might be a Mapillary problem.",
        MapillaryURL.searchSequenceURL(bounds, page)
      ), e);
    }
    MapillaryData.dataUpdated();
  }

  private static boolean isInside(MapillaryAbstractImage image) {
    return MapillaryLayer.getInstance().getData().getBounds().parallelStream().anyMatch(b -> b.contains(image.getLatLon()));
  }
}
