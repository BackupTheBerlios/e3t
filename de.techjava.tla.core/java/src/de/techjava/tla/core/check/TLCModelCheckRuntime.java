package de.techjava.tla.core.check;

import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.zambrovski.tla.RuntimeConfiguration;
import org.zambrovski.tla.tlc.TLCSimulator;

import de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration;
import de.techjava.tla.ui.extensions.ITLCModelCheckResult;
import de.techjava.tla.ui.extensions.ITLCModelCheckRuntime;
import de.techjava.tla.ui.views.ConsoleLogger;
import de.techjava.tla.ui.views.LogStream;


/**
 * checker wraper 
 *
 * @author Boris Gruschko ( Lufthansa Systems Business Solutions GmbH )
 * @version $Id: TLCModelCheckRuntime.java,v 1.1 2007/01/29 22:25:04 tlateam Exp $
 */
public class TLCModelCheckRuntime
		implements ITLCModelCheckRuntime, ITLCModelCheckConfiguration
{

	private IPath	rootDirectory;
	
	private IPath	workingDirectory;
	private IPath	configDirectory;
	private IPath[]	modulesLibraryPathes;

	private IResource	configFilePath;
	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckRuntime#startCheck(org.eclipse.core.resources.IResource[])
	 */
	public ITLCModelCheckResult[] startCheck(IResource[] resource)
	{
		String[] libs = new String[modulesLibraryPathes.length];
		
		for ( int i = 0; i < libs.length; i++ ) {
			libs[i] = modulesLibraryPathes[i].toOSString();
		}
		
		try {
	        RuntimeConfiguration conf = RuntimeConfiguration.get();
	        conf.setOutStream(new LogStream(ConsoleLogger.getLogger().getCheckerConsole()));
	        conf.setErrorStream(new LogStream(ConsoleLogger.getLogger().getCheckerConsole()));
	        conf.setWorkDirectory( workingDirectory.toOSString() );
	        conf.setConfigDirectoryName( configDirectory.toOSString() );
	        conf.addLibraryPath( libs );
	        conf.setRootDirectoryName(rootDirectory.toOSString());
	        
	        
	        String configFilename = null;
	        if (configFilePath != null) 
	        {
	         configFilename = configFilePath.getProjectRelativePath().lastSegment();   
	        }
	        
			TLCSimulator.getSimulator().simulate( resource[0].getProjectRelativePath().lastSegment(), configFilename, null);
		} catch ( Exception exc ) 
		{
			exc.printStackTrace();
		}
	    return null;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckRuntime#stopCheck(org.eclipse.core.resources.IResource[])
	 */
	public ITLCModelCheckResult[] stopCheck(IResource[] resource)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckRuntime#flushWorkingDirectory()
	 */
	public boolean flushWorkingDirectory()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getConfigDirectory()
	 */
	public IPath getConfigDirectory()
	{
		return this.configDirectory;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getModuleLibraryPath()
	 */
	public IPath[] getModuleLibraryPath()
	{
		return this.modulesLibraryPathes;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getRootDirectory()
	 */
	public IPath getRootDirectory()
	{
		return this.rootDirectory;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getSwitches()
	 */
	public Map getSwitches()
	{
		return null;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getWorkingDirectory()
	 */
	public IPath getWorkingDirectory()
	{
		return this.workingDirectory;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setModuleLibraryPath(org.eclipse.core.runtime.IPath[])
	 */
	public void setModuleLibraryPath(IPath[] paths)
	{
		this.modulesLibraryPathes = paths;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setConfigDirectory(org.eclipse.core.runtime.IPath)
	 */
	public void setConfigDirectory(IPath path)
	{
		this.configDirectory	=	path;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setRootDirectory(org.eclipse.core.runtime.IPath)
	 */
	public void setRootDirectory(IPath path)
	{
		this.rootDirectory = path;
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setWorkingDirectory(org.eclipse.core.runtime.IPath)
	 */
	public void setWorkingDirectory(IPath path)
	{
		this.workingDirectory =	path;
	}
    /**
     * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#getConfigFilename()
     */
    public IResource getConfigFilename() 
    {
        return this.configFilePath;
    }
    /**
     * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setConfigFileName(org.eclipse.core.runtime.IPath)
     */
    public void setConfigFileName(IResource configpath) 
    {
        this.configFilePath = configpath;
    }

	/**
	 * @see de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration#setSwitches(java.util.Map)
	 */
	public void setSwitches(Map properties)
	{
		RuntimeConfiguration	conf = RuntimeConfiguration.get();
		
		conf.setCheckerCheckDeadlock( getBooleanWithDefault(properties, MODEL_CHECK_DEADLOCK, MODEL_CHECK_DEADLOCK_DEFAULT)  );
		conf.setCheckerSimulateMode( getBooleanWithDefault(properties, MODEL_RUN_IN_SIMULATE_MODE, MODEL_RUN_IN_SIMULATE_MODE_DEFAULT));
		conf.setCheckerRunDepth( getIntWithDefault(properties, MODEL_RUN_DEPTH, Integer.parseInt( MODEL_RUN_DEPTH_DEFAULT ) ) );
		conf.setCheckerUseSeed( getBooleanWithDefault(properties, MODEL_USE_WITH_SEED, MODEL_USE_WITH_SEED_DEFAULT ) );
		if ( conf.isCheckerUseSeed() ) {
		    conf.setCheckerSeed( getIntWithDefault(properties, MODEL_WITH_SEED, Integer.parseInt( MODEL_WITH_SEED_DEFAULT ) ) );
		}
		conf.setCheckerUseAril( getBooleanWithDefault(properties, MODEL_USE_WITH_ARIL, MODEL_USE_WITH_ARIL_DEFAULT) );
		if ( conf.isCheckerUseAril() ) {
		    conf.setCheckerAril( getIntWithDefault(properties, MODEL_WITH_ARIL, Integer.parseInt( MODEL_WITH_ARIL_DEFAULT ) ) );
		}
		conf.setCheckerCoverage( getIntWithDefault(properties, MODEL_PRINT_COVERAGE, Integer.parseInt(MODEL_PRINT_COVERAGE_DEFAULT)));
		conf.setCheckerUseRecoverId( getBooleanWithDefault(properties, MODEL_USE_RECOVER_FROM, MODEL_USE_RECOVER_FROM_DEFAULT));
		if ( conf.isCheckerUseRecoverId() ) {
		    conf.setCheckerRecoverId( getStringWithDefault(properties,  MODEL_RECOVER_FROM, MODEL_RECOVER_FROM_DEFAULT));
		}
		
		conf.setCheckerUseDiffTrace( getBooleanWithDefault(properties, MODEL_USE_DIFF_TRACE, MODEL_USE_DIFF_TRACE_DEFAULT));
		if ( conf.isCheckerUseDiffTrace() ) {
		    conf.setCheckerDiffTrace( getIntWithDefault(properties, MODEL_DIFF_TRACE, Integer.parseInt( MODEL_DIFF_TRACE_DEFAULT ) ) );
		}
		conf.setCheckerTerse( getBooleanWithDefault(properties, MODEL_TERSE, MODEL_TERSE_DEFAULT) );
		conf.setCheckerWorkerCount( getIntWithDefault(properties, MODEL_WORKER_COUNT, Integer.parseInt( MODEL_WORKER_COUNT_DEFAULT )));
		conf.setCheckerNoWarning( getBooleanWithDefault(properties, MODEL_NO_WARNINGS, MODEL_NO_WARNINGS_DEFAULT));
	}
	/**
	 * Retrieves a boolean from attribute map
	 * @param map
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private boolean getBooleanWithDefault( Map map, String key, boolean defaultValue )
	{
		Object value = map.get( key );
		
		if ( value == null )
			return defaultValue;
		
		return ((Boolean)value).booleanValue();
	}
	/**
	 * Retrieves a string from attribute map
	 * @param map
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private String getStringWithDefault( Map map, String key, String defaultValue )
	{
		Object value = map.get( key );
		
		if ( value == null )
			return defaultValue;
		
		return (String)value;		
	}
	/**
	 * Retrieves an int from attribute map
	 * @param map
	 * @param key
	 * @param defaultValue
	 * @return
	 */
	private int getIntWithDefault( Map map, String key, int defaultValue )
	{
		Object value = map.get( key );
		
		if ( value == null )
			return defaultValue;
		
		return ((Integer)value).intValue();		
	}



}

/*-
 * $Log: TLCModelCheckRuntime.java,v $
 * Revision 1.1  2007/01/29 22:25:04  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:48:31  szambrovski
 * *** empty log message ***
 *
 * Revision 1.12  2004/10/26 14:26:54  sza
 * configuration file support added
 *
 * Revision 1.11  2004/10/23 16:39:46  sza
 * fix
 *
 * Revision 1.10  2004/10/20 15:08:50  sza
 * logging redirected to console
 *
 * Revision 1.9  2004/10/20 14:01:38  bgr
 * runtime configuration written through to the simulator
 *
 * Revision 1.8  2004/10/15 01:17:15  sza
 * working directory added
 *
 * Revision 1.7  2004/10/14 23:04:19  sza
 * methods renamed
 *
 * Revision 1.6  2004/10/14 22:58:25  sza
 * bug fixed
 *
 * Revision 1.5  2004/10/14 22:22:56  sza
 * matched the call parameters
 *
 * Revision 1.4  2004/10/14 21:38:16  bgr
 * checker runtime conf added
 *
 * Revision 1.3  2004/10/14 20:56:58  bgr
 * configuration moved
 *
 * Revision 1.2  2004/10/14 20:52:30  bgr
 * checker running
 *
 * Revision 1.1  2004/10/13 17:14:30  bgr
 * launcher built
 *
 */