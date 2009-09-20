package org.openstreetmap.josm.plugins.validator;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.plugins.validator.util.AgregatePrimitivesVisitor;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * The action that does the validate thing.
 * <p>
 * This action iterates through all active tests and give them the data, so that
 * each one can test it.
 *
 * @author frsantos
 */
public class ValidateAction extends JosmAction {
    private OSMValidatorPlugin plugin;

    /** Serializable ID */
    private static final long serialVersionUID = -2304521273582574603L;

    /** Last selection used to validate */
    private Collection<OsmPrimitive> lastSelection;

    /**
     * Constructor
     */
    public ValidateAction(OSMValidatorPlugin plugin) {
        super(tr("Validation"), "validator", tr("Performs the data validation"),
        Shortcut.registerShortcut("tools:validate", tr("Tool: {0}", tr("Validation")), KeyEvent.VK_V, Shortcut.GROUP_EDIT, Shortcut.SHIFT_DEFAULT), true);
        this.plugin = plugin;
    }

    public void actionPerformed(ActionEvent ev) {
        doValidate(ev, true);
    }

    /**
     * Does the validation.
     * <p>
     * If getSelectedItems is true, the selected items (or all items, if no one
     * is selected) are validated. If it is false, last selected items are
     * revalidated
     *
     * @param ev The event
     * @param getSelectedItems If selected or last selected items must be validated
     */
    public void doValidate(ActionEvent ev, boolean getSelectedItems) {
        if (plugin.validateAction == null || Main.map == null || !Main.map.isVisible())
            return;

        OSMValidatorPlugin.plugin.initializeErrorLayer();

        Collection<Test> tests = OSMValidatorPlugin.getEnabledTests(false);
        if (tests.isEmpty())
            return;

        Collection<OsmPrimitive> selection;
        if (getSelectedItems) {
            selection = Main.main.getCurrentDataSet().getSelected();
            if (selection.isEmpty()) {
                selection = Main.main.getCurrentDataSet().allNonDeletedPrimitives();
                lastSelection = null;
            } else {
                AgregatePrimitivesVisitor v = new AgregatePrimitivesVisitor();
                selection = v.visit(selection);
                lastSelection = selection;
            }
        } else {
            if (lastSelection == null)
                selection = Main.main.getCurrentDataSet().allNonDeletedPrimitives();
            else
                selection = lastSelection;
        }

        ValidationTask task = new ValidationTask(tests, selection, lastSelection);
        Main.worker.submit(task);        
    }

    @Override
    public void updateEnabledState() {
        setEnabled(getEditLayer() != null);
    }
    
    /**
     * Asynchronous task for running a collection of tests against a collection
     * of primitives 
     *
     */
    
    class ValidationTask extends PleaseWaitRunnable {
    	private Collection<Test> tests;
    	private Collection<OsmPrimitive> validatedPrimitmives;
    	private Collection<OsmPrimitive> formerValidatedPrimitives;
    	private boolean canceled;
        private List<TestError> errors;
        
        /**
         * 
         * @param tests  the tests to run 
         * @param validatedPrimitives the collection of primitives to validate. 
         * @param formerValidatedPrimitives the last collection of primitives being validates. May be null.
         */
    	public ValidationTask(Collection<Test> tests, Collection<OsmPrimitive> validatedPrimitives, Collection<OsmPrimitive> formerValidatedPrimitives) {
    		super(tr("Validating"), false /*don't ignore exceptions */);
    		this.validatedPrimitmives  = validatedPrimitives;
    		this.formerValidatedPrimitives = formerValidatedPrimitives;
    		this.tests = tests;
    	}
    	
		@Override
		protected void cancel() {
			this.canceled = true; 			
		}

		@Override
		protected void finish() {
			if (canceled) return;
			
			// update GUI on Swing EDT
			//
			Runnable r = new Runnable()  {
				public void run() {
			        plugin.validationDialog.tree.setErrors(errors);
			        plugin.validationDialog.setVisible(true);
			        DataSet.fireSelectionChanged(Main.main.getCurrentDataSet().getSelected());
				}				
			};
			if (SwingUtilities.isEventDispatchThread()) {
				r.run();				
			} else {
				SwingUtilities.invokeLater(r);
			}
		}

		@Override
		protected void realRun() throws SAXException, IOException,
				OsmTransferException {
			if (tests == null || tests.isEmpty()) return;		
	        errors = new ArrayList<TestError>(200);
	        getProgressMonitor().setTicksCount(tests.size() * validatedPrimitmives.size());
	        int testCounter = 0;
			for (Test test : tests) {				
				if (canceled) return;
				testCounter++;
				getProgressMonitor().setCustomText(tr("Test {0}/{1}: Starting {2}", testCounter, tests.size(),test.name));
	            test.setPartialSelection(formerValidatedPrimitives != null);
	            test.startTest(getProgressMonitor().createSubTaskMonitor(validatedPrimitmives.size(), false));
	            test.visit(validatedPrimitmives);
	            test.endTest();
	            errors.addAll(test.getErrors());
	        }
			tests = null;
	        if (Main.pref.getBoolean(PreferenceEditor.PREF_USE_IGNORE, true)) {
				getProgressMonitor().subTask(tr("Updating ignored errors ..."));
	            for (TestError error : errors) {
	            	if (canceled) return;
	                List<String> s = new ArrayList<String>();
	                s.add(error.getIgnoreState());
	                s.add(error.getIgnoreGroup());
	                s.add(error.getIgnoreSubGroup());
	                for (String state : s) {
	                    if (state != null && plugin.ignoredErrors.contains(state)) {
	                        error.setIgnored(true);
	                    }
	                }
	            }
	        }
		}
    }
}
