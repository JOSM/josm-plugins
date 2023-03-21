// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils.api;


import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.streetside.utils.TestUtil;

class JsonDecoderTest {

  @Test
  void testUtilityClass() {
    TestUtil.testUtilityClass(JsonDecoder.class);
  }

  @Test
  void testDecodeDoublePair() {
    assertNull(JsonDecoder.decodeDoublePair(null));
  }

  static void assertDecodesToNull(Function<JsonObject, ?> function, String...parts) {
    assertNull(function.apply(
      Json.createReader(new ByteArrayInputStream(String.join(" ", parts).getBytes(StandardCharsets.UTF_8))).readObject()
    ));
  }

}
