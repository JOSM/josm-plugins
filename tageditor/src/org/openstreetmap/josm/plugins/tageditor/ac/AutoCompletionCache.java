package org.openstreetmap.josm.plugins.tageditor.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * AutoCompletionCache temporarily holds a cache of keys with a list of
 * possible auto completion values for each key.
 * 
 * The cache can initialize itself from the current JOSM data set such that
 * <ol>
 *   <li>any key used in a tag in the data set is part of the key list in the cache</li>
 *   <li>any value used in a tag for a specific key is part of the autocompletion list of
 *     this key</li>
 * </ol>  
 * 
 * Building up auto completion lists should not
 * slow down tabbing from input field to input field. Looping through the complete
 * data set in order to build up the auto completion list for a specific input
 * field is not efficient enough, hence this cache.   
 *
 */
public class AutoCompletionCache {
	
	/** the cache */
	private HashMap<String, ArrayList<String>> cache;
	
	/**
	 * constructor 
	 */
	public AutoCompletionCache() {
		cache = new HashMap<String, ArrayList<String>>();
	}

	/**
	 * make sure, <code>key</code> is in the cache 
	 * 
	 * @param key  the key 
	 */
	protected void cacheKey(String key) {
		if (cache.containsKey(key)) {
			return;
		} else {
			cache.put(key, new ArrayList<String>());
		}
	}
	
	/**
	 * make sure, value is one of the auto completion values allowed for key 
	 * 
	 * @param key the key 
	 * @param value the value 
	 */
	protected void cacheValue(String key, String value) {
		cacheKey(key);
		ArrayList<String> values = cache.get(key);
		if (!values.contains(value)) {
			values.add(value);
		}
	}
	
	/**
	 * make sure, the keys and values of all tags held by primitive are
	 * in the auto completion cache 
	 *  
	 * @param primitive an OSM primitive 
	 */
	protected void cachePrimitive(OsmPrimitive primitive) {
		for (String key: primitive.keySet()) {
			String value = primitive.get(key);
			cacheValue(key, value);
		}
	}
	
	/**
	 * initializes the cache from the current JOSM dataset {@link OsmPrimitive}s.
	 * 
	 */
	public void initFromJOSMDataset() {
		cache = new HashMap<String, ArrayList<String>>();
		Collection<OsmPrimitive> ds = Main.ds.allNonDeletedPrimitives();
		for (OsmPrimitive primitive : ds) {
			cachePrimitive(primitive);
		}
	}
	
	/**
	 * replies the keys held by the cache
	 *  
	 * @return the list of keys held by the cache 
	 */
	public List<String> getKeys() {
		return new ArrayList<String>(cache.keySet());
	}
	
	
	/**
	 * replies the auto completion values allowed for a specific key. Replies
	 * an empty list if key is null or if key is not in {@link #getKeys()}.
	 * 
	 * @param key 
	 * @return the list of auto completion values  
	 */
	public List<String> getValues(String key) {
		if (!cache.containsKey(key)) {
			return new ArrayList<String>();
		} else {
			return cache.get(key);
		}
	}
}
