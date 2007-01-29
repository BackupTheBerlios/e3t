// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sat Jul 28 00:36:09 PDT 2001 by yuanyu

package tlc.tool.liveness;

import tlc.tool.TLCState;
import tlc.tool.Tool;
import tlc.util.Context;

abstract class LNState extends LiveExprNode {
  protected Context con;
  protected int tag;

  public LNState(Context con) {
    this.con = con;
  }

  public final int getLevel() { return 1; }

  public final boolean containAction() { return false; }
  
  public final boolean eval(Tool tool, TLCState s) {
    return this.eval(tool, s, TLCState.Empty);
  }

  public final Context getContext() { return this.con; }

  public final int getTag() { return this.tag; }

  public final void setTag(int t) { this.tag = t; }

}
