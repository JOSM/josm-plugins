// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pointinfo.ruian;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.LinkedList;
import java.util.logging.Level;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.apache.commons.jcs3.access.exception.InvalidArgumentException;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.plugins.pointinfo.PointInfoUtils;
import org.openstreetmap.josm.tools.Logging;

/**
 * Private class contains RUIAN data
 * @author Marián Kyral
 */
class RuianRecord {

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    private double mCoorLat, mCoorLon;
    private String mSource;

    private long mObjektRuianId;
    private int mObjektPodlazi;
    private int mObjektByty;
    private String mObjektZpusobVyuziti;
    private String mObjektZpusobVyuzitiKod;
    private String mObjektZpusobVyuzitiKey;
    private String mObjektZpusobVyuzitiVal;
    private String mObjektDokonceni;
    private String mObjektPlatiOd;

    private String mErrUser;
    private String mErrDate;
    private String mErrType;
    private String mErrNote;

    private ArrayList<ObjectWithoutGeometry> mSoBezGeometrie;

    private ArrayList<AddrPlaces> mAdresniMista;

    private long mParcelaRuianId;
    private String mParcelaDruhPozemku;
    private String mParcelaZpusobVyuziti;
    private String mParcelaPlatiOd;

    private long mUliceRuianId;
    private String mUliceJmeno;

    private long mKatastrRuianId;
    private String mKatastrNazev;
    private long mKatastrObecKod;
    private String mKatastrObec;
    private long mKatastrOkresKod;
    private String mKatastrOkres;
    private long mKatastrKrajKod;
    private String mKatastrKraj;
    // CHECKSTYLE.ON: SingleSpaceSeparator

    /**
     * Constructor
     *
     */
    RuianRecord() {
        init();
    }

    /**
     * Initialization
     *
     */
    private void init() {

        mCoorLat = 0;
        mCoorLon = 0;
        mSource = "";

        mObjektRuianId = 0;
        mObjektPodlazi = 0;
        mObjektByty = 0;
        mObjektZpusobVyuziti = "";
        mObjektZpusobVyuzitiKod = "";
        mObjektZpusobVyuzitiKey = "";
        mObjektZpusobVyuzitiVal = "";
        mObjektDokonceni = "";
        mObjektPlatiOd = "";

        mErrUser = "";
        mErrDate = "";
        mErrType = "";
        mErrNote = "";

        mSoBezGeometrie = new ArrayList<>();
        mAdresniMista = new ArrayList<>();

        mParcelaRuianId = 0;
        mParcelaDruhPozemku = "";
        mParcelaZpusobVyuziti = "";
        mParcelaPlatiOd = "";

        mUliceRuianId = 0;
        mUliceJmeno = "";

        mKatastrRuianId = 0;
        mKatastrNazev = "";
        mKatastrObecKod = 0;
        mKatastrObec = "";
        mKatastrOkresKod = 0;
        mKatastrOkres = "";
        mKatastrKrajKod = 0;
        mKatastrKraj = "";
    }

    /**
     * Parse given JSON string and fill variables with RUIAN data
     * @param jsonStr JSON string
     */
    public void parseJSON(String jsonStr) {

        init();

        JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(jsonStr.getBytes(StandardCharsets.UTF_8)));
        JsonObject obj = jsonReader.readObject();
        jsonReader.close();

        parseCoordinates(obj);

        // =========================================================================
        parseStavebniObjekt(obj);

        // =========================================================================
        parseNahlasenyProblem(obj);

        // =========================================================================
        parseSoBezGeometrie(obj);

        // =========================================================================
        parseAdresniMista(obj);

        // =========================================================================
        parseParcela(obj);

        // =========================================================================
        parseUlice(obj);

        // =========================================================================
        parseKatastr(obj);
    }

    private static JsonObject getSafeJsonObject(JsonObject obj, String key) {
        JsonValue val = obj.get(key);
        if (val instanceof JsonObject) {
            return (JsonObject) val;
        } else if (val instanceof JsonArray) {
            JsonArray array = (JsonArray) val;
            if (!array.isEmpty()) {
                return array.getJsonObject(0);
            }
        }
        throw new IllegalArgumentException("No value for " + key);
    }

    private void parseCoordinates(JsonObject obj) {
        try {
            JsonObject coorObjekt = obj.getJsonObject("coordinates");

            try {
                mCoorLat = Double.parseDouble(coorObjekt.getString("lat"));
            } catch (NumberFormatException e) {
                Logging.warn("coordinates.lat: {0}", e);
            }

            try {
                mCoorLon = Double.parseDouble(coorObjekt.getString("lon"));
            } catch (NumberFormatException e) {
                Logging.warn("coordinates.lon: {0}", e);
            }

            try {
                mSource = obj.getString("source");
            } catch (RuntimeException e) {
                Logging.warn("source: {0}", e);
            }

        } catch (ClassCastException e) {
            Logging.warn("coordinates: {0}", e);
        }
    }

    private void parseStavebniObjekt(JsonObject obj) {
        try {
            JsonObject stavebniObjekt = getSafeJsonObject(obj, "stavebni_objekt");

            try {
                mObjektRuianId = Long.parseLong(stavebniObjekt.getString("ruian_id", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "stavebni_objekt.ruian_id:", e);
            }

            try {
                mObjektPodlazi = Integer.parseInt(stavebniObjekt.getString("pocet_podlazi", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "stavebni_objekt.pocet_podlazi:", e);
            }

            try {
                mObjektByty = Integer.parseInt(stavebniObjekt.getString("pocet_bytu", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "stavebni_objekt.pocet_bytu:", e);
            }

            mObjektZpusobVyuziti = stavebniObjekt.getString("zpusob_vyuziti", "");

            mObjektZpusobVyuzitiKod = stavebniObjekt.getString("zpusob_vyuziti_kod", "");

            mObjektZpusobVyuzitiKey = stavebniObjekt.getString("zpusob_vyuziti_key", "");

            mObjektZpusobVyuzitiVal = stavebniObjekt.getString("zpusob_vyuziti_val", "");

            mObjektPlatiOd = stavebniObjekt.getString("plati_od", "");

            mObjektDokonceni = stavebniObjekt.getString("dokonceni", "");

        } catch (IllegalArgumentException e) {
            Logging.warn("stavebni_objekt: {0}", e);
        }
    }

    private void parseNahlasenyProblem(JsonObject obj) {
        try {
            JsonObject errObjekt = getSafeJsonObject(obj, "nahlaseny_problem");

            mErrUser = errObjekt.getString("uzivatel", "");

            mErrDate = errObjekt.getString("datum", "");

            mErrType = errObjekt.getString("duvod", "");

            mErrNote = errObjekt.getString("poznamka", "");

        } catch (IllegalArgumentException e) {
            Logging.warn("nahlaseny_problem: {0}", e);
        }
    }

    private void parseSoBezGeometrie(JsonObject obj) {
        try {
            JsonArray arr = obj.getJsonArray("so_bez_geometrie");

            for (int i = 0; i < arr.size(); i++) {
                JsonObject soBezGeom = arr.getJsonObject(i);
                ObjectWithoutGeometry so = new ObjectWithoutGeometry();

                try {
                    so.setRuianID(Long.parseLong(soBezGeom.getString("ruian_id", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "so_bez_geometrie.ruian_id:", e);
                }

                try {
                    so.setPodlazi(Integer.parseInt(soBezGeom.getString("pocet_podlazi", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "so_bez_geometrie.pocet_podlazi:", e);
                }

                try {
                    so.setByty(Integer.parseInt(soBezGeom.getString("pocet_bytu", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "so_bez_geometrie.pocet_bytu:", e);
                }

                so.setZpusobVyuziti(soBezGeom.getString("zpusob_vyuziti", ""));

                so.setZpusobVyuzitiKod(soBezGeom.getString("zpusob_vyuziti_kod", ""));

                so.setZpusobVyuzitiKey(soBezGeom.getString("zpusob_vyuziti_key", ""));

                so.setZpusobVyuzitiVal(soBezGeom.getString("zpusob_vyuziti_val", ""));

                so.setDokonceni(soBezGeom.getString("dokonceni", ""));

                so.setPlatiOd(soBezGeom.getString("plati_od", ""));

                try {
                    so.setVzdalenost(Float.parseFloat(soBezGeom.getString("vzdalenost", "")));
                } catch (NumberFormatException e) {
                    Logging.warn("so_bez_geometrie.vzdalenost: {0}", e);
                }

                mSoBezGeometrie.add(so);
            }
        } catch (ClassCastException e) {
            Logging.warn("so_bez_geometrie: {0}", e);
        }
    }

    private void parseAdresniMista(JsonObject obj) {
        try {
            JsonArray arr = obj.getJsonArray("adresni_mista");

            for (int i = 0; i < arr.size(); i++) {
                JsonObject adresniMisto = arr.getJsonObject(i);
                AddrPlaces am = new AddrPlaces();

                try {
                    am.setRuianID(Long.parseLong(adresniMisto.getString("ruian_id", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.ruian_id:", e);
                }

                try {
                    JsonArray node = adresniMisto.getJsonArray("pozice");
                    if (node.size() >= 2) {
                        am.setPosition(new LatLon(
                                LatLon.roundToOsmPrecision(node.getJsonNumber(1).doubleValue()),
                                LatLon.roundToOsmPrecision(node.getJsonNumber(0).doubleValue()))
                        );
                    }
                } catch (ClassCastException e) {
                    Logging.log(Level.WARNING, "adresni_mista.pozice:", e);
                }

                try {
                    am.setBudovaID(Long.parseLong(adresniMisto.getString("budova_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.budova_kod:", e);
                }

                am.setCisloTyp(adresniMisto.getString("cislo_typ", ""));

                am.setCisloDomovni(adresniMisto.getString("cislo_domovni", ""));

                am.setCisloOrientacni(adresniMisto.getString("cislo_orientacni", ""));

                try {
                    am.setUliceID(Long.parseLong(adresniMisto.getString("ulice_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.ulice_kod:", e);
                }

                am.setUlice(adresniMisto.getString("ulice", ""));

                try {
                    am.setCastObceID(Long.parseLong(adresniMisto.getString("cast_obce_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.cast_obce_kod:", e);
                }

                am.setCastObce(adresniMisto.getString("cast_obce", ""));

                try {
                    am.setMestskaCastID(Long.parseLong(adresniMisto.getString("mestska_cast_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.mestska_cast_kod:", e);
                }

                am.setMestskaCast(adresniMisto.getString("mestska_cast", ""));

                try {
                    am.setObecID(Long.parseLong(adresniMisto.getString("obec_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.obec_kod:", e);
                }

                am.setObec(adresniMisto.getString("obec", ""));

                try {
                    am.setOkresID(Long.parseLong(adresniMisto.getString("okres_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.okres_kod:", e);
                }

                am.setOkres(adresniMisto.getString("okres", ""));

                try {
                    am.setKrajID(Long.parseLong(adresniMisto.getString("kraj_kod", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.kraj_kod:", e);
                }

                am.setKraj(adresniMisto.getString("kraj", ""));

                am.setPsc(adresniMisto.getString("psc", ""));

                try {
                    am.setVzdalenost(Float.parseFloat(adresniMisto.getString("vzdalenost", "")));
                } catch (NumberFormatException e) {
                    Logging.log(Level.WARNING, "adresni_mista.vzdalenost:", e);
                }

                mAdresniMista.add(am);
            }
        } catch (ClassCastException e) {
            Logging.log(Level.WARNING, "adresni_mista:", e);
        }
    }

    private void parseParcela(JsonObject obj) {
        try {
            JsonObject parcela = getSafeJsonObject(obj, "parcela");

            try {
                mParcelaRuianId = Long.parseLong(parcela.getString("ruian_id", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "parcela.ruian_id:", e);
            }

            mParcelaDruhPozemku = parcela.getString("druh_pozemku", "");

            mParcelaZpusobVyuziti = parcela.getString("zpusob_vyuziti", "");

            mParcelaPlatiOd = parcela.getString("plati_od", "");
        } catch (InvalidArgumentException e) {
            Logging.log(Level.WARNING, "parcela:", e);
        }
    }

    private void parseUlice(JsonObject obj) {
        try {
            JsonObject ulice = getSafeJsonObject(obj, "ulice");

            try {
                mUliceRuianId = Long.parseLong(ulice.getString("ruian_id", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "ulice.ruian_id:", e);
            }

            mUliceJmeno = ulice.getString("jmeno", "");

        } catch (IllegalArgumentException e) {
            Logging.log(Level.WARNING, "ulice:", e);
        }
    }

    private void parseKatastr(JsonObject obj) {
        try {
            JsonObject katastr = getSafeJsonObject(obj, "katastr");

            try {
                mKatastrRuianId = Long.parseLong(katastr.getString("ruian_id", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "katastr.ruian_id:", e);
            }

            mKatastrNazev = katastr.getString("nazev", "");

            try {
                mKatastrObecKod = Long.parseLong(katastr.getString("obec_kod", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "katastr.obec_kod:", e);
            }

            mKatastrObec = katastr.getString("obec", "");

            try {
                mKatastrOkresKod = Long.parseLong(katastr.getString("okres_kod", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "katastr.okres_kod:", e);
            }

            mKatastrOkres = katastr.getString("okres", "");

            try {
                mKatastrKrajKod = Long.parseLong(katastr.getString("kraj_kod", ""));
            } catch (NumberFormatException e) {
                Logging.log(Level.WARNING, "katastr.kraj_kod:", e);
            }

            mKatastrKraj = katastr.getString("kraj", "");

        } catch (IllegalArgumentException e) {
            Logging.log(Level.WARNING, "katastr:", e);
        }
    }

    /**
     * Return Html text representation
     * @return String htmlText
     */
    public String getHtml() {

        String iconExtLink = "<img src=" +getClass().getResource(
                "/images/dialogs/open-external-link.png")+" border=0 alt=\"Zobrazit na externích stránkách\"/>";
        String iconExtLinkRuian = "<img src=" +getClass().getResource(
                "/images/dialogs/open-external-link.png")+" border=0 alt=\"Zobrazit na stránkách RUIAN\"/>";
        String iconExtLinkKn = "<img src=" +getClass().getResource(
                "/images/dialogs/open-external-link-kn.png")+" border=0 alt=\"Zobrazit na stránkách katastru nemovitostí\"/>";
        String iconCopyTags = "<img src=" +getClass().getResource(
                "/images/dialogs/copy-tags.png")+" border=0 alt=\"Kopírovat tagy\"/>";
        String iconCreateAddr = "<img src=" +getClass().getResource(
                "/images/dialogs/create-addr.png")+" border=0 alt=\"Vytvořit adresní bod\"/>";
        String iconCreateAddrRuian = "<img src=" +getClass().getResource(
                "/images/dialogs/create-addr-ruian.png")+" border=0 alt=\"Vytvořit adresní bod dle RUIANu\"/>";
        String iconRuianError = "<img src=" +getClass().getResource(
                "/images/dialogs/create-bug-report.png")+" border=0 alt=\"Nahlásit problém v datech\"/>";
        // CHECKSTYLE.OFF: LineLength
        String urlCpost = "https://www.postaonline.cz/vyhledani-psc?p_p_id=psc_WAR_pcpvpp&p_p_lifecycle=1&p_p_state=normal&p_p_mode=view&p_p_col_id=column-1&p_p_col_count=1&_psc_WAR_pcpvpp_struts.portlet.action=%2Fview%2FdetailPost&_psc_WAR_pcpvpp_struts.portlet.mode=view&_psc_WAR_pcpvpp_zipCode=";
        // CHECKSTYLE.ON: LineLength
        String urlStavebniObjekt = "https://vdp.cuzk.cz/vdp/ruian/stavebniobjekty/";
        String urlAdresniMisto = "https://vdp.cuzk.cz/vdp/ruian/adresnimista/";
        String urlParcela = "https://vdp.cuzk.cz/vdp/ruian/parcely/";
        String urlKatastralniUzemi = "https://vdp.cuzk.cz/vdp/ruian/katastralniuzemi/";
        String urlUlice = "https://vdp.cuzk.cz/vdp/ruian/ulice/";
        String urlMistniCast = "https://vdp.cuzk.cz/vdp/ruian/castiobce/";
        String urlMestskaCast = "https://vdp.cuzk.cz/vdp/ruian/mestskecasti/";
        String urlObec = "https://vdp.cuzk.cz/vdp/ruian/obce/";
        String urlOkres = "https://vdp.cuzk.cz/vdp/ruian/okresy/";
        String urlKraj = "https://vdp.cuzk.cz/vdp/ruian/vusc/";
        String urlVlastnici = "https://vdp.cuzk.cz/vdp/ruian/vlastnici?typ=";

        String urlRuianError = "http://ruian.poloha.net/building.php?kod=";

        StringBuilder r = new StringBuilder();

        if (mObjektRuianId == 0 &&
            mParcelaRuianId == 0 &&
            mAdresniMista.isEmpty() &&
            mUliceRuianId == 0 &&
            mKatastrRuianId == 0)
            return "";

        r.append("<html><body bgcolor=\"white\" color=\"black\" ><table><tr><td>");
        r.append("<br/>");
        if (mObjektRuianId > 0) {
            r.append("<i><u>Informace o budově</u></i><br/>").append("<b>RUIAN id: </b>")
                    .append(mObjektRuianId).append("&nbsp;&nbsp;<a href=").append(urlStavebniObjekt)
                    .append(mObjektRuianId).append(">").append(iconExtLinkRuian).append("</a>")
                    .append("&nbsp;&nbsp;<a href=").append(urlVlastnici).append("so&id=")
                    .append(mObjektRuianId).append(">").append(iconExtLinkKn).append("</a>")
                    .append("&nbsp;&nbsp;<a href=file://tags.copy/building>").append(iconCopyTags).append("</a>")
                    .append("&nbsp;&nbsp;<a href=").append(urlRuianError).append(mObjektRuianId).append(">")
                    .append(iconRuianError).append("</a><br/>");
            if (mAdresniMista.isEmpty()) r.append("<b>Budova: </b> bez č.p./č.e<br/>");
            else if ("Číslo popisné".equals(mAdresniMista.get(0).getCisloTyp())) r.append("<b>Budova: </b>s číslem popisným<br/>");
            else r.append("<b>Budova: </b>s číslem evidenčním<br/>");
            if (mObjektPodlazi > 0) r.append("<b>Počet podlaží: </b>").append(mObjektPodlazi).append("<br/>");
            if (mObjektByty > 0) r.append("<b>Počet bytů: </b>").append(mObjektByty).append("<br/>");
            r.append("<b>Způsob využití: </b>").append(mObjektZpusobVyuziti).append("<br/>")
                    .append("<b>Datum dokončení: </b>").append(mObjektDokonceni).append("<br/>")
                    .append("<b>Platí od: </b>").append(mObjektPlatiOd).append("<br/>");

            if (mAdresniMista.size() > 1) {
                r.append("<i><u>Informace o adrese</u></i><br/>");
                // More address places
                int i = 0;
                r.append("<br/>").append("<b>").append(mAdresniMista.get(i).getCisloTyp())
                        .append("</b> (více adres)<b>: </b>").append(mAdresniMista.get(i).getCisloDomovni())
                        .append("<br/>").append("<b>Část obce: </b>").append(mAdresniMista.get(i).getCastObce())
                        .append("&nbsp;&nbsp;<a href=").append(urlMistniCast).append(mAdresniMista.get(i).getCastObceID())
                        .append(">").append(iconExtLinkRuian).append("</a><br/>");
                if (!mAdresniMista.get(i).getMestskaCast().isEmpty()) {
                    r.append("<b>Městská část: </b>").append(mAdresniMista.get(i).getMestskaCast())
                            .append("&nbsp;&nbsp;<a href=").append(urlMestskaCast)
                            .append(mAdresniMista.get(i).getMestskaCastID()).append(">")
                            .append(iconExtLinkRuian).append("</a><br/>");
                }
                r.append("<b>Obec: </b>").append(mAdresniMista.get(i).getObec()).append("&nbsp;&nbsp;<a href=")
                        .append(urlObec).append(mAdresniMista.get(i).getObecID()).append(">")
                        .append(iconExtLinkRuian).append("</a><br/>").append("<b>Okres: </b>")
                        .append(mAdresniMista.get(i).getOkres()).append("&nbsp;&nbsp;<a href=")
                        .append(urlOkres).append(mAdresniMista.get(i).getOkresID()).append(">")
                        .append(iconExtLinkRuian).append("</a><br/>").append("<b>Kraj: </b>")
                        .append(mAdresniMista.get(i).getKraj()).append("&nbsp;&nbsp;<a href=")
                        .append(urlKraj).append(mAdresniMista.get(i).getKrajID()).append(">")
                        .append(iconExtLinkRuian).append("</a><br/>");

            } else if (mAdresniMista.size() == 1
                    && (mAdresniMista.get(0).getCisloDomovni() == null || mAdresniMista.get(0).getCisloDomovni().isEmpty())) {
                // Without building number
                int i = 0;
                r.append("<br/>").append("<i><u>Informace o adrese</u></i><br/>").append("<b>Budova: </b>")
                        .append(mAdresniMista.get(i).getCisloTyp()).append("<br/>");
                if (mAdresniMista.get(i).getMestskaCast().length() > 0) {
                    r.append("<b>Městská část: </b>").append(mAdresniMista.get(i).getMestskaCast())
                            .append("&nbsp;&nbsp;<a href=").append(urlMestskaCast)
                            .append(mAdresniMista.get(i).getMestskaCastID()).append(">").append(iconExtLinkRuian)
                            .append("</a><br/>");
                }
                r.append("<b>Obec: </b>").append(mAdresniMista.get(i).getObec()).append("&nbsp;&nbsp;<a href=")
                        .append(urlObec).append(mAdresniMista.get(i).getObecID()).append(">").append(iconExtLinkRuian)
                        .append("</a><br/>").append("<b>Okres: </b>").append(mAdresniMista.get(i).getOkres())
                        .append("&nbsp;&nbsp;<a href=").append(urlOkres).append(mAdresniMista.get(i).getOkresID())
                        .append(">").append(iconExtLinkRuian).append("</a><br/>").append("<b>Kraj: </b>")
                        .append(mAdresniMista.get(i).getKraj()).append("&nbsp;&nbsp;<a href=").append(urlKraj)
                        .append(mAdresniMista.get(i).getKrajID()).append(">").append(iconExtLinkRuian).append("</a><br/>");

            } else if (mAdresniMista.size() == 1) {
                // Only one address place
                int i = 0;
                String x = "";
                String xName = "";
                if (!mAdresniMista.get(i).getCisloOrientacni().isEmpty()) {
                    x = "/" + mAdresniMista.get(i).getCisloOrientacni();
                    xName = "/orientační";
                }
                r.append("<br/>")
                        .append("<i><u>Informace o adrese</u></i><br/>").append("<b>RUIAN id: </b>")
                        .append(mAdresniMista.get(i).getRuianID()).append("&nbsp;&nbsp;<a href=").append(urlAdresniMisto)
                        .append(mAdresniMista.get(i).getRuianID()).append(">").append(iconExtLinkRuian).append("</a>")
                        .append("&nbsp;&nbsp;<a href=file://tags.copy/address:").append(i).append(">").append(iconCopyTags)
                        .append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create/address:").append(i).append(">")
                        .append(iconCreateAddr).append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create-on-place/address:")
                        .append(i).append(">").append(iconCreateAddrRuian).append("</a><br/>").append("<b>")
                        .append(mAdresniMista.get(i).getCisloTyp()).append(xName).append(": </b>")
                        .append(mAdresniMista.get(i).getCisloDomovni()).append(x)
                 .append("<br/>");
                if (!mAdresniMista.get(i).getUlice().isEmpty()) {
                    r.append("<b>Ulice: </b>").append(mAdresniMista.get(i).getUlice()).append("&nbsp;&nbsp;<a href=")
                            .append(urlUlice).append(mAdresniMista.get(i).getUliceID()).append(">").append(iconExtLinkRuian)
                            .append("</a><br/>");
                }
                r.append("<b>Část obce: </b>").append(mAdresniMista.get(i).getCastObce()).append("&nbsp;&nbsp;<a href=")
                        .append(urlMistniCast).append(mAdresniMista.get(i).getCastObceID()).append(">").append(iconExtLinkRuian)
                        .append("</a><br/>");
                if (mAdresniMista.get(i).getMestskaCast().length() > 0) {
                    r.append("<b>Městská část: </b>").append(mAdresniMista.get(i).getMestskaCast()).append("&nbsp;&nbsp;<a href=")
                            .append(urlMestskaCast).append(mAdresniMista.get(i).getMestskaCastID()).append(">")
                            .append(iconExtLinkRuian).append("</a><br/>");
                }
                r.append("<b>Obec: </b>").append(mAdresniMista.get(i).getObec()).append("&nbsp;&nbsp;<a href=")
                        .append(urlObec).append(mAdresniMista.get(i).getObecID()).append(">").append(iconExtLinkRuian)
                        .append("</a><br/>").append("<b>Okres: </b>").append(mAdresniMista.get(i).getOkres())
                        .append("&nbsp;&nbsp;<a href=").append(urlOkres).append(mAdresniMista.get(i).getOkresID())
                        .append(">").append(iconExtLinkRuian).append("</a><br/>").append("<b>Kraj: </b>")
                        .append(mAdresniMista.get(i).getKraj()).append("&nbsp;&nbsp;<a href=").append(urlKraj)
                        .append(mAdresniMista.get(i).getKrajID()).append(">").append(iconExtLinkRuian).append("</a><br/>")
                        .append("<b>PSČ: </b>").append(mAdresniMista.get(i).getPsc()).append("&nbsp;&nbsp;<a href=")
                        .append(urlCpost).append(mAdresniMista.get(i).getPsc()).append(">").append(iconExtLinkRuian).append("</a><br/>");

            }
            r.append("<br/>");
        }

        // Reported errors
        if (mObjektRuianId > 0 && !mErrUser.isEmpty()) {
            r.append("<i><u>Nahlášený problém</u></i>").append("&nbsp;&nbsp;<a href=").append(urlRuianError)
                    .append(mObjektRuianId).append(">").append(iconExtLink).append("</a><br/>")
                    .append("<b>Nahlásil: </b>").append(mErrUser)
                    .append("<br/>").append("<b>Dne: </b>").append(mErrDate)
                    .append("<br/>").append("<b>Typ problému: </b>").append(mErrType)
             .append("<br/>");
            if (!mErrNote.isEmpty()) {
                r.append("<b>Poznámka: </b>").append(mErrNote)
                 .append("<br/>");
            }
            r.append("<br/>");
        }

        // Address places
        if (mAdresniMista.size() > 1 && mObjektRuianId > 0) {
            String x = "";
            if ("Číslo evidenční".equals(mAdresniMista.get(0).getCisloTyp())) {
                x = "ev.";
            }
            r.append("<i><u>Adresní místa</u></i><br/>");
            for (int i = 0; i < mAdresniMista.size(); i++) {
                r.append(mAdresniMista.get(i).getUlice()).append(" ").append(x).append(mAdresniMista.get(i).getCisloDomovni());
                if (!mAdresniMista.get(i).getCisloOrientacni().isEmpty()) {
                    r.append("/").append(mAdresniMista.get(i).getCisloOrientacni());
                }
                r.append("&nbsp;&nbsp;<a href=").append(urlAdresniMisto).append(mAdresniMista.get(i).getRuianID())
                        .append(">").append(iconExtLinkRuian).append("</a> ")
                        .append("&nbsp;&nbsp;<a href=file://tags.copy/address:").append(i).append(">")
                        .append(iconCopyTags).append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create/address:")
                        .append(i).append(">").append(iconCreateAddr).append("</a>")
                        .append("&nbsp;&nbsp;<a href=file://tags.create-on-place/address:").append(i).append(">")
                        .append(iconCreateAddrRuian).append("</a>")
                 .append("<br/>");
            }
            r.append("<br/>");
        }

        // Land
        if (mParcelaRuianId > 0) {
            // .append("&nbsp;&nbsp;<a href=file://tags.copy/parcel>"+ icon_copy_tags +"</a>")
            r.append("<i><u>Informace o pozemku</u></i>")
                    .append("<br/>").append("<b>RUIAN id: </b>").append(mParcelaRuianId).append("&nbsp;&nbsp;<a href=")
                    .append(urlParcela).append(mParcelaRuianId).append(">").append(iconExtLinkRuian).append("</a>")
                    .append("&nbsp;&nbsp;<a href=").append(urlVlastnici).append("pa&id=").append(mParcelaRuianId)
                    .append(">").append(iconExtLinkKn).append("</a><br/>").append("<b>Druh pozemku: </b>")
                    .append(mParcelaDruhPozemku).append("<br/>");
            if (!"".equals(mParcelaZpusobVyuziti)) r.append("<b>Způsob využití: </b>").append(mParcelaZpusobVyuziti).append("<br/>");
            r.append("<b>Platí od: </b>").append(mParcelaPlatiOd).append("<br/>")
             .append("<br/>");
        }

        // Street
        if (mUliceRuianId > 0) {
            r.append("<i><u>Informace o ulici</u></i><br/>").append("<b>RUIAN id: </b>").append(mUliceRuianId)
                    .append("&nbsp;&nbsp;<a href=").append(urlUlice).append(mUliceRuianId).append(">")
                    .append(iconExtLinkRuian).append("</a>").append("&nbsp;&nbsp;<a href=file://tags.copy/street>")
                    .append(iconCopyTags).append("</a><br/>").append("<b>Jméno: </b>").append(mUliceJmeno).append("<br/>")
             .append("<br/>");
        }

        // Cadastral area
        if (mKatastrRuianId > 0) {
            r.append("<b>Katastrální území: </b>").append(mKatastrNazev).append("&nbsp;&nbsp;<a href=")
                    .append(urlKatastralniUzemi).append(mKatastrRuianId).append(">").append(iconExtLinkRuian)
                    .append("</a><br/>").append("<b>Obec: </b>").append(mKatastrObec).append("&nbsp;&nbsp;<a href=")
                    .append(urlObec).append(mKatastrObecKod).append(">").append(iconExtLinkRuian).append("</a><br/>")
                    .append("<b>Okres: </b>").append(mKatastrOkres).append("&nbsp;&nbsp;<a href=").append(urlOkres)
                    .append(mKatastrOkresKod).append(">").append(iconExtLinkRuian).append("</a><br/>")
                    .append("<b>Kraj: </b>").append(mKatastrKraj).append("&nbsp;&nbsp;<a href=").append(urlKraj)
                    .append(mKatastrKrajKod).append(">").append(iconExtLinkRuian).append("</a><br/>")
             .append("<br/>");
        }

        // Near address places
        if (!mAdresniMista.isEmpty() && mObjektRuianId == 0) {
            r.append("<i><u>Adresní místa v okolí</u></i><br/>")
             .append("<table>");
            for (int i = 0; i < mAdresniMista.size(); i++) {
                StringBuilder x = new StringBuilder();
                if ("Číslo evidenční".equals(mAdresniMista.get(i).getCisloTyp())) {
                    x.append("ev.");
                }
                x.append(mAdresniMista.get(i).getCisloDomovni());
                if (!mAdresniMista.get(i).getCisloOrientacni().isEmpty()) {
                    x.append("/").append(mAdresniMista.get(i).getCisloOrientacni());
                }
                r.append("<tr><td bgcolor=#e5e5ff>");
                if (!mAdresniMista.get(i).getUlice().isEmpty()) {
                    r.append(mAdresniMista.get(i).getVzdalenost())
                            .append("</td><td valign=\"top\"  bgcolor=#e5e5ff>").append(mAdresniMista.get(i).getUlice())
                            .append(" ").append(x).append("<br/><u>").append(mAdresniMista.get(i).getObec()).append("</u>")
                            .append("</td><td valign=\"top\"  bgcolor=#e5e5ff>").append("<a href=").append(urlAdresniMisto)
                            .append(mAdresniMista.get(i).getRuianID()).append(">").append(iconExtLinkRuian).append("</a>")
                            .append("&nbsp;&nbsp;<a href=file://tags.copy/address:").append(i).append(">").append(iconCopyTags)
                            .append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create/address:").append(i).append(">")
                            .append(iconCreateAddr).append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create-on-place/address:")
                            .append(i).append(">").append(iconCreateAddrRuian).append("</a>");
                } else {
                    r.append(mAdresniMista.get(i).getVzdalenost())
                            .append("</td><td valign=\"top\"  bgcolor=#e5e5ff>").append(mAdresniMista.get(i).getCastObce())
                            .append(" ").append(x).append("&nbsp;");
                    if (!mAdresniMista.get(i).getCastObce().equals(mAdresniMista.get(i).getObec())) {
                        r.append("<br/><u>").append(mAdresniMista.get(i).getObec()).append("</u>");
                    }
                    r.append("</td><td valign=\"top\"  bgcolor=#e5e5ff>").append("<a href=").append(urlAdresniMisto)
                            .append(mAdresniMista.get(i).getRuianID()).append(">").append(iconExtLinkRuian).append("</a>")
                            .append("&nbsp;&nbsp;<a href=file://tags.copy/address:").append(i).append(">").append(iconCopyTags)
                            .append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create/address:").append(i).append(">")
                            .append(iconCreateAddr).append("</a>").append("&nbsp;&nbsp;<a href=file://tags.create-on-place/address:")
                            .append(i).append(">").append(iconCreateAddrRuian).append("</a>");
                }
                r.append("</td></tr>");
            }
            r.append("</table><br/>");
        }

        if (!mSoBezGeometrie.isEmpty()) {
            r.append("<i><u>Budovy bez geometrie v okolí</u></i><br/>")
             .append("<table>");
            for (int i = 0; i < mSoBezGeometrie.size(); i++) {
                r.append("<tr><td bgcolor=#e5e5ff>")
                 .append(mSoBezGeometrie.get(i).getVzdalenost())
                 .append("</td><td valign=\"top\"  bgcolor=#e5e5ff>")
                 .append(mSoBezGeometrie.get(i).getRuianID());
                if (mSoBezGeometrie.get(i).getZpusobVyuziti().length() > 0) {
                    r.append(" - ").append(mSoBezGeometrie.get(i).getZpusobVyuziti());
                }
                r.append("</td><td valign=\"top\"  bgcolor=#e5e5ff>").append("&nbsp;&nbsp;<a href=").append(urlStavebniObjekt)
                        .append(mSoBezGeometrie.get(i).getRuianID()).append(">").append(iconExtLinkRuian).append("</a> ")
                        .append("&nbsp;&nbsp;<a href=file://tags.copy/ghost:").append(i).append(">").append(iconCopyTags).append("</a></br>")
                 .append("</td></tr>");
            }
            r.append("</table><br/>")
             .append("<br/>");
        }

        r.append("<hr/>").append("<center><i><small>Zdroj: <a href=\"http://www.ruian.cz/\">").append(mSource)
                .append("</a></small></i></center>")
         .append("</td></tr></table></body></html>");

        return r.toString();
    }

    /**
     * Convert date from Czech to OSM format
     * @param ruianDate Date in RUIAN (Czech) format DD.MM.YYYY
     * @return String with date converted to OSM data format YYYY-MM-DD
     */
    String convertDate(String ruianDate) {
        String[] parts = ruianDate.split("\\.");
        try {
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            return year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day);
        } catch (NumberFormatException | IllegalFormatException e) {
            Logging.warn(e);
        }

        return "";
    }

    /**
     * Construct tag string for clipboard
     * @param k OSM Key
     * @param v OSM Value
     * @return String OSM tag string for clipboard
     */
    String tagToString(String k, String v) {
        return "\"" + k + "\"=\"" + v + "\"\n";
    }

    /**
     * Prepare OSM keys
     * @param keyType What to prepare (building, address, parcel. street)
     * @return String with OSM tags
     */
    String getKeys(String keyType) {
        StringBuilder c = new StringBuilder();

        // Copy building tags to clipboard
        if ("building".equals(keyType) && mObjektRuianId > 0) {
            c.append(tagToString("ref:ruian:building", Long.toString(mObjektRuianId)));
            if (!mObjektZpusobVyuzitiKey.isEmpty() &&
                !mObjektZpusobVyuzitiVal.isEmpty()
                    ) {
                c.append(tagToString(mObjektZpusobVyuzitiKey, mObjektZpusobVyuzitiVal));
            }
            if (mObjektPodlazi > 0) {
                c.append(tagToString("building:levels", Integer.toString(mObjektPodlazi)));
            }
            if (mObjektByty > 0) {
                c.append(tagToString("building:flats", Integer.toString(mObjektByty)));
            }
            if (mObjektDokonceni.length() > 0 && convertDate(mObjektDokonceni).length() > 0) {
                c.append(tagToString("start_date", convertDate(mObjektDokonceni)));
            }
            if (mObjektZpusobVyuzitiKod.length() > 0) {
                c.append(tagToString("building:ruian:type", mObjektZpusobVyuzitiKod));
            }
            c.append(tagToString("source", "cuzk:ruian"));
        }

        if (keyType.startsWith("ghost") && !mSoBezGeometrie.isEmpty()) {
            String[] key = keyType.split(":");
            int i = Integer.parseInt(key[1]);
            Logging.trace("Ghost ID: {0}", i);

            c.append(tagToString("ref:ruian:building", Long.toString(mSoBezGeometrie.get(i).getRuianID())));
            if (mSoBezGeometrie.get(i).getZpusobVyuzitiKey().length() > 0 &&
                    mSoBezGeometrie.get(i).getZpusobVyuzitiVal().length() > 0
                    ) {
                c.append(tagToString(mSoBezGeometrie.get(i).getZpusobVyuzitiKey(), mSoBezGeometrie.get(i).getZpusobVyuzitiVal()));
            }
            if (mSoBezGeometrie.get(i).getPodlazi() > 0) {
                c.append(tagToString("building:levels", Integer.toString(mSoBezGeometrie.get(i).getPodlazi())));
            }
            if (mSoBezGeometrie.get(i).getByty() > 0) {
                c.append(tagToString("building:flats", Integer.toString(mSoBezGeometrie.get(i).getByty())));
            }
            if (mSoBezGeometrie.get(i).getDokonceni().length() > 0 && convertDate(mSoBezGeometrie.get(i).getDokonceni()).length() > 0) {
                c.append(tagToString("start_date", convertDate(mSoBezGeometrie.get(i).getDokonceni())));
            }
            if (mSoBezGeometrie.get(i).getZpusobVyuzitiKod().length() > 0) {
                c.append(tagToString("building:ruian:type", mSoBezGeometrie.get(i).getZpusobVyuzitiKod()));
            }
            c.append(tagToString("source", "cuzk:ruian"));
        }

        // Copy address tags to clipboard
        if (keyType.startsWith("address") && !mAdresniMista.isEmpty()) {
            int i;

            String[] key = keyType.split(":");
            i = Integer.parseInt(key[1]);
            Logging.info("Address ID: " + i);

            // Only one address place
            if (!"Číslo evidenční".equals(mAdresniMista.get(i).getCisloTyp())) {
                // Cislo popisne
                c.append(tagToString("addr:conscriptionnumber", mAdresniMista.get(i).getCisloDomovni()));
            } else {
                // Cislo evidencni
                c.append(tagToString("addr:provisionalnumber", mAdresniMista.get(i).getCisloDomovni()));
            }

            // Cislo orientacni
            if (!mAdresniMista.get(i).getCisloOrientacni().isEmpty()) {
                c.append(tagToString("addr:streetnumber", mAdresniMista.get(i).getCisloOrientacni()));
            }

            // Domovni cislo
            StringBuilder addr = new StringBuilder();
            if (!"Číslo evidenční".equals(mAdresniMista.get(i).getCisloTyp())) {
                addr.append(mAdresniMista.get(i).getCisloDomovni());
            } else {
                addr.append("ev.").append(mAdresniMista.get(i).getCisloDomovni());
            }
            if (!mAdresniMista.get(i).getCisloOrientacni().isEmpty()) {
                addr.append("/").append(mAdresniMista.get(i).getCisloOrientacni());
            }
            c.append(tagToString("addr:housenumber", addr.toString()));

            // Street
            if (!mAdresniMista.get(i).getUlice().isEmpty()) {
                c.append(tagToString("addr:street", mAdresniMista.get(i).getUlice()));
            }
            //RUIAN ID
            if (mAdresniMista.get(i).getRuianID() > 0) {
                c.append(tagToString("ref:ruian:addr", Long.toString(mAdresniMista.get(i).getRuianID())));
            }

            // Place
            if (!mAdresniMista.get(i).getCastObce().isEmpty()) {
                c.append(tagToString("addr:place", mAdresniMista.get(i).getCastObce()));
            }

            if (!mAdresniMista.get(i).getMestskaCast().isEmpty()) {
                c.append(tagToString("addr:suburb", mAdresniMista.get(i).getMestskaCast()));
            }

            // City
            if (!mAdresniMista.get(i).getObec().isEmpty()) {
                c.append(tagToString("addr:city", mAdresniMista.get(i).getObec()));
            }

            // Postcode
            if (!mAdresniMista.get(i).getPsc().isEmpty()) {
                c.append(tagToString("addr:postcode", mAdresniMista.get(i).getPsc()));
            }

            // Country
            c.append(tagToString("addr:country", "CZ"));

            // Source
            c.append(tagToString("source:addr", "cuzk:ruian"));
        }

        // Copy parcel tags to clipboard

        // Copy street tags to clipboard
        if ("street".equals(keyType) && mUliceRuianId > 0) {
            c.append(tagToString("ref:ruian:street", Long.toString(mUliceRuianId)));
            c.append(tagToString("name", mUliceJmeno));
            c.append(tagToString("source", "cuzk:ruian"));
        }

        return c.toString();
    }

    /**
     * Create new address poing on current location with given tags
     * @param cmd What to do: create on clicked position or on ruian position
     * @param t OSM tags in string
     */
    void createAddrPoint(String cmd, String t) {
        Collection<Command> commands = new LinkedList<>();
        Node node;
        if (cmd.startsWith("tags.create-on-place")) {
            String[] key = cmd.split(":");
            int i = Integer.parseInt(key[1]);
            node = new Node(mAdresniMista.get(i).getPosition());
        } else {
            node = new Node(new LatLon(mCoorLat, mCoorLon));
        }
        commands.add(new AddCommand(MainApplication.getLayerManager().getEditDataSet(), node));

        Collection<OsmPrimitive> coll = new LinkedList<>();
        coll.add(node);

        TagCollection tc = new TagCollection();
        ArrayList<String> list = new ArrayList<>(Arrays.asList(t.split("\n")));
        for (String line : list) {
            String[] tag = line.split("\"=\"");
            Logging.info("<" + tag[0] + ">. <" + tag[1] +">");
            tc.add(new Tag(tag[0].substring(1), tag[1].substring(0, tag[1].length()-1)));
        }

        tc.applyTo(coll);

        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Add new address point"), commands));
    }

    /**
     * Perform given action
     *  e.g.: copy tags to clipboard
     * @param act Action to be performed
     */
    public void performAction(String act) {

        Logging.info("act: " + act.substring(7));
        String[] params = act.substring(7).split("/");
        if (!"tags.copy".equals(params[0]) && !params[0].startsWith("tags.create")) {
            return;
        }

        String task = getKeys(params[1]);

        // Copy tags to clipboard
        if ("tags.copy".equals(params[0]) && !task.isEmpty()) {
            ClipboardUtils.copyString(task);
            PointInfoUtils.showNotification(tr("Tags copied to clipboard."), "info");
        }

        // Create address node
        if (params[0].startsWith("tags.create") && !task.isEmpty()) {
            createAddrPoint(act.substring(7), task);
            PointInfoUtils.showNotification(tr("New address point added."), "info");
        }
    }
}
