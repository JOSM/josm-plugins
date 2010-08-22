package org.openstreetmap.josm.plugins.remotecontrol;

/**
 * This class should replace PermissionPref because it allows explicit 
 * specification of the permission's default value.
 * 
 * @author Bodo Meissner
 */
@SuppressWarnings("deprecation")
public class PermissionPrefWithDefault extends PermissionPref {

	boolean defaultVal = true;

	public PermissionPrefWithDefault(String pref, boolean defaultVal, String message) {
		super(pref, message);
		this.defaultVal = defaultVal;
	}

	public PermissionPrefWithDefault(PermissionPref prefWithoutDefault) {
		super(prefWithoutDefault.pref, prefWithoutDefault.message);
	}
}
