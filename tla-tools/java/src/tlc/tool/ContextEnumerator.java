// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue Nov  9 11:06:41 PST 1999 by yuanyu

package tlc.tool;

import tlasany.semantic.SymbolNode;
import tlc.util.Context;
import tlc.value.TupleValue;
import tlc.value.Value;
import tlc.value.ValueEnumeration;
import util.Assert;

public final class ContextEnumerator {
  private Context con;
  private Object[] vars;
  private ValueEnumeration[] enums;
  private Value[] currentElems;
  private boolean isDone;
  
  public ContextEnumerator(Object[] vars, ValueEnumeration[] enums, Context con) {
    this.con = con;
    this.vars = vars;
    this.enums = enums;
    this.currentElems = new Value[enums.length];
    this.isDone = false;
    for (int i = 0; i < enums.length; i++) {
      this.currentElems[i] = this.enums[i].nextElement();
      if (this.currentElems[i] == null) {
	this.isDone = true;
	break;
      }
    }
  }
  
  public final Context nextElement() {
    Context con1 = this.con;
    if (this.isDone) return null;
    for (int i = 0; i < enums.length; i++) {
      if (this.vars[i] instanceof SymbolNode) {
	con1 = con1.cons((SymbolNode)this.vars[i], this.currentElems[i]);
      }
      else {
	SymbolNode[] varList = (SymbolNode[])this.vars[i];
	Value argVal = this.currentElems[i];
	if (!(argVal instanceof TupleValue)) {
	  Assert.fail("Argument mismatch in operator application." + varList[0]);
	}
	Value[] valList = ((TupleValue)argVal).elems;
	if (varList.length != valList.length) {
	  Assert.fail("Argument mismatch in operator application." + varList[0]);
	}
	for (int j = 0; j < varList.length; j++) {
	  con1 = con1.cons(varList[j], valList[j]);
	}
      }
    }
    for (int i = 0; i < enums.length; i++) {
      this.currentElems[i] = this.enums[i].nextElement();
      if (this.currentElems[i] != null) break;
      if (i == this.enums.length - 1) {
	this.isDone = true;
	break;
      }
      this.enums[i].reset();
      this.currentElems[i] = this.enums[i].nextElement();
    }
    return con1;
  }

}

