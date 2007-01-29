// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed Dec  5 22:37:20 PST 2001 by yuanyu 

package tlc;

import java.io.File;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.tool.DFIDModelChecker;
import tlc.tool.ModelChecker;
import tlc.tool.Simulator;
import tlc.tool.TLCTrace;
import tlc.util.FileUtil;
import tlc.util.RandomGenerator;
import tlc.value.Value;
import util.FP64;
import util.UniqueString;

public class TLC {
  /*
   * This TLA checker (TLC) provides the following functionalities:
   *  1. Simulation of TLA+ specs: java tlc.TLC -simulate spec[.tla]
   *  2. Model checking of TLA+ specs: java tlc.TLC [-modelcheck] spec[.tla]
   *
   * The command line also provides the following options:
   *  o -config file: provide the config file.
   *    Defaults to spec.cfg if not provided
   *  o -deadlock: do not check for deadlock.
   *    Defaults to checking deadlock if not specified
   *  o -depth num: specify the depth of random simulation 
   *    Defaults to 100 if not specified
   *  o -seed num: provide the seed for random simulation
   *    Defaults to a random seed if not specified
   *  o -aril num: Adjust the seed for random simulation
   *    Defaults to 0 if not specified
   *  o -recover path: recover from the checkpoint at path
   *    Defaults to scratch run if not specified
   *  o -metadir path: store metadata in the directory at path
   *    Defaults to specdir/states if not specified
   *  o -workers num: the number of TLC worker threads
   *    Defaults to 1
   *  o -dfid num: use depth-first iterative deepening with initial depth num
   *  o -cleanup: clean up the states directory
   *  o -dump file: dump all the states into file
   *  o -difftrace: when printing trace, show only
   *                the differences between successive states
   *    Defaults to printing full state descriptions if not specified
   *    (Added by Rajeev Joshi)
   *  o -terse: do not expand values in Print statement
   *    Defaults to expand value if not specified
   *  o -coverage seconds: collect coverage information on the spec,
   *                       print out the information every seconds
   *    Defaults to no coverage if not specified
   *  o -continue: continue running even when invariant is violated
   *    Defaults to stop at the first violation if not specified
   *  o -nowarning: disable all the warnings
   *    Defaults to report warnings if not specified
   *  o -fp num: use the num'th irreducible polynomial from the list
   *    stored in the class FP64.
   */
  public static void main(String[] args) {
    RuntimeConfiguration.get().getOutStream().println("TLC " + TLCGlobals.versionOfTLC);

    String mainFile = null;
    String configFile = null;
    boolean deadlock = true;
    boolean cleanup = false;
    String dumpFile = null;    
    int traceDepth = 100;
    boolean noSeed = true;
    long seed = 0;
    long aril = 0;
    int fpIndex = 0;
    String fromChkpt = null;
    int typeOfCheck = 2;    // Default to model checking

    int index = 0;
    while (index < args.length) {
      if (args[index].equals("-simulate")) 
      {
          if (typeOfCheck > 1) typeOfCheck = 1;
          index++;
      } else if (args[index].equals("-modelcheck")) 
      {
          index++;
      } else if (args[index].equals("-config")) 
      {
          index++;
          if (index < args.length) 
          {
              configFile = args[index];
              int len = configFile.length();
              if (configFile.startsWith(".cfg", len-4)) 
              {
                  configFile = configFile.substring(0, len-4);
              }
              index++;
          } else {
              printErrorMsg("Error: expect a file name for -config option.");
              return;
          }
      } else if (args[index].equals("-difftrace")) 
      {
          index++;
          TLCTrace.setPrintDiffsOnly() ;
      } else if (args[index].equals("-deadlock")) 
      {
          index++;
          deadlock = false;
      } else if (args[index].equals("-cleanup")) 
      {
          index++;
          cleanup = true;
      } else if (args[index].equals("-nowarning")) 
      {
          index++;
          TLCGlobals.warn = false;
      } else if (args[index].equals("-dump")) 
      {
          index++;
          if (index < args.length) 
          {
              dumpFile = args[index];
              int len = dumpFile.length();
              if (!(dumpFile.startsWith(".dump", len-5))) 
              {
                  dumpFile = dumpFile + ".dump";
              }
              index++;
          } else 
          {
              printErrorMsg("Error: A file name for dumping states required.");
              return;
          }
      } else if (args[index].equals("-depth")) 
      {
          index++;
          if (index < args.length) 
          {
              try 
              {
                  traceDepth = Integer.parseInt(args[index]);
                  index++;
              } catch (Exception e) 
              {
                  printErrorMsg("Error: An integer for trace length required. But encountered " +
                          args[index]);
                  return;
              }
          } else 
          {
              printErrorMsg("Error: trace length required.");
              return;
          }
      } else if (args[index].equals("-seed")) 
      {
        index++;
        if (index < args.length) 
        {
          try 
          {
            seed = Long.parseLong(args[index]);
            index++;
            noSeed = false;
          } catch (Exception e) 
          {
            printErrorMsg("Error: An integer for seed required. But encountered " +
			  args[index]);
            return;
          }
        } else {
          printErrorMsg("Error: seed required.");
          return;
        }
      } else if (args[index].equals("-aril")) 
      {
        index++;
        if (index < args.length) {
          try {
            aril = Long.parseLong(args[index]);
            index++;
          }
          catch (Exception e) {
            printErrorMsg("Error: An integer for aril required. But encountered " +
			  args[index]);
            return;
          }
        } else 
        {
          printErrorMsg("Error: aril required.");
          return;
        }
      } else if (args[index].equals("-recover")) 
      {
        index++;
        if (index < args.length) {
          fromChkpt = args[index++] + File.separator;
        } else 
        {
            printErrorMsg("Error: need to specify the metadata directory for recovery.");
            return;
        }
      } else if (args[index].equals("-metadir")) 
      {
        index++;
        if (index < args.length) {
          TLCGlobals.metaDir = args[index++] + File.separator;
        } else {
            printErrorMsg("Error: need to specify the metadata directory.");
            return;
        }
      } else if (args[index].equals("-workers")) {
        index++;
        if (index < args.length) 
        {
          try 
          {
              int num = Integer.parseInt(args[index]);
              if (num < 1) 
              {
                  printErrorMsg("Error: at least one worker required.");
                  return;
              }
              TLCGlobals.setNumWorkers(num);
              index++;
          } catch (Exception e) {
            printErrorMsg("Error: worker number required. But encountered " +
			  args[index]);
            return;
          }
        } else {
          printErrorMsg("Error: expect an integer for -workers option.");
          return;
        }
      } else if (args[index].equals("-dfid")) 
      {
          index++;
          if (index < args.length) 
          {
			  try 
			  {
			    int d = Integer.parseInt(args[index]);
			    if (d < 0) 
			    {
			      printErrorMsg("Error: expect a nonnegative integer for -dfid option.");
			      return;
			    }
		        TLCGlobals.DFIDMax = d;
		        index++;
		      } catch (Exception e) 
		      {
		            printErrorMsg("Errorexpect a nonnegative integer for -dfid option. " +
					  "But encountered " + args[index]);
		            return;
		      }
          } else 
          {
              printErrorMsg("Error: expect a nonnegative integer for -dfid option.");
              return;
          }
      } else if (args[index].equals("-terse")) 
      {
          index++;
          Value.expand = false;
      } else if (args[index].equals("-coverage")) 
      {
          index++;
          TLCGlobals.coverage = true;
          if (index < args.length) 
          {
              try 
              {
                  TLCGlobals.coverageInterval = Integer.parseInt(args[index]) * 60000;
                  index++;
              } catch (Exception e) 
              {
                  printErrorMsg("Error: An integer for coverage report interval required." +
                          " But encountered " + args[index]);
                  return;
              }
          } else 
          {
              printErrorMsg("Error: coverage report interval required.");
              return;
          }
      } else if (args[index].equals("-continue")) 
      {
          index++;
          TLCGlobals.continuation = true;
      } else if (args[index].equals("-fp")) 
      {
          index++;
          if (index < args.length) 
          {
              try 
              {
                  fpIndex = Integer.parseInt(args[index]);
                  if (fpIndex < 0 || fpIndex >= FP64.Polys.length) 
                  {
                      printErrorMsg("Error: The number for -fp must be between 0 and " +
                              (FP64.Polys.length-1) + " (inclusive).");
                      return;
                  }
                  index++;
              } catch (Exception e) 
              {
                  printErrorMsg("Error: A number for -fp is required. But encountered " +
                          args[index]);
                  return;
              }
          } else 
          {
              printErrorMsg("Error: expect an integer for -workers option.");
              return;
          }
      } else 
      {
        if (args[index].charAt(0) == '-') {
          printErrorMsg("Error: unrecognized option: " + args[index]);
          return;
        }
        if (mainFile != null) 
        {
          printErrorMsg("Error: more than one input files: " + mainFile +
			" and " + args[index]);
          return;
        }
        mainFile = args[index++];
        int len = mainFile.length();
        if (mainFile.startsWith(".tla", len-4)) 
        {
          mainFile = mainFile.substring(0, len-4);
        }
      }
    }
    
    if (mainFile == null) 
    {
      printErrorMsg("Error: Missing input TLA+ module.");
      return;
    }
    if (configFile == null) configFile = mainFile;

    try 
    {
      // Initialize:
      if (fromChkpt != null) 
      {
        // We must recover the intern var table as early as possible
        UniqueString.internTbl.recover(fromChkpt);
      }
      if (cleanup && fromChkpt == null) 
      {
        // clean up the states directory only when not recovering
        FileUtil.deleteDir(new File(TLCGlobals.metaRoot), true);
      }
      FP64.Init(fpIndex);
      
      // Start checking:
      switch (typeOfCheck) 
      {
      case 1:
      {
          // random simulation
          RandomGenerator rng = new RandomGenerator();
          if (noSeed) 
          {
              seed = rng.nextLong();
              rng.setSeed(seed);
          } else 
          {
              rng.setSeed(seed, aril);
          }
          
          RuntimeConfiguration.get().getOutStream().println("Running Random Simulation with seed " + seed + ".");
          Simulator simulator = new Simulator(mainFile, configFile, deadlock,
					      traceDepth, rng, seed);
          simulator.simulate();
          break;
      }
      case 2:
      {
          // model checking
          RuntimeConfiguration.get().getOutStream().println("Model-checking");
          if (TLCGlobals.DFIDMax == -1) 
          {
              ModelChecker mc = new ModelChecker(mainFile, configFile, dumpFile,
					       deadlock, fromChkpt, true);
              mc.modelCheck();
          } else 
          {
              DFIDModelChecker mc = new DFIDModelChecker(mainFile, configFile, dumpFile,
						       deadlock, fromChkpt, true);
              mc.modelCheck();
          }
          break;
      }
    } // end switch
 } catch (Throwable e) 
 {
      System.gc();
      // e.printStackTrace();
      if (e instanceof StackOverflowError) {
        RuntimeConfiguration.get().getErrStream().println("Error: This was a Java StackOverflowError. It was probably\n" +
                           "the result of an incorrect recursive function definition\n" +
                           "that caused TLC to enter an infinite loop when trying to\n" +
                           "compute the function or its application to an element in\n" +
                           "its putative domain.");
      }
      else if (e instanceof OutOfMemoryError) {
	RuntimeConfiguration.get().getErrStream().println("Error: Java ran out of memory.  Running Java with a larger memory\n" +
			   "allocation pool (heap) may fix this.  But it won't help if some\n" +
			   "state has an enormous number of successor states, or if TLC must\n" +
			   "compute the value of a huge set.");
      }
      else {
	RuntimeConfiguration.get().getErrStream().println("Error: " + e.getMessage());
      }
    }
  }

  private static void printErrorMsg(String msg) {
    RuntimeConfiguration.get().getErrStream().println(msg);
    RuntimeConfiguration.get().getErrStream().println("Usage: java tlc.TLC [-option] inputfile");
  }

}
