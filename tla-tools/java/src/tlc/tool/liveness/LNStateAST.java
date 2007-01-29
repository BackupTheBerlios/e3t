// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sat Jul 28 00:37:08 PDT 2001 by yuanyu

package tlc.tool.liveness;

import tlasany.semantic.ExprNode;
import tlc.tool.TLCState;
import tlc.tool.Tool;
import tlc.util.Context;
import tlc.value.BoolValue;
import tlc.value.Value;
import util.Assert;

class LNStateAST extends LNState {
  protected ExprNode body;

  public LNStateAST(ExprNode body, Context con) {
    super(con);
    this.body = body;
  }

  public final ExprNode getBody() { return this.body; }

  public final boolean eval(Tool tool, TLCState s1, TLCState s2) {
    Value val = tool.eval(this.body, con, s1);
    if (!(val instanceof BoolValue)) {
      Assert.fail("A state predicate was evaluated to a non-boolean value.");
    }
    return ((BoolValue)val).val;
  }
  
  public final void toString(StringBuffer sb, String padding) {
    sb.append(this.body);
  }
}
