package org.openstreetmap.josm.plugins.validator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.*;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.GBC;

/**
 * Parent class for all validation tests.
 * <p>
 * A test is a primitive visitor, so that it can access to all data to be
 * validated. These primitives are always visited in the same order: nodes
 * first, then ways.
 * 
 * @author frsantos
 */
public class Test implements Visitor
{
	/** Name of the test */
	protected String name;
	
	/** Description of the test */
	protected String description;
	
    /** Whether this test is enabled. Enabled by default */
    protected boolean enabled = true;

    /** The preferences check for validation */
    protected JCheckBox checkEnabled;

    /** The preferences check for validation on upload */
    protected JCheckBox checkBeforeUpload;
    
    /** Whether this test must check before upload. Enabled by default */
    protected boolean testBeforeUpload = true;

    /** Whether this test is performing just before an upload */
    protected boolean isBeforeUpload;

	/** The list of errors */
	protected List<TestError> errors = new ArrayList<TestError>(30);

	/** Whether the test is run on a partial selection data */
	protected boolean partialSelection;

	/**
	 * Constructor
	 * @param name Name of the test
	 * @param description Description of the test
	 */
	public Test(String name, String description)
	{
		this.name = name;
		this.description = description;
	}
	
	/**
	 * Constructor
	 * @param name Name of the test
	 */
	public Test(String name)
	{
		this.name = name;
	}
	
	/**
	 * Initializes any global data used this tester.
	 * @param plugin The plugin
	 * @throws Exception When cannot initialize the test
	 */ 
	public static void initialize(@SuppressWarnings("unused") OSMValidatorPlugin plugin) throws Exception {}
	
	/**
	 * Notification of the start of the test. The tester can initialize the
	 * structures it may need for this test
	 */ 
	public void startTest() 
	{
		errors = new ArrayList<TestError>(30);
	}

	/**
	 * Flag notifying that this test is run over a partial data selection
	 * @param partialSelection Whether the test is on a partial selection data
	 */ 
	public void setPartialSelection(boolean partialSelection) 
	{
		this.partialSelection = partialSelection;
	}
	
	/**
	 * Gets the validation errors accumulated until this moment.
	 * @return The list of errors 
	 */
	public List<TestError> getErrors() 
	{
		return errors;
	}
	
	/**
	 * Notification of the end of the test. The tester may perform additional
	 * actions and destroy the used structures 
	 */
	public void endTest() {}

    /**
     * Visits all primitives to be tested. These primitives are always visited
     * in the same order: nodes first, then ways.
     * 
     * @param selection The primitives to be tested
     */
    public void visit(Collection<OsmPrimitive> selection) 
    {
        for (OsmPrimitive p : selection)
        {
        	if( !p.deleted )
        		p.visit(this);
        }
    }

    public void visit(Node n) {}

	public void visit(Way w) {}

	/**
	 * Allow the tester to manage its own preferences 
	 * @param testPanel The panel to add any preferences component
	 */
	public void addGui(@SuppressWarnings("unused") JPanel testPanel) 
	{
		checkEnabled = new JCheckBox(name, enabled);
		checkEnabled.setToolTipText(description);
        checkEnabled.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setGuiEnabled(checkEnabled.isSelected() || checkBeforeUpload.isSelected() );
            }
        });
		testPanel.add(checkEnabled, GBC.std().insets(20,0,0,0));
		
        checkBeforeUpload = new JCheckBox();
        checkBeforeUpload.setSelected(testBeforeUpload);
        checkBeforeUpload.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                setGuiEnabled(checkEnabled.isSelected() || checkBeforeUpload.isSelected() );
            }
        });
        testPanel.add(checkBeforeUpload, GBC.eop().insets(20,0,0,0));
	}

    /**
     * Enables or disables the test in the preferences gui
     * @param enabled
     */
    public void setGuiEnabled(boolean enabled)
    {
    }   

	/**
	 * Called when the used submits the preferences
	 */
	public void ok() 
	{
		enabled = checkEnabled.isSelected();
		testBeforeUpload = checkBeforeUpload.isSelected();
	}
	
	/**
	 * Fixes the error with the appropiate command
	 * 
	 * @param testError
	 * @return The command to fix the error
	 */
	public Command fixError(@SuppressWarnings("unused") TestError testError)
	{
		return null;
	}
	
	/**
	 * Returns true if the given error can be fixed automatically
	 * 
	 * @param testError The error to check if can be fixed
	 * @return true if the error can be fixed
	 */
	public boolean isFixable(@SuppressWarnings("unused") TestError testError)
	{
		return false;
	}

    /**
     * Returns true if this plugin must check the uploaded data before uploading
     * @return true if this plugin must check the uploaded data before uploading
     */
    public boolean testBeforeUpload()
    {
        return testBeforeUpload;
    }

    /**
     * Sets the flag that marks an upload check
     * @param isUpload if true, the test is before upload
     */
    public void setBeforeUpload(boolean isUpload)
    {
        this.isBeforeUpload = isUpload;
    }
}
