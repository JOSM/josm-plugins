// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.Test;

/**
 * Tests the {@link OAuthPortListener} class.
 *
 * @author nokutu
 * @see OAuthPortListener
 *
 */
public class OAuthPortListenerTest {

  /**
   * Test that the threads responds when the browser makes the request.
   */
  @Test
  public void responseTest() {
    OAuthPortListener t = new OAuthPortListener(null);
    t.start();
    try {
      synchronized (this) {
        this.wait(500);
      }
    } catch (InterruptedException e1) {
    }
    try {
      URL url = new URL("http://localhost:"+OAuthPortListener.PORT+"?access_token=access_token");
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      BufferedReader in = new BufferedReader(new InputStreamReader(
          con.getInputStream()));
      in.readLine();
      assertEquals(OAuthPortListener.RESPONSE, in.readLine());
    } catch (Exception e) {
      fail();
    }
  }
}
