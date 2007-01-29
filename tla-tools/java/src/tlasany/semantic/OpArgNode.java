// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;

import tlasany.parser.SyntaxTreeNode;
import tlasany.st.TreeNode;
import util.UniqueString;

/** 
 * This class represents operators of arity > 0 used as arguments to
 * other operators.  Such operator instances are used in syntactic
 * positions where expressions would usually occur, i.e. as arguments
 * to operators or as the RHS of a substitution involved in module
 * instantiation.
 */

public class OpArgNode extends ExprOrOpArgNode {

  private SymbolNode   op;      // The OpDefNode, OpDeclNode, or FormalParamNode 
                                // corresponding to THIS OpArgNode
  private UniqueString name;    // The string name of the full compound name of this operator
  private int          arity;   // The correct arity for this operator
  private ModuleNode   mn;      // the Module in which THIS OpArgNode appears

  /* Used only for construction of a "null" OpArg */
  public OpArgNode(UniqueString name) {
    super(OpArgKind, SyntaxTreeNode.nullSTN);
    this.name = name;
    this.arity = -2;
  }

  /** 
   * The primary constructor; we allow for the case that op may be
   * null because in the case of some kind of semantic error
   * (unresolved symbol) we want to be able to continue semantic
   * analysis.
   */
  public OpArgNode(SymbolNode op, TreeNode stn, ModuleNode mn)
  throws AbortException {
    super(OpArgKind, stn);
    
    // if op is an OpDefNode, OpDeclNode, or FormalParamNode
    this.op        = op;
    this.name      = op.getName();
    this.arity     = op.getArity();
    this.mn        = mn;
  }

  public final SymbolNode   getOp()        { return this.op; }

  public final int          getArity()     { return this.arity; }

  public final UniqueString getName()      { return this.name; }

  public final ModuleNode   getModule()    { return this.mn; }

  /* Level check */
  public final boolean levelCheck() {
    return this.op.levelCheck();
  }

  public final int getLevel() { return this.op.getLevel(); }
  
  public final HashSet getLevelParams() {
    return this.op.getLevelParams();
  }

  public final SetOfLevelConstraints getLevelConstraints() {
    return this.op.getLevelConstraints();
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    return this.op.getArgLevelConstraints();
  }

  public final HashSet getArgLevelParams() {
    return this.op.getArgLevelParams();
  }

  /**  
   * walkGraph, levelDataToString, and toString methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    return "Level: "               + this.getLevel()               + "\n" +
           "LevelParameters: "     + this.getLevelParams()         + "\n" +
           "LevelConstraints: "    + this.getLevelConstraints()    + "\n" +
           "ArgLevelConstraints: " + this.getArgLevelConstraints() + "\n" +
           "ArgLevelParams: "      + this.getArgLevelParams()      + "\n" ;
  }

  public final void walkGraph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);
    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(new Integer(myUID), this);
  }

  public final String toString(int depth) {
    if (depth <= 0) return "";

    return "\n*OpArgNode: " + ( name != null ? name.toString() : "null") + 
      "  " + super.toString(depth) + 
      "  arity: " + arity +
      "  op: " + (op != null ? "" + ((SemanticNode)op).getUid() : "null" );
  }

}
