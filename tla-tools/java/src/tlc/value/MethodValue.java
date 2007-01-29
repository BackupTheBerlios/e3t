// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Sep 22 13:18:45 PDT 2000 by yuanyu

package tlc.value;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import tlc.tool.EvalException;
import util.Assert;

public class MethodValue extends OpValue implements Applicable {
  public Method md;
  
  /* Constructor */
  public MethodValue(Method md) { this.md = md; }

  public final byte getKind() { return METHODVALUE; }

  public final int compareTo(Object obj) {
    Assert.fail("Attempted to compare operator " + this.toString() +
		" with value:\n" + ppr(obj.toString()));
    return 0;       // make compiler happy
  }
  
  public final boolean equals(Object obj) {
    Assert.fail("Attempted to check equality of operator " + this.toString() +
		" with value:\n" + ppr(obj.toString()));
    return false;   // make compiler happy
  }

  public final boolean member(Value elem) {
    Assert.fail("Attempted to check if the value:\n" + ppr(elem.toString()) +
		"\nis an element of operator " + this.toString());
    return false;   // make compiler happy
  }

  public final boolean isFinite() {
    Assert.fail("Attempted to check if the operator " + this.toString() +
		" is a finite set.");
    return false;   // make compiler happy
  }

  public final Value apply(Value arg, int control) {
    Assert.fail("It is a TLC bug: Should use the other apply method.");
    return null;   // make compiler happy
  }

  public final Value apply(Value[] args, int control) {
    Value res = null;
    try {
      res = (Value)this.md.invoke(null, args);
    }
    catch (Exception e) {
      String msg = "Attempted to apply the operator overridden by the java " +
	"method\n" + this.md + ",\nbut it produced the following error:\n";
      if (e instanceof InvocationTargetException) {
	Throwable e1 = ((InvocationTargetException)e).getTargetException();
	throw new EvalException(msg + e1.getMessage() + "\n");
      }
      Assert.fail(msg + e.getMessage());
    }
    return res;
  }

  public final Value select(Value arg) {
    Assert.fail("It is a TLC bug: Attempted to call MethodValue.select().");
    return null;   // make compiler happy    
  }
  
  public final Value takeExcept(ValueExcept ex) {
    Assert.fail("Attempted to appy EXCEPT construct to the operator " +
		this.toString() + ".");
    return null;   // make compiler happy
  }

  public final Value takeExcept(ValueExcept[] exs) {
    Assert.fail("Attempted to apply EXCEPT construct to the operator " +
		this.toString() + ".");
    return null;   // make compiler happy
  }

  public final Value getDomain() {
    Assert.fail("Attempted to compute the domain of the operator " +
		this.toString() + ".");
    return EmptySet;   // make compiler happy
  }
  
  public final int size() {
    Assert.fail("Attempted to compute the number of elements in the operator " +
		this.toString() + ".");
    return 0;   // make compiler happy
  }

  /* Should never normalize an operator. */
  public final boolean isNormalized() {
    Assert.fail("It is a TLC bug: Attempted to normalize an operator.");
    return true;  // make compiler happy
  }
  
  public final void normalize() {
    Assert.fail("It is a TLC bug: Attempted to normalize an operator.");
  }

  public final boolean isDefined() { return true; }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    Assert.fail("It is a TLC bug: Attempted to initialize an operator.");
    return false;   // make compiler happy
  }

  /* String representation of the value.  */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    return sb.append("<Java Method: " + this.md + ">");
  }

}
