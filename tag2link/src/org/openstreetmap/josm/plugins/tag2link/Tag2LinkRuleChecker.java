//    JOSM tag2link plugin.
//    Copyright (C) 2011 Don-vip & FrViPofm
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package org.openstreetmap.josm.plugins.tag2link;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.plugins.tag2link.data.Link;
import org.openstreetmap.josm.plugins.tag2link.data.Rule;
import org.openstreetmap.josm.plugins.tag2link.data.Rule.EvalResult;
import org.openstreetmap.josm.plugins.tag2link.data.Rule.MatchingTag;
import org.openstreetmap.josm.plugins.tag2link.data.Source;
import org.openstreetmap.josm.plugins.tag2link.io.SourcesReader;

public class Tag2LinkRuleChecker implements Tag2LinkConstants {

    private static Collection<Source> sources = new ArrayList<Source>();
    
    private static boolean initialized = false;
        
    public static void init() {
        if (!initialized) {
            sources = SourcesReader.readSources();
            initialized = true;
        }
    }
    
    private static String findValue(String arg, Collection<MatchingTag> matchingTags) {
		for (MatchingTag tag : matchingTags) {
			if (tag.params.containsKey(arg)) {
				return tag.params.get(arg);
			}
		}
		return null;
    }
    
    private static Collection<Link> processEval(EvalResult eval, Rule rule, Source source) {
    	Collection<Link> result = new ArrayList<Link>();
        if (eval.matches()) {
            for (Link link : rule.links) {
            	Link copy = new Link(link);
            	copy.name = copy.name.replaceAll("%name%", source.name);
				Matcher m = Pattern.compile("%([^%]*)%").matcher(copy.url);
				while (m.find()) {
					String arg = m.group(1);
					String val = findValue(arg, eval.matchingTags);
					if (val == null && arg.contains(":")) {
						String[] vars = arg.split(":");
						for (int i = 0; val == null && i < vars.length-1; i++) {
							val = findValue(vars[i], eval.matchingTags);
						}
						if (val == null) {
							// Default value
							val = vars[vars.length-1];
						}
					}
					if (val == null) {
						Matcher lm = Pattern.compile("lang(?:\\(\\p{Lower}{2,}(?:,\\p{Lower}{2,})*\\))?(?::(\\p{Lower}{2,}))?").matcher(arg);
						if (lm.matches()) {
							if (lm.groupCount() == 0) {
								// TODO: get JOSM current language
							} else {
								// TODO: parse next groups
							}
						}
					}
					if (val != null) {
						try {
							// Special hack for Wikipedia that prevents spaces being replaced by "+" characters, but by "_"
							if (copy.url.contains("wikipedia.")) {
								val = val.replaceAll(" ", "_");
							}
							// Encode param to be included in the URL, except if it is the URL itself !
							if (!m.group().equals(copy.url)) {
								val = URLEncoder.encode(val, UTF8_ENCODING);
							}
							// Finally replace parameter
							copy.url = copy.url.replaceFirst(m.group(), val);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("Invalid argument: "+arg);
					}
				}
            	result.add(copy);
            }
        }
        return result;
    }
    
    public static Collection<Link> getLinks(IPrimitive p) {
        Collection<Link> result = new ArrayList<Link>();
        for (Source source : sources) {
            for (Rule rule : source.rules) {
                result.addAll(processEval(rule.evaluates(p), rule, source));
            }
        }
        return result;
    }

	public static Collection<Link> getLinks(Tag tag) {
		Collection<Link> result = new ArrayList<Link>();
        for (Source source : sources) {
            for (Rule rule : source.rules) {
                result.addAll(processEval(rule.evaluates(tag), rule, source));
            }
        }
		return result;
	}
}
