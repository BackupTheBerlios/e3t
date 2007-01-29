package org.zambrovski.tla.tlasany;

import java.util.Properties;
import java.util.Vector;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.drivers.SANY;

/**
 * The starting point for the 
 * TLA+ Syntactic Analyser
 * 
 * This class should be used instead of tlsany.SANY
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: Runner.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class Runner {

    public static void main(String[] args) 
    {
        RuntimeConfiguration config = RuntimeConfiguration.get();
        
        // Parse and process the command line switches, which are
        // distinguished by the fact that they begin with a '-' character.
        // Once the first command line element that is NOT a switch is
        // encountered, the rest are presumed to be file names.
        
        Properties 	properties = new Properties();
        Vector 		filenameArgs = new Vector();
        for (int i = 0; i < args.length; i++) 
        {
            if ( args[i].charAt(0) == '-' || args[i].charAt(0) == '+') 
            {
                // switch
                if (args[i].startsWith("--ROOT=", 0) || args[i].startsWith("--root=", 0))
                {
                    config.setRootDirectoryName(args[i].substring(7));
                } else if (args[i].startsWith("--LIB=", 0) || args[i].startsWith("--lib=", 0)){
                    config.addLibraryPath(new String[]{args[i].substring(6)});                    
                } else {

	                Boolean value = null;
	                if (args[i].charAt(0) == '-') 
	                {
	                    value = new Boolean(false);
	                } else if (args[i].charAt(0) == '+') 
	                {
	                    value = new Boolean(true);
	                } else {
	                }
	                if (args[i].charAt(1) == 's' || args[i].charAt(1) == 'S') 
	                {
	                    properties.put(RuntimeConfiguration.SWITCH_SEMANTICS, value);
	                } else if (args[i].charAt(1) == 'l' || args[i].charAt(1) == 'L') 
	                {
	                    properties.put(RuntimeConfiguration.SWITCH_LEVELCHECK, value);
	                } else if (args[i].charAt(1) == 'd' || args[i].charAt(1) == 'D')
	                {
	                    properties.put(RuntimeConfiguration.SWITCH_DEBUGGING, value);
	                } else if (args[i].charAt(1) == 't' || args[i].charAt(1) == 'T')
	                {
	                    properties.put(RuntimeConfiguration.SWITCH_STATS, value);
	                } else {
	                    RuntimeConfiguration.get().getOutStream().println("Illegal switch: " + args[i]);
	                    RuntimeConfiguration.get().getOutStream().println("Usage : Runner [{+|-}s][{+|-}l][{+|-}d][{+|-}t][] filename[ filename [...]]");
	                    RuntimeConfiguration.get().getOutStream().println(" --root=path : Path to root directory");
	                    RuntimeConfiguration.get().getOutStream().println(" s           : Symantic Analysis");
	                    RuntimeConfiguration.get().getOutStream().println(" l           : Level Checking");
	                    RuntimeConfiguration.get().getOutStream().println(" d           : Debugging");
	                    RuntimeConfiguration.get().getOutStream().println(" l           : Statistics");
	                }
                }                
            } else 
            {
                // filename
                filenameArgs.add(args[i]);
            }
            config.setSwitches(properties);
        }
        
        SANY.SANYmain(filenameArgs);
    }

}

/*
 * $Log: Runner.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:56  szambrovski
 * sf cvs init
 *
 * Revision 1.3  2004/10/20 14:58:53  sza
 * System.err replaced
 *
 * Revision 1.2  2004/10/14 20:56:59  bgr
 * configuration moved
 *
 * Revision 1.1  2004/10/06 21:57:23  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 01:03:27  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:58:10  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:54:41  sza
 * init
 *
 *
 */