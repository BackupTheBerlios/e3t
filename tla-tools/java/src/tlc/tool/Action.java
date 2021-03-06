// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.

package tlc.tool;

import java.io.Serializable;

import tlasany.semantic.SemanticNode;
import tlc.util.Context;

public final class Action implements ToolGlobals, Serializable {
  /* A TLA+ action.   */

  /* Fields  */
  public SemanticNode pred;     // Expression of the action
  public Context con;           // Context of the action

  /* Constructors */
  public Action(SemanticNode pred, Context con) {
    this.pred = pred;
    this.con = con;
  }

  /* Returns a string representation of this action.  */
  public final String toString() {
    return "<Action " + pred.toString() + ">";
  }

  public final String getLocation() {
    return "<Action " + pred.getLocation() + ">";
  }
}
