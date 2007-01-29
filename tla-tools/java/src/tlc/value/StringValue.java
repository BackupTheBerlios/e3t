// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Aug 10 15:06:37 PDT 2001 by yuanyu

package tlc.value;

import util.Assert;
import util.FP64;
import util.UniqueString;

public class StringValue extends Value {
  public UniqueString val; 

  /* Constructor */
  public StringValue(String str) {
    this.val = UniqueString.intern(str);
  }

  public StringValue(UniqueString var) {
    this.val = var;
  }
  
  public final byte getKind() { return STRINGVALUE; }

  public final UniqueString getVal() { return this.val; }

  public final int compareTo(Object obj) {
    if (obj instanceof StringValue) {
      return this.val.compareTo(((StringValue)obj).val);
    }
    if (!(obj instanceof ModelValue)) {
      Assert.fail("Attempted to compare string " + ppr(this.toString()) +
		  " with non-string:\n" + ppr(obj.toString()));
    }
    return 1;
  }

  public final boolean equals(Object obj) {
    if (obj instanceof StringValue) {
      return this.val.equals(((StringValue)obj).getVal());
    }
    if (!(obj instanceof ModelValue)) {
      Assert.fail("Attempted to check equality of string " + ppr(this.toString()) +
		  " with non-string:\n" + ppr(obj.toString()));
    }
    return false;
  }

  public final boolean member(Value elem) {
    Assert.fail("Attempted to check if the value:\n" + ppr(elem.toString()) +
		"\nis an element of the string " + ppr(this.toString()));
    return false;     // make compiler happy
  }

  public final boolean isFinite() {
    Assert.fail("Attempted to check if the string " + ppr(this.toString()) +
		" is a finite set.");
    return false;     // make compiler happy
  }
  
  public final Value takeExcept(ValueExcept ex) {
    if (ex.idx < ex.path.length) {
      Assert.fail("Attempted to apply EXCEPT construct to the string " +
		  ppr(this.toString()) + ".");
    }
    return ex.value;
  }

  public final Value takeExcept(ValueExcept[] exs) {
    if (exs.length != 0) {
      Assert.fail("Attempted to apply EXCEPT construct to the string " +
		  ppr(this.toString()) + ".");
    }
    return this;
  }

  public final int size() {
    Assert.fail("Attempted to compute the number of elements in the string " +
		ppr(this.toString()) + ".");
    return 0;       // make compiler happy
  }

  public final boolean isNormalized() { return true; }

  public final void normalize() { /*SKIP*/ }

  public final boolean isDefined() { return true; }

  public final Value deepCopy() { return this; }

  public final boolean assignable(Value val) {
    return ((val instanceof StringValue) &&
	    this.equals(val));
  }

  /* The fingerprint method */
  public final long fingerPrint(long fp) {
    fp = FP64.Extend(fp, STRINGVALUE) ;
    fp = FP64.Extend(fp, this.val.length()) ;
    fp = FP64.Extend(fp, this.val.toString());
    return fp;
  }

  public final Value permute(MVPerm perm) { return this; }

  /* The string representation of the value. */
  public final StringBuffer toString(StringBuffer sb, int offset) {
    return sb.append("\"" + this.val + "\"");
  }

}
