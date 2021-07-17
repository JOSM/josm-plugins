// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.http2;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.plugins.http2.Http2Client.Http2Response;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit test of {@link Http2Client}
 */
@BasicPreferences
class Http2ClientTest {

    /**
     * Unit test of {@link Http2Response#parseDate}
     */
    @Test
    void testParseDate() {
        assertEquals(786297600000L, Http2Response.parseDate("Thu, 01 Dec 1994 16:00:00 GMT"));
        assertEquals(783459811000L, Http2Response.parseDate("Sat, 29 Oct 1994 19:43:31 GMT"));
        assertEquals(784903526000L, Http2Response.parseDate("Tue, 15 Nov 1994 12:45:26 GMT"));
        // == RFC 2616 ==
        // All HTTP date/time stamps MUST be represented in Greenwich Mean Time (GMT), without exception.
        assertEquals(0L, Http2Response.parseDate("Thu, 01 Jan 1970 01:00:00 CET"));
        // == RFC 2616 ==
        // HTTP/1.1 clients and caches MUST treat other invalid date formats,
        // especially including the value "0", as in the past (i.e., "already
        // expired").
        assertEquals(0L, Http2Response.parseDate("0"));
        assertEquals(0L, Http2Response.parseDate("foo-bar"));
    }

    @Test
    void testCreateRequest() throws Exception {
        HttpRequest request = new Http2Client(new URL("https://foo.bar"), "GET").createRequest();
        assertEquals("GET", request.method());
        assertEquals(Duration.ofSeconds(30), request.timeout().get());
        assertEquals(new URI("https://foo.bar"), request.uri());
        assertEquals(Optional.empty(), request.version());
        Map<String, List<String>> headers = request.headers().map();
        assertEquals(2, headers.size());
        List<String> encodings = headers.get("Accept-Encoding");
        assertEquals(1, encodings.size());
        assertEquals("gzip, deflate", encodings.get(0));
        List<String> userAgents = headers.get("User-Agent");
        assertEquals(1, userAgents.size());
        assertTrue(userAgents.get(0).startsWith("JOSM/1.5 ("), userAgents.get(0));
        assertEquals("https://foo.bar GET", request.toString());
    }

    @Test
    void testCreateRequest_invalidURI() throws Exception {
        // From https://josm.openstreetmap.de/ticket/21126
        // URISyntaxException for URL not formatted strictly according to RFC2396
        // See chapter "2.4.3. Excluded US-ASCII Characters"
        assertTrue(assertThrows(IOException.class, () -> new Http2Client(
                new URL("https://commons.wikimedia.org/w/api.php?format=xml&action=query&list=geosearch&gsnamespace=6&gslimit=500&gsprop=type|name&gsbbox=52.2804692|38.1772755|52.269721|38.2045051"), "GET")
                .createRequest()).getCause().getMessage().startsWith("Illegal character in query at index 116:"));
    }
}
