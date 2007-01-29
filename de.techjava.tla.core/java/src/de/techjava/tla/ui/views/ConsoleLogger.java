package de.techjava.tla.ui.views;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

import de.techjava.tla.core.CorePlugin;

/**
 * Console logger writing to tla log view
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: ConsoleLogger.java,v 1.1 2007/01/29 22:25:04 tlateam Exp $
 */
public class ConsoleLogger 
{
    private static ConsoleLogger logger; 
    
    
    private TLAConsole		 parserConsole;
    private TLAConsole		 checkerConsole;
    private IPreferenceStore store;
    /**
     * 
     *
     */
    private ConsoleLogger()
    {
        store = CorePlugin.getDefault().getPreferenceStore();
        parserConsole 	= new TLAConsole("TLA+ Parser Console", null, TLAConsole.TYPE_PARSER);
        checkerConsole 	= new TLAConsole("TLA+ Checker Console", null, TLAConsole.TYPE_CHECKER);
        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		IConsole[] consoles = manager.getConsoles();
		manager.addConsoles(new IConsole[]{parserConsole, checkerConsole});
    }
    
    /**
     * Retrieves logger instance
     * @return 
     */
    public static ConsoleLogger getLogger()
    {
        if (logger == null) 
        {
            logger = new ConsoleLogger();
        }
        return logger;
    }
    /**
     * Retrieves the parser console
     * @return
     */
	public TLAConsole getParserConsole()
	{
	    return parserConsole;
	}
	/**
	 * Retrieves the model checker console
	 * @return
	 */
	public TLAConsole getCheckerConsole()
	{
	    return checkerConsole;
	}

}

/*
 * $Log: ConsoleLogger.java,v $
 * Revision 1.1  2007/01/29 22:25:04  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:48:31  szambrovski
 * *** empty log message ***
 *
 * Revision 1.5  2004/10/23 16:39:37  sza
 * console fix
 *
 * Revision 1.4  2004/10/20 15:08:49  sza
 * logging redirected to console
 *
 * Revision 1.3  2004/10/20 13:59:26  sza
 * console
 *
 * Revision 1.2  2004/10/20 11:50:28  sza
 * init
 *
 * Revision 1.1  2004/10/19 15:20:09  sza
 * init
 *
 *
 */