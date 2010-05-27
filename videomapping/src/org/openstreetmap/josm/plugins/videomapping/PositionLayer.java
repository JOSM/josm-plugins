package org.openstreetmap.josm.plugins.videomapping;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.*;

import static org.openstreetmap.josm.tools.I18n.*;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;

public class PositionLayer extends Layer implements MouseListener,MouseMotionListener, KeyListener {
	private List<WayPoint> ls;
	private GpsPlayer l;
	private Collection<WayPoint> selected;
	private TimerTask ani;
	private boolean dragIcon=false; //do we move the icon by hand?
	private Point mouse;
	private ImageIcon icon;
		
	public PositionLayer(String name, final List<WayPoint> ls) {
		super(name);
		this.ls=ls;
		l= new GpsPlayer(ls);
		selected = new ArrayList<WayPoint>();
		icon=ImageProvider.get("videomapping.png");
		Action a = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				System.err.println("!!!boom!!!");
			}};		
		Main.map.mapView.addMouseListener(this);
		Main.map.mapView.addMouseMotionListener(this);
		
		//Main.panel.addKeyListener(this);
		Main.map.mapView.addKeyListener(this);
		//Main.contentPane.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),"pressed");
		//Main.contentPane.getActionMap().put("pressed",a);

		//Main.contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),a);
		//Main.contentPane.getActionMap().put("doSomething",a);
						
		TimerTask ani=new TimerTask() {
			
			@Override
			//some cheap animation stuff
			public void run() {				
				l.next();
				Main.map.mapView.repaint();
			}
		};
		Timer t= new Timer();
		//t.schedule(ani,2000,2000);
		l.next();l.next();
		
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public Object getInfoComponent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component[] getMenuEntries() {
        return new Component[]{
                new JMenuItem(LayerListDialog.getInstance().createShowHideLayerAction(this)),
                new JMenuItem(LayerListDialog.getInstance().createDeleteLayerAction(this)),
                new JSeparator(),
                //TODO here my stuff
                new JSeparator(),
                new JMenuItem(new LayerListPopup.InfoAction(this))};//TODO here infos about the linked videos
	}
	  


	@Override
	public String getToolTipText() {
		return tr("Shows current position in the video");
	}

	// no merging necessary
	@Override
	public boolean isMergable(Layer arg0) {
		return false;
	}

	@Override
	public void mergeFrom(Layer arg0) {
		
	}

	
	
	@Override
	//Draw the current position
	public void paint(Graphics2D g, MapView map, Bounds bound) {
		Point p;
		//TODO Source out redundant calculations
		//TODO make icon transparent
		//draw all GPS points
		g.setColor(Color.GREEN);
		for(WayPoint n: ls) {
			p = Main.map.mapView.getPoint(n.getEastNorth());
			g.drawOval(p.x - 2, p.y - 2, 4, 4);
			}
		g.setColor(Color.RED);
		//draw selected GPS points
		for(WayPoint n:selected)
		{
			p = Main.map.mapView.getPoint(n.getEastNorth());
			g.drawOval(p.x - 2, p.y - 2, 4, 4);
		}
		//draw surrounding points
		g.setColor(Color.YELLOW);
		if(l.getPrev()!=null)
		{
			p = Main.map.mapView.getPoint(l.getPrev().getEastNorth());
			g.drawOval(p.x - 2, p.y - 2, 4, 4);
			Point p2 = Main.map.mapView.getPoint(l.getCurr().getEastNorth());
			g.drawLine(p.x, p.y, p2.x, p2.y);
		}
		if(l.getNext()!=null)
		{
			p = Main.map.mapView.getPoint(l.getNext().getEastNorth());
			g.drawOval(p.x - 2, p.y - 2, 4, 4);
			Point p2 = Main.map.mapView.getPoint(l.getCurr().getEastNorth());
			g.drawLine(p.x, p.y, p2.x, p2.y);
		}
		//draw cam icon
		if(dragIcon)
		{
			if(mouse!=null)
			{
				p=mouse;
				icon.paintIcon(null, g, p.x-icon.getIconWidth()/2, p.y-icon.getIconHeight()/2);
			}
		}
		else
		{
			if (l.getCurr()!=null){
			p=Main.map.mapView.getPoint(l.getCurr().getEastNorth());
			icon.paintIcon(null, g, p.x-icon.getIconWidth()/2, p.y-icon.getIconHeight()/2);
			g.drawString(l.getCurr().getTime().toString(), p.x, p.y);
			}
		}
	}

	private void markNearestNode(Point mouse) {
		final int MAX=10; 
		Point p;		
		Rectangle rect = new Rectangle(mouse.x-MAX/2,mouse.y-MAX/2,mouse.x+MAX/2,mouse.y+MAX/2);
		//iterate through all possible notes
		for(WayPoint n : ls)
		{
			p = Main.map.mapView.getPoint(n.getEastNorth());
			if (rect.contains(p))
			{				
				selected.add(n);
			}
			
		}	
	}
	
	private WayPoint getNearestPoint(Point mouse)
	{
		final int MAX=10;
		Point p;
		Rectangle rect = new Rectangle(mouse.x-MAX/2,mouse.y-MAX/2,mouse.x+MAX/2,mouse.y+MAX/2);
		//iterate through all possible notes
		for(WayPoint n : ls) //TODO this is not very clever, what better way to find this WP?
		{
			p = Main.map.mapView.getPoint(n.getEastNorth());
			if (rect.contains(p))
			{				
				return n;
			}
			
		}
		return null;
		
	}
	
	@Override
	public void visitBoundingBox(BoundingXYVisitor arg0) {
		// TODO dunno what to do here

	}

	//mark selected points
	public void mouseClicked(MouseEvent e) {		
	}

	public void mouseEntered(MouseEvent arg0) {	
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON1) {
			//is it on the cam icon?
			if (l.getCurr()!=null)
			{
				if (getIconRect().contains(e.getPoint()))
				{
					mouse=e.getPoint();
					dragIcon=true;
					//ani.cancel();
				}
			}
		}
		
	}

	public void mouseReleased(MouseEvent e) {
		
		//only on leftclicks of our layer
		if(e.getButton() == MouseEvent.BUTTON1) {
			if(dragIcon)
			{
				dragIcon=false;
			}
			else
			{
				//JOptionPane.showMessageDialog(Main.parent,"test");
				markNearestNode(e.getPoint());
				WayPoint wp = getNearestPoint(e.getPoint());
				if(wp!=null)
				{
					l.jump(wp);			
				}
			}
			Main.map.mapView.repaint();
		}
		
	}
	
	//gets point on the line between
	private Point getInterpolated(Point m)
	{		
		Point highest = Main.map.mapView.getPoint(l.getCurr().getEastNorth());
		Point smallest = Main.map.mapView.getPoint(l.getNext().getEastNorth());
		float slope=(float)(highest.y-smallest.y) / (float)(highest.x - smallest.x);
		
		m.y = highest.y+Math.round(slope*(m.x-highest.x));
		if(m.x<smallest.x)
		{
			m=smallest;
		}
		if(m.x>highest.x) m=highest;
		System.out.println((m));
		return m;
	}
	
	public void mouseDragged(MouseEvent e) {		
		if(dragIcon)
		{			
			mouse=e.getPoint();
			//restrict to GPS track
			mouse=getInterpolated(mouse);

			Main.map.mapView.repaint();
		}
	}

	private Rectangle getIconRect()
	{
		Point p = Main.map.mapView.getPoint(l.getCurr().getEastNorth());
		return new Rectangle(p.x-icon.getIconWidth()/2,p.y-icon.getIconHeight()/2,icon.getIconWidth(),icon.getIconHeight());
	}
	
	public void mouseMoved(MouseEvent e) {		
		
		if (l.getCurr()!=null)
		{						
			if (getIconRect().contains(e.getPoint()))
			{
				Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
			else
			{
				Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
		
	}

	public void keyPressed(KeyEvent e) {
		int i;
		System.out.println(e.getKeyCode());
		switch(e.getKeyCode())
		{
			case KeyEvent.VK_RIGHT:
				{
					l.jump(10);
				};break;
			case KeyEvent.VK_LEFT:
			{
				l.jump(-10);

			};break;
			case KeyEvent.VK_SPACE:
			{
				ani.cancel();
			}
		}
		
	}

	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

}
