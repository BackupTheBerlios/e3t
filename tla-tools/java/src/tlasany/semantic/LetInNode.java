// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import tlasany.explorer.ExploreNode;
import tlasany.st.TreeNode;
import tlasany.utilities.Strings;

public class LetInNode extends ExprNode
implements ExploreNode, LevelConstants {

  /**
   * This node represents a LET expression, for example 
   *
   * LET Foo(a) == a + x 
   *     Bar == Foo(a) + a 
   * IN body 
   */

  private OpDefNode[] opDefs;
  private InstanceNode[] insts;
  private ExprNode body;

  /* Constructor */
  public LetInNode(TreeNode treeNode, OpDefNode[] defs, InstanceNode[] insts,
		   ExprNode bdy) {
    super(LetInKind, treeNode);
    this.opDefs = defs;
    this.insts = insts;
    this.body = bdy;
  }

  /**
   * Return the array of LET operator definitions. In the example above,
   * getLets()[1] is the OpDefNode representing the definition of Bar.
   */
  public final OpDefNode[] getLets() { return this.opDefs; }

  /* Return the body of the LET expression (the IN expression). */
  public final ExprNode getBody() { return this.body; }

  /* Level checking */
  private boolean levelCorrect;
  private int level;
  private HashSet levelParams; 
  private SetOfLevelConstraints levelConstraints;
  private SetOfArgLevelConstraints argLevelConstraints;
  private HashSet argLevelParams;

  public final boolean levelCheck() {
    if (this.levelConstraints != null) return this.levelCorrect;
    
    // Level check all the components:
    this.levelCorrect = true;
    for (int i = 0; i < this.opDefs.length; i++) {
      if (!this.opDefs[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    if (!this.body.levelCheck()) {
      this.levelCorrect = false;
    }
    for (int i = 0; i < this.insts.length; i++) {
      if (!this.insts[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }

    // Calculate level information:
    this.level = this.body.getLevel();
    this.levelParams = this.body.getLevelParams();

    this.levelConstraints = new SetOfLevelConstraints();
    this.levelConstraints.putAll(this.body.getLevelConstraints());
    for (int i = 0; i < this.opDefs.length; i++) {
      this.levelConstraints.putAll(opDefs[i].getLevelConstraints());
    }

    this.argLevelConstraints = new SetOfArgLevelConstraints();
    this.argLevelConstraints.putAll(this.body.getArgLevelConstraints());
    for (int i = 0; i < this.opDefs.length; i++) {
      this.argLevelConstraints.putAll(opDefs[i].getArgLevelConstraints());
    }

    this.argLevelParams = new HashSet();
    this.argLevelParams.addAll(this.body.getArgLevelParams());
    for (int i = 0; i < this.opDefs.length; i++) {
      FormalParamNode[] params = this.opDefs[i].getParams();
      Iterator iter = this.opDefs[i].getArgLevelParams().iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	if (!alp.occur(params)) {
	  this.argLevelParams.add(alp);
	}
      }
    }
    for (int i = 0; i < this.insts.length; i++) {
      this.argLevelParams.addAll(this.insts[i].getArgLevelParams());
    }
    return this.levelCorrect;
  }

  public final int getLevel() { return this.level; }

  public final HashSet getLevelParams() { return this.levelParams; }

  public final SetOfLevelConstraints getLevelConstraints() { 
    return this.levelConstraints; 
  }
  
  public final SetOfArgLevelConstraints getArgLevelConstraints() { 
    return this.argLevelConstraints; 
  }
  
  public final HashSet getArgLevelParams() { 
    return this.argLevelParams; 
  }

  /**
   * toString, levelDataToString, and walkGraph methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    return "Level: "               + this.level               + "\n" +
           "LevelParameters: "     + this.levelParams         + "\n" +
           "LevelConstraints: "    + this.levelConstraints    + "\n" +
           "ArgLevelConstraints: " + this.argLevelConstraints + "\n" +
           "ArgLevelParams: "      + this.argLevelParams      + "\n" ;
  }

  public final void walkGraph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);

    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(new Integer(myUID), this);
    if (opDefs != null) {
      for (int i = 0; i < opDefs.length; i++) {
        if (opDefs[i] != null) opDefs[i].walkGraph(semNodesTable);
      }
    }
    if (body != null) body.walkGraph(semNodesTable);
  }

  public final String toString(int depth) {
    if (depth <= 0) return "";

    String ret = "\n*LetInNode: " + super.toString(depth);
    for (int i = 0; i < opDefs.length; i++) {
      ret += Strings.indent(2,"\nDef:" + Strings.indent(2, opDefs[i].toString(depth-1)));
    }
    ret += Strings.indent(2, "\nBody:" + Strings.indent(2, body.toString(depth-1)));
    return ret;
  }

}
