/* Copyright 2014 Malcolm Herring
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * For a copy of the GNU General Public License, see <http://www.gnu.org/licenses/>.
 */

package jrender;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import render.ChartContext;
import render.Renderer;
import render.Rules;
import s57.S57map;
import s57.S57map.Feature;
import s57.S57map.Snode;

public class Jrender {

	static String srcdir;
	static String dstdir;
	static int xtile;
	static int ytile;
	static int zoom;
	static double minlat;
	static double minlon;
	static double maxlat;
	static double maxlon;
	static ArrayList<String> buf;
	static ArrayList<String> send;
	static HashMap<String, Boolean> deletes;
	static Context context;
	static S57map map;
	static int empty;

	static class Context implements ChartContext {
		
	  static double top = 0;
	  static double mile = 0;
	  
	  public Context () {
			top = (1.0 - Math.log(Math.tan(Math.toRadians(maxlat)) + 1.0 / Math.cos(Math.toRadians(maxlat))) / Math.PI) / 2.0 * 256.0 * 4096.0;
			mile = 768 / ((maxlat - minlat) * 60);
	  }
	  
		@Override
		public Point2D getPoint(Snode coord) {
			double x = (Math.toDegrees(coord.lon) - minlon) * 256.0 * 2048.0 / 180.0;
			double y = ((1.0 - Math.log(Math.tan(coord.lat) + 1.0 / Math.cos(coord.lat)) / Math.PI) / 2.0 * 256.0 * 4096.0) - top;
			return new Point2D.Double(x, y);
		}

		@Override
		public double mile(Feature feature) {
			return mile;
		}
	}
	
	public static void tileMap(ArrayList<String> buf, String idir, int zoom) throws IOException {
		String k = "";
		String v = "";
		
		double lat = 0;
		double lon = 0;
		long id = 0;

		BufferedImage img;
		boolean inOsm = false;
		boolean inNode = false;
		boolean inWay = false;
		boolean inRel = false;
		
		context = new Context();

		for (String ln : buf) {
			if (inOsm) {
				if ((inNode || inWay || inRel) && (ln.contains("<tag"))) {
					k = v = "";
					String[] token = ln.split("k=");
					k = token[1].split("[\"\']")[1];
					token = token[1].split("v=");
					v = token[1].split("[\"\']")[1];
					if (!k.isEmpty() && !v.isEmpty()) {
						map.addTag(k, v);
					}
				}
				if (inNode) {
					if (ln.contains("</node")) {
						inNode = false;
						map.tagsDone(id);
					}
				} else if (ln.contains("<node")) {
					for (String token : ln.split("[ ]+")) {
						if (token.matches("^id=.+")) {
							id = Long.parseLong(token.split("[\"\']")[1]);
						} else if (token.matches("^lat=.+")) {
							lat = Double.parseDouble(token.split("[\"\']")[1]);
						} else if (token.matches("^lon=.+")) {
							lon = Double.parseDouble(token.split("[\"\']")[1]);
						}
					}
					map.addNode(id, lat, lon);
					if (ln.contains("/>")) {
						map.tagsDone(id);
					} else {
						inNode = true;
					}
				} else if (inWay) {
					if (ln.contains("<nd")) {
						long ref = 0;
						for (String token : ln.split("[ ]+")) {
							if (token.matches("^ref=.+")) {
								ref = Long.parseLong(token.split("[\"\']")[1]);
							}
						}
						map.addToEdge(ref);
					}
					if (ln.contains("</way")) {
						inWay = false;
						map.tagsDone(id);
					}
				} else if (ln.contains("<way")) {
					for (String token : ln.split("[ ]+")) {
						if (token.matches("^id=.+")) {
							id = Long.parseLong(token.split("[\"\']")[1]);
						}
					}
					map.addEdge(id);
					if (ln.contains("/>")) {
						map.tagsDone(0);
					} else {
						inWay = true;
					}
				} else if (ln.contains("</osm")) {
					map.mapDone();
					inOsm = false;
					break;
				} else if (inRel) {
					if (ln.contains("<member")) {
						String type = "";
						String role = "";
						long ref = 0;
						for (String token : ln.split("[ ]+")) {
							if (token.matches("^ref=.+")) {
								ref = Long.parseLong(token.split("[\"\']")[1]);
							} else if (token.matches("^type=.+")) {
								type = (token.split("[\"\']")[1]);
							} else if (token.matches("^role=.+")) {
								String str[] = token.split("[\"\']");
								if (str.length > 1) {
									role = (token.split("[\"\']")[1]);
								}
							}
						}
						if ((role.equals("outer") || role.equals("inner")) && type.equals("way"))
							map.addToArea(ref, role.equals("outer"));
					}
					if (ln.contains("</relation")) {
						inRel = false;
						map.tagsDone(id);
					}
				} else if (ln.contains("<relation")) {
					for (String token : ln.split("[ ]+")) {
						if (token.matches("^id=.+")) {
							id = Long.parseLong(token.split("[\"\']")[1]);
						}
					}
					map.addArea(id);
					if (ln.contains("/>")) {
						map.tagsDone(id);
					} else {
						inRel = true;
					}
				}
			} else if (ln.contains("<osm")) {
				inOsm = true;
				map = new S57map();
				map.addNode(1, maxlat, minlon);
				map.addNode(2, minlat, minlon);
				map.addNode(3, minlat, maxlon);
				map.addNode(4, maxlat, maxlon);
				map.bounds.minlat = Math.toRadians(minlat);
				map.bounds.maxlat = Math.toRadians(maxlat);
				map.bounds.minlon = Math.toRadians(minlon);
				map.bounds.maxlon = Math.toRadians(maxlon);
			}
		}
		
//		img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		Rectangle rect = new Rectangle(2048, 2048);
		img = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_ARGB);
		Renderer.reRender(img.createGraphics(), rect, zoom, 0.05, map, context);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", bos);
//		empty = bos.size();
//		tile(zoom, 1, 0, 0);
		FileOutputStream fos = new FileOutputStream(dstdir + "tst_" + zoom + ".png");
		bos.writeTo(fos);
		fos.close();

//		for (int z = 12; z <= 18; z++) {
			DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
			Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
			SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
			svgGenerator.setBackground(Rules.Bwater);
			svgGenerator.clearRect(rect.x, rect.y, rect.width, rect.height);
			svgGenerator.setSVGCanvasSize(rect.getSize());
			svgGenerator.setClip(rect.x, rect.y, rect.width, rect.height);
//			svgGenerator.translate(-256, -256);
			Renderer.reRender(svgGenerator, rect, zoom, 0.05, map, context);
			svgGenerator.stream(dstdir + "tst_" + zoom + ".svg");
//		}
	}
	
	static void tile(int zoom, int s, int xn, int yn) throws IOException {
		int scale = (int) Math.pow(2, zoom - 12);
		int xdir = (scale * xtile) + xn;
		int ynam = (scale * ytile) + yn;
		BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		g2.scale(s, s);
		g2.translate(-(256 + (xn * 256 / s)), -(256 + (yn * 256 / s)));
		Renderer.reRender(g2, new Rectangle(256, 256), zoom, 1, map, context);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(img, "png", bos);
		if (bos.size() > empty) {
			String dstnam = dstdir + zoom + "/" + xdir + "/" + ynam + ".png";
			deletes.remove(dstnam);
			send.add("put " + dstnam + " cache/tiles-" + zoom + "-" + xdir + "-" + ynam + ".png");
			File ofile = new File(dstdir + "/" + zoom + "/" + xdir + "/");
			ofile.mkdirs();
			FileOutputStream fos = new FileOutputStream(dstdir + "/" + zoom + "/" + xdir + "/" + ynam + ".png");
			bos.writeTo(fos);
			fos.close();
			if (send.size() > 10) {
				PrintWriter writer = new PrintWriter(srcdir + zoom + "-" + xdir + "-" + ynam + ".send", "UTF-8");
				for (String str : send) {
					writer.println(str);
				}
				writer.close();
				send = new ArrayList<String>();
			}
		}
		if ((zoom >= 12) && (zoom < 18) && ((zoom < 16) || (bos.size() > empty))) {
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					tile((zoom + 1), (s * 2), (xn * 2 + x), (yn * 2 + y));
				}
			}
		}
	}
	
	static void clean(int z, int xn, int yn) throws Exception {
		int scale = (int) Math.pow(2, z - 12);
		int xdir = (scale * xtile) + xn;
		int ynam = (scale * ytile) + yn;
		String delnam = dstdir + z + "/" + xdir + "/" + ynam + ".png";
		File delfile = new File(delnam);
		if (delfile.exists()) {
			deletes.put(delnam, true);
			delfile.delete();
		}
		if ((z < 18)) {
			for (int x = 0; x < 2; x++) {
				for (int y = 0; y < 2; y++) {
					clean((z + 1), (xn * 2 + x), (yn * 2 + y));
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		srcdir = args[0];
		dstdir = args[1];
		zoom = Integer.parseInt(args[2]);
		xtile = Integer.parseInt(args[3]);
		ytile = Integer.parseInt(args[4]);
		buf = new ArrayList<String>();
		send = new ArrayList<String>();
		deletes = new HashMap<String, Boolean>();
		BufferedReader in = new BufferedReader(new FileReader(srcdir + zoom + "-" + xtile + "-" + ytile + ".osm"));
		String ln;
		while ((ln = in.readLine()) != null) {
			if (ln.contains("<bounds")) {
				for (String token : ln.split("[ ]+")) {
					if (token.matches("^minlat=.+")) {
						minlat = Double.parseDouble(token.split("[\"\']")[1]);
					} else if (token.matches("^minlon=.+")) {
						minlon = Double.parseDouble(token.split("[\"\']")[1]);
					} else if (token.matches("^maxlat=.+")) {
						maxlat = Double.parseDouble(token.split("[\"\']")[1]);
					} else if (token.matches("^maxlon=.+")) {
						maxlon = Double.parseDouble(token.split("[\"\']")[1]);
					}
				}
			} else {
				buf.add(ln);
			}
		}
		in.close();
		if (zoom == 12) {
			clean(12, 0, 0);
		}
		tileMap(buf, dstdir, zoom);
		if ((send.size() + deletes.size()) > 0) {
			PrintWriter writer = new PrintWriter(srcdir + zoom + "-" + xtile + "-" + ytile + ".send", "UTF-8");
			for (String str : send) {
				writer.println(str);
			}
			for (String del : deletes.keySet()) {
				writer.println("rm " + del);
			}
			writer.close();
		}
		System.exit(0);
	}
}
