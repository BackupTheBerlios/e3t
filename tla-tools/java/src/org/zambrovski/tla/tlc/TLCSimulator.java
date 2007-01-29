package org.zambrovski.tla.tlc;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.TLCGlobals;
import tlc.tool.DFIDModelChecker;
import tlc.tool.ModelChecker;
import tlc.tool.Simulator;
import tlc.tool.TLCTrace;
import tlc.util.RandomGenerator;
import tlc.value.Value;
import util.FP64;
import util.UniqueString;



/**
 * TLC Simulator
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: TLCSimulator.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class TLCSimulator 
{
    private static TLCSimulator 	instance;

    private TLCSimulator()
    {
        
    }
    
    /**
     * Retrieves a working simulator copy
     * @return
     */
    public static TLCSimulator getSimulator()
    {
        if (instance == null) 
        {
            instance = new TLCSimulator();
        }
        return instance;
    }
    
    public void simulate( String launchFile, String configFile, String dumpFile )
    {
        RuntimeConfiguration config = RuntimeConfiguration.get();
        
        if (launchFile.endsWith(".tla")) 
        {
            launchFile = launchFile.substring(0, launchFile.length() - 4);
        }
        
        if (configFile == null) 
        {
            configFile = launchFile;
        } else {
            if (configFile.endsWith(".cfg")) 
            {
                configFile = configFile.substring(0, configFile.length() - 4);
            }
        }
        if (dumpFile != null && dumpFile.endsWith(".dump"))
        {
            dumpFile = dumpFile.substring(0, dumpFile.length() - 5);
        } 
        
        int fpIndex = 0;
        boolean preProcess	  = true;		// preprocess
        
        TLCTrace.setPrintDiffsOnly(config.isCheckerUseDiffTrace());			// diff trace only
        TLCGlobals.warn 			= config.isCheckerNoWarning();			// false = no warning
        TLCGlobals.metaDir 			= config.getWorkDirectory();			// working directory
        TLCGlobals.setNumWorkers(config.getCheckerWorkerCount());			// number of workers
        TLCGlobals.DFIDMax 			= -1;									// non-negative, if != -1 run MC else DFIDMC
        Value.expand = config.isCheckerTerse();								// terse
        TLCGlobals.coverageInterval = config.getCheckerCoverage() * 1000;	// coverage in seconds
        TLCGlobals.continuation 	= config.isCheckerUseRecoverId(); 		// continue

        config.getOutStream().println("Starting...");
        config.getOutStream().println("-------------");
        config.getOutStream().println("Main file   : " + launchFile);
        config.getOutStream().println("Config file : " + configFile);
        config.getOutStream().println("Dump file   : " + ((dumpFile == null) ? "no set" : dumpFile));
        config.getOutStream().println("-------------");
        config.getOutStream().println("Root Dir    : " + config.getRootDirectoryName());
        config.getOutStream().println("Config Dir  : " + config.getConfigDirectoryName());
        config.getOutStream().println("Work Dir    : " + config.getWorkDirectory());
        config.getOutStream().println("-------------");
        
        try 
        {
            
	        if (config.getCheckerRecoverId() != null) 
	        {
	          // We must recover the intern var table as early as possible
	          UniqueString.internTbl.recover(config.getCheckerRecoverId());
	        }
	
	        
	        // initialize
	        FP64.Init(fpIndex);
	        
	        if (config.isCheckerSimulateMode())
	        {
                RandomGenerator randomGenerator = new RandomGenerator();
                long seed;
                if (config.isCheckerUseSeed()) 
                {
                    seed = config.getCheckerSeed();
                    if (config.isCheckerUseAril())
                    {
                        randomGenerator.setSeed(seed, config.getCheckerAril());    
                    } else {
                        randomGenerator.setSeed(seed);  
                    }
                    
                } else 
                {
                    seed = randomGenerator.nextLong();
                    randomGenerator.setSeed(seed);
                }
                RuntimeConfiguration.get().getOutStream().println("Running Random Simulation with seed " + seed + ".");	                
                
                Simulator simulator = new Simulator( launchFile, configFile, dumpFile, config.isCheckerCheckDeadlock(), config.getCheckerRunDepth(), Long.MAX_VALUE, randomGenerator, seed);
                simulator.simulate();
	        } else {
    	        if (TLCGlobals.DFIDMax == -1) 
                {
                    ModelChecker 		mc = new ModelChecker( launchFile, configFile, dumpFile, config.isCheckerCheckDeadlock(), config.getCheckerRecoverId(), preProcess);
                    mc.modelCheck();
                    
                } else 
                {
                    DFIDModelChecker	mc = new DFIDModelChecker(launchFile, configFile, dumpFile, config.isCheckerCheckDeadlock(), config.getCheckerRecoverId(), preProcess);
                    mc.modelCheck();
                    
                }
	        }
	        config.getOutStream().println("-------------");    
            
            
        } catch ( Exception e )
        {
            e.printStackTrace(config.getErrStream());
        }
    }
    
}

/*
 * $Log: TLCSimulator.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:56  szambrovski
 * sf cvs init
 *
 * Revision 1.7  2004/10/20 14:58:53  sza
 * System.err replaced
 *
 * Revision 1.6  2004/10/14 23:06:36  sza
 * *** empty log message ***
 *
 * Revision 1.5  2004/10/14 22:18:51  sza
 * runtime configuration usage
 *
 * Revision 1.4  2004/10/14 20:56:59  bgr
 * configuration moved
 *
 * Revision 1.3  2004/10/14 20:52:31  bgr
 * checker running
 *
 * Revision 1.2  2004/10/13 17:14:29  bgr
 * launcher buil
 *
 * Revision 1.1  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
 *
 *
 */