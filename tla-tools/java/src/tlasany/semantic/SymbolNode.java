// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import tlasany.st.TreeNode;
import util.UniqueString;

/** 
 * Abstract class extended by classes that represent the meaning of an
 * identifier, including generalized IDs.  
 *
 * Extended by OpDefOrDeclNode (and it subclasses OpDefNode, OpDeclNode), 
 * FormalParamNode, and ModuleNode
 */

public abstract class SymbolNode extends LevelNode {

  protected final UniqueString name;    // the name of this symbol
  
  SymbolNode(int kind, TreeNode stn, UniqueString name) {
    super(kind, stn);
    this.name = name;
  }
  
  /** 
   * This method returns the UniqueString for the printable name of
   * the symbol being declared or defined. For example, if this node
   * is an operator definition:                                                    
   *                                                                 
   *   Foo(a, b) == a*b                                              
   *                                                                 
   * getName() is the UniqueString for "Foo".
   */
  public final UniqueString getName() { return this.name; }

  /* Returns the arity of the operator named by the symbol.  */
  public abstract int getArity();

  /**
   * Returns true iff the symbol is local to its module; does not
   * apply to FormalParamNodes or ModuleNodes.
   */
  public abstract boolean isLocal();

  /**
   * Returns true iff the OpApplNode test has the proper types of
   * arguments for the operator as declared in module mn.
   */
  public abstract boolean match(OpApplNode test, ModuleNode mn) throws AbortException ;

  public final boolean occur(SymbolNode[] params) {
    for (int i = 0; i < params.length; i++) {
      if (this == params[i]) return true;
    }
    return false;
  }

  public final boolean isParam() {
    return (this instanceof OpDeclNode ||
	    this instanceof FormalParamNode);
  }
}
