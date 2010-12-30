package oseam.seamarks;

import javax.swing.ImageIcon;
import javax.swing.JRadioButton;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;

import oseam.dialogs.OSeaMAction;

abstract public class SeaMark {

	protected OSeaMAction dlg = null;

	public OSeaMAction getDlg() {
		return dlg;
	}

	protected SeaMark(OSeaMAction dia) {
		dlg = dia;
		region = Main.pref.get("tomsplugin.IALA").equals("A") ? Reg.A : Reg.B;
	}

	public enum Reg {
		A, B
	}

	private Reg region = Reg.A;

	public Reg getRegion() {
		return region;
	}

	public void setRegion(Reg reg) {
		region = reg;
	}

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String nam) {
		name = nam;
	}

	public enum Obj {
		UNKNOWN, BCNCAR, BCNISD, BCNLAT, BCNSAW, BCNSPP, BOYCAR, BOYISD, BOYLAT, BOYSAW, BOYSPP, LITMAJ, LITMIN, LITFLT, LITVES, LNDMRK, MORFAC
	}

	public static final EnumMap<Obj, String> objects = new EnumMap<Obj, String>(Obj.class);
	static {
		objects.put(Obj.BCNCAR, "beacon_cardinal");
		objects.put(Obj.BCNISD, "beacon_isolated_danger");
		objects.put(Obj.BCNLAT, "beacon_lateral");
		objects.put(Obj.BCNSAW, "beacon_safe_water");
		objects.put(Obj.BCNSPP, "beacon_special_purpose");
		objects.put(Obj.BOYCAR, "buoy_cardinal");
		objects.put(Obj.BOYISD, "buoy_isolated_danger");
		objects.put(Obj.BOYLAT, "buoy_lateral");
		objects.put(Obj.BOYSAW, "buoy_safe_water");
		objects.put(Obj.BOYSPP, "buoy_special_purpose");
		objects.put(Obj.LITMAJ, "light_major");
		objects.put(Obj.LITMIN, "light_minor");
		objects.put(Obj.LITFLT, "light_float");
		objects.put(Obj.LITVES, "light_vessel");
		objects.put(Obj.LNDMRK, "landmark");
		objects.put(Obj.MORFAC, "mooring");

	}

	private Obj object = Obj.UNKNOWN;

	public Obj getObject() {
		return object;
	}

	public void setObject(Obj obj) {
		object = obj;
	}

	public enum Ent {
		BODY, BUOY, BEACON, FLOAT, TOPMARK, DAYMARK, LIGHT
	}

	public enum Col {
		UNKNOWN, WHITE, RED, ORANGE, AMBER, YELLOW, GREEN, BLUE, VIOLET, BLACK, RED_GREEN_RED, GREEN_RED_GREEN, RED_WHITE, BLACK_YELLOW, BLACK_YELLOW_BLACK, YELLOW_BLACK, YELLOW_BLACK_YELLOW, BLACK_RED_BLACK
	}

	public static final EnumMap<Col, String> colours = new EnumMap<Col, String>(Col.class);
	static {
		colours.put(Col.WHITE, "white");
		colours.put(Col.RED, "red");
		colours.put(Col.ORANGE, "orange");
		colours.put(Col.AMBER, "amber");
		colours.put(Col.YELLOW, "yellow");
		colours.put(Col.GREEN, "green");
		colours.put(Col.BLUE, "blue");
		colours.put(Col.VIOLET, "violet");
		colours.put(Col.BLACK, "black");
		colours.put(Col.RED_GREEN_RED, "red;green;red");
		colours.put(Col.GREEN_RED_GREEN, "green;red;green");
		colours.put(Col.RED_WHITE, "red;white");
		colours.put(Col.BLACK_YELLOW, "black;yellow");
		colours.put(Col.BLACK_YELLOW_BLACK, "black;yellow;black");
		colours.put(Col.YELLOW_BLACK, "yellow;black");
		colours.put(Col.YELLOW_BLACK_YELLOW, "yellow;black;yellow");
		colours.put(Col.BLACK_RED_BLACK, "black;red;black");
	}

	private Col bodyColour = Col.UNKNOWN;

	public Col getColour(Ent ent) {
		switch (ent) {
		case BODY:
		case BUOY:
		case BEACON:
		case FLOAT:
			return bodyColour;
		case TOPMARK:
			return topColour;
		case DAYMARK:
			return dayColour;
		case LIGHT:
			return lightColour[sectorIndex];
		}
		return Col.UNKNOWN;
	}

	public void setColour(Ent ent, Col col) {
		switch (ent) {
		case BODY:
		case BUOY:
		case BEACON:
		case FLOAT:
			bodyColour = col;
			break;
		case TOPMARK:
			topColour = col;
			break;
		case DAYMARK:
			dayColour = col;
			break;
		case LIGHT:
			lightColour[sectorIndex] = col;
			break;
		}
	}

	public enum Cat {
		UNKNOWN, LAT_PORT, LAT_STBD, LAT_PREF_PORT, LAT_PREF_STBD, CARD_NORTH, CARD_EAST, CARD_SOUTH, CARD_WEST, LIGHT_HOUSE, LIGHT_MAJOR, LIGHT_MINOR, LIGHT_VESSEL, LIGHT_FLOAT, MOORING_BUOY
	}

	public static final EnumMap<Cat, String> categories = new EnumMap<Cat, String>(Cat.class);
	static {
		categories.put(Cat.LAT_PORT, "port");
		categories.put(Cat.LAT_STBD, "starboard");
		categories.put(Cat.LAT_PREF_PORT, "preferred_channel_port");
		categories.put(Cat.LAT_PREF_STBD, "preferred_channel_starboard");
		categories.put(Cat.CARD_NORTH, "north");
		categories.put(Cat.CARD_EAST, "east");
		categories.put(Cat.CARD_SOUTH, "south");
		categories.put(Cat.CARD_WEST, "west");
		categories.put(Cat.MOORING_BUOY, "mooring_buoy");
	}

	private Cat category = Cat.UNKNOWN;

	public Cat getCategory() {
		return category;
	}

	public void setCategory(Cat cat) {
		category = cat;
	}

	public enum Shp {
		UNKNOWN, PILLAR, SPAR, CAN, CONE, SPHERE, BARREL, FLOAT, SUPER, BEACON, TOWER, STAKE, PERCH
	}

	public static final EnumMap<Shp, String> shapes = new EnumMap<Shp, String>(Shp.class);
	static {
		shapes.put(Shp.PILLAR, "pillar");
		shapes.put(Shp.SPAR, "spar");
		shapes.put(Shp.CAN, "can");
		shapes.put(Shp.CONE, "conical");
		shapes.put(Shp.SPHERE, "sphere");
		shapes.put(Shp.BARREL, "barrel");
		shapes.put(Shp.FLOAT, "float");
		shapes.put(Shp.SUPER, "super-buoy");
		shapes.put(Shp.BEACON, "beacon");
		shapes.put(Shp.TOWER, "tower");
		shapes.put(Shp.STAKE, "stake");
		shapes.put(Shp.PERCH, "perch");
	}

	private Shp shape = Shp.UNKNOWN;

	public Shp getShape() {
		return shape;
	}

	public void setShape(Shp styl) {
		shape = styl;
	}

	public enum Top {
		NONE, CAN, CONE, SPHERE, X_SHAPE, NORTH, SOUTH, EAST, WEST, SPHERES2
	}

	public static final EnumMap<Top, String> tops = new EnumMap<Top, String>(Top.class);
	static {
		tops.put(Top.CAN, "cylinder");
		tops.put(Top.CONE, "cylinder");
		tops.put(Top.SPHERE, "SPHERE");
		tops.put(Top.X_SHAPE, "X-SHAPE");
		tops.put(Top.NORTH, "2 cones up");
		tops.put(Top.SOUTH, "2 cones down");
		tops.put(Top.EAST, "2 cones base together");
		tops.put(Top.WEST, "2 cones points together");
		tops.put(Top.SPHERES2, "2 spheres");
	}

	private Top topShape = Top.NONE;
	private Col topColour = Col.UNKNOWN;

	public boolean hasTopmark() {
		return (topShape != Top.NONE);
	}

	public Top getTopmark() {
		return topShape;
	}

	public void setTopmark(Top top) {
		topShape = top;
	}

	public enum Day {
		NONE, BOARD, DIAMOND, TRIANGLE, TRIANGLE_INV, SQUARE
	}

	public static final EnumMap<Day, String> days = new EnumMap<Day, String>(Day.class);
	static {
		days.put(Day.BOARD, "board");
		days.put(Day.DIAMOND, "diamond");
		days.put(Day.TRIANGLE, "triangle, point up");
		days.put(Day.TRIANGLE_INV, "triangle, point down");
		days.put(Day.SQUARE, "square");
	}

	private Day dayShape = Day.NONE;
	private Col dayColour = Col.UNKNOWN;

	public boolean hasDaymark() {
		return (dayShape != Day.NONE);
	}

	public Day getDaymark() {
		return dayShape;
	}

	public void setDaymark(Day day) {
		dayShape = day;
	}

	private boolean Radar = false;

	public boolean hasRadar() {
		return Radar;
	}

	public void setRadar(boolean radar) {
		Radar = radar;
	}

	private boolean Racon = false;

	public boolean hasRacon() {
		return Racon;
	}

	public void setRacon(boolean racon) {
		Racon = racon;
	}

	public enum Rtb {
		UNKNOWN, RACON, RAMARK, LEADING
	}

	private Rtb RaType = Rtb.UNKNOWN;

	public Rtb getRaType() {
		return RaType;
	}

	public void setRaType(Rtb type) {
		RaType = type;
	}

	private String RaconGroup = "";

	public String getRaconGroup() {
		return RaconGroup;
	}

	public void setRaconGroup(String raconGroup) {
		RaconGroup = raconGroup;
	}

	private boolean FogSignal = false;

	public boolean hasFog() {
		return FogSignal;
	}

	public void setFog(boolean fog) {
		FogSignal = fog;
	}

	public enum Fog {
		UNKNOWN, HORN, SIREN, DIA, BELL, WHIS, GONG, EXPLOS
	}

	private Fog FogSound = Fog.UNKNOWN;

	public Fog getFogSound() {
		return FogSound;
	}

	public void setFogSound(Fog sound) {
		FogSound = sound;
	}

	private String FogGroup = "";

	public String getFogGroup() {
		return FogGroup;
	}

	public void setFogGroup(String group) {
		FogGroup = group;
	}

	private String FogPeriod = "";

	public String getFogPeriod() {
		return FogPeriod;
	}

	public void setFogPeriod(String period) {
		FogPeriod = period;
	}

	private boolean Fired = false;

	public boolean isFired() {
		return Fired;
	}

	public void setFired(boolean fired) {
		Fired = fired;
	}

	private boolean Sectored = false;

	public boolean isSectored() {
		return Sectored;
	}

	public void setSectored(boolean sectored) {
		Sectored = sectored;
		if (sectored) {
			lightColour[0] = Col.UNKNOWN;
		} else {
			setsectorIndex(0);
			setLightChar("");
			setLightColour(Col.UNKNOWN);
			setLightGroup("");
			setHeight("");
			setRange("");
			setBearing1("");
			setBearing2("");
			setRadius("");
		}
	}

	private int sectorIndex = 0;

	public int getsectorIndex() {
		return sectorIndex;
	}

	public void setsectorIndex(int sector) {
		sectorIndex = sector;
	}

	private String[] LightChar = new String[10];

	public String getLightChar() {
		if (LightChar[sectorIndex] == null)
			return (LightChar[0]);
		return LightChar[sectorIndex];
	}

	public void setLightChar(String lightChar) {
		if (sectorIndex == 0) {
			LightChar = new String[10];
			LightChar[0] = lightChar;
		} else if (LightChar[0].isEmpty())
			LightChar[sectorIndex] = lightChar;
	}

	private Col[] lightColour = new Col[10];

	public Col getLightColour() {
		if (lightColour[sectorIndex] == null)
			return (lightColour[0]);
		return lightColour[sectorIndex];
	}

	public void setLightColour(Col col) {
		lightColour[sectorIndex] = col;
	}

	private String[] LightGroup = new String[10];

	public String getLightGroup() {
		if (LightGroup[sectorIndex] == null)
			return (LightGroup[0]);
		return LightGroup[sectorIndex];
	}

	public void setLightGroup(String lightGroup) {
		if (sectorIndex == 0)
			LightGroup = new String[10];
		LightGroup[sectorIndex] = lightGroup;
	}

	protected void setLightGroup(Map<String, String> keys) {
		String s = "";
		if (keys.containsKey("seamark:light:group")) {
			s = keys.get("seamark:light:group");
			setLightGroup(s);
		}
	}

	private String[] Height = new String[10];

	public String getHeight() {
		if (Height[sectorIndex] == null)
			return (Height[0]);
		return Height[sectorIndex];
	}

	public void setHeight(String height) {
		if (sectorIndex == 0)
			Height = new String[10];
		Height[sectorIndex] = height;
	}

	private String[] Range = new String[10];

	public String getRange() {
		if (Range[sectorIndex] == null)
			return (Range[0]);
		return Range[sectorIndex];
	}

	public void setRange(String range) {
		if (sectorIndex == 0)
			Range = new String[10];
		Range[sectorIndex] = range;
	}

	private String[] Bearing1 = new String[10];

	public String getBearing1() {
		if (Bearing1[sectorIndex] == null)
			return (Bearing1[0]);
		return Bearing1[sectorIndex];
	}

	public void setBearing1(String bearing) {
		if (sectorIndex == 0)
			Bearing1 = new String[10];
		Bearing1[sectorIndex] = bearing;
	}

	private String[] Bearing2 = new String[10];

	public String getBearing2() {
		if (Bearing2[sectorIndex] == null)
			return (Bearing2[0]);
		return Bearing2[sectorIndex];
	}

	public void setBearing2(String bearing) {
		if (sectorIndex == 0)
			Bearing2 = new String[10];
		Bearing2[sectorIndex] = bearing;
	}

	private String[] Radius = new String[10];

	public String getRadius() {
		if (Radius[sectorIndex] == null)
			return (Radius[0]);
		return Radius[sectorIndex];
	}

	public void setRadius(String radius) {
		if (sectorIndex == 0)
			Radius = new String[10];
		Radius[sectorIndex] = radius;
	}

	private String[] LightPeriod = new String[10];

	public String getLightPeriod() {
		if (LightPeriod[sectorIndex] == null)
			return (LightPeriod[0]);
		return LightPeriod[sectorIndex];
	}

	public void setLightPeriod(String lightPeriod) {
		String regex = "^[\\d\\s.]+$";

		if (!lightPeriod.isEmpty()) {

			Pattern pat = Pattern.compile(regex);
			Matcher matcher = pat.matcher(lightPeriod);

			if (matcher.find()) {
				// setErrMsg(null);
			} else {
				// setErrMsg("Must be a number");
				lightPeriod = "";
				// dlg.tfM01RepeatTime.requestFocus();
			}
		}
		if (sectorIndex == 0)
			LightPeriod = new String[10];
		LightPeriod[sectorIndex] = lightPeriod;
	}

	public void parseMark() {

		String str;
		Map<String, String> keys;
		keys = dlg.node.getKeys();

		if (keys.containsKey("seamark:topmark:shape")) {
			str = keys.get("seamark:topmark:shape");
		}

		setFired(false);
		setSectored(false);
		Iterator it = keys.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String key = (String) entry.getKey();
			String value = ((String) entry.getValue()).trim();
			if (key.contains("seamark:light:")) {
				setFired(true);
				int index = 0;
				key = key.substring(14);
				if (key.matches("^\\d:.*")) {
					index = key.charAt(0) - '0';
					key = key.substring(2);
				} else if (key.matches("^\\d$")) {
					index = key.charAt(0) - '0';
					String values[] = value.split(":");
					if (values[0].equals("red"))
						lightColour[index] = Col.RED;
					else if (values[0].equals("green"))
						lightColour[index] = Col.GREEN;
					else if (values[0].equals("white"))
						lightColour[index] = Col.WHITE;
					if (values.length > 1)
						Bearing1[index] = values[1];
					if (values.length > 2)
						Bearing2[index] = values[2];
					if (values.length > 3)
						Radius[index] = values[3];
				} else {
					index = 0;
				}
				if (index != 0)
					setSectored(true);
				if (key.equals("colour")) {
					if (value.equals("red"))
						lightColour[index] = Col.RED;
					else if (value.equals("green"))
						lightColour[index] = Col.GREEN;
					else if (value.equals("white"))
						lightColour[index] = Col.WHITE;
				} else if (key.equals("character")) {
					LightChar[index] = value;
				} else if (key.equals("group")) {
					LightGroup[index] = value;
				} else if (key.equals("period")) {
					LightPeriod[index] = value;
				} else if (key.equals("height")) {
					Height[index] = value;
				} else if (key.equals("range")) {
					Range[index] = value;
				}
			}
		}

		setFog(false);
		setRadar(false);
		setRacon(false);
		if (keys.containsKey("seamark:fog_signal") || keys.containsKey("seamark:fog_signal:category")
				|| keys.containsKey("seamark:fog_signal:group") || keys.containsKey("seamark:fog_signal:period")) {
			setFog(true);
			if (keys.containsKey("seamark:fog_signal:category")) {
				str = keys.get("seamark:fog_signal:category");
				if (str.equals("horn"))
					setFogSound(Fog.HORN);
				else if (str.equals("siren"))
					setFogSound(Fog.SIREN);
				else if (str.equals("diaphone"))
					setFogSound(Fog.DIA);
				else if (str.equals("bell"))
					setFogSound(Fog.BELL);
				else if (str.equals("whis"))
					setFogSound(Fog.WHIS);
				else if (str.equals("gong"))
					setFogSound(Fog.GONG);
				else if (str.equals("explosive"))
					setFogSound(Fog.EXPLOS);
				else
					setFogSound(Fog.UNKNOWN);
			}
			if (keys.containsKey("seamark:fog_signal:group"))
				setFogGroup(keys.get("seamark:fog_signal:group"));
			if (keys.containsKey("seamark:fog_signal:period"))
				setFogPeriod(keys.get("seamark:fog_signal:period"));
		}

		if (keys.containsKey("seamark:radar_transponder") || keys.containsKey("seamark:radar_transponder:category")
				|| keys.containsKey("seamark:radar_transponder:group")) {
			setRacon(true);
			if (keys.containsKey("seamark:radar_transponder:category")) {
				str = keys.get("seamark:radar_transponder:category");
				if (str.equals("racon"))
					setRaType(Rtb.RACON);
				else if (str.equals("ramark"))
					setRaType(Rtb.RAMARK);
				else if (str.equals("leading"))
					setRaType(Rtb.LEADING);
				else
					setRaType(Rtb.UNKNOWN);
			}
			if (keys.containsKey("seamark:radar_transponder:group"))
				setRaconGroup(keys.get("seamark:radar_transponder:group"));
		} else if (keys.containsKey("seamark:radar_reflector"))
			setRadar(true);
	}

	public void paintSign() {

		String imgStr = "/images/";
		String shpStr = shapes.get(shape);
		if (shpStr == null)
			dlg.panelMain.topIcon.setIcon(null);
		else {
			imgStr += shpStr;
			if (shpStr.equals("perch")) {
				imgStr += (getCategory() == Cat.LAT_PORT ? "_port" : "_stbd");
			} else {
				String colStr = colours.get(bodyColour);
				if (colStr != null)
					imgStr += ("_" + colStr);
			}
			imgStr += ".png";
			dlg.panelMain.topIcon.setIcon(new ImageIcon(getClass().getResource(imgStr)));
		}

		if (hasRadar()) {
			dlg.panelMain.radarIcon.setIcon(new ImageIcon(getClass().getResource("/images/Radar_Reflector_355.png")));
		} else if (hasRacon()) {
			dlg.panelMain.radarIcon.setIcon(new ImageIcon(getClass().getResource("/images/Radar_Station.png")));
			// if (getRaType() != 0) {
			// String c = (String) dlg.cbM01Racon.getSelectedItem();
			// if ((getRaType() == RATYPE_RACON) && !getRaconGroup().isEmpty())
			// c += ("(" + getRaconGroup() + ")");
			// dlg.lM01RadarMarkeys.setText(c);
			// }
		}

		if (hasFog()) {
			dlg.panelMain.fogIcon.setIcon(new ImageIcon(getClass().getResource("/images/Fog_Signal.png")));
			// if (getFogSound() != 0) {
			// String c = (String) dlg.cbM01Fog.getSelectedItem();
			// if (!getFogGroup().isEmpty())
			// c += ("(" + getFogGroup() + ")");
			// if (!getFogPeriod().isEmpty())
			// c += (" " + getFogPeriod() + "s");
			// dlg.lM01FogMarkeys.setText(c);
			// }
		}

		if (isFired()) {
			String lp, c;
			String tmp = null;
			int i1;

			Col col = getColour(Ent.LIGHT);
			if (col == Col.WHITE) {
				dlg.panelMain.lightIcon.setIcon(new ImageIcon(getClass().getResource("/images/Light_White_120.png")));
			} else if (col == Col.RED) {
				dlg.panelMain.lightIcon.setIcon(new ImageIcon(getClass().getResource("/images/Light_Red_120.png")));
			} else if (col == Col.GREEN) {
				dlg.panelMain.lightIcon.setIcon(new ImageIcon(getClass().getResource("/images/Light_Green_120.png")));
			} else {
				dlg.panelMain.lightIcon.setIcon(new ImageIcon(getClass().getResource("/images/Light_Magenta_120.png")));
			}

			/*
			 * c = getLightChar(); if (c.contains("+")) { i1 = c.indexOf("+"); tmp =
			 * c.substring(i1, c.length()); c = c.substring(0, i1); if
			 * (!getLightGroup().isEmpty()) { c = c + "(" + getLightGroup() + ")"; }
			 * if (tmp != null) c = c + tmp; } else if (!getLightGroup().isEmpty()) c
			 * = c + "(" + getLightGroup() + ")"; c = c + " " + getLightColour(); lp =
			 * getLightPeriod(); if (!lp.isEmpty()) c = c + " " + lp + "s";
			 */}
	}

	public void saveSign() {
		Iterator<String> it = dlg.node.getKeys().keySet().iterator();
		String str;

		Main.pref.put("tomsplugin.IALA", getRegion() == Reg.A ? "A" : "B");

		while (it.hasNext()) {
			str = it.next();
			if (str.contains("seamark"))
				if (!str.equals("seamark")) {
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, str, null));
				}
		}
		if (!name.isEmpty())
			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:name", name));

		String objStr = objects.get(object);
		if (objStr != null) {
			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:type", objStr));

			str = categories.get(category);
			if (str != null)
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:" + objStr + ":category", str));

			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:" + objStr + ":shape", shapes.get(shape)));

			str = colours.get(bodyColour);
			if (str != null)
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:" + objStr + ":colour", str));

			switch (bodyColour) {
			case RED_GREEN_RED:
			case GREEN_RED_GREEN:
			case BLACK_YELLOW:
			case BLACK_YELLOW_BLACK:
			case YELLOW_BLACK:
			case YELLOW_BLACK_YELLOW:
			case BLACK_RED_BLACK:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:" + objStr + ":colour_pattern", "horizontal stripes"));
				break;
			case RED_WHITE:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:" + objStr + ":colour_pattern", "vertical stripes"));
				break;
			}
		}

		str = tops.get(topShape);
		if (str != null) {
			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:topmark:shape", str));

			str = colours.get(topColour);
			if (str != null)
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:topmark:colour", str));
		}

		str = days.get(dayShape);
		if (str != null) {
			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:daymark:shape", str));

			str = colours.get(dayColour);
			if (str != null)
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:daymark:colour", str));
		}

		Col colour;
		if (isFired()) {
			if ((colour = lightColour[0]) != Col.UNKNOWN)
				if (colour == Col.RED) {
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:colour", "red"));
				} else if (colour.equals("G")) {
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:colour", "green"));
				} else if (colour.equals("W")) {
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:colour", "white"));
				}
			if (!LightPeriod[0].isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:period", LightPeriod[0]));
			if (!LightChar[0].isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:character", LightChar[0]));
			if (!LightGroup[0].isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:group", LightGroup[0]));
			if (!Height[0].isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:height", Height[0]));
			if (!Range[0].isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:range", Range[0]));
			for (int i = 1; i < 10; i++) {
				if ((colour = lightColour[i]) != Col.UNKNOWN)
					if (colour.equals("R")) {
						Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":colour", "red"));
						if ((Bearing1[i] != null) && (Bearing2[i] != null) && (Radius[i] != null))
							Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i, "red:" + Bearing1[i] + ":"
									+ Bearing2[i] + ":" + Radius[i]));
					} else if (colour.equals("G")) {
						Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":colour", "green"));
						if ((Bearing1[i] != null) && (Bearing2[i] != null) && (Radius[i] != null))
							Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i, "green:" + Bearing1[i] + ":"
									+ Bearing2[i] + ":" + Radius[i]));
					} else if (colour.equals("W")) {
						Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":colour", "white"));
						if ((Bearing1[i] != null) && (Bearing2[i] != null) && (Radius[i] != null))
							Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i, "white:" + Bearing1[i] + ":"
									+ Bearing2[i] + ":" + Radius[i]));
					}
				if (LightPeriod[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":period", LightPeriod[i]));
				if (LightChar[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":character", LightChar[i]));
				if (LightGroup[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":group", LightGroup[i]));
				if (Height[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":height", Height[i]));
				if (Range[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":range", Range[i]));
				if (Bearing1[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":sector_start", Bearing1[i]));
				if (Bearing2[i] != null)
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:light:" + i + ":sector_end", Bearing2[i]));
			}
		}
		if (hasRadar()) {
			Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_reflector", "yes"));
		}
		if (hasRacon()) {
			switch (RaType) {
			case RACON:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_transponder:category", "racon"));
				if (!getRaconGroup().isEmpty())
					Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_transponder:group", getRaconGroup()));
				break;
			case RAMARK:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_transponder:category", "ramark"));
				break;
			case LEADING:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_transponder:category", "leading"));
				break;
			default:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:radar_transponder", "yes"));
			}
		}
		if (hasFog()) {
			switch (getFogSound()) {
			case HORN:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "horn"));
				break;
			case SIREN:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "siren"));
				break;
			case DIA:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "diaphone"));
				break;
			case BELL:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "bell"));
				break;
			case WHIS:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "whistle"));
				break;
			case GONG:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "gong"));
				break;
			case EXPLOS:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:category", "explosive"));
				break;
			default:
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal", "yes"));
			}
			if (!getFogGroup().isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:group", getFogGroup()));
			if (!getFogPeriod().isEmpty())
				Main.main.undoRedo.add(new ChangePropertyCommand(dlg.node, "seamark:fog_signal:period", getFogPeriod()));
		}
	}

}
