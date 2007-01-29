// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;

import tlasany.explorer.ExploreNode;
import tlasany.st.TreeNode;
import util.UniqueString;

/**
 * This node represents a string literal in the specification--for
 * example "abc".  The only information added to the SemanticNode
 * superclass is the level information and the UniqueString
 * representation of the string.
 */

public class StringNode extends ExprNode implements ExploreNode {

  private UniqueString value;

  public StringNode(TreeNode stn, boolean strip) {
    super(StringKind, stn); 

    this.value = stn.getUS();
    if (strip) {
      // Strip off quote marks from image in stn
      String str = this.value.toString();
      str = str.substring(1, str.length()-1);
      this.value = UniqueString.uniqueStringOf(str);
    }
  }

  /**
   * Returns the UniqueString representation of the string.
   */
  public final UniqueString getRep() { return this.value; }

  /* Level Checking */
  public final boolean levelCheck() {
    return true;
  }

  public final int getLevel() { return ConstantLevel; }

  public final HashSet getLevelParams() { return EmptySet; }

  public final SetOfLevelConstraints getLevelConstraints() {
    return EmptyLC;
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    return EmptyALC;
  }

  public final HashSet getArgLevelParams() { return EmptySet; }

  /**
   * toString, levelDataToString, & walkGraph methods to implement
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
    return "\n*StringNode: " + super.toString(depth) 
                             + "Value: '" + value + "'" 
                             + " Length: " + value.length();
  }

}
