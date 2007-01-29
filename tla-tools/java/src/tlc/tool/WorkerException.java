// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed Dec  5 15:35:42 PST 2001 by yuanyu   

package tlc.tool;


public class WorkerException extends Exception {
  public TLCState state1;
  public TLCState state2;
  public boolean keepCallStack;

  public WorkerException(String msg) {
    this(msg, null, null, false);
  }
  
  public WorkerException(String msg, TLCState s1, TLCState s2,
			 boolean keep) {
    super(msg);
    this.state1 = s1;
    this.state2 = s2;
    this.keepCallStack = keep;
  }

}
