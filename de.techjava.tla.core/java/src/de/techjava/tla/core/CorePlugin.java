package de.techjava.tla.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class CorePlugin 
	extends AbstractUIPlugin 
{
	//The shared instance.
	private static CorePlugin plugin;
	//Resource bundle.
	private ResourceBundle resourceBundle;
    private static final String PLUGIN_ID 	= "de.techjava.tla.core";
	
	/**
	 * The constructor.
	 */
	public CorePlugin() {
		super();
		plugin = this;
		try 
		{
			resourceBundle = ResourceBundle.getBundle("de.techjava.tla.core.CorePluginResources");
		} catch (MissingResourceException x) 
		{
			resourceBundle = null;
		}
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static CorePlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns the string from the plugin's resource bundle,
	 * or 'key' if not found.
	 */
	public static String getResourceString(String key) 
	{
		ResourceBundle bundle = CorePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	/**
	 * Logs an error
	 * @param description Error description
	 * @param cause cause of an error
	 */
	public static void logError(String description, Throwable cause)
	{
	    getDefault().getLog().log(
				new Status(
					IStatus.ERROR,
					CorePlugin.PLUGIN_ID,
					0,
					description,
					cause));
	    
	}


	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
}
