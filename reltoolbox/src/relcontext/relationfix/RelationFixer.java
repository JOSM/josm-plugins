package relcontext.relationfix;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Relation;

public abstract class RelationFixer {
	
	private List<String> applicableTypes;
	
	{
		applicableTypes = new ArrayList<String>();
	}
	/**
	 * Construct new RelationFixer by only one applicable relation type 
	 * @param type
	 */
	public RelationFixer(String type) {
		applicableTypes.add(type);
	}
	
	/**
	 * Construct new RelationFixer by an array of applicable types 
	 * @param types
	 */
	public RelationFixer(String[] types) {
		for(String type: types) {
			applicableTypes.add(type);
		}
	}
	
	/**
	 * Check if given relation is of needed type. You may override this method to check first type
	 * and then check desired relation properties.
	 * Note that this only verifies if current RelationFixer can be used to check and fix given relation
	 * Deeper relation checking is at {@link isRelationGood}  
	 * 
	 * @param rel Relation to check
	 * @return true if relation can be verified by current RelationFixer
	 */
	public boolean isFixerApplicable(Relation rel) {
		if (rel == null)
			return false;
		if (!rel.hasKey("type"))
			return false;
		
		String type = rel.get("type");
		for(String oktype: applicableTypes)
			if (oktype.equals(type)) 
				return true;
		
		return false;
	}
	
	/**
	 * Check if given relation is OK. That means if all roles are given properly, all tags exist as expected etc.
	 * Should be written in children classes.
	 * 
	 * @param rel Relation to verify
	 * @return true if given relation is OK
	 */
	public abstract boolean isRelationGood(Relation rel);
	
	/**
	 * Fix relation and return new relation with fixed tags, roles etc.
	 * Note that is not obligatory to return true for isRelationGood for new relation
	 * 
	 * @param rel Relation to fix
	 * @return command that fixes the relation {@code null} if it cannot be fixed or is already OK
	 */
	public abstract Command fixRelation(Relation rel);
}
