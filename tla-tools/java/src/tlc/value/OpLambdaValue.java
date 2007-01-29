// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Sep 22 13:18:45 PDT 2000 by yuanyu

package tlc.value;

import tlasany.semantic.FormalParamNode;
import tlasany.semantic.OpDefNode;
import tlc.tool.TLCState;
import tlc.tool.Tool;
import tlc.util.Context;
import util.Assert;

public class OpLambdaValue extends OpValue implements Applicable {
  public OpDefNode opDef;       // the operator definition.
  public Tool tool;
  public Context con;
  public TLCState state;
  public TLCState pstate;
  
  /* Constructor */
  public OpLambdaValue(OpDefNode op, Tool tool,	Context con,
		       TLCState state, TLCState pstate) {
    this.opDef = op;
    this.tool = tool;
    this.state = state;
    this.con = con;
    this.pstate = pstate;
  }

  public final byte getKind() { return OPLAMBDAVALUE; }

  public final int compareTo(Object obj) {
    Assert.fail("Attempted to compare operator " + ppr(this.toString()) +
		" with value:\n" + ppr(obj.toString()));
    return 0;       // make compiler happy
  }
  
  public final boolean equals(Object obj) {
    Assert.fail("Attempted to check equality of operator " + ppr(this.toString()) +
		" with value:\n" + ppr(obj.toString()));
    return false;   // make compiler happy
  }

  public final boolean member(Value elem) {
    Assert.fail("Attempted to check if the value:\n" + ppr(elem.toString()) +
		"\nis an element of operator " + ppr(this.toString()));
    return false;   // make compiler happy
  }

  public final boolean isFinite() {
    Assert.fail("Attempted to check if the operator " + ppr(this.toString()) +
		" is a finite set.");
    return false;   // make compiler happy
  }

  public final Value apply(Value arg, int control) {
    Assert.fail("Error(TLC): Should use the other apply method.");
    return null;   // make compiler happy
  }

  public final Value apply(Value[] args, int control) {
    int alen = this.opDef.getArity();
    if (alen != args.length) {
      Assert.fail("Applying the operator " + ppr(this.toString()) +
		  " with wrong number of arguments.");
    }
    Context c1 = this.con;
    FormalParamNode[] formals = this.opDef.getParams();    
    for (int i = 0; i < alen; i++) {
      c1 = c1.cons(formals[i], args[i]);
    }
    return this.tool.eval(this.opDef.getBody(), c1, this.state, this.pstate,
			  control);
  }

  public final Value select(Value arg) {
    Assert.fail("Error(TLC): attempted to call OpLambdaValue.select().");
    return null;   // make compiler happy    
  }
  
  public final Value takeExcept(ValueExcept ex) {
    Assert.fail("Attempted to appy EXCEPT construct to the operator " +
		ppr(this.toString()) + ".");
    return null;   // make compiler happy
  }

  public final Value takeExcept(ValueExcept[] exs) {
    Assert.fail("Attempted to apply EXCEPT construct to the operator " +
		ppr(this.toString()) + ".");
    return null;   // make compiler happy
  }

  public final Value getDomain() {
    Assert.fail("Attempted to compute the domain of the operator " +
		ppr(this.toString()) + ".");
    return EmptySet;   // make compiler happy
  }
  
  public final int size() {
    Assert.fail("Attempted to compute the number of elements in the operator " +
		ppr(this.toString()) + ".");
    return 0;   // make compiler happy
  }

  /* Should never normalize an operator. */
  public final boolean isNormalized() {
    Assert.fail("Should not normalize an operator.");
    return true;  // make compiler happy
  }
  
  public final void normalize() {
    Assert.fail("Should not normalize an operator.");
  }

  public final boolean isDefined() { return true; }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    Assert.fail("Should not initialize an operator.");
    return false;   // make compiler happy
  }

  /* String representation of the value.  */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    String opName = this.opDef.getName().toString();
    return sb.append("<Operator ").append(opName).append(">");
  }

}
