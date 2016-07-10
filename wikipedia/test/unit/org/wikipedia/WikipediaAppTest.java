package org.wikipedia;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.wikipedia.WikipediaApp.WikipediaEntry;
import org.wikipedia.WikipediaApp.WikipediaLangArticle;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class WikipediaAppTest {
    @Before
    public void setUp() throws Exception {
        Main.initApplicationPreferences();
    }

    @Test
    public void testMediawikiLocale() throws Exception {
        assertThat(WikipediaApp.getMediawikiLocale(Locale.GERMANY), is("de-de"));
        assertThat(WikipediaApp.getMediawikiLocale(Locale.GERMAN), is("de"));
        assertThat(WikipediaApp.getMediawikiLocale(Locale.UK), is("en-gb"));
        assertThat(WikipediaApp.getMediawikiLocale(Locale.CANADA), is("en-ca"));
    }

    @Test
    public void testPartitionList() {
        assertThat(
                WikipediaApp.partitionList(Arrays.asList(1, 2, 3, 4, 5), 2),
                is(Arrays.asList(
                        Arrays.asList(1, 2),
                        Arrays.asList(3, 4),
                        Arrays.asList(5)
                ))
        );
    }

    @Test
    public void testParseFromUrl1() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseFromUrl("https://de.wikipedia.org/wiki/Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromUrl2() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseFromUrl("http://de.wikipedia.org/wiki/%C3%96sterreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromUrl3() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseFromUrl("http://de.wikipedia.org/wiki/Sternheim_%26_Emanuel");
        assertThat(actual.article, is("Sternheim_&_Emanuel"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromUrl4() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseFromUrl("//de.wikipedia.org/wiki/Reichstagsgeb%C3%A4ude");
        assertThat(actual.article, is("Reichstagsgebäude"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag0() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseTag("wikipedia", "Österreich");
        assertThat(actual, nullValue());
    }

    @Test
    public void testParseFromTag1() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseTag("wikipedia", "de:Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag2() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseTag("wikipedia:de", "Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag3() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseTag("wikipedia:de", "de:Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testParseFromTag4() {
        final WikipediaLangArticle actual = WikipediaLangArticle.parseTag("wikipedia", "https://de.wikipedia.org/wiki/Österreich");
        assertThat(actual.article, is("Österreich"));
        assertThat(actual.lang, is("de"));
    }

    @Test
    public void testGetInterwikiArticles1() {
        final Collection<WikipediaLangArticle> iw = WikipediaApp.getInterwikiArticles("de", "Österreich");
        assertThat(iw, hasItem(new WikipediaLangArticle("en", "Austria")));
        assertThat(iw, hasItem(new WikipediaLangArticle("no", "Østerrike")));
        assertThat(iw, hasItem(new WikipediaLangArticle("ko", "오스트리아")));
    }

    @Test
    public void testGetInterwikiArticles2() {
        final Collection<WikipediaLangArticle> iw = WikipediaApp.getInterwikiArticles("en", "Ampersand");
        assertThat(iw, hasItem(new WikipediaLangArticle("fi", "&")));
    }

    @Test
    public void testGetCoordinates() throws Exception {
        assertThat(WikipediaApp.getCoordinateForArticle("de", "Marchreisenspitze"), is(new LatLon(47.1725, 11.30833333)));
        assertThat(WikipediaApp.getCoordinateForArticle("en", "Austria"), is(new LatLon(47.33333333, 13.33333333)));
        assertThat(WikipediaApp.getCoordinateForArticle("en", "Foobar2000"), nullValue());
    }

    @Test
    public void testGetBrowserUrl() {
        final WikipediaEntry entry = new WikipediaEntry("de", "Sternheim & Emanuel");
        assertThat(entry.getBrowserUrl(), is("https://de.wikipedia.org/wiki/Sternheim_%26_Emanuel"));
    }

    @Test
    public void testFromCoordinates() throws Exception {
        final List<WikipediaEntry> entries = WikipediaApp.getEntriesFromCoordinates("de",
                new LatLon(52.5179786, 13.3753321), new LatLon(52.5192215, 13.3768705));
        assertTrue(Utils.exists(entries, new Predicate<WikipediaEntry>() {
            @Override
            public boolean evaluate(WikipediaEntry entry) {
                return "Reichstagsgebäude".equals(entry.wikipediaArticle) && "de".equals(entry.wikipediaLang);
            }
        }));
    }

    @Test
    public void testForQuery() throws Exception {
        final List<WikipediaApp.WikidataEntry> de = WikipediaApp.getWikidataEntriesForQuery("de", "Österreich", Locale.GERMAN);
        final List<WikipediaApp.WikidataEntry> en = WikipediaApp.getWikidataEntriesForQuery("de", "Österreich", Locale.ENGLISH);
        assertThat(de.get(0).wikipediaArticle, is("Q40"));
        assertThat(de.get(0).wikipediaLang, is("wikidata"));
        assertThat(de.get(0).label, is("Österreich"));
        assertThat(de.get(0).description, is("Staat in Mitteleuropa"));
        assertThat(en.get(0).label, is("Austria"));
        assertThat(en.get(0).description, is("country in Central Europe"));
    }

    @Test
    public void testFromCoordinatesWikidata() throws Exception {
        final List<WikipediaEntry> entries = WikipediaApp.getEntriesFromCoordinates("wikidata",
                new LatLon(47.20, 11.30), new LatLon(47.22, 11.32));
        assertTrue(Utils.exists(entries, new Predicate<WikipediaEntry>() {
            @Override
            public boolean evaluate(WikipediaEntry entry) {
                return "Q865406".equals(entry.wikipediaArticle) && "wikidata".equals(entry.wikipediaLang) && "Birgitzer Alm".equals(entry.label);
            }
        }));
    }

    @Test
    public void testGetWikidataForArticles() throws Exception {
        final Map<String, String> map = WikipediaApp.getWikidataForArticles("en",
                Arrays.asList("London", "Vienna", "Völs, Tyrol", "a-non-existing-article"));
        assertThat(map.get("London"), is("Q84"));
        assertThat(map.get("Vienna"), is("Q1741"));
        assertThat(map.get("Völs, Tyrol"), is("Q278250"));
        assertThat(map.get("a-non-existing-article"), nullValue());
        assertThat(map.size(), is(4));
    }

    @Test
    public void testGetLabelForWikidata() throws Exception {
        assertThat(WikipediaApp.getLabelForWikidata("Q1741", Locale.GERMAN), is("Wien"));
        assertThat(WikipediaApp.getLabelForWikidata("Q1741", Locale.ENGLISH), is("Vienna"));
        // fallback to any label
        assertThat(WikipediaApp.getLabelForWikidata("Q21849466", new Locale("aa")), is("Leoben - Straßennamen mit Geschichte"));
        // not found -> null
        assertThat(WikipediaApp.getLabelForWikidata("Q" + Long.MAX_VALUE, Locale.ENGLISH), nullValue());
        final WikipediaApp.WikidataEntry q84 = new WikipediaApp.WikidataEntry("Q84", null, null, null);
        final WikipediaApp.WikidataEntry q1741 = new WikipediaApp.WikidataEntry("Q1741", null, null, null);
        final List<WikipediaApp.WikidataEntry> twoLabels = WikipediaApp.getLabelForWikidata(Arrays.asList(q84, q1741), Locale.GERMAN);
        assertThat(twoLabels.get(0).label, is("London"));
        assertThat(twoLabels.get(1).label, is("Wien"));
    }

    @Test(expected = RuntimeException.class)
    public void testGetLabelForWikidataInvalidId() throws Exception {
        WikipediaApp.getLabelForWikidata("Qxyz", Locale.ENGLISH);
    }

    @Test
    public void testWIWOSMStatus() throws Exception {
        final WikipediaEntry entry1 = new WikipediaEntry("en", "Vienna");
        final WikipediaEntry entry2 = new WikipediaEntry("en", "London");
        final WikipediaEntry entry3 = new WikipediaEntry("en", "a-non-existing-article");
        WikipediaApp.updateWIWOSMStatus("en", Arrays.asList(entry1, entry2, entry3));
        assertThat(entry1.getWiwosmStatus(), is(true));
        assertThat(entry2.getWiwosmStatus(), is(true));
        assertThat(entry3.getWiwosmStatus(), is(false));
    }
}
