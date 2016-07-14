// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.mapillary.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.plugins.mapillary.AbstractTest;
import org.openstreetmap.josm.plugins.mapillary.MapillaryAbstractImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryData;
import org.openstreetmap.josm.plugins.mapillary.MapillaryImage;
import org.openstreetmap.josm.plugins.mapillary.MapillaryLayer;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandDelete;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandImport;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandJoin;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandMove;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandTurn;
import org.openstreetmap.josm.plugins.mapillary.history.commands.CommandUnjoin;
import org.openstreetmap.josm.plugins.mapillary.history.commands.MapillaryCommand;

/**
 * Tests the command record system.
 *
 * @author nokutu
 */
public class MapillaryRecordTest extends AbstractTest {

  private MapillaryRecord record;
  private MapillaryImage img1;
  private MapillaryImage img2;
  private MapillaryImage img3;

  /**
   * Creates a new {@link MapillaryRecord} object and 3 {@link MapillaryImage}
   * objects.
   */
  @Before
  public void setUp() {
    record = new MapillaryRecord();
    img1 = new MapillaryImage("key1__________________", new LatLon(0.1, 0.1), 0.1);
    img2 = new MapillaryImage("key2__________________", new LatLon(0.2, 0.2), 0.2);
    img3 = new MapillaryImage("key3__________________", new LatLon(0.3, 0.3), 0.3);
    MapillaryLayer.getInstance().getData().getImages().clear();
  }

  /**
   * Test commands in general.
   */
  @Test
  public void commandTest() {
    MapillaryCommand cmd12 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img2})),
            0.1, 0.1);
    MapillaryCommand cmd23 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img2, this.img3})),
            0.1, 0.1);
    MapillaryCommand cmd13 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img3})),
            0.1, 0.1);
    MapillaryCommand cmd1 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1})), 0.1, 0.1);
    MapillaryCommand cmd31 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img3, this.img1})),
            0.2, 0.2);
    this.record.addCommand(cmd12);
    this.record.addCommand(cmd23);

    assertEquals(1, this.record.position);
    assertEquals(2, this.record.commandList.size());

    this.record.undo();

    assertEquals(0, this.record.position);
    assertEquals(2, this.record.commandList.size());

    this.record.addCommand(cmd1);

    assertEquals(1, this.record.position);

    this.record.addCommand(cmd13);

    assertEquals(2, this.record.position);
    assertEquals(3, this.record.commandList.size());

    this.record.undo();
    this.record.redo();

    assertEquals(2, this.record.position);
    assertEquals(3, this.record.commandList.size());

    this.record.addCommand(cmd31);

    assertEquals(2, this.record.position);
    assertEquals(3, this.record.commandList.size());

    this.record.addCommand(cmd1);

    assertEquals(3, this.record.position);
    assertEquals(4, this.record.commandList.size());
  }

  /**
   * Tests {@link CommandMove} class.
   */
  @Test
  public void commandMoveTest() {
    CommandMove cmd1 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{img1, img2})),
            0.1, 0.1);
    CommandMove cmd2 = new CommandMove(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{img1, img2})),
            0.1, 0.1);

    this.record.addCommand(cmd1);

    assertEquals(0.1, this.img1.getMovingLatLon().lat(), 0.01);

    this.record.undo();

    assertEquals(0.0, this.img1.getMovingLatLon().lat(), 0.01);

    this.record.redo();

    assertEquals(0.1, this.img1.getMovingLatLon().lat(), 0.01);

    this.record.addCommand(cmd2);
    this.record.undo();

    assertEquals(-0.1, this.img1.getMovingLatLon().lat(), 0.01);

    this.record.redo();

    assertEquals(0.1, this.img1.getMovingLatLon().lat(), 0.01);
  }

  /**
   * Tests {@link CommandTurn} class.
   */
  @Test
  public void commandTurnTest() {
    CommandTurn cmd1 = new CommandTurn(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img2})),
            0.2);
    CommandTurn cmd2 = new CommandTurn(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img2})),
            0.1);

    this.record.addCommand(cmd1);
    this.record.undo();

    assertEquals(-0.1, this.img1.getMovingCa(), 0.01);

    this.record.redo();

    assertEquals(0.1, this.img1.getMovingCa(), 0.01);

    this.record.addCommand(cmd2);
    this.record.undo();

    assertEquals(-0.2, this.img1.getMovingCa(), 0.01);

    this.record.redo();

    assertEquals(0.1, this.img1.getMovingCa(), 0.01);
  }

  /**
   * Tests {@link CommandJoin} class.
   */
  @Test
  public void commandJoinClass() {
    CommandJoin cmd1 = new CommandJoin(Arrays.asList(new MapillaryAbstractImage[]{img1, img2}));
    CommandJoin cmd2 = new CommandJoin(Arrays.asList(new MapillaryAbstractImage[]{img2, img3}));

    this.record.addCommand(cmd1);
    assertEquals(2, img1.getSequence().getImages().size());
    assertEquals(img2, img1.next());
    this.record.undo();
    assertEquals(1, img1.getSequence().getImages().size());
    this.record.redo();
    this.record.addCommand(cmd2);
    assertEquals(3, img1.getSequence().getImages().size());
    assertEquals(img3, img1.next().next());

    try {
      this.record.addCommand(new CommandJoin(Arrays.asList(new MapillaryAbstractImage[]{img1, img2, img3})));
      fail();
    } catch (IllegalArgumentException e) {
      // Expected output.
    }
  }

  /**
   * Tests {@link CommandUnjoin} class.
   */
  @Test
  public void commandUnjoinClass() {
    CommandJoin join1 = new CommandJoin(
            Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img2}));
    CommandJoin join2 = new CommandJoin(
            Arrays.asList(new MapillaryAbstractImage[]{this.img2, this.img3}));

    CommandUnjoin cmd1 = new CommandUnjoin(
            Arrays.asList(new MapillaryAbstractImage[]{this.img1, this.img2}));
    CommandUnjoin cmd2 = new CommandUnjoin(
            Arrays.asList(new MapillaryAbstractImage[]{this.img2, this.img3}));

    this.record.addCommand(join1);
    this.record.addCommand(join2);

    this.record.addCommand(cmd1);
    assertEquals(1, this.img1.getSequence().getImages().size());
    this.record.undo();
    assertEquals(3, this.img1.getSequence().getImages().size());
    this.record.redo();
    this.record.addCommand(cmd2);
    assertEquals(1, this.img1.getSequence().getImages().size());
    assertEquals(1, this.img2.getSequence().getImages().size());

    try {
      this.record.addCommand(new CommandUnjoin(Arrays.asList(new MapillaryAbstractImage[]{img1, img2, img3})));
      fail();
    } catch (IllegalArgumentException e) {
      // Expected output.
    }
  }

  /**
   * Test {@link CommandDelete} class.
   */
  @Test
  public void commandDeleteTest() {
    CommandJoin join1 = new CommandJoin(Arrays.asList(new MapillaryAbstractImage[]{img1, img2}));
    CommandJoin join2 = new CommandJoin(Arrays.asList(new MapillaryAbstractImage[]{img2, img3}));

    CommandDelete cmd1 = new CommandDelete(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{img1})));
    CommandDelete cmd2 = new CommandDelete(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{img2, img3})));

    this.record.addCommand(join1);
    this.record.addCommand(join2);

    MapillaryLayer
        .getInstance()
        .getData()
        .add(new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{img1, img2, img3})));

    this.record.addCommand(cmd1);
    assertFalse(MapillaryLayer.getInstance().getData().getImages().contains(this.img1));
    assertEquals(null, this.img2.previous());
    this.record.undo();
    assertTrue(MapillaryLayer.getInstance().getData().getImages().contains(this.img1));
    this.record.redo();
    this.record.addCommand(cmd2);
    assertEquals(0, MapillaryLayer.getInstance().getData().size());
  }

  /**
   * Test {@link CommandImport} class.
   */
  @Test
  public void commandImportTest() {
    MapillaryData data = MapillaryLayer.getInstance().getData();
    data.remove(data.getImages());

    CommandImport cmd1 = new CommandImport(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img1})));
    CommandImport cmd2 = new CommandImport(
            new ConcurrentSkipListSet<>(Arrays.asList(new MapillaryAbstractImage[]{this.img2, this.img3})));

    this.record.addCommand(cmd1);
    assertEquals(1, data.size());
    this.record.undo();
    assertEquals(0, data.size());
    this.record.redo();
    this.record.addCommand(cmd2);
    assertEquals(3, data.size());
  }
}
