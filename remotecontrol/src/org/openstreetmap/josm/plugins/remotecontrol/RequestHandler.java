package org.openstreetmap.josm.plugins.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashMap;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;

/**
 * This is the parent of all classes that handle a specific command 
 * in remote control plug-in.
 *   
 * @author Bodo Meissner
 */
public abstract class RequestHandler
{
	/** The GET request arguments */
	protected HashMap<String,String> args;
	
	/** The request URL without "GET". */
    protected String request;

    /** default response */
    protected String content = "OK\r\n";
    /** default content type */
    protected String contentType = "text/plain";

    /** will be filled with the command assigned to the subclass */
    private String myCommand;
    
    /**
     * Check permission and parameters and handle request.
     * 
     * @throws RequestHandlerForbiddenException
     * @throws RequestHandlerBadRequestException
     * @throws RequestHandlerErrorException
     */
    final void handle() throws RequestHandlerForbiddenException, RequestHandlerBadRequestException, RequestHandlerErrorException
    {
    	checkPermission();
    	checkMandatoryParams();
    	handleRequest();
    }
    
    /**
     * Handle a specific command sent as remote control.
     * 
     * This method of the subclass will do the real work.  
     * 
     * @throws RequestHandlerErrorException
     * @throws RequestHandlerBadRequestException 
     */
    protected abstract void handleRequest() throws RequestHandlerErrorException, RequestHandlerBadRequestException;

    /**
     * Get a specific message to ask the user for permission for the operation
     * requested via remote control.
     * 
     * This message will be displayed to the user if the preference
     * remotecontrol.always-confirm is true.
     * 
     * @return the message
     */
    abstract public String getPermissionMessage();

    /**
     * Get a PermissionPref object containing the name of a special permission
     * preference to individually allow the requested operation and an error
     * message to be displayed when a disabled operation is requested.
     * 
     * Default is not to check any special preference. Override this in a 
     * subclass to define permission preference and error message.
     * 
     * @return the preference name and error message or null
     */
    public PermissionPref getPermissionPref()
    {
        /* Example:
        return new PermissionPref("fooobar.remotecontrol",
        "RemoteControl: foobar forbidden by preferences");
        */
        return null;
    }

    protected String[] getMandatoryParams()
    {
    	return null;
    }
    
    /**
     * Check permissions in preferences and display error message 
     * or ask for permission.
     * 
     * @throws RequestHandlerForbiddenException
     */
    final public void checkPermission() throws RequestHandlerForbiddenException
    {
        /* 
         * If the subclass defines a specific preference and if this is set
         * to false, abort with an error message.
         */
        PermissionPref permissionPref = getPermissionPref();
        if((permissionPref != null) && (permissionPref.pref != null))
        {
            if (!Main.pref.getBoolean(permissionPref.pref, true)) {
                System.out.println(permissionPref.message);
                throw new RequestHandlerForbiddenException();
            }
        }

        /* Does the user want to confirm everything?
         * If yes, display specific confirmation message.
         */
        if (Main.pref.getBoolean("remotecontrol.always-confirm", false)) {
            if (JOptionPane.showConfirmDialog(Main.parent,
                "<html>" + getPermissionMessage() +
                "<br>" + tr("Do you want to allow this?"),
                tr("Confirm Remote Control action"),
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                    throw new RequestHandlerForbiddenException();
            }
        }
    }

    /**
     * Set request URL and parse args.
     * 
     * @param url The request URL.
     */
	public void setUrl(String url) {
		this.request = url;
		parseArgs();
	}

	/**
	 * Parse the request parameters as key=value pairs.
	 * The result will be stored in this.args.
	 * 
	 * Can be overridden by subclass.
	 */
	protected void parseArgs() {
		StringTokenizer st = new StringTokenizer(this.request, "&?");
		HashMap<String, String> args = new HashMap<String, String>();
		// ignore first token which is the command
		if(st.hasMoreTokens()) st.nextToken();
		while (st.hasMoreTokens()) {
			String param = st.nextToken();
			int eq = param.indexOf("=");
			if (eq > -1)
				args.put(param.substring(0, eq),
                         param.substring(eq + 1));
		}
		this.args = args;
	}
	
	void checkMandatoryParams() throws RequestHandlerBadRequestException
	{
		String[] mandatory = getMandatoryParams();
		if(mandatory == null) return;
		
		boolean error = false;
		for(int i = 0; i < mandatory.length; ++i)
		{
			String key = mandatory[i];
			String value = args.get(key);
			if((value == null) || (value.length() == 0))
			{
				error = true;
				System.out.println("'" + myCommand + "' remote control request must have '" + key + "' parameter");
			}
		}
		if(error) throw new RequestHandlerBadRequestException();
	}
	
	/**
	 * Save command associated with this handler.
	 * 
	 * @param command The command.
	 */
	public void setCommand(String command)
	{
		if(command.charAt(0) == '/') command = command.substring(1);
		myCommand = command;
	}
}