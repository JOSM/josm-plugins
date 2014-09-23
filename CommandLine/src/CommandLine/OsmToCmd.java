/*
 *	  OsmToCmd.java
 *
 *	  Copyright 2011 Hind <foxhind@gmail.com>
 *
 */
package CommandLine;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.UTFInputStreamReader;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

final class OsmToCmd {
    private final CommandLine parentPlugin;
    private final DataSet targetDataSet;
    private final LinkedList<Command> cmds = new LinkedList<>();
    private final HashMap<PrimitiveId, OsmPrimitive> externalIdMap; // Maps external ids to internal primitives

    public OsmToCmd(CommandLine parentPlugin, DataSet targetDataSet) {
        this.parentPlugin = parentPlugin;
        this.targetDataSet = targetDataSet;
        externalIdMap = new HashMap<>();
    }

    public void parseStream(InputStream stream) throws IllegalDataException {
        try {
            InputSource inputSource = new InputSource(UTFInputStreamReader.create(stream, "UTF-8"));
            SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            Parser handler = new Parser();
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
            parser.parse(inputSource, handler);
        } catch(ParserConfigurationException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch (SAXParseException e) {
            throw new IllegalDataException(tr("Line {0} column {1}: ", e.getLineNumber(), e.getColumnNumber()) + e.getMessage(), e);
        } catch(SAXException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch(Exception e) {
            throw new IllegalDataException(e);
        }
    }

    public LinkedList<Command> getCommandList() {
        return cmds;
    }

    private class Parser extends DefaultHandler implements LexicalHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String msg) throws XmlParsingException {
            throw new XmlParsingException(msg).rememberLocation(locator);
        }

        private OsmPrimitive currentPrimitive;
        //private long currentExternalId;
        private final List<Node> currentWayNodes = new ArrayList<>();
        private final List<RelationMember> currentRelationMembers = new ArrayList<>();

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                if (qName.equals("osm")) {
                    if (atts == null) {
                        throwException(tr("Missing mandatory attribute ''{0}'' of XML element {1}.", "version", "osm"));
                        return;
                    }
                    String v = atts.getValue("version");
                    if (v == null) {
                        throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
                        return;
                    }
                    if ( !(v.equals("0.6")) ) {
                        throwException(tr("Unsupported version: {0}", v));
                        return;
                    }

                    // ---- PARSING NODES AND WAYS ----

                } else if (qName.equals("node")) {
                    Node n = new Node();
                    NodeData source = new NodeData();
                    source.setCoor(new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon")));
                    readCommon(atts, source);
                    Node target = (Node)targetDataSet.getPrimitiveById( source.getUniqueId(), source.getType() );

                    if (target == null || !(source.isModified() || source.isDeleted()) )
                        n.load(source);
                    else {
                        n.cloneFrom(target);
                        n.load(source);
                    }

                    currentPrimitive = n;
                    externalIdMap.put(source.getPrimitiveId(), n);
                }
                else if (qName.equals("way")) {
                    Way w = new Way();
                    WayData source = new WayData();
                    readCommon(atts, source);
                    Way target = (Way)targetDataSet.getPrimitiveById( source.getUniqueId(), source.getType() );

                    if (target == null || !(source.isModified() || source.isDeleted()) )
                        w.load(source);
                    else {
                        w.cloneFrom(target);
                        w.load(source);
                    }

                    currentPrimitive = w;
                    currentWayNodes.clear();
                    externalIdMap.put(source.getPrimitiveId(), w);
                }
                else if (qName.equals("nd")) {
                    if (atts.getValue("ref") == null)
                        throwException(tr("Missing mandatory attribute ''{0}'' on <nd> of way {1}.", "ref", currentPrimitive.getUniqueId()));
                    long id = getLong(atts, "ref");
                    if (id == 0)
                        throwException(tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", id) );
                    Node node = (Node)externalIdMap.get(new SimplePrimitiveId(id, OsmPrimitiveType.NODE));
                    if (node == null || node.isModified()) {
                        node = (Node)targetDataSet.getPrimitiveById( new SimplePrimitiveId(id, OsmPrimitiveType.NODE) );
                        if (node == null)
                            throwException(tr("Missing definition of new object with id {0}.", id));
                    }
                    currentWayNodes.add(node);
                }
                // ---- PARSING RELATIONS ----

                else if (qName.equals("relation")) {
                    Relation r = new Relation();
                    RelationData source = new RelationData();
                    readCommon(atts, source);
                    Relation target = (Relation)targetDataSet.getPrimitiveById( source.getUniqueId(), source.getType() );

                    if (target == null || !(source.isModified() || source.isDeleted()) )
                        r.load(source);
                    else {
                        r.cloneFrom(target);
                        r.load(source);
                    }

                    currentPrimitive = r;
                    currentRelationMembers.clear();
                    externalIdMap.put(source.getPrimitiveId(), r);
                }
                else if (qName.equals("member")) {
                    if (atts.getValue("ref") == null)
                        throwException(tr("Missing mandatory attribute ''{0}'' on <member> of relation {1}.", "ref", currentPrimitive.getUniqueId()));
                    long id = getLong(atts, "ref");
                    if (id == 0)
                        throwException(tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", id) );

                    OsmPrimitiveType type = OsmPrimitiveType.NODE;
                    String value = atts.getValue("type");
                    if (value == null) {
                        throwException(tr("Missing attribute ''type'' on member {0} in relation {1}.", Long.toString(id), Long.toString(currentPrimitive.getUniqueId())));
                    }
                    try {
                        type = OsmPrimitiveType.fromApiTypeName(value);
                    }
                    catch(IllegalArgumentException e) {
                        throwException(tr("Illegal value for attribute ''type'' on member {0} in relation {1}. Got {2}.", Long.toString(id), Long.toString(currentPrimitive.getUniqueId()), value));
                    }

                    String role = atts.getValue("role");

                    OsmPrimitive member = externalIdMap.get(new SimplePrimitiveId(id, type));
                    if (member == null) {
                        member = targetDataSet.getPrimitiveById(new SimplePrimitiveId(id, type));
                        if (member == null)
                            throwException(tr("Missing definition of new object with id {0}.", id));
                    }
                    RelationMember relationMember = new RelationMember(role, member);
                    currentRelationMembers.add(relationMember);
                }

                // ---- PARSING TAGS (applicable to all objects) ----

                else if (qName.equals("tag")) {
                    String key = atts.getValue("k");
                    String value = atts.getValue("v");
                    if (key == null || value == null) {
                        throwException(tr("Missing key or value attribute in tag."));
                        return;
                    }
                    currentPrimitive.put(key.intern(), value.intern());
                }
                else {
                    Main.warn(tr("Undefined element ''{0}'' found in input stream. Skipping.", qName));
                }
            }
            catch (Exception e) {
                throw new SAXParseException(e.getMessage(), locator, e);
            }
        }

        @Override
        public void endElement(String namespaceURI, String localName, String qName) {
            if (qName.equals("node")) {
                if (currentPrimitive.isDeleted()) {
                    cmds.add(new DeleteCommand( targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()) ));
                }
                else if (currentPrimitive.isModified()) {
                    cmds.add(new ChangeCommand(Main.main.getEditLayer(), targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()), currentPrimitive));
                }
                else if (currentPrimitive.isNew()) {
                    cmds.add(new AddCommand(currentPrimitive));
                }
            }
            else if (qName.equals("way")) {
                ((Way)currentPrimitive).setNodes(currentWayNodes);
                if (currentPrimitive.isDeleted()) {
                    cmds.add(new DeleteCommand( targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()) ));
                }
                else if (currentPrimitive.isModified()) {
                    cmds.add(new ChangeCommand(Main.main.getEditLayer(), targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()), currentPrimitive));
                }
                else if (currentPrimitive.isNew()) {
                    cmds.add(new AddCommand(currentPrimitive));
                }
            }
            else if (qName.equals("relation")) {
                ((Relation)currentPrimitive).setMembers(currentRelationMembers);
                if (currentPrimitive.isDeleted()) {
                    cmds.add(new DeleteCommand( targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()) ));
                }
                else if (currentPrimitive.isModified()) {
                    cmds.add(new ChangeCommand(Main.main.getEditLayer(), targetDataSet.getPrimitiveById(currentPrimitive.getPrimitiveId()), currentPrimitive));
                }
                else if (currentPrimitive.isNew()) {
                    cmds.add(new AddCommand(currentPrimitive));
                }
            }
        }

        @Override
        public void comment(char[] ch, int start, int length) {
            parentPlugin.printHistory(String.valueOf(ch));
        }

        @Override
        public void startCDATA() {
        }

        @Override
        public void endCDATA() {
        }

        @Override
        public void startEntity(String name) {
        }

        @Override
        public void endEntity(String name) {
        }

        @Override
        public void startDTD(String name, String publicId, String systemId) {
        }

        @Override
        public void endDTD() {
        }

        private double getDouble(Attributes atts, String value) {
            return Double.parseDouble(atts.getValue(value));
        }

        private long getLong(Attributes atts, String name) throws SAXException {
            String value = atts.getValue(name);
            if (value == null) {
                throwException(tr("Missing required attribute ''{0}''.",name));
            }
            try {
                return Long.parseLong(value);
            }
            catch(NumberFormatException e) {
                throwException(tr("Illegal long value for attribute ''{0}''. Got ''{1}''.",name, value));
            }
            return 0; // should not happen
        }

        private User createUser(String uid, String name) throws SAXException {
            if (uid == null) {
                if (name == null)
                    return null;
                return User.createLocalUser(name);
            }
            try {
                long id = Long.parseLong(uid);
                return User.createOsmUser(id, name);
            }
            catch(NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''uid''. Got ''{0}''.", uid));
            }
            return null;
        }

        void readCommon(Attributes atts, PrimitiveData current) throws SAXException {
            current.setId(getLong(atts, "id"));
            if (current.getUniqueId() == 0) {
                throwException(tr("Illegal object with ID=0."));
            }

            String time = atts.getValue("timestamp");
            if (time != null && time.length() != 0) {
                current.setTimestamp(DateUtils.fromString(time));
            }

            String user = atts.getValue("user");
            String uid = atts.getValue("uid");
            current.setUser(createUser(uid, user));

            String visible = atts.getValue("visible");
            if (visible != null) {
                current.setVisible(Boolean.parseBoolean(visible));
            }

            String versionString = atts.getValue("version");
            int version = 0;
            if (versionString != null) {
                try {
                    version = Integer.parseInt(versionString);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.", Long.toString(current.getUniqueId()), versionString));
                }
            }
            current.setVersion(version);

            String action = atts.getValue("action");
            if (action == null) {
                // do nothing
            } else if (action.equals("delete")) {
                current.setDeleted(true);
                current.setModified(current.isVisible());
            } else if (action.equals("modify")) {
                current.setModified(true);
            }

            String v = atts.getValue("changeset");
            if (v == null) {
                current.setChangesetId(0);
            } else {
                try {
                    current.setChangesetId(Integer.parseInt(v));
                } catch(NumberFormatException e) {
                    if (current.getUniqueId() <= 0) {
                        Main.warn(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.", v, current.getUniqueId()));
                        current.setChangesetId(0);
                    } else {
                        throwException(tr("Illegal value for attribute ''changeset''. Got {0}.", v));
                    }
                }
                if (current.getChangesetId() <=0) {
                    if (current.getUniqueId() <= 0) {
                        Main.warn(tr("Illegal value for attribute ''changeset'' on new object {1}. Got {0}. Resetting to 0.", v, current.getUniqueId()));
                        current.setChangesetId(0);
                    } else {
                        throwException(tr("Illegal value for attribute ''changeset''. Got {0}.", v));
                    }
                }
            }
        }
    }
}
