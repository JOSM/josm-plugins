// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.streetside.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class StreetsideURLTest {
    @Test
    void testParseNextFromHeaderValue() throws MalformedURLException {
        String headerVal = "<https://a.streetside.com/v3/sequences?page=1&per_page=200"
                + "&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"first\", "
                + "<https://a.streetside.com/v3/sequences?page=2&per_page=200"
                + "&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"prev\", "
                + "<https://a.streetside.com/v3/sequences?page=4&per_page=200"
                + "&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2>; rel=\"next\"";
        assertEquals(URI.create(
                "https://a.streetside.com/v3/sequences?page=4&per_page=200&client_id=TG1sUUxGQlBiYWx2V05NM0pQNUVMQTo2NTU3NTBiNTk1NzM1Y2U2")
                .toURL(), StreetsideURL.APIv3.parseNextFromLinkHeaderValue(headerVal));
    }

    @Test
    void testParseNextFromHeaderValue2() throws MalformedURLException, URISyntaxException {
        String headerVal = "<https://urlFirst>; rel=\"first\", " + "rel = \"next\" ; < ; , "
                + "rel = \"next\" ; <https://urlNext> , " + "<https://urlPrev>; rel=\"prev\"";
        assertEquals(new URI("https://urlNext").toURL(), StreetsideURL.APIv3.parseNextFromLinkHeaderValue(headerVal));
    }

    @Test
    void testParseNextFromHeaderValueNull() {
        assertNull(StreetsideURL.APIv3.parseNextFromLinkHeaderValue(null));
    }

    @Test
    void testParseNextFromHeaderValueMalformed() {
        assertNull(StreetsideURL.APIv3.parseNextFromLinkHeaderValue("<###>; rel=\"next\", blub"));
    }

    @Disabled
    @Test
    void testBrowseImageURL() throws MalformedURLException {
        fail("Needs editing for MS Streetside");
        // assertEquals(new URL("https://www.streetside.com/map/im/1234567890123456789012"),
        // StreetsideURL.MainWebsite.browseImage("1234567890123456789012"));
    }

    @Test
    void testIllegalBrowseImageURL() {
        assertThrows(IllegalArgumentException.class, () -> StreetsideURL.MainWebsite.browseImage(null));
    }

    @Test
    void testString2MalformedURL() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            NoSuchMethodException, SecurityException {
        Method method = StreetsideURL.class.getDeclaredMethod("string2URL", String[].class);
        method.setAccessible(true);
        // this simply invokes string2URL("malformed URL")
        Assertions.assertNull(method.invoke(null, (Object) new String[] { "malformed URL" }));
        // invokes string2URL(null)
        Assertions.assertNull(method.invoke(null, (Object) null));
    }

    @Test
    void testUtilityClass() {
        TestUtil.testUtilityClass(StreetsideURL.class);
        TestUtil.testUtilityClass(StreetsideURL.APIv3.class);
        TestUtil.testUtilityClass(StreetsideURL.MainWebsite.class);
    }
}
