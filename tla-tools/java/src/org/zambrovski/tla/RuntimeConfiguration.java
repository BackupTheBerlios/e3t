package org.zambrovski.tla;

import java.io.PrintStream;
import java.util.Properties;

/**
 * Runtime Configuration
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: RuntimeConfiguration.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class RuntimeConfiguration 
{
    /**
     * System property to set for additional TLA-Libraries
     */
    public final static String PROPERTY_TLA_LIBRARY = "TLA-Library";
 
    public final static String SWITCH_DEBUGGING 	= "switch_debugging";
    public final static String SWITCH_LEVELCHECK 	= "switch_levelcheck";
    public final static String SWITCH_SEMANTICS 	= "switch_semantics";
    public final static String SWITCH_STATS 		= "switch_debugging";

    
    private static RuntimeConfiguration instance;
    
    private String[] 		libraryPath;
    private String	 		projectRootPath;
    private String 			projectConfigPath;
    private String 			projectWorkPath;
    private PrintStream		errorStream;
    private PrintStream		outStream;

    private boolean parsing			= true;
    private boolean stats			= false;
    private boolean debugging		= false;
    private boolean levelCheck		= true;
    private boolean semantics 		= true;
    
	private	boolean	checkerCheckDeadlock;
	private	boolean	checkerSimulateMode;
	private	int		checkerRunDepth;
	private	boolean	checkerUseSeed;
	private	int		checkerSeed;
	private boolean	checkerUseAril;
	private	int		checkerAril;
	private	int		checkerCoverage;
	private boolean	checkerRecoverFrom;
	private	String	checkerRecoverId;
	private boolean	checkerUseDiffTrace;
	private	int		checkerDiffTrace;
	private boolean	checkerTerse;
	private	int		checkerWorkerCount;
	private	boolean	checkerNoWarning;

    private boolean doTLAStackTrace;


    


    
    /**
     * Private constructor to avoid instantiation
     */
    private RuntimeConfiguration()
    {
        this.readSystemLibraryPath();
        this.errorStream = System.err;
        this.outStream	 = System.out;
    }
    /**
     * Retrieves a working instance
     * @return configuration instance
     */
    public static RuntimeConfiguration get()
    {
        if (instance == null)
        {
            instance = new RuntimeConfiguration();
        }
        return instance;
    }
    /**
     * Adds the path to system libraries
     * @param libraries
     */
    public void addLibraryPath(String[] libraryPath)
    {
        if (this.libraryPath == null) 
        {
            this.libraryPath = libraryPath;    
        } else {
            String[] newLibraryPath = new String[libraryPath.length + this.libraryPath.length];
            System.arraycopy(this.libraryPath, 0, newLibraryPath, 0, this.libraryPath.length);
            System.arraycopy(libraryPath, 0, newLibraryPath, this.libraryPath.length, libraryPath.length);
            this.libraryPath = newLibraryPath;
        }

    }
    /**
     * @param errorStream The errorStream to set.
     */
    public void setErrorStream(PrintStream errorStream) {
        this.errorStream = errorStream;
    }
    /**
     * @param outStream The outStream to set.
     */
    public void setOutStream(PrintStream outStream) {
        this.outStream = outStream;
    }
    /**
     * Clears the library path
     */
    public void clearLibraryPath()
    {
        readSystemLibraryPath();
    }
    /**
     * Reads the system property
     */
    private void readSystemLibraryPath()
    {
        String systemLibraryPath = System.getProperty(PROPERTY_TLA_LIBRARY);
        if (systemLibraryPath != null) 
        {
            this.libraryPath = new String[1];
            this.libraryPath[0] = systemLibraryPath;
        }
    }
    /**
     * Sets the name of the root directory
     * @param name a pathname to root directory
     */
    public void setRootDirectoryName(String name)
    {
        this.projectRootPath = name;
    }
    
    public void setConfigDirectoryName(String name)
    {
        this.projectConfigPath = name;
    }
    
    public String getConfigDirectoryName()
    {
        return this.projectConfigPath;
    }
    
    /**
     * Retrieves the name of the directory the TLA files are relative to
     * @return an absolute filename ending with file separator
     */
    public String getRootDirectoryName() {
        
        return this.projectRootPath;
    }
    /**
     * Retrieves an array of system library paths
     * @return
     */
    public String[] getLibraries() 
    {
        return this.libraryPath;
    }
    /**
     * Logs an error
     * @param e
     */
    public void logError(Exception e) 
    {
        e.printStackTrace(errorStream);
    }
    /**
     * Perform parsing ?
     * @return
     */
    public boolean doParsing() 
    {
       return parsing;
    }
    /**
     * Perform semantic analysis ?
     * @return
     */
    public boolean doSemanticAnalysis() 
    {
        return semantics;
    }
    /**
     * Perfom level checking ?
     * @return
     */
    public boolean doLevelChecking() 
    {
        return levelCheck;
    }
    /**
     * Perform debugging ?
     * @return
     */
    public boolean doDebugging() {
        return debugging;
    }
    /**
     * Create stats ?
     * @return
     */
    public boolean doStats() {
        return stats;
    }
    /**
     * Set switches
     * @param properties a property set containing switches
     */
    public void setSwitches(Properties properties)
    {
        this.debugging 	= (properties.get(SWITCH_DEBUGGING) == null) 	? this.debugging 	: ((Boolean)properties.get(SWITCH_DEBUGGING)).booleanValue();
        this.stats 		= (properties.get(SWITCH_STATS) == null) 		? this.stats 		: ((Boolean)properties.get(SWITCH_STATS)).booleanValue();
        this.semantics 	= (properties.get(SWITCH_SEMANTICS) == null) 	? this.semantics 	: ((Boolean)properties.get(SWITCH_SEMANTICS)).booleanValue();
        this.levelCheck = (properties.get(SWITCH_LEVELCHECK) == null) 	? this.levelCheck 	: ((Boolean)properties.get(SWITCH_LEVELCHECK)).booleanValue();
    }
    /**
     * Retrieves out stream
     * @return
     */
    public PrintStream getOutStream() 
    {
        return this.outStream;
    }
    /**
     * Retrieves out stream
     * @return
     */
    public PrintStream getErrStream() 
    {
        return this.errorStream;
    }
    

	/**
	 * @return Returns the checkerAril.
	 */
	public int getCheckerAril()
	{
		return checkerAril;
	}
	/**
	 * @param checkerAril The checkerAril to set.
	 */
	public void setCheckerAril(int checkerAril)
	{
		this.checkerAril = checkerAril;
	}
	/**
	 * @return Returns the checkerCheckDeadlock.
	 */
	public boolean isCheckerCheckDeadlock()
	{
		return checkerCheckDeadlock;
	}
	/**
	 * @param checkerCheckDeadlock The checkerCheckDeadlock to set.
	 */
	public void setCheckerCheckDeadlock(boolean checkerCheckDeadlock)
	{
		this.checkerCheckDeadlock = checkerCheckDeadlock;
	}
	/**
	 * @return Returns the checkerCoverage.
	 */
	public int getCheckerCoverage()
	{
		return checkerCoverage;
	}
	/**
	 * @param checkerCoverage The checkerCoverage to set.
	 */
	public void setCheckerCoverage(int checkerCoverage)
	{
		this.checkerCoverage = checkerCoverage;
	}
	/**
	 * @return Returns the checkerDiffTrace.
	 */
	public int getCheckerDiffTrace()
	{
		return checkerDiffTrace;
	}
	/**
	 * @param checkerDiffTrace The checkerDiffTrace to set.
	 */
	public void setCheckerDiffTrace(int checkerDiffTrace)
	{
		this.checkerDiffTrace = checkerDiffTrace;
	}
	/**
	 * @return Returns the checkerNoWarning.
	 */
	public boolean isCheckerNoWarning()
	{
		return checkerNoWarning;
	}
	/**
	 * @param checkerNoWarning The checkerNoWarning to set.
	 */
	public void setCheckerNoWarning(boolean checkerNoWarning)
	{
		this.checkerNoWarning = checkerNoWarning;
	}
	/**
	 * @return Returns the checkerRecoverFrom.
	 */
	public boolean isCheckerUseRecoverId()
	{
		return checkerRecoverFrom;
	}
	/**
	 * @param checkerRecoverFrom The checkerRecoverFrom to set.
	 */
	public void setCheckerUseRecoverId(boolean checkerRecoverFrom)
	{
		this.checkerRecoverFrom = checkerRecoverFrom;
	}
	/**
	 * @return Returns the checkerRecoverId.
	 */
	public String getCheckerRecoverId()
	{
		return checkerRecoverId;
	}
	/**
	 * @param checkerRecoverId The checkerRecoverId to set.
	 */
	public void setCheckerRecoverId(String checkerRecoverId)
	{
		this.checkerRecoverId = checkerRecoverId;
	}
	/**
	 * @return Returns the checkerRunDepth.
	 */
	public int getCheckerRunDepth()
	{
		return checkerRunDepth;
	}
	/**
	 * @param checkerRunDepth The checkerRunDepth to set.
	 */
	public void setCheckerRunDepth(int checkerRunDepth)
	{
		this.checkerRunDepth = checkerRunDepth;
	}
	/**
	 * @return Returns the checkerSeed.
	 */
	public int getCheckerSeed()
	{
		return checkerSeed;
	}
	/**
	 * @param checkerSeed The checkerSeed to set.
	 */
	public void setCheckerSeed(int checkerSeed)
	{
		this.checkerSeed = checkerSeed;
	}
	/**
	 * @return Returns the checkerSimulateMode.
	 */
	public boolean isCheckerSimulateMode()
	{
		return checkerSimulateMode;
	}
	/**
	 * @param checkerSimulateMode The checkerSimulateMode to set.
	 */
	public void setCheckerSimulateMode(boolean checkerSimulateMode)
	{
		this.checkerSimulateMode = checkerSimulateMode;
	}
	/**
	 * @return Returns the checkerTerse.
	 */
	public boolean isCheckerTerse()
	{
		return checkerTerse;
	}
	/**
	 * @param checkerTerse The checkerTerse to set.
	 */
	public void setCheckerTerse(boolean checkerTerse)
	{
		this.checkerTerse = checkerTerse;
	}
	/**
	 * @return Returns the checkerUseAril.
	 */
	public boolean isCheckerUseAril()
	{
		return checkerUseAril;
	}
	/**
	 * @param checkerUseAril The checkerUseAril to set.
	 */
	public void setCheckerUseAril(boolean checkerUseAril)
	{
		this.checkerUseAril = checkerUseAril;
	}
	/**
	 * @return Returns the checkerUseDiffTrace.
	 */
	public boolean isCheckerUseDiffTrace()
	{
		return checkerUseDiffTrace;
	}
	/**
	 * @param checkerUseDiffTrace The checkerUseDiffTrace to set.
	 */
	public void setCheckerUseDiffTrace(boolean checkerUseDiffTrace)
	{
		this.checkerUseDiffTrace = checkerUseDiffTrace;
	}
	/**
	 * @return Returns the checkerUseSeed.
	 */
	public boolean isCheckerUseSeed()
	{
		return checkerUseSeed;
	}
	/**
	 * @param checkerUseSeed The checkerUseSeed to set.
	 */
	public void setCheckerUseSeed(boolean checkerUseSeed)
	{
		this.checkerUseSeed = checkerUseSeed;
	}
	/**
	 * @return Returns the checkerWorkerCount.
	 */
	public int getCheckerWorkerCount()
	{
		return checkerWorkerCount;
	}
	/**
	 * @param checkerWorkerCount The checkerWorkerCount to set.
	 */
	public void setCheckerWorkerCount(int checkerWorkerCount)
	{
		this.checkerWorkerCount = checkerWorkerCount;
	}
    /**
     * @return
     */
    public String getWorkDirectory() 
    {
        return this.projectWorkPath;
    }
    
    public void setWorkDirectory(String workDirectory)
    {
        this.projectWorkPath = workDirectory;
    }
    /**
     * @return
     */
    public boolean doTLAStackTrace() 
    {
        return this.doTLAStackTrace;
    }
    
    public void setTLAStackTrace(boolean doStackTrace)
    {
        this.doTLAStackTrace = doStackTrace;
    }
}

/*
 * $Log: RuntimeConfiguration.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:56  szambrovski
 * sf cvs init
 *
 * Revision 1.6  2004/10/20 15:06:05  sza
 * streams replaced
 *
 * Revision 1.5  2004/10/20 14:58:52  sza
 * System.err replaced
 *
 * Revision 1.4  2004/10/14 23:06:36  sza
 * *** empty log message ***
 *
 * Revision 1.3  2004/10/14 22:23:22  sza
 * *** empty log message ***
 *
 * Revision 1.2  2004/10/14 21:38:16  bgr
 * checker runtime conf added
 *
 * Revision 1.1  2004/10/14 20:56:59  bgr
 * configuration moved
 *
 * Revision 1.2  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
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