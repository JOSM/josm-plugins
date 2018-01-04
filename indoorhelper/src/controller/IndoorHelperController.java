/*
 * Indoorhelper is a JOSM plug-in to support users when creating their own indoor maps.
 *  Copyright (C) 2016  Erik Gruschka
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package controller;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ValidateAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.mapmode.SelectAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.tools.Shortcut;

import model.IndoorHelperModel;
import model.TagCatalog.IndoorObject;
import views.LevelSelectorView;
import views.ToolBoxView;

/**
*
* Class for the Controller which provides the communication between
* the IndoorHelperModel and the different views.
*
* @author egru
* @author rebsc
*
*/
public class IndoorHelperController {

   private IndoorHelperModel model;
   private ToolBoxView toolboxView;
   private String sep;
   private String levelValue, levelNum;
   private MapFrame map;
   private DrawAction drawAction;
   private SelectAction selectAction;
   private SpaceAction SpaceAction;
   private transient Shortcut SpaceShortcut;
   private EnterAction EnterAction;
   private transient Shortcut EnterShortcut;
   private boolean OuterHelp,InnerHelp,LevelHelp;
   private Collection<OsmPrimitive> innerRelation;
   private LevelSelectorView selectorView;

   /**
    * Constructor for the {@link IndoorHelperController} which initiates model and views.
    *
    */
   public IndoorHelperController() {

	   this.model = new IndoorHelperModel();
       this.toolboxView = new ToolBoxView();

       this.sep = System.getProperty("file.separator");
       setPluginPreferences(true);

       // Multipolygon actions
       this.drawAction = new DrawAction();
       this.map = MainApplication.getMap();
       this.selectAction = new SelectAction(map);

       // Ui elements
       toolboxView.setAllUiElementsEnabled(true);
       toolboxView.setROUiElementsEnabled(false);

       addToolboxListeners();
       MainApplication.getMap().addToggleDialog(toolboxView);

       // Shortcuts
       SpaceShortcut = Shortcut.registerShortcut("mapmode:space",
               tr(""), KeyEvent.VK_SPACE, Shortcut.DIRECT);
       this.SpaceAction = new SpaceAction();
       MainApplication.registerActionShortcut(SpaceAction,SpaceShortcut);

       EnterShortcut = Shortcut.registerShortcut("mapmode:ALT",
               tr(""), KeyEvent.VK_ENTER, Shortcut.DIRECT);
       this.EnterAction = new EnterAction();
       MainApplication.registerActionShortcut(EnterAction,EnterShortcut);

       // Helper
       OuterHelp = false;
       InnerHelp = false;
       LevelHelp = false;
	   innerRelation = null;
	   levelValue = new String();
	   levelNum = new String();

   }


    /**
     * Adds the button- and box-listeners to the {@link ToolBoxView}.
     */
    private void addToolboxListeners() {

        if (this.toolboxView != null) {
            this.toolboxView.setApplyButtonListener(new ToolApplyButtonListener());
            this.toolboxView.setLevelCheckBoxListener(new ToolLevelCheckBoxListener());
            this.toolboxView.setHelpButtonListener(new ToolHelpButtonListener());
            this.toolboxView.setAddLevelButtonListener(new ToolAddLevelButtonListener());
            this.toolboxView.setObjectItemListener(new ToolObjectItemListener());
            this.toolboxView.setOuterButtonListener(new ToolOuterButtonListener());
            this.toolboxView.setInnerButtonListener(new ToolInnerButtonListener());
            this.toolboxView.setMultiCheckBoxListener(new ToolMultiCheckBoxListener());
            this.toolboxView.setPreset1Listener(new Preset1Listener());
            this.toolboxView.setPreset2Listener(new Preset2Listener());
            this.toolboxView.setPreset3Listener(new Preset3Listener());
            this.toolboxView.setPreset4Listener(new Preset4Listener());
        }
    }

    /**
     * Adds the button-listeners to the {@link LevelSelectorView}.
     */
    private void addLevelSelectorListeners() {
        if (this.selectorView != null) {
            this.selectorView.setOkButtonListener(new ToolLevelOkButtonListener());
            this.selectorView.setCancelButtonListener(new ToolLevelCancelButtonListener());
            this.selectorView.setSelectorWindowListener(new ToolSelectorWindowSListener());
        }

    }

/*************************************************
* TOOLBOX LISTENER
*
*/

   /**
    * The listener which provides the handling of the apply button.
    * Gets the texts which were written by the user and writes them to the OSM-data.
    * After that it checks the tagged data  with the built-in validator file.
    *
    * @author egru
    * @author rebsc
    */
   class ToolApplyButtonListener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {

           IndoorObject indoorObject = toolboxView.getSelectedObject();

        	   // collecting all tags
               List<Tag> tags = new ArrayList<>();
               if (toolboxView.getLevelCheckBoxStatus() == false && !levelValue.equals("")) {
            	   tags.add(new Tag(tr("level"),levelValue));
               }
           	   if (!toolboxView.getLevelNameText().isEmpty() && !toolboxView.getLevelCheckBoxStatus()) {
           		   tags.add(new Tag(tr("level_name"),toolboxView.getLevelNameText()));
           	   }
               if (!toolboxView.getNameText().isEmpty()) {
                   tags.add(new Tag(tr("name"), toolboxView.getNameText()));
               }
               if (!toolboxView.getRefText().isEmpty()) {
                   tags.add(new Tag(tr("ref"), toolboxView.getRefText()));
               }
               if (!toolboxView.getRepeatOnText().isEmpty()){
           			tags.add(new Tag(tr("repeat_on"),toolboxView.getRepeatOnText()));
           	   }
           	   if (!toolboxView.getLevelNameText().isEmpty() && !toolboxView.getLevelCheckBoxStatus()) {
           		   tags.add(new Tag(tr("level_name"),toolboxView.getLevelNameText()));
           	   }

           // Tagging to OSM Data
           model.addTagsToOSM(indoorObject, tags);

           // Reset ui elements
           toolboxView.resetUiElements();

           //Do the validation process
           ValidateAction validateAction = new ValidateAction();
           validateAction.doValidate(true);

           refreshPresets();

       }
   }

   /**
    * The listener which is called when a new item in the object list is selected.
    *
    * @author egru
    * @author rebsc
    *
    */
   class ToolObjectItemListener implements ItemListener {

       @Override
       public void itemStateChanged(ItemEvent e) {
           if (toolboxView.getSelectedObject().equals(IndoorObject.ROOM)) {
               toolboxView.setNRUiElementsEnabled(true);
               toolboxView.setROUiElementsEnabled(false);
           }
           else if (toolboxView.getSelectedObject().equals(IndoorObject.STEPS) ||
           		toolboxView.getSelectedObject().equals(IndoorObject.ELEVATOR)) {
       	 		toolboxView.setROUiElementsEnabled(true);
       	 		toolboxView.setNRUiElementsEnabled(true);
        	}
           else {
               toolboxView.setROUiElementsEnabled(false);
           }
       }
   }

   /**
    * The listener which is called when the LevelCheckBox is selected.
    *
    * @author rebsc
    */
   class ToolLevelCheckBoxListener implements ItemListener{
       @Override
       public void itemStateChanged(ItemEvent e) {
    	   if(e.getStateChange() == ItemEvent.SELECTED) {
    		   toolboxView.setLVLUiElementsEnabled(false);
    	   }
    	   else {
    		   toolboxView.setLVLUiElementsEnabled(true);
    	   }
       }
   }

   /**
    * The listener which is called when the helpbutton got pushed.
    *
    * @author rebsc
    */
   static class ToolHelpButtonListener implements ActionListener{

	   @Override
	   public void actionPerformed(ActionEvent e) {
		   String topic = tr("ToolB");
		   HelpBrowser.setUrlForHelpTopic(Optional.ofNullable(topic).orElse(tr("/")));
	   }
   }

   /**
    * The listener which is called when the addlevelbutton got pushed.
    *
    * @author rebsc
    */
   class ToolAddLevelButtonListener implements ActionListener{

	   @Override
	   public void actionPerformed(ActionEvent e) {

		   if(selectorView == null) {
			   selectorView = new LevelSelectorView();
			   addLevelSelectorListeners();

			   //Show LevelSelectorView
			   selectorView.setVisible(true);
		   } else {
			   //Put focus back on LevelSelectorView
			   selectorView.toFront();
		   }

	   }
   }

   /**
    * The listener which is called when the MultiCheckBox is selected.
    *
    * @author rebsc
    */
   class ToolMultiCheckBoxListener implements ItemListener{
       @Override
       public void itemStateChanged(ItemEvent e) {
    	   if(e.getStateChange() == ItemEvent.SELECTED) {
    		   toolboxView.setMultiUiElementsEnabled(false);
    	   }
    	   else {
    		   toolboxView.setMultiUiElementsEnabled(true);
    	   }
       }
   }

   /**
    * The listener which is called when the OUTER Button got pushed.
    *
    * @author rebsc
    */
   class ToolOuterButtonListener implements ActionListener{

	   @Override
	   public void actionPerformed(ActionEvent e) {
		   // Select drawing action
		   map.selectMapMode(drawAction);

		   // For space shortcut to add the relation after spacebar got pushed {@link SpaceAction}
		   OuterHelp = true;
		   InnerHelp = false;
	   }
   }

   /**
    * The listener which is called when the INNER Button got pushed.
    *
    * @author rebsc
    */
   class ToolInnerButtonListener implements ActionListener {
	   @Override
	   public void actionPerformed(ActionEvent e) {
		   // Select drawing action
		   map.selectMapMode(drawAction);

		   // For space shortcut to edit the relation after enter got pushed {@link SpaceAction}{@link EnterAction}
		   InnerHelp = true;
		   OuterHelp = false;

	   }
   }

   /**
    * Listener for preset button 1.
    * @author egru
    *
    */
   class Preset1Listener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
           model.addTagsToOSM(toolboxView.getPreset1());
       }
   }

   /**
    * Listener for preset button 2.
    * @author egru
    *
    */
   class Preset2Listener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
           model.addTagsToOSM(toolboxView.getPreset2());
       }
   }

   /**
    * Listener for preset button 3.
    * @author egru
    *
    */
   class Preset3Listener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
           model.addTagsToOSM(toolboxView.getPreset3());
       }
   }

   /**
    * Listener for preset button 4.
    * @author egru
    *
    */
   class Preset4Listener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
           model.addTagsToOSM(toolboxView.getPreset4());
       }
   }

   /**
    * Updates the preset button from the current ranking.
    */
   private void refreshPresets() {
       toolboxView.setPresetButtons(model.getPresetRanking());
   }

/*************************************************
* LEVEL SELCTOR VIEW LISTENER
*
*/
   /**
    * Specific listener for the applyButton
    * @author rebsc
    *
    */
   class ToolLevelOkButtonListener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
    	   LevelHelp = true;

    	   //Get insert level number out of SelectorView
    	   if(!selectorView.getLevelNumber().equals("")) {
    		   levelNum = selectorView.getLevelNumber();

    		   //Unset visibility
    		   selectorView.dispose();
    		   //Select draw-action
    		   map.selectMapMode(drawAction);

    	   }else {
    		   JOptionPane.showMessageDialog(null, tr("Please insert a value."), tr("Error"), JOptionPane.ERROR_MESSAGE);
    	   }

    	   selectorView = null;
       }
   }

   /**
    * Specific listener for the cancelButton
    * @author rebsc
    *
    */
   class ToolLevelCancelButtonListener implements ActionListener {

       @Override
       public void actionPerformed(ActionEvent e) {
           selectorView.dispose();
           selectorView = null;
       }
   }

   /**
    * General listener for LevelSelectorView window
    * @author rebsc
    *
    */
   class ToolSelectorWindowSListener implements WindowListener{

	@Override
	public void windowClosed(WindowEvent e) {
		selectorView = null;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		selectorView = null;
	}

	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub

	}
   }


/*************************************************
* SHORTCUT METHODS
*
*/
   /**
    * Shortcut for Spacebar
    * @author rebsc
    */
   private class SpaceAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	@Override
       public void actionPerformed(ActionEvent e) {
           if(OuterHelp) {

        	   //Create new relation and add the currently drawn object to it
        	   model.addRelation("outer");
        	   map.selectMapMode(selectAction);
        	   OuterHelp = false;

        	   //Clear currently selection
        	   MainApplication.getLayerManager().getEditDataSet().clearSelection();
           }
           else if(InnerHelp) {

        	   //Save new drawn relation for adding
        	   innerRelation = MainApplication.getLayerManager().getEditDataSet().getAllSelected();
   		   	   map.selectMapMode(selectAction);

        	   //Clear currently selection
        	   MainApplication.getLayerManager().getEditDataSet().clearSelection();
           }
           else if(LevelHelp) {

               List<Tag> tags = new ArrayList<>();
        	   tags.add(new Tag(tr("level"),levelNum));

        	   //Add level tag
        	   model.addTagsToOSM(tags);

        	   //Change action
        	   map.selectMapMode(selectAction);
        	   LevelHelp = false;
           }
       }
   }

   /**
    * Shortcut for Enter
    * @author rebsc
    */
   private class EnterAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	@Override
       public void actionPerformed(ActionEvent e) {

    	   if(InnerHelp && !OuterHelp) {

    		   // Edit the new drawn relation member to selected relation
    		   model.editRelation("inner", innerRelation);
    		   InnerHelp = false;

    	   }else if((InnerHelp && OuterHelp) || (OuterHelp && !InnerHelp)){
    		   JOptionPane.showMessageDialog(null, tr("Please press spacebar first to add \"outer\" object to relation."), tr("Relation-Error"), JOptionPane.ERROR_MESSAGE);
    		   resetHelper();
    	   }
       }
   }

/*************************************************
* FILTER METHODS
*
*/
   /**
    * Function which unset the disabled state of currently hidden and/or disabled objects which have a
    * specific tag (key). Just unsets the disabled state if object has a tag-value which is part of the
    * current working level.
    * Specific example: key: repeat_on ; value: 1-4;
    * 					If current selected workinglevel is "3" all objects with the leveltag "level=3" are hidden but the
    * 					objects with the key "repeat_on" and the value which includes "3" or is (minValue+1) - maxValue (4-...),
    * 					because repeat_on tag starts on the current workinglevel+1.
    * @param key sepcific key to unset hidden objects which contains it
    * @author rebsc
    */
   public void unsetSpecificKeyFilter(String key) {

	 Collection<OsmPrimitive> p = Main.main.getEditDataSet().allPrimitives();
	 Map<String, String> tags = new HashMap<>();
	 Integer level = Integer.parseInt(levelValue);
	 Integer firstVal, secVal;

	 //Find all primitives with the specific tag and check if value is part of the current
	 //workinglevel. After that unset the disabled status.
	 for(OsmPrimitive osm: p) {
		 if((osm.isDisabledAndHidden() || osm.isDisabled()) && osm.hasKey(key)) {

			 tags = osm.getInterestingTags();

			 for(Map.Entry<String, String> e: tags.entrySet()) {
				if(e.getKey().equals(key)) {
					String val = e.getValue();

					//Extract values
					if(val.indexOf("-") == 0) {
						firstVal = (Integer.parseInt(val.split("-",2)[1].split("-",2)[0]))*-1;
						secVal = Integer.parseInt(val.split("-",2)[1].split("-",2)[1]);
					}else {
						firstVal = Integer.parseInt(val.split("-")[0]);
						secVal = Integer.parseInt(val.split("-")[1]);
					}

					//Compare values to current working level
					if(level >= ((firstVal)-1) && level <= secVal) {
						osm.unsetDisabledState();
					}else {
						osm.setDisabledState(true);
					}
				}

			 }

		 }
	 }

   }

  /**
   * Function which updates the current working level tag
   *
   * @param indoorLevel: current working level
   */
   public void setIndoorLevel(String indoorLevel) {
	   this.toolboxView.setLevelLabel(indoorLevel);
   }

   /**
    * Function which gets the current working level tag
    *
    * @param indoorLevel: current working level
    */
    public void getIndoorLevel(String indoorLevel) {
    	levelValue = indoorLevel;

    }


/*************************************************
* HELPER METHODS
*
*/

   /**
    * Function which resets the helper for relation adding
    */
   private void resetHelper() {
	   InnerHelp = false;
	   OuterHelp = false;
   }

    /**
     * Forces JOSM to load the validator and mappaint settings.
     */
    private void updateSettings() {
        Main.pref.init(false);
        MapCSSTagChecker tagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
            if (tagChecker != null) {
                OsmValidator.initializeTests(Collections.singleton(tagChecker));
            }

            MapPaintStyles.readFromPreferences();
    }

   /**
    * Enables or disables the preferences for the mapcss-style and the validator.
    *
    * @param enabled Activates or disables the settings.
    */
	@SuppressWarnings("deprecation")
	private void setPluginPreferences(boolean enabled) {
       Map<String, Setting<?>> settings = Main.pref.getAllSettings();

       MapListSetting validatorMapListSetting = (MapListSetting) settings.
               get("validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries");
       List<Map<String, String>> validatorMaps = new ArrayList<>();
       if (validatorMapListSetting != null) {
           validatorMaps = validatorMapListSetting.getValue();
       }

       MapListSetting styleMapListSetting = (MapListSetting) settings.
               get("mappaint.style.entries");
       List<Map<String, String>> styleMaps = new ArrayList<>();
       if (styleMapListSetting != null) {
           styleMaps = styleMapListSetting.getValue();
       }

       if (enabled) {
           //set the validator active

           List<Map<String, String>> validatorMapsNew = new ArrayList<>();
           if (!validatorMaps.isEmpty()) {
               validatorMapsNew.addAll(validatorMaps);
           }

           for (Map<String, String> map : validatorMapsNew) {
               if (map.containsValue(tr("Indoor"))) {
                   validatorMapsNew.remove(map);
                   break;
               }
           }

           Map<String, String> indoorValidator = new HashMap<>();
           indoorValidator.put("title", "Indoor");
           indoorValidator.put("active", "true");
           indoorValidator.put("url", Main.pref.getUserDataDirectory()+ sep +"validator" +
                   sep + "indoorhelper.validator.mapcss");

           validatorMapsNew.add(indoorValidator);
           Main.pref.putListOfMaps("validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries",
                   validatorMapsNew);

           //set mappaint active

           List<Map<String, String>> styleMapsNew = new ArrayList<>();
           if (!styleMaps.isEmpty()) {
               styleMapsNew.addAll(styleMaps);
           }

           for (Map<String, String> map : styleMapsNew) {
               if (map.containsValue(tr("Indoor"))) {
                   styleMapsNew.remove(map);
                   break;
               }
           }
           Map<String, String> indoorMapPaint = new HashMap<>();
           indoorMapPaint.put("title", tr("Indoor"));
           indoorMapPaint.put("active", "true");
           indoorMapPaint.put("url", Main.pref.getUserDataDirectory() + sep + "styles"
                   + sep + "sit.mapcss");
           styleMapsNew.add(indoorMapPaint);
           Main.pref.putListOfMaps("mappaint.style.entries", styleMapsNew);

           updateSettings();
       } else {
           //set the validator inactive


           List<Map<String, String>> validatorMapsNew = new ArrayList<>();
           if (!validatorMaps.isEmpty()) {
               validatorMapsNew.addAll(validatorMaps);
           }

           for (Map<String, String> map : validatorMapsNew) {
               if (map.containsValue(tr("Indoor"))) {
                   validatorMapsNew.remove(map);
                   break;
               }
           }
           Map<String, String> indoorValidator = new HashMap<>();
           indoorValidator.put("title", tr("Indoor"));
           indoorValidator.put("active", "false");
           indoorValidator.put("url", Main.pref.getUserDataDirectory()+ sep +"validator" +
                   sep + "indoorhelper.validator.mapcss");

           validatorMapsNew.add(indoorValidator);
           Main.pref.putListOfMaps("validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries",
                   validatorMapsNew);


           //set mappaint inactive


           List<Map<String, String>> styleMapsNew = new ArrayList<>();
           if (!styleMaps.isEmpty()) {
               styleMapsNew.addAll(styleMaps);
           }
           for (Map<String, String> map : styleMapsNew) {
               if (map.containsValue(tr("Indoor"))) {
                   styleMapsNew.remove(map);
                   break;
               }
           }
           Map<String, String> indoorMapPaint = new HashMap<>();
           indoorMapPaint.put("title", tr("Indoor"));
           indoorMapPaint.put("active", "false");
           indoorMapPaint.put("url", Main.pref.getUserDataDirectory() + sep + "styles"
                   + sep + "sit.mapcss");
           styleMapsNew.add(indoorMapPaint);
           Main.pref.putListOfMaps("mappaint.style.entries", styleMapsNew);

           updateSettings();
       }
   }





/**
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
}