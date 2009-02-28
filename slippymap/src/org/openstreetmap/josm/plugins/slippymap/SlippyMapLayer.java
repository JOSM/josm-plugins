package org.openstreetmap.josm.plugins.slippymap;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Class that displays a slippy map layer.
 * 
 * @author Frederik Ramm <frederik@remote.org>
 * @author LuVar <lubomir.varga@freemap.sk>
 * 
 */
public class SlippyMapLayer extends Layer implements ImageObserver,
        PreferenceChangedListener
{
	public int currentZoomLevel = 14;
    //ArrayList<HashMap<SlippyMapKey, SlippyMapTile>> tileStorage = null;
	private HashMap<SlippyMapKey, SlippyMapTile>[] tileStorage = null;

    Point[][]                                  pixelpos    = new Point[21][21];
    LatLon                                     lastTopLeft;
    LatLon                                     lastBotRight;
    int                                        z12x0, z12x1, z12y0, z12y1;
    private Image                              bufferImage;
    private SlippyMapTile                      clickedTile;
    private boolean                            needRedraw;
    private JPopupMenu                         tileOptionMenu;

    @SuppressWarnings("serial")
    public SlippyMapLayer()
    {
        super(tr("Slippy Map"));
        background = true;

        clearTileStorage();

        tileOptionMenu = new JPopupMenu();
        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Load Tile"))
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (clickedTile != null)
                {
                    clickedTile.loadImage();
                    needRedraw = true;
                    Main.map.repaint();
                }
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Show Tile Status"))
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (clickedTile != null)
                {
                    clickedTile.loadMetadata();
                    needRedraw = true;
                    Main.map.repaint();
                }
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Request Update"))
        {
            public void actionPerformed(ActionEvent ae)
            {
                if (clickedTile != null)
                {
                    clickedTile.requestUpdate();
                    needRedraw = true;
                    Main.map.repaint();
                }
            }
        }));

        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Load All Tiles"))
        {
            public void actionPerformed(ActionEvent ae)
            {
                loadAllTiles();
                needRedraw = true;
                Main.map.repaint();
            }
        }));
        
        //increase and decrease commands
        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Increase zoom"))
        {
            public void actionPerformed(ActionEvent ae)
            {
            	increaseZoomLevel();
                needRedraw = true;
                Main.map.repaint();
            }
        }));
        
        tileOptionMenu.add(new JMenuItem(new AbstractAction(tr("Decrease zoom"))
        {
            public void actionPerformed(ActionEvent ae)
            {
            	decreaseZoomLevel();
                needRedraw = true;
                Main.map.repaint();
            }
        }));
        //end of adding menu commands

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                Main.map.mapView.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        if (e.getButton() != MouseEvent.BUTTON3)
                            return;
                        clickedTile = getTileForPixelpos(e.getX(), e.getY());
                        tileOptionMenu.show(e.getComponent(), e.getX(), e
                                .getY());
                    }
                });

                listeners.add(new LayerChangeListener()
                        {
                            public void activeLayerChange(Layer oldLayer,
                                    Layer newLayer)
                            {
                            }

                            public void layerAdded(Layer newLayer)
                            {
                            }

                            public void layerRemoved(Layer oldLayer)
                            {
                                Main.pref.listener.remove(SlippyMapLayer.this);
                            }
                        });
            }
        });

        Main.pref.listener.add(this);
    }

    /**
     * Zoom in, go closer to map.
     */
    public void increaseZoomLevel() {
    	if(currentZoomLevel < SlippyMapPreferences.getMaxZoomLvl()) {
    		currentZoomLevel++;
    		//if(SlippyMapPreferences.getAutoloadTiles()) {
    		//	loadAllTiles();
    		//}
    	}
    }
    
    /**
     * Zoom out from map.
     */
    public void decreaseZoomLevel() {
    	if(currentZoomLevel > 10) {
    		currentZoomLevel--;
    		//if(SlippyMapPreferences.getAutoloadTiles()) {
    		//	loadAllTiles();
    		//}
    	}
    }
    
    public void clearTileStorage()
    {
    	int maxZoom = SlippyMapPreferences.getMaxZoomLvl();
    	// +1 because of array indexed from 0.
        tileStorage = new HashMap[maxZoom+1];

        for (int i = 0; i < maxZoom+1; i++)
            tileStorage[i] = new HashMap<SlippyMapKey, SlippyMapTile>();
    }

    void loadAllTiles()
    {
        MapView mv = Main.map.mapView;
        LatLon topLeft = mv.getLatLon(0, 0);
        LatLon botRight = mv.getLatLon(mv.getWidth(), mv.getHeight());
        z12x0 = lonToTileX(topLeft.lon());
        z12x1 = lonToTileX(botRight.lon());
        z12y0 = latToTileY(topLeft.lat());
        z12y1 = latToTileY(botRight.lat());
        if (z12x0 > z12x1)
        {
            int tmp = z12x0;
            z12x0 = z12x1;
            z12x1 = tmp;
        }
        if (z12y0 > z12y1)
        {
            int tmp = z12y0;
            z12y0 = z12y1;
            z12y1 = tmp;
        }
        //if there is more than 18 tiles on screen in any direction, do not load all tiles!
        if (z12x1 - z12x0 > 18) {
        	System.out.println("Not downloading all tiles because there is more than 18 tiles on X axis!");
            return;
        }
        if (z12y1 - z12y0 > 18) {
        	System.out.println("Not downloading all tiles because there is more than 18 tiles on Y axis!");
        	return;
        }
        
        for (int x = z12x0 - 1; x <= z12x1; x++)
        {
            for (int y = z12y0 - 1; y <= z12y1; y++)
            {
            	SlippyMapKey key = new SlippyMapKey(x,y);

                SlippyMapTile tile = tileStorage[currentZoomLevel].get(key);

                if (tile == null)
                    tileStorage[currentZoomLevel].put(key,
                            tile = new SlippyMapTile(x, y, currentZoomLevel));

                if (tile.getImage() == null)
                    tile.loadImage();
            }
        }
    }

    /**
     */
    @Override
    public void paint(Graphics g, MapView mv)
    {
        LatLon topLeft = mv.getLatLon(0, 0);
        LatLon botRight = mv.getLatLon(mv.getWidth(), mv.getHeight());
        Graphics oldg = g;

        if (lastTopLeft != null && lastBotRight != null
                && topLeft.equalsEpsilon(lastTopLeft)
                && botRight.equalsEpsilon(lastBotRight) && bufferImage != null
                && mv.getWidth() == bufferImage.getWidth(null)
                && mv.getHeight() == bufferImage.getHeight(null) && !needRedraw)
        {

            g.drawImage(bufferImage, 0, 0, null);
            return;
        }

        needRedraw = false;
        lastTopLeft = topLeft;
        lastBotRight = botRight;
        bufferImage = mv.createImage(mv.getWidth(), mv.getHeight());
        g = bufferImage.getGraphics();

        z12x0 = lonToTileX(topLeft.lon());
        z12x1 = lonToTileX(botRight.lon());
        z12y0 = latToTileY(topLeft.lat());
        z12y1 = latToTileY(botRight.lat());

        if (z12x0 > z12x1)
        {
            int tmp = z12x0;
            z12x0 = z12x1;
            z12x1 = tmp;
        }
        if (z12y0 > z12y1)
        {
            int tmp = z12y0;
            z12y0 = z12y1;
            z12y1 = tmp;
        }
        
        if (z12x1 - z12x0 > 18)
            return;
        if (z12y1 - z12y0 > 18)
            return;

        for (int x = z12x0 - 1; x <= z12x1 + 1; x++)
        {
            double lon = tileXToLon(x);
            for (int y = z12y0 - 1; y <= z12y1 + 1; y++)
            {
                LatLon tmpLL = new LatLon(tileYToLat(y), lon);
                pixelpos[x - z12x0 + 1][y - z12y0 + 1] = mv.getPoint(Main.proj
                        .latlon2eastNorth(tmpLL));
            }
        }

        int fontHeight = g.getFontMetrics().getHeight();

        g.setColor(Color.DARK_GRAY);

        float fadeBackground = SlippyMapPreferences.getFadeBackground();
        
        for (int x = z12x0 - 1; x <= z12x1; x++)
        {
            for (int y = z12y0 - 1; y <= z12y1; y++)
            {
            	SlippyMapKey key = new SlippyMapKey(x,y);
                SlippyMapTile tile;
                try
                {
                	tile = tileStorage[currentZoomLevel].get(key);
                } catch (IndexOutOfBoundsException ex) {
                	throw new RuntimeException("currentZoomLevel=" + currentZoomLevel + " and tile storage array have just size=" + tileStorage.length + " and maxZoomLvl in preferences is " + SlippyMapPreferences.getMaxZoomLvl() + ".", ex);
                }

                if (tile == null)
                {
                	tile = new SlippyMapTile(x, y, currentZoomLevel);
                    tileStorage[currentZoomLevel].put(key, tile);
                    if(SlippyMapPreferences.getAutoloadTiles()) {
                    	//TODO probably do on background
                    	tile.loadImage();
            		}
                }
                Image img = tile.getImage();

                if (img != null)
                {
                    Point p = pixelpos[x - z12x0 + 1][y - z12y0 + 1];
                    Point p2 = pixelpos[x - z12x0 + 2][y - z12y0 + 2];
                    g.drawImage(img, p.x, p.y, p2.x - p.x, p2.y - p.y, this);
                    
                    if(fadeBackground != 0f) {
	                    //dimm by painting opaque rect...
	                    g.setColor(new Color(1f,1f,1f,fadeBackground));
	                    g.fillRect(p.x, p.y, p2.x - p.x, p2.y - p.y);
                    }//end of if dim != 0
                }//end of if img != null
            }//end of for
        }//end of for

        for (int x = z12x0 - 1; x <= z12x1; x++)
        {
            Point p = pixelpos[x - z12x0 + 1][0];

            if (x % 32 == 0)
            {
                // level 7 tile boundary
                g.fillRect(p.x - 1, 0, 3, mv.getHeight());
            }
            else
            {
                g.drawLine(p.x, 0, p.x, mv.getHeight());
            }

            for (int y = z12y0 - 1; y <= z12y1; y++)
            {
            	SlippyMapKey key = new SlippyMapKey(x,y);
                int texty = p.y + 2 + fontHeight;
                SlippyMapTile tile = tileStorage[currentZoomLevel].get(key);
                if(tile == null) {
                	continue;
                }
                p = pixelpos[x - z12x0 + 1][y - z12y0 + 2];
                g.drawString("x=" + x + " y=" + y + " z=" + currentZoomLevel + "", p.x + 2, texty);
                texty += 1 + fontHeight;
                if ((x % 32 == 0) && (y % 32 == 0))
                {
                    g.drawString("x=" + x / 32 + " y=" + y / 32 + " z=7",
                            p.x + 2, texty);
                    texty += 1 + fontHeight;
                }

                String md = tile.getMetadata();
                if (md != null)
                {
                    g.drawString(md, p.x + 2, texty);
                    texty += 1 + fontHeight;
                }

                if (tile.getImage() == null)
                {
                    g.drawString(tr("image not loaded"), p.x + 2, texty);
                    texty += 1 + fontHeight;
                }

                if (x == z12x0 - 1)
                {
                    if (y % 32 == 31)
                    {
                        g.fillRect(0, p.y - 1, mv.getWidth(), 3);
                    }
                    else
                    {
                        g.drawLine(0, p.y, mv.getWidth(), p.y);
                    }
                }
            } //end of for
        }

        oldg.drawImage(bufferImage, 0, 0, null);

        if((z12x1 - z12x0 < 2) || (z12y1 - z12y0 < 2)) {
        	if(SlippyMapPreferences.getAutozoom()) {
        		increaseZoomLevel();
        	}
        	this.paint(oldg, mv);
        }
        
        if((z12x1 - z12x0 > 6) || (z12y1 - z12y0 > 6)) {
        	if(SlippyMapPreferences.getAutozoom()) {
        		decreaseZoomLevel();
        	}
        	this.paint(oldg, mv);
        }
        
        g.drawString("currentZoomLevel=" + currentZoomLevel, 120, 120);
    }//end of paint metod

    SlippyMapTile getTileForPixelpos(int px, int py)
    {
        int tilex = z12x1;
        int tiley = z12y1;
        for (int x = z12x0; x <= z12x1; x++)
        {

            if (pixelpos[x - z12x0 + 1][0].x > px)
            {
                tilex = x - 1;
                break;
            }
        }
        if (tilex == -1)
            return null;
        for (int y = z12y0; y <= z12y1; y++)
        {

            if (pixelpos[0][y - z12y0 + 1].y > py)
            {
                tiley = y - 1;
                break;
            }
        }
        if (tiley == -1)
            return null;

        SlippyMapKey key = new SlippyMapKey(tilex,tiley);
        SlippyMapTile tile = tileStorage[currentZoomLevel].get(key);
        if (tile == null)
            tileStorage[currentZoomLevel].put(key,
                    tile = new SlippyMapTile(tilex, tiley, currentZoomLevel));
        return tile;
    }

    @Override
    public Icon getIcon()
    {
        return ImageProvider.get("slippymap");
    }

    @Override
    public Object getInfoComponent()
    {
        return null;
    }

    @Override
    public Component[] getMenuEntries()
    {
        return new Component[]
        {
                new JMenuItem(new LayerListDialog.ShowHideLayerAction(this)),
                new JMenuItem(new LayerListDialog.DeleteLayerAction(this)),
                new JSeparator(),
                // color,
                new JMenuItem(new RenameLayerAction(associatedFile, this)),
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this)) };
    }

    @Override
    public String getToolTipText()
    {
        return null;
    }

    @Override
    public boolean isMergable(Layer other)
    {
        return false;
    }

    @Override
    public void mergeFrom(Layer from)
    {
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v)
    {
    }

    private int latToTileY(double lat)
    {
        double l = lat / 180 * Math.PI;
        double pf = Math.log(Math.tan(l) + (1 / Math.cos(l)));
        return (int) (Math.pow(2.0, currentZoomLevel-1) * (Math.PI - pf) / Math.PI);
    }

    private int lonToTileX(double lon)
    {
        return (int) (Math.pow(2.0, currentZoomLevel-3) * (lon + 180.0) / 45.0);
    }

    private double tileYToLat(int y)
    {
        return Math.atan(Math.sinh(Math.PI - (Math.PI * y / Math.pow(2.0, currentZoomLevel-1)))) * 180
                / Math.PI;
    }

    private double tileXToLon(int x)
    {
        return x * 45.0 / Math.pow(2.0, currentZoomLevel-3) - 180.0;
    }

    public boolean imageUpdate(Image img, int infoflags, int x, int y,
            int width, int height)
    {

        boolean done = ((infoflags & (ERROR | FRAMEBITS | ALLBITS)) != 0);
        // Repaint immediately if we are done, otherwise batch up
        // repaint requests every 100 milliseconds
        needRedraw = true;
        Main.map.repaint(done ? 0 : 100);
        return !done;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openstreetmap.josm.data.Preferences.PreferenceChangedListener#preferenceChanged(java.lang.String,
     *      java.lang.String)
     */
    public void preferenceChanged(String key, String newValue)
    {
        if (key.startsWith(SlippyMapPreferences.PREFERENCE_PREFIX))
        {
//            System.err.println(this + ".preferenceChanged('" + key + "', '"
//                    + newValue + "') called");

            clearTileStorage();
        }
    }

    @Override
    public void destroy()
    {
        Main.pref.listener.remove(SlippyMapLayer.this);
    }
}
