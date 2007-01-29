// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.

package tlasany.semantic;

class ArgLevelParam {

  SymbolNode op;
  int        i;
  SymbolNode param;

  /* Creates new ArgLevelParam */
  public ArgLevelParam(SymbolNode op, int i, SymbolNode param) {
    this.op = op;
    this.i = i;
    this.param = param;
  }

  public final boolean occur(SymbolNode[] symbols) {
    for (int i = 0; i < symbols.length; i++) {
      if (this.op == symbols[i] ||
	  this.param == symbols[i]) {
	return true;
      }
    }
    return false;
  }

  public final boolean equals(Object obj) {
    if (obj instanceof ArgLevelParam) {
      ArgLevelParam alp = (ArgLevelParam)obj;
      return ((this.op == alp.op) &&
	      (this.i == alp.i) &&
	      (this.param == alp.param));
    }
    return false;
  }

  public final int hashCode() {
    return this.op.hashCode() + this.i + this.param.hashCode();
  }

  public final String toString() {
    return "<" + this.op + ", " + this.i + ", " + this.param + ">";
  }

}
