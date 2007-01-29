// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;

import tlasany.st.TreeNode;

public abstract class LevelNode extends SemanticNode {

  // This class is empty. It inherits all the methods it requires
  // from the ExprOrOpArgNode class.
  LevelNode(int kind, TreeNode stn) { super(kind, stn); }
  
  /**
   * Check whether an expr or opArg is level correct, and if so,
   * calculates the level information for the expression. Returns
   * true iff this is level correct.
   */
  public abstract boolean levelCheck();

  public abstract int getLevel();

  public abstract HashSet getLevelParams();

  public abstract SetOfLevelConstraints getLevelConstraints();

  public abstract SetOfArgLevelConstraints getArgLevelConstraints();

  public abstract HashSet getArgLevelParams();

  public abstract String levelDataToString();

}
