// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import tlasany.st.TreeNode;
import util.UniqueString;

/**
 * An assume prove node represents something like                       
 *                                                                      
 *    ASSUME 1. A                                                       
 *           2. B                                                       
 *    PROVE  C                                                          
 *                                                                      
 * The method getAssumes() returns the list A, B, which are either
 * AssumeProveNode's or ExprNode's.  The method getProve() returns
 * C. We think that C must be an OpApplNode, but we may decide later
 * that it can also be an AssumeProveNode.  The method getAssumeNames
 * returns the list of strings "1", "2", as InternalStrings.  The
 * AssumeProve node corresponding to
 *                                                                      
 *    ASSUME A                                                          
 *    PROVE  B                                                          
 *                                                                      
 * has GetAssumeNames() equal to null.
 */

public abstract class AssumeProveNode extends SemanticNode {

  // Constructor
  public AssumeProveNode(TreeNode stn) {
    super(AssumeProveKind, stn);
  }

  public final SemanticNode[] getAssumes() {
    return null;
  }
  public final UniqueString[] getAssumeNames() {
    return null;
  }
  public final ExprNode getProve() {
    return null;
  }
  
}
