// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class StreetsideURLTest {
    // TODO: replace with Streetside URL @rrh
    private static final String CLIENT_ID_QUERY_PART = "client_id=T1Fzd20xZjdtR0s1VDk5OFNIOXpYdzoxNDYyOGRkYzUyYTFiMzgz";

    public static class APIv3 {

        /*@Ignore
        @Test
        public void testSearchDetections() {
          assertUrlEquals(StreetsideURL.APIv3.searchDetections(null), "https://a.streetside.com/v3/detections", CLIENT_ID_QUERY_PART);
        }

        @Ignore
        @Test
        public void testSearchImages() {
          assertUrlEquals(StreetsideURL.APIv3.searchImages(null), "https://a.streetside.com/v3/images", CLIENT_ID_QUERY_PART);
        }

        @Ignore
        @Test
        public void testSubmitChangeset() throws MalformedURLException {
          assertEquals(
        new URL("https://a.streetside.com/v3/changesets?" + CLIENT_ID_QUERY_PART),
        StreetsideURL.APIv3.submitChangeset()
          );
        }*/
    }

    @Test
    void testParseNextFromHeaderValue() throws MalformedURLException {
        String headerVal = "<https://a.streetside.com/v3/sequences?page=1&per_page=200&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"first\", "
                + "<https://a.streetside.com/v3/sequences?page=2&per_page=200&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"prev\", "
                + "<https://a.streetside.com/v3/sequences?page=4&per_page=200&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"next\"";
        assertEquals(new URL(
                "https://a.streetside.com/v3/sequences?page=4&per_page=200&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2"),
                StreetsideURL.APIv3.parseNextFromLinkHeaderValue(headerVal));
    }

    @Test
    void testParseNextFromHeaderValue2() throws MalformedURLException {
        String headerVal = "<https://urlFirst>; rel=\"first\", " + "rel = \"next\" ; < ; , "
                + "rel = \"next\" ; <https://urlNext> , " + "<https://urlPrev>; rel=\"prev\"";
        assertEquals(new URL("https://urlNext"), StreetsideURL.APIv3.parseNextFromLinkHeaderValue(headerVal));
    }

    @Test
    void testParseNextFromHeaderValueNull() {
        assertNull(StreetsideURL.APIv3.parseNextFromLinkHeaderValue(null));
    }

    @Test
    void testParseNextFromHeaderValueMalformed() {
        assertNull(StreetsideURL.APIv3.parseNextFromLinkHeaderValue("<###>; rel=\"next\", blub"));
    }

    /*public static class Cloudfront {
    @Ignore
    @Test
    public void testThumbnail() {
      assertUrlEquals(StreetsideURL.VirtualEarth.streetsideTile("arbitrary_key", true), "https://d1cuyjsrcm0gby.cloudfront.net/arbitrary_key/thumb-2048.jpg");
      assertUrlEquals(StreetsideURL.VirtualEarth.streetsideTile("arbitrary_key2", false), "https://d1cuyjsrcm0gby.cloudfront.net/arbitrary_key2/thumb-320.jpg");
    }
    }*/

    @Disabled
    @Test
    void testBrowseImageURL() throws MalformedURLException {
        assertEquals(new URL("https://www.streetside.com/map/im/1234567890123456789012"),
                StreetsideURL.MainWebsite.browseImage("1234567890123456789012"));
    }

    @Test
    void testIllegalBrowseImageURL() {
        assertThrows(IllegalArgumentException.class, () -> StreetsideURL.MainWebsite.browseImage(null));
    }

    @Disabled
    @Test
    void testConnectURL() {
        /*assertUrlEquals(
        StreetsideURL.MainWebsite.connect("http://redirect-host/ä"),
        "https://www.streetside.com/connect",
        CLIENT_ID_QUERY_PART,
        "scope=user%3Aread+public%3Aupload+public%3Awrite",
        "response_type=token",
        "redirect_uri=http%3A%2F%2Fredirect-host%2F%C3%A4"
        );

        assertUrlEquals(
        StreetsideURL.MainWebsite.connect(null),
        "https://www.streetside.com/connect",
        CLIENT_ID_QUERY_PART,
        "scope=user%3Aread+public%3Aupload+public%3Awrite",
        "response_type=token"
        );

        assertUrlEquals(
        StreetsideURL.MainWebsite.connect(""),
        "https://www.streetside.com/connect",
        CLIENT_ID_QUERY_PART,
        "scope=user%3Aread+public%3Aupload+public%3Awrite",
        "response_type=token"
        );*/
    }

    @Disabled
    @Test
    void testUploadSecretsURL() throws MalformedURLException {
        /*assertEquals(
        new URL("https://a.streetside.com/v2/me/uploads/secrets?"+CLIENT_ID_QUERY_PART),
        StreetsideURL.uploadSecretsURL()
        );*/
    }

    @Disabled
    @Test
    void testUserURL() throws MalformedURLException {
        /*assertEquals(
        new URL("https://a.streetside.com/v3/me?"+CLIENT_ID_QUERY_PART),
        StreetsideURL.APIv3.userURL()
        );*/
    }

    @Test
    void testString2MalformedURL() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        Method method = StreetsideURL.class.getDeclaredMethod("string2URL", String[].class);
        method.setAccessible(true);
        Assertions.assertNull(method.invoke(null, new Object[] { new String[] { "malformed URL" } })); // this simply invokes string2URL("malformed URL")
        Assertions.assertNull(method.invoke(null, new Object[] { null })); // invokes string2URL(null)
    }

    @Test
    void testUtilityClass() {
        TestUtil.testUtilityClass(StreetsideURL.class);
        TestUtil.testUtilityClass(StreetsideURL.APIv3.class);
        TestUtil.testUtilityClass(StreetsideURL.VirtualEarth.class);
        TestUtil.testUtilityClass(StreetsideURL.MainWebsite.class);
    }

    private static void assertUrlEquals(URL actualUrl, String expectedBaseUrl, String... expectedParams) {
        final String actualUrlString = actualUrl.toString();
        assertEquals(expectedBaseUrl,
                actualUrlString.contains("?") ? actualUrlString.substring(0, actualUrlString.indexOf('?'))
                        : actualUrlString);
        String[] actualParams = actualUrl.getQuery() == null ? new String[0] : actualUrl.getQuery().split("&");
        assertEquals(expectedParams.length, actualParams.length);
        for (int exIndex = 0; exIndex < expectedParams.length; exIndex++) {
            boolean parameterIsPresent = false;
            for (int acIndex = 0; !parameterIsPresent && acIndex < actualParams.length; acIndex++) {
                parameterIsPresent |= actualParams[acIndex].equals(expectedParams[exIndex]);
            }
            Assertions.assertTrue(parameterIsPresent, expectedParams[exIndex] + " was expected in the query string of "
                    + actualUrl + " but wasn't there.");
        }
    }
}
