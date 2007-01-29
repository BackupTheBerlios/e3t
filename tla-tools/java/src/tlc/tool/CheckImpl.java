// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.

package tlc.tool;

import java.io.IOException;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.TLCGlobals;

public abstract class CheckImpl extends ModelChecker {
  /**
   * CheckImpl is intended to be used with a simulation-based
   * verification engine.  It checks whether the states generated
   * during simulation are legal based on the abstract view of a
   * formal model. It also records coverage information on the
   * (partial) state space TLC is creating, and subsequently uses that
   * information to guide the simulation engine to corner cases that
   * are hard to get.
   **/
  
  private static int TraceDuration = 30000;

  public CheckImpl(String specFile, String configFile, boolean deadlock,
		   int depth, String fromChkpt)
  throws IOException {
    super(specFile, configFile, null, deadlock, fromChkpt, true);
    this.depth = depth;
    this.curState = null;
    this.coverSet = new DiskFPSet(-1);
    this.coverSet.init(TLCGlobals.getNumWorkers(), this.metadir, specFile+"_cs");
    this.stateEnum = null;
  }

  /**
   * theFPSet contains the states in the partial state space created by
   * running TLC.  coverSet contains the states obtained by calls to
   * getState.  trace is the union of theFPSet and coverSet. An uncovered
   * state is a state that is in theFPSet-coverSet.
   */
  private int depth;                      // the depth of the state space
  private long stateCnt;                  // the number of states
  private FPSet coverSet;                 // the set of covered states
  private TLCState curState;              // the current state
  private TLCTrace.Enumerator stateEnum;  // the enumerator for reachable state
  private long lastTraceTime;             // the time the last trace was generated
  
  /**
   * The main task of initialization is to create a sufficiently large
   * (probably partial) state space.  The argument depth is the
   * depth of the (partial) state space we want to create. 
   */
  public final void init() throws Throwable {
    boolean recovered = this.recover();
    if (!recovered) {
      this.doInit();
    }
    RuntimeConfiguration.get().getOutStream().println("Creating a partial state space of depth " +
		       this.depth + " ...");
    if (!this.runTLC(this.depth)) { System.exit(1); }
    this.lastTraceTime = System.currentTimeMillis();
    this.stateEnum = this.trace.elements();    
  }

  /* Reset the internal state of CheckImpl.  */
  public final void reset() throws IOException {
    this.curState = null;
    this.stateEnum.reset(-1);
  }

  /**
   * Creates a (partial) state space with st as the root and
   * depth as the depth.
   */
  public final void makeStateSpace(TLCState st, int depth) throws Exception {
    int depth1= this.trace.getLevel(st.uid) + depth;
    this.theStateQueue = new DiskStateQueue(this.metadir);
    this.theStateQueue.enqueue(st);
    if (!this.runTLC(depth1)) { System.exit(1); }
  }
  
  /**
   * This method gets a new state from the external world.
   * It returns null if there is nothing available.
   */
  public abstract TLCState getState();

  /* This method exports a trace to the external world.  */
  public abstract void exportTrace(TLCStateInfo[] trace) throws IOException;
  
  /* Returns true iff s1 is reachable from s0 (s0 -+-> s1). */
  public final boolean checkReachability(TLCState s0, TLCState s1) {
    Action next = this.tool.getNextStateSpec();    
    if (!this.tool.isValid(next, s0, s1)) {
      RuntimeConfiguration.get().getErrStream().println("The following transition is illegal: ");
      RuntimeConfiguration.get().getErrStream().println(s0);
      RuntimeConfiguration.get().getErrStream().println(s1);
      return false;
    }
    int cnt = this.impliedActions.length;
    for (int i = 0; i < cnt; i++) {
      if (!this.tool.isValid(this.impliedActions[i], s0, s1)) {
	RuntimeConfiguration.get().getErrStream().println("Error: Action property " + this.tool.getImpliedActNames()[i] +
			   " is violated.");
	RuntimeConfiguration.get().getErrStream().println(s0);
	RuntimeConfiguration.get().getErrStream().println(s1);
	return false;
      }
    }
    return true;
  }

  /**
   * Returns true iff the state satisfies all the invariant properties.
   * It also records the state if it is not seen before.
   */
  public final boolean checkState(TLCState state) throws IOException {
    // Record the state in coverSet and theFPSet:
    long fp = state.fingerPrint();
    boolean seen = this.coverSet.put(fp);
    if (!seen) {
      if (!this.theFPSet.contains(fp)) {
	long loc = this.trace.writeState(this.curState.uid, fp);
	state.uid = loc;
	// Check invariant properties of the state:
	int cnt = this.invariants.length;
	for (int j = 0; j < cnt; j++) {
	  if (!this.tool.isValid(this.invariants[j], state)) {
	    // We get here because of invariant violation:
	    RuntimeConfiguration.get().getErrStream().println("Error: Invariant " + this.tool.getInvNames()[j] +
			       " is violated. The behavior up to this point is:");
	    return false;
	  }
	}
      }
    }
    return true;
  }

  /**
   * Generate a trace ending with an uncovered state. Returns
   * null if there is no more uncovered state in the current
   * state space.
   */
  public final TLCStateInfo[] generateNewTrace() throws IOException {
    long pos = -1;
    while ((pos = this.stateEnum.nextPos()) != -1) {
      long fp = this.stateEnum.nextFP();
      if (!this.coverSet.contains(fp)) break;
    }
    if (pos == -1) return null;
    return this.trace.getTrace(pos, true);
  }

  /**
   * The main method to check a trace.  It gets the states in a
   * trace by calling the getState method.  For each new state
   * obtained by getState, checkTrace checks if it satisfies all
   * the invariant properties and if the transition is legal.
   */
  public final void checkTrace() throws IOException {
    this.curState = this.getState();
    if (this.curState == null) return;
    this.checkState(this.curState);
    
    while (true) {
      // Get the next state:
      TLCState state = this.getState();
      if (state == null) break;
      this.checkState(state);
      this.checkReachability(this.curState, state);
      this.curState = state;
    }
  }

  public final void export() throws IOException {
    // Check if we need a trace:
    long curTime = System.currentTimeMillis();
    if (curTime - this.lastTraceTime > TraceDuration) {
      TLCStateInfo[] states = this.generateNewTrace();
      if (states != null) {
	this.exportTrace(states);
      }
      this.lastTraceTime = curTime;	
    }
  }

}
