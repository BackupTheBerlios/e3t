// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu May 31 13:22:22 PDT 2001 by yuanyu

package tlc.tool;

import java.io.Serializable;

public abstract class DistApp implements TraceApp, Serializable {

  public abstract String getAppName();
  public abstract Boolean getCheckDeadlock();
  public abstract Boolean getPreprocess();  

  public abstract String getFileName();
  public abstract String getMetadir();
  public abstract boolean canRecover();
  
  /* Returns a list of initial states. */
  public abstract TLCState[] getInitStates() throws WorkerException;

  /* Returns a list of successor states of the state s. */
  public abstract TLCState[] getNextStates(TLCState s)
  throws WorkerException;

  /* Checks if the state is a valid state. */
  public abstract void checkState(TLCState s1, TLCState s2)
  throws WorkerException;

  /* Checks if the state satisfies the state constraints. */
  public abstract boolean isInModel(TLCState s);

  /* Checks if a pair of states satisfy the action constraints. */
  public abstract boolean isInActions(TLCState s1, TLCState s2);

  /* Reconstruct the initial state whose fingerprint is fp. */
  public abstract TLCStateInfo getState(long fp);
  
  /* Reconstruct the next state of state s whose fingerprint is fp. */
  public abstract TLCStateInfo getState(long fp, TLCState s);

  /* Reconstruct the info for the transition from s to s1. */
  public abstract TLCStateInfo getState(TLCState s1, TLCState s);

  /* Enables call stack tracing. */
  public abstract void setCallStack();

  /* Prints call stack.  */
  public abstract void printCallStack();
  
}
