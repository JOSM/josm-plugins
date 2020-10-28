package net.simon04.comfort0;

import static org.CustomMatchers.hasSize;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.StringReader;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.PseudoCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import net.simon04.comfort0.level0l.parsergen.Level0LParser;
import net.simon04.comfort0.level0l.parsergen.ParseException;

public class EditLevel0LActionTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    private SequenceCommand buildChangeCommands(DataSet dataSet) throws ParseException {
        final String level0l = "node 1831881213: 54.0900666, 12.2539381 #Neu Broderstorf (54.0900666, 12.2539381)\n" +
                "  name = Neu Broderstorf\n" +
                "  traffic_sign = city_limit\n";
        final List<PrimitiveData> primitives = new Level0LParser(new StringReader(level0l)).primitives();
        return EditLevel0LAction.buildChangeCommands(dataSet, primitives);
    }

    private <T extends PseudoCommand> T buildChangeCommand(DataSet dataSet, Class<T> type) throws ParseException {
        SequenceCommand commands = buildChangeCommands(dataSet);
        assertThat(commands.getChildren(), hasSize(1));
        final PseudoCommand command = commands.getChildren().iterator().next();
        assertThat(command, instanceOf(type));
        return type.cast(command);
    }

    @Test
    public void test() throws Exception {
        final Node node = new Node(1831881213, 42);
        node.setCoor(new LatLon(54.0900666, 12.2539381));
        final DataSet dataSet = new DataSet(node);

        ChangePropertyCommand command = buildChangeCommand(dataSet, ChangePropertyCommand.class);
        assertThat(command.getTags(), is(new TagMap("name", "Neu Broderstorf", "traffic_sign", "city_limit")));

        node.put("name", "Neu Broderstorf");
        node.put("traffic_sign", "city_limit");
        assertThat(buildChangeCommands(dataSet), nullValue());

        node.put("fixme", "delete me!");
        command = buildChangeCommand(dataSet, ChangePropertyCommand.class);
        assertThat(command.getTags(), is(new TagMap("name", "Neu Broderstorf", "traffic_sign", "city_limit", "fixme", "")));
        node.remove("fixme");

        node.setCoor(new LatLon(55.0900666, 13.2539381));
        assertThat(buildChangeCommand(dataSet, MoveCommand.class), notNullValue());
    }
}
