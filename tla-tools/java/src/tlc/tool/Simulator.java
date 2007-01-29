// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu Jan 10 11:22:26 PST 2002 by yuanyu

package tlc.tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.semantic.SemanticNode;
import tlc.TLCGlobals;
import tlc.tool.liveness.LiveCheck1;
import tlc.tool.liveness.LiveException;
import tlc.util.ObjLongTable;
import tlc.util.RandomGenerator;

public class Simulator {
  /* Constructors  */
  public Simulator(
          String specFile, 
          String configFile, 
          String traceFile,
		  boolean deadlock, 
		  int traceDepth, 
		  long traceNum,
		  RandomGenerator rng, 
		  long seed) 
  {
    this(	specFile, 
            configFile, 
            traceFile, 
            deadlock, 
            traceDepth,
            traceNum, 
            rng, 
            seed, 
            true);
  }
  public Simulator(
          String specFile, 
          String configFile, 
          boolean deadlock,
		  int traceDepth, 
		  RandomGenerator rng, 
		  long seed) 
  {
      this(	specFile, 
              configFile, 
              null, 
              deadlock, 
              traceDepth,
              Long.MAX_VALUE, 
              rng, 
              seed
      );
  }
  
  private Simulator(
          String specFile, 
          String configFile, 
          String traceFile,
		  boolean deadlock, 
		  int traceDepth, 
		  long traceNum,
		  RandomGenerator rng, 
		  long seed, 
		  boolean preprocess) 
  {
    int lastSep = specFile.lastIndexOf(File.separatorChar);
    String specDir = (lastSep == -1) ? "" : specFile.substring(0, lastSep+1);
    specFile = specFile.substring(lastSep+1);
    this.tool = new Tool(specDir, specFile, configFile);
    this.tool.init(preprocess);   // parse and process the spec

    this.checkDeadlock = deadlock;
    this.checkLiveness = !this.tool.livenessIsTrue();
    this.actions = this.tool.getActions();
    this.invariants = this.tool.getInvariants();
    this.impliedActions = this.tool.getImpliedActions();
    this.numOfGenStates = 0;
    if (traceDepth != -1) 
    {
      this.stateTrace = new TLCState[traceDepth];
      this.actionTrace = new Action[traceDepth];
      this.traceDepth = traceDepth;
    }
    else {
      this.stateTrace = new TLCState[0];
      this.actionTrace = new Action[0];      
      this.traceDepth = Long.MAX_VALUE;
    }
    this.traceFile = traceFile;
    this.traceNum = traceNum;
    this.rng = rng;
    this.seed = seed;
    this.aril = 0;
    this.astCounts = new ObjLongTable(10);
    // Initialization for liveness checking
    if (this.checkLiveness) 
    {
        LiveCheck1.initSim(this.tool);
    }
  }


  /* Fields */
  private Tool tool;
  private Action[] actions;          // the subactions
  private Action[] invariants;       // the invariants to be checked
  private Action[] impliedActions;   // the implied-actions to be checked  
  private boolean checkDeadlock;     // check deadlock?
  private boolean checkLiveness;     // check liveness?
  private long numOfGenStates;
  private TLCState[] stateTrace;
  private Action[] actionTrace;
  private String traceFile;
  private long traceDepth;
  private long traceNum;
  private RandomGenerator rng;
  private long seed;
  private long aril;
  private ObjLongTable astCounts;
  
  /*
   * This method does simulation on a TLA+ spec. Its argument specifies
   * the main module of the TLA+ spec.
   */
  public void simulate() throws Exception {
    StateVec theInitStates = null;
    TLCState curState = null;

    // Compute the initial states:
    try {
      theInitStates = this.tool.getInitStates();
      this.numOfGenStates = theInitStates.size();
      for (int i = 0; i < theInitStates.size(); i++) {
	curState = theInitStates.elementAt(i);
	if (this.tool.isGoodState(curState)) {
	  for (int j = 0; j < this.invariants.length; j++) {
	    if (!this.tool.isValid(this.invariants[j], curState)) {
	      // We get here because of invariant violation:
	      RuntimeConfiguration.get().getErrStream().println("Error: Invariant " + this.tool.getInvNames()[j] +
				 " is violated by the initial state:");
	      RuntimeConfiguration.get().getErrStream().println(curState.toString());
	      return;
	    }
	  }
	}
	else {
	  RuntimeConfiguration.get().getErrStream().println("Error: The state is not completely specified by " +
			     "the initial state predicate:");	  
	  RuntimeConfiguration.get().getErrStream().println(curState.toString());
	  return;
	}
      }
    }
    catch (Exception e) {
      // e.printStackTrace();
      RuntimeConfiguration.get().getErrStream().println(e.getMessage());
      if (curState != null) {
	RuntimeConfiguration.get().getErrStream().println("While working on the initial state:");
	RuntimeConfiguration.get().getErrStream().println(curState.toString());
      }
      this.printSummary();
      return;
    }
    if (this.numOfGenStates == 0) {
      RuntimeConfiguration.get().getErrStream().println("Error: There is no state satisfying the initial state predicate.");
      return;
    }
    theInitStates.deepNormalize();

    // Start progress report thread:
    ProgressReport report = new ProgressReport();
    report.start();

    // Start simulating:
    int traceIdx = 0;
    int idx = 0;
    try {
      for (int traceCnt = 1; traceCnt <= this.traceNum; traceCnt++) {
	traceIdx = 0;
	this.aril = rng.getAril();
	curState = this.randomState(theInitStates);
	boolean inConstraints = this.tool.isInModel(curState);
	
	while (traceIdx < this.traceDepth) {
	  if (traceIdx < this.stateTrace.length) {
	    this.stateTrace[traceIdx] = curState;
	    traceIdx++;
	  }

	  if (!inConstraints) break;

	  StateVec nextStates = this.randomNextStates(curState);
	  if (nextStates == null) {
	    if (this.checkDeadlock) {
	      // We get here because of deadlock:
	      this.printBehavior(curState, traceIdx, "Error: Deadlock reached.");	    
	      return;
	    }
	    break;	    
	  }
	  for (int i = 0; i < nextStates.size(); i++) {
	    this.numOfGenStates++;
	    TLCState state = nextStates.elementAt(i);

	    if (TLCGlobals.coverage) {
	      ((TLCStateMutSource)state).addCounts(this.astCounts);
	    }

	    if (this.tool.isGoodState(state)) {
	      try {
		for (idx = 0; idx < this.invariants.length; idx++) {
		  if (!this.tool.isValid(this.invariants[idx], state)) {
		    // We get here because of invariant violation:
		    String msg = "Error: Invariant "+ this.tool.getInvNames()[idx] + " is violated.";
		    this.printBehavior(state, traceIdx, msg);
		    return;
		  }
		}
	      }
	      catch (Exception e) {
		String msg = "Error: Evaluating invariant " + this.tool.getInvNames()[idx] +
		  " failed. " + e.getMessage();
		this.printBehavior(state, traceIdx, msg);
		return;
	      }
	      try {
		for (idx = 0; idx < this.impliedActions.length; idx++) {
		  if (!this.tool.isValid(this.impliedActions[idx], curState, state)) {
		    // We get here because of implied-action violation:
		    String msg = "Error: Implied-action " + this.tool.getImpliedActNames()[idx] +
		      " is violated. The behavior up to this point is:";
		    this.printBehavior(state, traceIdx, msg);
		    return;
		  }
		}
	      }
	      catch (Exception e) {
		String msg = "Error: Evaluating implied-action " +
		  this.tool.getImpliedActNames()[idx] + " failed. " + e.getMessage();
		this.printBehavior(state, traceIdx, msg);
		return;
	      }
	    }
	    else {
	      String msg = "Error: Successor state is not completely specified " +
		"by the next-state action:";
	      this.printBehavior(state, traceIdx, msg);
	      return;
	    }
	  }
	  TLCState s1 = this.randomState(nextStates);
	  inConstraints = (this.tool.isInModel(s1) &&
			   this.tool.isInActions(curState, s1));
	  curState = s1;
	}

	// Check if the current trace satisfies liveness properties.
        if (this.checkLiveness) {
          LiveCheck1.checkTrace(stateTrace, traceIdx);
        }

	// Write the trace out if desired.  The trace is printed in the
	// format of TLA module, so that it can be read by TLC again. 
	if (this.traceFile != null) {
	  String fileName = this.traceFile + traceCnt;
	  FileOutputStream fos = new FileOutputStream(fileName);
	  PrintWriter pw = new PrintWriter(fos);
	  pw.println("---------------- MODULE " + fileName + " -----------------");
	  for (idx = 0; idx < traceIdx; idx++) {
	    pw.println("STATE_" + (idx+1) + " == ");
	    pw.println(this.stateTrace[idx] + "\n");
	  }
	  pw.println("=================================================");	  
	  pw.close();
	  fos.close();
	}
      }
    }
    catch (Throwable e) {
      // e.printStackTrace();      
      if (e instanceof LiveException) {
	this.printSummary();
      }
      else {
	this.printBehavior(curState, traceIdx, e.getMessage());
      }
    }
  }
  
  /**
   * Prints out the simulation behavior, in case of an error.
   * (unless we're at maximum depth, in which case don't!)
   */
  public final void printBehavior(TLCState state, int traceIdx, String msg) {
    RuntimeConfiguration.get().getErrStream().println(msg);
    if (this.traceDepth == Long.MAX_VALUE) {
      RuntimeConfiguration.get().getErrStream().println("The error state is: ");
      RuntimeConfiguration.get().getErrStream().println(state.toString());
    }
    else {
      RuntimeConfiguration.get().getErrStream().println("The behavior up to this point is:");
      for (int idx = 0; idx < traceIdx; idx++) {
	RuntimeConfiguration.get().getErrStream().println("STATE " + (idx+1) + ": ");	
        RuntimeConfiguration.get().getErrStream().println(this.stateTrace[idx]);
      }
      RuntimeConfiguration.get().getErrStream().println("STATE " + (traceIdx+1) + ": ");      
      RuntimeConfiguration.get().getErrStream().println(state.toString());
    }
    this.printSummary();
  }

  /**
   * This method returns a state that is randomly chosen from the set
   * of states.  It returns null if the set of states is empty.
   */
  public final TLCState randomState(StateVec states) throws EvalException {
    int len = states.size();
    if (len > 0) {
      int index = (int)Math.floor(this.rng.nextDouble() * len);
      return states.elementAt(index);
    }
    return null;
  }

  /**
   * This method returns the set of next states generated by a randomly
   * chosen action.  It returns null if there is no possible next state.
   */
  public final StateVec randomNextStates(TLCState state) {
    int len = this.actions.length;
    int index = (int)Math.floor(this.rng.nextDouble() * len);
    int p = this.rng.nextPrime();
    for (int i = 0; i < len; i++) {
      StateVec pstates = this.tool.getNextStates(this.actions[index], state);
      if (!pstates.empty()) {
	return pstates;
      }
      index = (index + p) % len;
    }
    return null;
  }
    
  public final void printSummary() {
    this.reportCoverage();
    RuntimeConfiguration.get().getOutStream().println("The number of states generated: " + this.numOfGenStates);
    RuntimeConfiguration.get().getOutStream().println("Simulation using seed " + seed + " and aril " + this.aril);
  }

  public final void reportCoverage() {
    if (TLCGlobals.coverage) {
      RuntimeConfiguration.get().getOutStream().println("The coverage stats:");
      ObjLongTable counts = this.tool.getPrimedLocs();
      ObjLongTable.Enumerator keys = this.astCounts.keys();
      Object key;
      while ((key = keys.nextElement()) != null) {
	String loc = ((SemanticNode)key).getLocation().toString();	
	counts.add(loc, astCounts.get(key));
      }
      Object[] skeys = counts.sortStringKeys();
      for (int i = 0; i < skeys.length; i++) {
	long val = counts.get(skeys[i]);
	RuntimeConfiguration.get().getOutStream().println("  " + skeys[i] + ": " + val);
      }
    }
  }

  final class ProgressReport extends Thread {
    public void run() {
      int count = TLCGlobals.coverageInterval/TLCGlobals.progressInterval;
      try {
	while (true) {
	  synchronized(this) { this.wait(TLCGlobals.progressInterval); }
	  RuntimeConfiguration.get().getOutStream().println("Progress: " + numOfGenStates + " states checked.");
	  if (count > 1) {
	    count--;
	  }
	  else {
	    reportCoverage();
	    count = TLCGlobals.coverageInterval/TLCGlobals.progressInterval;
	  }
	}
      }
      catch (Exception e) {
	RuntimeConfiguration.get().getErrStream().println("Error: Progress report thread died.");
      }
    }
  }

}
