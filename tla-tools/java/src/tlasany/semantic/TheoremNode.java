// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;

import tlasany.st.TreeNode;
import tlasany.utilities.Strings;

/**
 * This class represents an unnamed theorem
 */

public class TheoremNode extends SemanticNode {

  ModuleNode  module;
  ExprNode    theoremExpr;
  boolean     localness;
  
  /** 
   * Constructor -- expr is the statement (i.e. expression) of the theorem
   */
  public TheoremNode(TreeNode stn, ExprNode theorem, boolean local, ModuleNode mn) {
    super(TheoremKind, stn);
    this.theoremExpr = theorem;
    this.localness = local;
    this.module = mn;
  }

  /* Returns the statement of the theorem  */
  public final ExprNode getTheorem() { return this.theoremExpr; }

  public final boolean isLocal() { return this.localness; }

  /* Level checking */
  public final boolean levelCheck() {
    return this.theoremExpr.levelCheck();
  }

  public final int getLevel() {
    return this.theoremExpr.getLevel();
  }

  public final HashSet getLevelParams() {
    return this.theoremExpr.getLevelParams();
  }

  public final SetOfLevelConstraints getLevelConstraints() {
    return this.theoremExpr.getLevelConstraints();
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    return this.theoremExpr.getArgLevelConstraints();
  }

  public final HashSet getArgLevelParams() {
    return this.theoremExpr.getArgLevelParams();
  }
  
  /**
   * toString, levelDataToString, and walkGraph methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    return "Level: "               + this.getLevel()               + "\n" +
           "LevelParameters: "     + this.getLevelParams()         + "\n" +
           "LevelConstraints: "    + this.getLevelConstraints()    + "\n" +
           "ArgLevelConstraints: " + this.getArgLevelConstraints() + "\n" +
           "ArgLevelParams: "      + this.getArgLevelParams()      + "\n";
  }

  public final String toString(int depth) {
    if (depth <= 0) return "";
    return Strings.indent(2, "\n*TheoremNode " + super.toString( depth ) +
			  "   local: " + localness +
			  Strings.indent(2,theoremExpr.toString(depth-1)));
  }

  public final void walkGraph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);
    if (semNodesTable.get(uid) != null) return;
    semNodesTable.put(uid, this);
    theoremExpr.walkGraph(semNodesTable);
  }

}

