/* Copyright (c) 2008, Henrik Niehaus
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of the project nor the names of its 
 *    contributors may be used to endorse or promote products derived from this 
 *    software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.openstreetmap.josm.plugins.osb.api;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.plugins.osb.ConfigKeys;
import org.openstreetmap.josm.plugins.osb.api.util.HttpUtils;

public class DownloadAction {
	
	private final String CHARSET = "UTF-8";
	
	public void execute(DataSet dataset, Bounds bounds) throws IOException {
		// create the URI for the data download
		String uri = Main.pref.get(ConfigKeys.OSB_API_URI_DOWNLOAD);

		// check zoom level
		if(Main.map.mapView.zoom() > 15 || Main.map.mapView.zoom() < 9) {
			return;
		} 
		
		// add query params to the uri
		StringBuilder sb = new StringBuilder(uri)
			.append("?b=").append(bounds.min.lat())
			.append("&t=").append(bounds.max.lat())
			.append("&l=").append(bounds.min.lon())
			.append("&r=").append(bounds.max.lon());
		uri = sb.toString();

		// download the data
		String content = HttpUtils.get(uri, null, CHARSET);
		
		// clear dataset
		dataset.nodes.clear();
		dataset.relations.clear();
		dataset.ways.clear();
		
		// parse the data
		parseData(dataset, content);
	}

	private void parseData(DataSet dataSet, String content) {
		String idPattern = "\\d+";
		String floatPattern = "\\d+\\.\\d+";
		String pattern = "putAJAXMarker\\(("+idPattern+"),("+floatPattern+"),("+floatPattern+"),\"(.*)\",([01])\\)";
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(content);
		while(m.find()) {
			double lat = Double.parseDouble(m.group(3));
			double lon = Double.parseDouble(m.group(2));
			LatLon latlon = new LatLon(lat, lon); 
			Node osmNode = new Node(latlon);
			osmNode.id = Long.parseLong(m.group(1));
			osmNode.put("id", m.group(1));
			osmNode.put("note", m.group(4));
			osmNode.put("openstreetbug", "FIXME");
			osmNode.put("state", m.group(5));
			dataSet.addPrimitive(osmNode);
		} 
	}
}
