// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sun Aug  5 00:49:49 PDT 2001 by yuanyu

package tlc.tool.liveness;

import tlc.tool.TLCState;
import tlc.tool.Tool;

class LNBool extends LiveExprNode {
  public static final LNBool TRUE = new LNBool(true);
  public static final LNBool FALSE = new LNBool(false);
  
  protected boolean b;

  public LNBool(boolean b) { this.b = b; }

  public final boolean eval(Tool tool, TLCState s1, TLCState s2) {
    return this.b;
  }

  public final int getLevel() { return 0; }

  public final boolean containAction() { return false; }

  public final void toString(StringBuffer sb, String padding) {
    sb.append(this.b ? "TRUE" : "FALSE");
  }

}
