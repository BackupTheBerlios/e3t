// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Sep 22 13:56:36 PDT 2000 by yuanyu

package tlc.tool.liveness;

import tlc.tool.TLCState;
import tlc.tool.Tool;
import util.Assert;

class LNEven extends LiveExprNode {
  protected LiveExprNode body;

  public LNEven(LiveExprNode body) { this.body = body; }

  public final LiveExprNode getBody() { return this.body; }

  public final int getLevel() { return 3; }

  public final boolean containAction() { return this.body.containAction(); }  

  public final boolean eval(Tool tool, TLCState s1, TLCState s2) {
    Assert.fail("Can not evaluate a temporal formula <>F.");
    return false;    // make compiler happy    
  }

  public final void toString(StringBuffer sb, String padding) {
    sb.append("<>");
    this.getBody().toString(sb, padding+"  ");
  }
}

