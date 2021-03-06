// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Jul 27 10:47:59 PDT 2001 by yuanyu   

package tlc.tool;

import java.io.File;
import java.io.IOException;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.TLCGlobals;
import tlc.value.Value;
import util.FP64;
import util.UniqueString;

public class TLCApp extends DistApp {

  /* Constructors  */
  public TLCApp(String specFile, String configFile, boolean deadlock,
		String fromChkpt)
  throws IOException {
    this(specFile, configFile, deadlock, fromChkpt, true);
  }
  
  public TLCApp(String specFile, String configFile, boolean deadlock,
		String fromChkpt, boolean preprocess)
  throws IOException {
    int lastSep = specFile.lastIndexOf(File.separatorChar);
    String specDir = (lastSep == -1) ? "" : specFile.substring(0, lastSep+1);
    specFile = specFile.substring(lastSep+1);
    this.tool = new Tool(specDir, specFile, configFile);
    this.tool.init(preprocess);
    this.checkDeadlock = deadlock;
    this.preprocess = preprocess;
    this.impliedInits = this.tool.getImpliedInits();
    this.invariants = this.tool.getInvariants();
    this.impliedActions = this.tool.getImpliedActions();
    this.actions = this.tool.getActions();
    this.fromChkpt = fromChkpt;
    this.metadir = ModelChecker.makeMetaDir(specDir, fromChkpt);
  }

  public TLCApp(String specDir, String specFile, String configFile,
		Boolean deadlock, Boolean preprocess)
  throws IOException {
    this.tool = new Tool(specDir, specFile, configFile);
    this.checkDeadlock = deadlock.booleanValue();
    this.preprocess = preprocess.booleanValue();
    this.tool.init(this.preprocess);    
    this.impliedInits = this.tool.getImpliedInits();
    this.invariants = this.tool.getInvariants();
    this.impliedActions = this.tool.getImpliedActions();
    this.actions = this.tool.getActions();
  }

  /* Fields  */
  public Tool tool;
  public Action[] invariants;        // the invariants to be checked
  public Action[] impliedInits;      // the implied-inits to be checked
  public Action[] impliedActions;    // the implied-actions to be checked
  public Action[] actions;           // the subactions
  private boolean checkDeadlock;     // check deadlock?
  private boolean preprocess;        // preprocess?
  private String fromChkpt = null;   // recover from this checkpoint
  private String metadir = null;     // the directory pathname for metadata
  
  public final String getAppName() { return "tlc.tool.TLCApp"; }

  public final Boolean getCheckDeadlock() {
    return new Boolean(this.checkDeadlock);
  }

  public final Boolean getPreprocess() {
    return new Boolean(this.preprocess);
  }
  
  public final String getFileName() { return this.tool.rootFile; }

  public final String getMetadir() { return this.metadir; }

  public final boolean canRecover() { return this.fromChkpt != null; }  

  public final TLCState[] getInitStates() throws WorkerException {
    StateVec theInitStates = this.tool.getInitStates();
    TLCState[] res = new TLCState[theInitStates.size()];
    for (int i = 0; i < theInitStates.size(); i++) {
      TLCState curState = theInitStates.elementAt(i);
      if (!this.tool.isGoodState(curState)) {
	String msg = "Error: Initial state is not completely specified by the" +
	  " initial predicate.";
	throw new WorkerException(msg, curState, null, false);
      }
      res[i] = (TLCState)curState;
    }
    return res;
  }

  public final TLCState[] getNextStates(TLCState curState) throws WorkerException {
    StateVec nextStates = new StateVec(10);
    for (int i = 0; i < this.actions.length; i++) {
      Action curAction = this.actions[i];
      StateVec nstates = this.tool.getNextStates(curAction, (TLCState)curState);
      nextStates = nextStates.addElements(nstates);
    }
    int len = nextStates.size();
    if (len == 0 && this.checkDeadlock) {
      throw new WorkerException("Error: deadlock reached.", curState, null, false);
    }
    TLCState[] res = new TLCState[nextStates.size()];
    for (int i = 0; i < nextStates.size() ; i++) {
      TLCState succState = nextStates.elementAt(i) ;
      if (!this.tool.isGoodState(succState)) {
	String msg = "Error: Successor state is not completely specified by" +
	  " the next-state action.";
	throw new WorkerException(msg, curState, succState, false);
      }
      res[i] = succState;
    }
    return res;
  }

  public final void checkState(TLCState s1, TLCState s2) throws WorkerException {
    TLCState ts2 = (TLCState)s2;
    for (int i = 0; i < this.invariants.length; i++) {
      if (!tool.isValid(this.invariants[i], ts2)) {
	// We get here because of invariant violation:
	String msg = "Error: Invariant " + this.tool.getInvNames()[i] + " is violated.";
	throw new WorkerException(msg, s1, s2, false);
      }
    }
    if (s1 == null) {
      for (int i = 0; i < this.impliedInits.length; i++) {
	if (!this.tool.isValid(this.impliedInits[i], ts2)) {
	  // We get here because of implied-inits violation:
	  String msg = "Error: Implied-init " + this.tool.getImpliedInitNames()[i] +
	    " is violated.";
	  throw new WorkerException(msg, s1, s2, false);
	}
      }
    }
    else {
      TLCState ts1 = (TLCState)s1;
      for (int i = 0; i < this.impliedActions.length; i++) {
	if (!tool.isValid(this.impliedActions[i], ts1, ts2)) {
	  // We get here because of implied-action violation:
	  String msg = "Error: Implied-action " + this.tool.getImpliedActNames()[i] +
	    " is violated.";
	  throw new WorkerException(msg, s1, s2, false);
	}
      }
    }
  }

  public final boolean isInModel(TLCState s) {
    return this.tool.isInModel((TLCState)s);
  }


  public final boolean isInActions(TLCState s1, TLCState s2) {
    return this.tool.isInActions((TLCState)s1, (TLCState)s2);
  }

  /* Reconstruct the initial state whose fingerprint is fp. */
  public final TLCStateInfo getState(long fp) {
    return this.tool.getState(fp);
  }

  /* Reconstruct the next state of state s whose fingerprint is fp. */
  public final TLCStateInfo getState(long fp, TLCState s) {
    return this.tool.getState(fp, s);
  }

  /* Reconstruct the info for the transition from s to s1. */
  public TLCStateInfo getState(TLCState s1, TLCState s) {
    return this.tool.getState(s1, s);
  }

  public final void setCallStack() { this.tool.setCallStack(); }

  public final void printCallStack() {
    this.tool.printCallStack();
  }

  public static TLCApp create(String args[]) throws IOException {
    String specFile = null;
    String configFile = null;
    boolean deadlock = true;
    int fpIndex = 0;
    String fromChkpt = null;
    
    int index = 0;
    while (index < args.length) {
      if (args[index].equals("-config")) {
	index++;
	if (index < args.length) {
	  configFile = args[index];
	  int len = configFile.length();
	  if (configFile.startsWith(".cfg", len-4)) {
	    configFile = configFile.substring(0, len-4);
	  }
	  index++;
	}
	else {
	  printErrorMsg("Error: configuration file required.");
	  System.exit(0);
	}
      }
      else if (args[index].equals("-deadlock")) {
	index++;
	deadlock = false;
      }
      else if (args[index].equals("-recover")) {
        index++;
        if (index < args.length) {
          fromChkpt = args[index++] + File.separator;
        }
        else {
	  printErrorMsg("Error: need to specify the metadata directory for recovery.");
          System.exit(0);
        }
      }
      else if (args[index].equals("-coverage")) {
	index++;
	TLCGlobals.coverage = true;
	if (index < args.length) {
	  try {
	    TLCGlobals.coverageInterval = Integer.parseInt(args[index]) * 1000;
	    index++;
	  }
	  catch (Exception e) {
	    printErrorMsg("Error: An integer for coverage report interval required." +
			  " But encountered " + args[index]);
	    System.exit(0);
	  }
	}
	else {
	  printErrorMsg("Error: coverage report interval required.");
	  System.exit(0);
	}
      }
      else if (args[index].equals("-terse")) {
	index++;
	Value.expand = false;
      }
      else if (args[index].equals("-nowarning")) {
	index++;
	TLCGlobals.warn = false;
      }
      else if (args[index].equals("-fp")) {
	index++;
        if (index < args.length) {
          try {
	    fpIndex = Integer.parseInt(args[index]);
	    if (fpIndex < 0 || fpIndex >= FP64.Polys.length) {
	      printErrorMsg("Error: The number for -fp must be between 0 and " +
			    (FP64.Polys.length-1) + " (inclusive).");
	      System.exit(0);
	    }
            index++;
          }
          catch (Exception e) {
            printErrorMsg("Error: A number for -fp is required. But encountered " +
			  args[index]);
            System.exit(0);
          }
        }
        else {
          printErrorMsg("Error: expect an integer for -workers option.");
          System.exit(0);
        }
      }
      else {
	if (args[index].charAt(0) == '-') {
	  printErrorMsg("Error: unrecognized option: " + args[index]);
	  System.exit(0);
	}
	if (specFile != null) {
	  printErrorMsg("Error: more than one input files: " + specFile
			+ " and " + args[index]);
	  System.exit(0);
	}
	specFile = args[index++];
	int len = specFile.length();
	if (specFile.startsWith(".tla", len-4)) {
	  specFile = specFile.substring(0, len-4);
	}
      }
    }
    
    if (specFile == null) {
      printErrorMsg("Error: Missing input TLA+ module.");
      System.exit(0);
    }
    if (configFile == null) configFile = specFile;

    if (fromChkpt != null) {
      // We must recover the intern table as early as possible
      UniqueString.internTbl.recover(fromChkpt);
    }
    FP64.Init(fpIndex);
    
    return new TLCApp(specFile, configFile, deadlock, fromChkpt);
  }

  private static void printErrorMsg(String msg) {
    RuntimeConfiguration.get().getErrStream().println(msg);
    RuntimeConfiguration.get().getErrStream().println("Usage: java tlc.tool.TLCServer [-option] inputfile");
  }

}
