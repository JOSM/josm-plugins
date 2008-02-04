package wmsplugin;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.io.ProgressInputStream;

public class WMSImage implements Serializable
{
	String constURL;
	protected BufferedImage theImage;
	protected double grabbedScale;
	protected EastNorth topLeft, bottomRight;
	double dEast, dNorth;	

	public WMSImage(String constURL)
	{
		this.constURL = constURL;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		constURL = (String) in.readObject();
		topLeft = (EastNorth) in.readObject();
		bottomRight = (EastNorth) in.readObject();
		dEast = in.readDouble();
		dNorth = in.readDouble();
		grabbedScale = in.readDouble();
		theImage = (BufferedImage) ImageIO.read(ImageIO.createImageInputStream(in));
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		System.out.println("writ" + theImage.getWidth(null));
		out.writeObject(constURL);
		out.writeObject(topLeft);
		out.writeObject(bottomRight);
		out.writeDouble(dEast);
		out.writeDouble(dNorth);
		out.writeDouble(grabbedScale);
		ImageIO.write(theImage, "png", ImageIO.createImageOutputStream(out));
	}

	public void grab(NavigatableComponent nc) throws IOException
	{
		EastNorth topLeft = nc.getEastNorth(0,0);
		grabbedScale = nc.getScale();  // scale is enPerPixel

		this.topLeft = topLeft;

		try
		{
			URL url = getURL(nc);
			doGrab(url);
		}
		catch(MalformedURLException e)
		{
			System.out.println("Illegal url. Error="+e);
		}
	}

	public void grab(NavigatableComponent nc,double minlat,double minlon,
			double maxlat,double maxlon) throws IOException
	{
		LatLon p = new LatLon(minlat,minlon),
				p2 = new LatLon(maxlat,maxlon);

		grabbedScale = nc.getScale(); // enPerPixel

		topLeft = Main.proj.latlon2eastNorth(new LatLon(maxlat,minlon));
		bottomRight = Main.proj.latlon2eastNorth(new LatLon(minlat,maxlon));

		int widthPx = (int)((bottomRight.east()-topLeft.east())/grabbedScale),
		heightPx = (int)
		((topLeft.north()-bottomRight.north()) / grabbedScale);

		try
		{
			URL url =  doGetURL(p.lon(),p.lat(),
									p2.lon(),p2.lat(),widthPx,heightPx);
			doGrab(url);
		}
		catch(MalformedURLException e)
		{
			System.out.println("Illegal url. Error="+e);
		}
	}

	private URL getURL(NavigatableComponent nc) throws MalformedURLException
	{
		double widthEN = nc.getWidth()*grabbedScale,
		heightEN = nc.getHeight()*grabbedScale;
		LatLon p = Main.proj.eastNorth2latlon(new EastNorth
				(topLeft.east(), topLeft.north()-heightEN));
		LatLon p2 = Main.proj.eastNorth2latlon(new EastNorth
				(topLeft.east()+widthEN, topLeft.north()));
		return doGetURL(p.lon(),p.lat(),p2.lon(),p2.lat(),
						(int)(widthEN/grabbedScale),
						(int)(heightEN/grabbedScale) );
	}

	public static NumberFormat latLonFormat = new DecimalFormat("###0.0000000");

	protected URL doGetURL(double w,double s,double e,double n, int wi, 
					int ht) throws MalformedURLException
	{
		String str = constURL + "&bbox="
			+ latLonFormat.format(w) + ","
			+ latLonFormat.format(s) + ","
			+ latLonFormat.format(e) + ","
			+ latLonFormat.format(n)
			+ "&width=" + wi + "&height=" + ht;
		return new URL(str.replace(" ", "%20"));
	}

	protected void doGrab (URL url) throws IOException
	{
		InputStream is = new ProgressInputStream(
			url.openConnection(), Main.pleaseWaitDlg);
		theImage = ImageIO.read(is);
		is.close();
		Main.map.repaint();
	}

	public void displace (double dEast, double dNorth)
	{
	 	this.dEast += dEast;	
	 	this.dNorth += dNorth;	
	}

	public boolean contains(EastNorth eastNorth)
	{
		double e1 = topLeft.east()+dEast, 
			   e2 = bottomRight.east()+dEast,
			   n1 = bottomRight.north()+dNorth,
			   n2 = topLeft.north()+dNorth;

		boolean b =  eastNorth.east()>=e1 && eastNorth.east()<=e2 &&
				eastNorth.north()>=n1 && eastNorth.north()<=n2;
		return b;
	}

	public void paint(Graphics g,NavigatableComponent nc) 
	{
		if (theImage != null)
		{
			double zoomInFactor = grabbedScale / nc.getScale();

			// Find the image x and y of the supplied bottom left
			// This will be the difference in EastNorth units, divided by the
			// grabbed scale in EastNorth/pixel.

			int w = theImage.getWidth(null), h=theImage.getHeight(null);
			EastNorth topLeftDisplaced  = 
				new EastNorth(topLeft.east()+dEast, topLeft.north()+dNorth);
			Point displacement = Main.map.mapView.getPoint(topLeftDisplaced);
			g.drawImage(theImage,displacement.x,displacement.y,
					(int)(displacement.x+w*zoomInFactor),
					(int)(displacement.y+h*zoomInFactor),
					0,0,w,h,null);
		}
	}

}
