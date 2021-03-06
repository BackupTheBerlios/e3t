// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sat Jul 28 00:36:03 PDT 2001 by yuanyu

package tlc.tool.liveness;

import tlc.tool.TLCState;
import tlc.tool.Tool;

class LNNext extends LiveExprNode {
  protected LiveExprNode body;

  public LNNext(LiveExprNode body) { this.body = body;}

  public final LiveExprNode getBody() { return this.body; }

  public final int getLevel() { return 2; }

  public final boolean containAction() { return this.body.containAction(); }
  
  public final boolean eval(Tool tool, TLCState s1, TLCState s2) {
    return this.body.eval(tool, s2, TLCState.Empty);
  }

  public final void toString(StringBuffer sb, String padding) {
    sb.append("()");
    this.getBody().toString(sb, padding + "  ");
  }
}

