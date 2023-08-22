// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.pointinfo.catastro;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

/**
 * Class to contain Catastro data
 * @author Javier Sánchez Portero
 */
class CatastroRecord {

    public static final String fURL = "http://ovc.catastro.meh.es/OVCServWeb/"+
        "OVCWcfLibres/OVCFotoFachada.svc/RecuperarFotoFachadaGet?ReferenciaCatastral=";
    public static final String cURL = "http://www.catastro.meh.es/";
    public static final String cSource = "Dirección General del Catastro";
    public static final String eURL = "https://www.verexif.com/ver.php?foto_file=&foto_url=";

    private ArrayList<JsonObject> errors;
    private ArrayList<JsonObject> coords;

    /**
     * Constructor
     *
     */
    CatastroRecord() {
        init();
    }

    /**
     * Initialization
     *
     */
    private void init() {
        errors = new ArrayList<>();
        coords = new ArrayList<>();
    }

    /**
     * Parse given XML string and fill variables with Catastro data
     * @param xmlStr XML string with Catastro Consulta_RCCOOR response
     */
    public void parseXML(String xmlStr) {
        init();
        try {
            InputStream input = new ByteArrayInputStream(xmlStr.getBytes(StandardCharsets.UTF_8));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();
            parseErrors(doc);
            parseCoordenadas(doc);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            Logging.warn(e);
        }
    }

    private void parseErrors(Document doc) {
        NodeList nList = doc.getElementsByTagName("err");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String code = eElement.getElementsByTagName("cod").item(0).getTextContent();
                String desc = eElement.getElementsByTagName("des").item(0).getTextContent();
                JsonObject error = Json.createObjectBuilder()
                    .add("code", code)
                    .add("desc", desc)
                    .build();
                errors.add(error);
                Logging.info("Error: (" + code + ") " + desc);
            }
        }
    }
    
    private void parseCoordenadas(Document doc) {
        NodeList nList = doc.getElementsByTagName("coord");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;
                String ref = eElement.getElementsByTagName("pc1").item(0).getTextContent()
                    + eElement.getElementsByTagName("pc2").item(0).getTextContent();
                String address = eElement.getElementsByTagName("ldt")
                    .item(0).getTextContent();
                JsonObject coord = Json.createObjectBuilder()
                    .add("ref", ref)
                    .add("address", address)
                    .build();
                coords.add(coord);
                Logging.info("Referencia Catastral: " + ref);
                Logging.info("Dirección: " + address);
            }
        }
    }
    
    /**
     * Return Html text representation
     * @return String htmlText
     */
    public String getHtml() {
        StringBuilder r = new StringBuilder();
        r.append("<html><body bgcolor=\"white\" color=\"black\" ><table><tr><td>");
        r.append("<br/>");
        for (JsonObject coord: coords) {
            r.append("<i><u>Información de la parcela</u></i><br/>");
            r.append("<b>Referencia Catastral: </b>").append(coord.getString("ref")).append("<br/>");
            r.append("<b>Dirección: </b>").append(coord.getString("address")).append("<br/>");
            r.append("<b>Fotografía de fachada:</b> <a href=\"").append(fURL)
                    .append(coord.getString("ref"))
                    .append("\">")
                    .append("Enlace a web de Catastro</a> (<a href=\"")
                    .append(eURL).append(fURL)
                    .append(coord.getString("ref")).append("\">metadatos</a>)<br/><br/>");
        }
        if (!errors.isEmpty()) {
            r.append("<i><u>Errores</u></i><br/>");
        }
        for (JsonObject error: errors) {
            r.append("(").append(error.getString("code")).append(") ");
            r.append(error.getString("desc")).append("<br/>");
        }
        r.append("<hr/>");
        r.append("<center><i>Fuente: <a href=\"" + cURL +"\">" + cSource + "</a></i></center>");
        r.append("</td></tr></table></body></html>");
        return r.toString();
    }

    /**
     * Perform given action
     *  e.g.: copy tags to clipboard
     * @param act Action to be performed
     */
    public void performAction(String act) {
    }
}
