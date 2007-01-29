// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import tlasany.st.TreeNode;

/**
 * This class represents a proof.  The initial implementation of the
 * interface may not provide any methods for this node class other
 * than those provided by any SemanticNode.  
 */
public abstract class ProofNode extends SemanticNode {

  public ProofNode(TreeNode stn) { super(ProofKind, stn); }
}
