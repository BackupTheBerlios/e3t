// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Aug 10 15:07:47 PDT 2001 by yuanyu

package tlc.value;

import java.util.Enumeration;
import java.util.Hashtable;

import util.Assert;
import util.FP64;
import util.UniqueString;

public class ModelValue extends Value {
  private static int count = 0;
  private static Hashtable mvTable = new Hashtable();
  public static ModelValue[] mvs = null;

  public UniqueString val;
  public int index;
  
  /* Constructor */
  private ModelValue(String val) {
    this.val = UniqueString.intern(val);
    this.index = count++;
  }

  /* Make str a new model value, if it is not one yet.  */
  public static ModelValue make(String str) {
    ModelValue mv = (ModelValue)mvTable.get(str);
    if (mv != null) return mv;
    mv = new ModelValue(str);
    mvTable.put(str, mv);
    return mv;
  }

  /* Collect all the model values defined thus far. */
  public static void setValues() {
    mvs = new ModelValue[mvTable.size()];    
    Enumeration enum = mvTable.elements();
    while (enum.hasMoreElements()) {
      ModelValue mv = (ModelValue)enum.nextElement();
      mvs[mv.index] = mv;
    }
  }

  public final byte getKind() { return MODELVALUE; }

  public final int compareTo(Object obj) {
    if (obj instanceof ModelValue) {
      return this.val.compareTo(((ModelValue)obj).val);
    }
    return -1;
  }

  public final boolean equals(Object obj) {
    return (obj instanceof ModelValue &&
	    this.val.equals(((ModelValue)obj).val));
  }

  public final boolean member(Value elem) {
    Assert.fail("Attempted to check if the value:\n" + ppr(elem.toString()) +
		"\nis an element of the model value " + ppr(this.toString()));
    return false;   // make compiler happy
  }

  public final boolean isFinite() {
    Assert.fail("Attempted to check if the model value " + ppr(this.toString()) +
		" is a finite set.");
    return false;   // make compiler happy
  }
  
  public final Value takeExcept(ValueExcept ex) {
    if (ex.idx < ex.path.length) {
      Assert.fail("Attempted to apply EXCEPT construct to the model value " +
		  ppr(this.toString()) + ".");
    }
    return ex.value;
  }
  
  public final Value takeExcept(ValueExcept[] exs) {
    if (exs.length != 0) {
      Assert.fail("Attempted to apply EXCEPT construct to the model value " +
		  ppr(this.toString()) + ".");
    }
    return this;
  }

  public final int size() {
    Assert.fail("Attempted to compute the number of elements in the model value " +
		ppr(this.toString()) + ".");
    return 0;   // make compiler happy
  }

  public final boolean isNormalized() { return true; }

  public final void normalize() { /*nop*/ }

  public final boolean isDefined() { return true; }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    return ((val instanceof ModelValue) &&
	    this.val.equals(((ModelValue)val).val));
  }

  /* The fingerprint methods */
  public final long fingerPrint(long fp) {
    return this.val.fingerPrint(FP64.Extend(fp, MODELVALUE));
  }

  public final Value permute(MVPerm perm) {
    Value res = perm.get(this);
    if (res == null) return this;
    return res;
  }

  /* The string representation. */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    return sb.append(this.val);
  }

}
