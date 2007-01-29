// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Sep 22 21:53:09 PDT 2000 by yuanyu

package tlc.tool.liveness;

import tlasany.semantic.ExprNode;
import tlc.tool.EvalControl;
import tlc.tool.TLCState;
import tlc.tool.Tool;
import tlc.util.Context;
import tlc.value.BoolValue;
import tlc.value.Value;
import util.Assert;

public class LNAction extends LiveExprNode {
  protected ExprNode body;
  protected ExprNode subscript;
  protected boolean isBox;  // <A>_v: A /\ v'!=v or [A]_v: A \/ v'=v
  protected Context con;
  protected int tag;

  public LNAction(ExprNode body, Context con, ExprNode subscript,
		  boolean isBox) {
    this.body = body;
    this.subscript = subscript;
    this.isBox = isBox;
    this.con = con;
  }

  public LNAction(ExprNode body, Context con) {
    this.body = body;
    this.subscript = null;
    this.isBox = false;
    this.con = con;
  }

  public final int getTag() { return this.tag; }

  public final void setTag(int t) { this.tag = t; }

  public final int getLevel() { return 2; }

  public final boolean containAction() { return true; }

  public final boolean eval(Tool tool, TLCState s1, TLCState s2) {
    if (this.subscript != null) {
      Value v1 = tool.eval(this.subscript, con, s1, TLCState.Empty, EvalControl.Clear);
      Value v2 = tool.eval(this.subscript, con, s2, null, EvalControl.Clear);
      boolean isStut = v1.equals(v2);
      if (this.isBox) {
	if (isStut) return true;
      }
      else {
	if (isStut) return false;
      }
    }
    Value val = tool.eval(this.body, con, s1, s2, EvalControl.Clear);
    if (!(val instanceof BoolValue)) {
      Assert.fail("Encountered an action predicate that's not a boolean.");
    }
    return ((BoolValue)val).val;
  }

  public final void toString(StringBuffer sb, String padding) {
    if (this.subscript == null) {
      this.body.toString(sb, padding);
    }
    else {
      sb.append((this.isBox) ? "[" : "<");
      this.body.toString(sb, padding + " ");
      sb.append(((this.isBox) ? "]_" : ">_") + this.subscript);
    }
  }

}
