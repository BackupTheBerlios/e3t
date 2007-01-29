// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue Oct 23 16:48:38 PDT 2001 by yuanyu

package tlc.tool;

import java.io.IOException;
import java.io.Serializable;

import tlasany.semantic.OpDeclNode;
import tlasany.semantic.SemanticNode;
import tlasany.semantic.SymbolNode;
import tlc.value.Value;
import tlc.value.ValueInputStream;
import tlc.value.ValueOutputStream;
import util.UniqueString;

public abstract class TLCState implements Cloneable, Serializable {
  public long uid = -1;   // Must be set to a non-negative number
  
  // Set by subclasses. Cannot set until we know what the variables are.
  public static TLCState Empty = null;

  // The state variables.
  protected static OpDeclNode[] vars = null;
  
  public static void setVariables(OpDeclNode[] variables) {
    vars = variables;
    UniqueString.setVariables(variables);
  }

  public void read(ValueInputStream vis) throws IOException {
    this.uid = vis.readLongNat();
  }
  
  public void write(ValueOutputStream vos) throws IOException {
    vos.writeLongNat(this.uid);
  }

  public abstract TLCState bind(UniqueString name, Value value, SemanticNode expr);
  public abstract TLCState bind(SymbolNode id, Value value, SemanticNode expr);  
  public abstract TLCState unbind(UniqueString name);
  public abstract Value lookup(UniqueString var);
  public abstract boolean containsKey(UniqueString var);
  public abstract TLCState copy();
  public abstract TLCState deepCopy();
  public abstract StateVec addToVec(StateVec states);
  public abstract void deepNormalize();
  public abstract long fingerPrint();
  public abstract boolean allAssigned();
  public abstract TLCState createEmpty();

  /* Returns a string representation of this state.  */
  public abstract String toString();
  public abstract String toString(TLCState lastState);
}
