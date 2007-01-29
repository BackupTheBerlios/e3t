// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sat Feb 17 12:07:55 PST 2001 by yuanyu 

package tlc.tool;

public class TLCStateInfo {
  public TLCState state;
  public Object info;

  public TLCStateInfo(TLCState s, Object info) {
    this.state = s;
    this.info = info;
  }

  public final long fingerPrint() {
    return this.state.fingerPrint();
  }

  public final String toString() {
    return this.state.toString();
  }
  
}
