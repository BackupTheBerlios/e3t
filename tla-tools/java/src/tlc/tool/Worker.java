// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed Dec  5 15:35:42 PST 2001 by yuanyu   

package tlc.tool;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.util.IdThread;
import tlc.util.ObjLongTable;

public class Worker extends IdThread {
  /**
   * Multi-threading helps only when running on multiprocessors. TLC
   * can pretty much eat up all the cycles of a processor running
   * single threaded.  We expect to get linear speedup with respect
   * to the number of processors.
   */
  private ModelChecker tlc;
  private StateQueue squeue;
  private ObjLongTable astCounts;

  public Worker(int id, ModelChecker tlc) {
    super(id);
    this.tlc = tlc;
    this.squeue = tlc.theStateQueue;
    this.astCounts = new ObjLongTable(10);
  }

  public final ObjLongTable getCounts() { return this.astCounts; }

  /**
   * This method gets a state from the queue, generates all the
   * possible next states of the state, checks the invariants, and
   * updates the state set and state queue.
   */
  public final void run() {
    TLCState curState = null;
    try {
      while (true) {
	curState = (TLCState)this.squeue.sDequeue();
	if (curState == null) {
	  synchronized(this.tlc) {
	    this.tlc.setDone();
	    this.tlc.notify();
	  }
	  this.squeue.finishAll();	  
	  return;
	}
	if (this.tlc.doNext(curState, this.astCounts)) return;
      }
    }
    catch (Throwable e) {
      // Something bad happened. Quit ...
      // e.printStackTrace();
      synchronized(this.tlc) {
	if (this.tlc.setErrState(curState, null, true)) {
          RuntimeConfiguration.get().getErrStream().println("Error: " + e.getMessage());
	}
	this.squeue.finishAll();
	this.tlc.notify();
      }
      return;
    }
  }

}
