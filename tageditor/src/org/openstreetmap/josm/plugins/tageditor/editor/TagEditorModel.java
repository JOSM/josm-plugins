// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.tageditor.editor;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.plugins.tageditor.preset.Item;
import org.openstreetmap.josm.plugins.tageditor.preset.Tag;
import org.openstreetmap.josm.plugins.tageditor.tagspec.KeyValuePair;
import static org.openstreetmap.josm.plugins.tageditor.josm.CompatibilityUtil.getCurrentDataSet;

/**
 * TagEditorModel is a table model.
 * 
 * 
 * @author gubaer
 *
 */
@SuppressWarnings("serial")
public class TagEditorModel extends AbstractTableModel {
	static private final Logger logger = Logger.getLogger(TagEditorModel.class.getName());

	static public final String PROP_DIRTY = TagEditorModel.class.getName() + ".dirty";

	/** the list holding the tags */
	private  ArrayList<TagModel> tags = null;
	private  ArrayList<Item> items = null;

	/** indicates whether the model is dirty */
	private boolean dirty =  false;
	private PropertyChangeSupport propChangeSupport = null;

	private DefaultComboBoxModel appliedPresets = null;


	/**
	 * constructor
	 */
	public TagEditorModel(){
		tags = new ArrayList<TagModel>();
		items = new ArrayList<Item>();
		propChangeSupport = new PropertyChangeSupport(this);
		appliedPresets = new DefaultComboBoxModel();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.addPropertyChangeListener(listener);
	}

	public void removeProperyChangeListener(PropertyChangeListener listener) {
		propChangeSupport.removePropertyChangeListener(listener);
	}

	protected void fireDirtyStateChanged(final boolean oldValue, final boolean newValue) {
		propChangeSupport.firePropertyChange(PROP_DIRTY, oldValue, newValue);
	}

	protected void setDirty(boolean newValue) {
		boolean oldValue = dirty;
		dirty = newValue;
		if (oldValue != newValue) {
			fireDirtyStateChanged(oldValue, newValue);
		}
	}

	public int getColumnCount() {
		return 2;
	}

	public int getRowCount() {
		return tags.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		if (rowIndex >= getRowCount())
			throw new IndexOutOfBoundsException("unexpected rowIndex: rowIndex=" + rowIndex);

		TagModel tag = tags.get(rowIndex);
		switch(columnIndex) {
		case 0:
		case 1: return tag;

		default:
			throw new IndexOutOfBoundsException("unexpected columnIndex: columnIndex=" + columnIndex);
		}
	}


	/**
	 * removes all tags in the model
	 */
	public void clear() {
		tags.clear();
		items.clear();
		setDirty(true);
		fireTableDataChanged();
	}

	/**
	 * adds a tag to the model
	 * 
	 * @param tag the tag. Must not be null.
	 * 
	 * @exception IllegalArgumentException thrown, if tag is null
	 */
	public void add(TagModel tag) {
		if (tag == null)
			throw new IllegalArgumentException("argument 'tag' must not be null");
		tags.add(tag);
		setDirty(true);
		fireTableDataChanged();
	}


	public void prepend(TagModel tag) {
		if (tag == null)
			throw new IllegalArgumentException("argument 'tag' must not be null");
		tags.add(0, tag);
		setDirty(true);
		fireTableDataChanged();
	}


	/**
	 * adds a tag given by a name/value pair to the tag editor model.
	 * 
	 * If there is no tag with name <code>name</name> yet, a new {@link TagModel} is created
	 * and append to this model.
	 * 
	 * If there is a tag with name <code>name</name>, <code>value</code> is merged to the list
	 * of values for this tag.
	 * 
	 * @param name the name; converted to "" if null
	 * @param value the value; converted to "" if null
	 */
	public void add(String name, String value) {
		name = (name == null) ? "" : name;
		value = (value == null) ? "" : value;

		TagModel tag = get(name);
		if (tag == null) {
			tag = new TagModel(name, value);
			add(tag);
		} else {
			tag.addValue(value);
		}
		setDirty(true);
	}


	/**
	 * replies the tag with name <code>name</code>; null, if no such tag exists
	 * @param name the tag name
	 * @return the tag with name <code>name</code>; null, if no such tag exists
	 */
	public TagModel get(String name) {
		name = (name == null) ? "" : name;
		for (TagModel tag : tags) {
			if (tag.getName().equals(name))
				return tag;
		}
		return null;
	}

	public TagModel get(int idx) {
		TagModel tagModel = tags.get(idx);
		return tagModel;
	}



	@Override public boolean isCellEditable(int row, int col) {
		// all cells are editable
		return true;
	}


	/**
	 * deletes the names of the tags given by tagIndices
	 * 
	 * @param tagIndices a list of tag indices
	 */
	public void deleteTagNames(int [] tagIndices) {
		if (tags == null)
			return;
		for (int tagIdx : tagIndices) {
			TagModel tag = tags.get(tagIdx);
			if (tag != null) {
				tag.setName("");
			}
		}
		fireTableDataChanged();
		setDirty(true);
	}

	/**
	 * deletes the values of the tags given by tagIndices
	 * 
	 * @param tagIndices the lit of tag indices
	 */
	public void deleteTagValues(int [] tagIndices) {
		if (tags == null)
			return;
		for (int tagIdx : tagIndices) {
			TagModel tag = tags.get(tagIdx);
			if (tag != null) {
				tag.setValue("");
			}
		}
		fireTableDataChanged();
		setDirty(true);
	}

	/**
	 * deletes the tags given by tagIndices
	 * 
	 * @param tagIndices the list of tag indices
	 */
	public void deleteTags(int [] tagIndices) {
		if (tags == null)
			return;
		ArrayList<TagModel> toDelete = new ArrayList<TagModel>();
		for (int tagIdx : tagIndices) {
			TagModel tag = tags.get(tagIdx);
			if (tag != null) {
				toDelete.add(tag);
			}
		}
		for (TagModel tag : toDelete) {
			tags.remove(tag);
		}
		fireTableDataChanged();
		setDirty(true);
	}


	/**
	 * creates a new tag and appends it to the model
	 */
	public void appendNewTag() {
		TagModel tag = new TagModel();
		tags.add(tag);
		fireTableDataChanged();
		setDirty(true);
	}

	/**
	 * makes sure the model includes at least one (empty) tag
	 */
	public void ensureOneTag() {
		if (tags.size() == 0) {
			appendNewTag();
		}
	}

	/**
	 * initializes the model with the tags in the current JOSM selection
	 */
	public void initFromJOSMSelection() {
		Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
		clear();
		for (OsmPrimitive element : selection) {
			for (String key : element.keySet()) {
				String value = element.get(key);
				add(key,value);
			}
		}
		sort();
		setDirty(false);
	}


	/**
	 * checks whether the tag model includes a tag with a given key
	 * 
	 * @param key  the key
	 * @return true, if the tag model includes the tag; false, otherwise
	 */
	public boolean includesTag(String key) {
		if (key == null) return false;
		for (TagModel tag : tags) {
			if (tag.getName().equals(key))
				return true;
		}
		return false;
	}


	protected Command createUpdateTagCommand(Collection<OsmPrimitive> primitives, TagModel tag) {

		// tag still holds an unchanged list of different values for the same key.
		// no property change command required
		if (tag.getValueCount() > 1)
			return null;

		// tag name holds an empty key. Don't apply it to the selection.
		//
		if (tag.getName().trim().equals(""))
			return null;

		String newkey = tag.getName();
		String newvalue = tag.getValue();

		ChangePropertyCommand command = new ChangePropertyCommand(primitives,newkey, newvalue);
		return command;
	}

	protected Command createDeleteTagsCommand(Collection<OsmPrimitive> primitives) {

		List<String> currentkeys = getKeys();
		ArrayList<Command> commands = new ArrayList<Command>();

		for (OsmPrimitive primitive : primitives) {
			if (primitive.keys == null) {
				continue;
			}
			for (String oldkey : primitive.keys.keySet()) {
				if (!currentkeys.contains(oldkey)) {
					ChangePropertyCommand deleteCommand =
						new ChangePropertyCommand(primitive,oldkey,null);
					commands.add(deleteCommand);
				}
			}
		}

		SequenceCommand command = new SequenceCommand(
				trn("Remove old keys from up to {0} object", "Remove old keys from up to {0} objects", primitives.size(), primitives.size()),
				commands
		);

		return command;
	}

	/**
	 * updates the tags of the primitives in the current selection with the
	 * values in the current tag model
	 * 
	 */
	public void updateJOSMSelection() {
		ArrayList<Command> commands = new ArrayList<Command>();
		Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
		if (selection == null)
			return;
		for (TagModel tag : tags) {
			Command command = createUpdateTagCommand(selection,tag);
			if (command != null) {
				commands.add(command);
			}
		}
		Command deleteCommand = createDeleteTagsCommand(selection);
		if (deleteCommand != null) {
			commands.add(deleteCommand);
		}

		SequenceCommand command = new SequenceCommand(
				trn("Updating properties of up to {0} object", "Updating properties of up to {0} objects", selection.size(), selection.size()),
				commands
		);

		// executes the commands and adds them to the undo/redo chains
		Main.main.undoRedo.add(command);
	}


	/**
	 * replies the list of keys of the tags managed by this model
	 * 
	 * @return the list of keys managed by this model
	 */
	public List<String> getKeys() {
		ArrayList<String> keys = new ArrayList<String>();
		for (TagModel tag: tags) {
			if (!tag.getName().trim().equals("")) {
				keys.add(tag.getName());
			}
		}
		return keys;
	}

	/**
	 * sorts the current tags according alphabetical order of names
	 */
	protected void sort() {
		java.util.Collections.sort(
				tags,
				new Comparator<TagModel>() {
					public int compare(TagModel self, TagModel other) {
						return self.getName().compareTo(other.getName());
					}
				}
		);
	}




	/**
	 * applies the tags defined for a preset item to the tag model.
	 * 
	 * Mandatory tags are added to the list of currently edited tags.
	 * Optional tags are not added.
	 * The model remembers the currently applied presets.
	 * 
	 * @param item  the preset item. Must not be null.
	 * @exception IllegalArgumentException thrown, if item is null
	 * 
	 */
	public void applyPreset(Item item) {
		if (item == null)
			throw new IllegalArgumentException("argument 'item' must not be null");
		// check whether item is already applied
		//
		for(int i=0; i < appliedPresets.getSize(); i++) {
			if (appliedPresets.getElementAt(i).equals(item))
				// abort - preset already applied
				return;
		}

		// apply the tags proposed by the preset
		//
		for(Tag tag : item.getTags()) {
			if (!tag.isOptional()) {
				if (!includesTag(tag.getKey())) {
					TagModel tagModel = new TagModel(tag.getKey(),tag.getValue());
					prepend(tagModel);
				} else {
					TagModel tagModel = get(tag.getKey());
					// only overwrite an existing value if the preset
					// proposes a value. I.e. don't overwrite
					// existing values for tag 'name' with an empty string
					//
					if (tag.getValue() != null) {
						tagModel.setValue(tag.getValue());
					}
				}
			}
		}

		// remember the preset and make it the current preset
		//
		appliedPresets.addElement(item);
		appliedPresets.setSelectedItem(item);
		fireTableDataChanged();
	}


	/**
	 * applies a tag given by a {@see KeyValuePair} to the model
	 * 
	 * @param pair the key value pair
	 */
	public void applyKeyValuePair(KeyValuePair pair) {
		TagModel tagModel = get(pair.getKey());
		if (tagModel == null) {
			tagModel = new TagModel(pair.getKey(), pair.getValue());
			prepend(tagModel);
		} else {
			tagModel.setValue(pair.getValue());
		}
		fireTableDataChanged();
	}


	public DefaultComboBoxModel getAppliedPresetsModel() {
		return appliedPresets;
	}

	public void removeAppliedPreset(Item item) {
		if (item == null)
			return;
		for (Tag tag: item.getTags()) {
			if (tag.getValue() != null) {
				// preset tag with explicit key and explicit value. Remove tag model
				// from the current model if both the key and the value match
				//
				TagModel tagModel = get(tag.getKey());
				if (tagModel !=null && tag.getValue().equals(tagModel.getValue())) {
					tags.remove(tagModel);
					setDirty(true);
				}
			} else {
				// preset tag with required key. No explicit value given. Remove tag
				// model with the respective key
				//
				TagModel tagModel = get(tag.getKey());
				if (tagModel != null) {
					tags.remove(tagModel);
					setDirty(true);
				}
			}
		}
		appliedPresets.removeElement(item);
		fireTableDataChanged();
	}

	public void clearAppliedPresets() {
		appliedPresets.removeAllElements();
		fireTableDataChanged();
	}

	public void highlightCurrentPreset() {
		fireTableDataChanged();
	}


	/**
	 * updates the name of a tag and sets the dirty state to  true if
	 * the new name is different from the old name.
	 * 
	 * @param tag   the tag
	 * @param newName  the new name
	 */
	public void updateTagName(TagModel tag, String newName) {
		String oldName = tag.getName();
		tag.setName(newName);
		if (! newName.equals(oldName)) {
			setDirty(true);
		}
	}

	/**
	 * updates the value value of a tag and sets the dirty state to true if the
	 * new name is different from the old name
	 * 
	 * @param tag  the tag
	 * @param newValue  the new value
	 */
	public void updateTagValue(TagModel tag, String newValue) {
		String oldValue = tag.getValue();
		tag.setValue(newValue);
		if (! newValue.equals(oldValue)) {
			setDirty(true);
		}
	}
}
