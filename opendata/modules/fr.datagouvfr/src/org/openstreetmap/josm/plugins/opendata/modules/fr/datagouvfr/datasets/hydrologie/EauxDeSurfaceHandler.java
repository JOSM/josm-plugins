// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.opendata.modules.fr.datagouvfr.datasets.hydrologie;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.plugins.opendata.core.io.archive.DefaultArchiveHandler;
import org.openstreetmap.josm.plugins.opendata.modules.fr.datagouvfr.datasets.DataGouvDataSetHandler;
import org.openstreetmap.josm.tools.Pair;

public class EauxDeSurfaceHandler extends DataGouvDataSetHandler {

	private static final String ZIP_PATTERN = "FR(.*)_SW";
	private static final String SHP_PATTERN = "FR_(.*)_SWB_.W_20......";
	
	private static final class WaterAgency {
		public final String code;
		public final String name;
		public final String suffix;
		public WaterAgency(String code, String name, String suffix) {
			this.code = code;
			this.name = name;
			this.suffix = suffix;
		}
	}
	
	private static final WaterAgency[] waterAgencies = new WaterAgency[]{
		new WaterAgency("A",  "Escaut Somme", "Escaut-Somme-30381967"),
		new WaterAgency("B1", "Meuse", "Meuse-30381855"),
		new WaterAgency("B2", "Sambre", "Sambre-30381857"),
		new WaterAgency("C",  "Rhin", "Rhin-30381951"),
		new WaterAgency("D",  "Rhône Méditerranée", "Rhône-Méditerranée-30382014"),
		new WaterAgency("E",  "Corse", "Corse-30381905"),
		new WaterAgency("F",  "Adour Garonne", "Adour-Garonne-30381839"),
		new WaterAgency("G",  "Loire Bretagne", "Loire-Bretagne-30381904"),
		new WaterAgency("I",  "Guadeloupe", "Guadeloupe-30381899"),
		new WaterAgency("J",  "Martinique", "Martinique-30381935"),
		new WaterAgency("K",  "Guyane", "Guyane-30381988"),
		new WaterAgency("L",  "La Réunion", "Réunion-30381991"),
	};
	
	public EauxDeSurfaceHandler() {
		setName("Eaux de surface");
		setArchiveHandler(new InternalZipHandler());
	}
	
	@Override
	public boolean acceptsFilename(String filename) {
		boolean result = acceptsZipFilename(filename, ZIP_PATTERN) || acceptsShpFilename(filename, SHP_PATTERN);
		if (result) {
			setNationalPortalPath(findPortalSuffix(filename));
		}
		return result;
	}
	
	@Override
	public boolean acceptsUrl(String url) {
		boolean result = super.acceptsUrl(url);
		if (result) {
			setNationalPortalPath(findPortalSuffix(url));
		}
		return result;
	}

	private String findPortalSuffix(String filename) {
		for (String pattern : new String[]{ZIP_PATTERN, SHP_PATTERN}) {
			Matcher m = Pattern.compile(".*"+pattern+"\\....").matcher(filename);
			if (m.matches()) {
				for (int i =0; i<waterAgencies.length; i++) {
					if (waterAgencies[i].code.equals(m.group(1))) {
						return "Couche-SIG-des-caractéristiques-des-bassins-2010-%3A-eaux-de-surface---"+waterAgencies[i].suffix;
					}
				}
			}
		}
		return null;
	}

	@Override
	public void updateDataSet(DataSet ds) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public List<Pair<String, URL>> getDataURLs() {
		List<Pair<String, URL>> result = new ArrayList<>();
		try {
			for (WaterAgency wa : waterAgencies) {
				result.add(getDownloadURL(wa));
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return result;
	}

	private Pair<String, URL> getDownloadURL(WaterAgency a) throws MalformedURLException {
		return new Pair<>("SurfaceWater_"+a.name, new URL("http://www.rapportage.eaufrance.fr/sites/default/files/SIG/FR"+a.code+"_SW.zip"));
	}
	
	private class InternalZipHandler extends DefaultArchiveHandler {
		@Override
		public void notifyTempFileWritten(File file) {
			if (file.getName().matches(SHP_PATTERN.replace("(.*)", "F")+"\\.prj")) { // Adour-Garonne .prj files cannot be parsed because they do not contain quotes... 
				try {
					BufferedReader reader = new BufferedReader(new FileReader(file));
					String line = reader.readLine();
					reader.close();
					if (!line.contains("\"")) {
						for (String term : new String[]{"GCS_ETRS_1989", "D_ETRS_1989", "GRS_1980", "Greenwich", "Degree"}) {
							line = line.replace(term, "\""+term+"\"");
						}
						BufferedWriter writer = new BufferedWriter(new FileWriter(file));
						writer.write(line);
						writer.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
