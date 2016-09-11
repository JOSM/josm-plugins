// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.io.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.utils.MapillaryURL;

/**
 * This thread downloads one of the images in a given area.
 *
 * @author nokutu
 * @see MapillarySquareDownloadManagerThread
 */
public class MapillaryImageInfoDownloadThread extends Thread {
  private final Bounds bounds;
  private final int page;
  private final ExecutorService ex;

  /**
   * Main constructor.
   *
   * @param ex {@link ExecutorService} object that is executing this thread.
   * @param bounds the bounds inside which the image info should be downloaded
   * @param page the pagenumber of the results that should be retrieved
   */
  public MapillaryImageInfoDownloadThread(ExecutorService ex, Bounds bounds, int page) {
    this.bounds = bounds;
    this.page = page;
    this.ex = ex;
  }

  @Override
  public void run() {
    try (
      BufferedReader br = new BufferedReader(new InputStreamReader(
        MapillaryURL.searchImageInfoURL(bounds, page, null).openStream(), "UTF-8"
      ));
    ) {
      try (JsonReader reader = Json.createReader(br)) {
        JsonObject jsonObj = reader.readObject();
        if (!jsonObj.getBoolean("more"))
          this.ex.shutdown();
        JsonArray jsonArr = jsonObj.getJsonArray("ims");
        JsonObject data;
        for (int i = 0; i < jsonArr.size(); i++) {
          data = jsonArr.getJsonObject(i);
          String key = data.getString("key");
          for (MapillaryAbstractImage image : MapillaryLayer.getInstance().getData().getImages()) {
            if (
              image instanceof MapillaryImage
                && ((MapillaryImage) image).getKey().equals(key)
                && ((MapillaryImage) image).getUser() == null
              ) {
              ((MapillaryImage) image).setUser(data.getString("user"));
              ((MapillaryImage) image).setCapturedAt(data.getJsonNumber("captured_at").longValue());
              if (!data.isNull("location")) {
                ((MapillaryImage) image).setLocation(data.getString("location"));
              }
            }
          }
        }
      }
    } catch (IOException e) {
      Main.error(e);
    }
  }
}
