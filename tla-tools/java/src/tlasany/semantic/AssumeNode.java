// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;

import tlasany.st.TreeNode;
import tlasany.utilities.Strings;

/**
 * This class represents an assumption about the definitions in a module.                         
 */
public class AssumeNode extends SemanticNode {

  ModuleNode  module;
  ExprNode    assumeExpr;
  boolean     localness;

  public AssumeNode(TreeNode stn, ExprNode expr, boolean local, ModuleNode mn) {
    super(AssumeKind, stn);
    this.assumeExpr = expr;
    this.localness = local;
    this.module = mn;
  }

  /* Returns the expression that is the statement of the assumption */
  public final ExprNode getAssume() { return this.assumeExpr; }

  /**
   * Returns true iff this assumption is local to the module in which
   * it occurs.
   */
  public final boolean isLocal() { return this.localness; }
  
  /* Level checking */
  public final boolean levelCheck() {
    boolean res = this.assumeExpr.levelCheck();

    // Verify that the assumption is constant level
    if (this.assumeExpr.getLevel() != ConstantLevel) {
      errors.addError(getTreeNode().getLocation(),
		      "Level error: assumptions must be level 0 (Constant), " +
		      "but this one has level " + this.getLevel() + "." );
    }
    return res;
  }
  
  public final int getLevel() {
    return this.assumeExpr.getLevel();
  }

  public final HashSet getLevelParams() {
    return this.assumeExpr.getLevelParams();
  }

  public final SetOfLevelConstraints getLevelConstraints() {
    return this.assumeExpr.getLevelConstraints();
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    return this.assumeExpr.getArgLevelConstraints();
  }

  public final HashSet getArgLevelParams() {
    return this.assumeExpr.getArgLevelParams();
  }

  /**
   * toString(), levelDataToString(), and walkGraph() methods
   */
  public final String levelDataToString() { 
    return "Level: "               + getLevel()               + "\n" +
           "LevelParameters: "     + getLevelParams()         + "\n" +
           "LevelConstraints: "    + getLevelConstraints()    + "\n" +
           "ArgLevelConstraints: " + getArgLevelConstraints() + "\n" +
           "ArgLevelParams: "      + getArgLevelParams()      + "\n" ;
  }

  /**
   * Displays this node as a String, implementing ExploreNode
   * interface; depth parameter is a bound on the depth of the portion
   * of the tree that is displayed.
   */
  public final String toString (int depth) {
    if (depth <= 0) return "";
    return Strings.indent(2, "\n*AssumeNode " + super.toString( depth ) +
			  "   local: " + localness +
			  Strings.indent(2,assumeExpr.toString(depth-1)) );
  }

  /**
   * walkGraph finds all reachable nodes in the semantic graph and
   * inserts them in the Hashtable semNodesTable for use by the
   * Explorer tool.
   */
  public final void walkGraph (Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);

    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(uid, this);
    assumeExpr.walkGraph(semNodesTable);
  }

}
