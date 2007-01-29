// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Sat Mar 25 22:39:46 PST 2000 by yuanyu

package tlc.tool.liveness;


public class PossibleErrorModel {
  int[] EAAction;    // <>[]act's
  int[] AEState;     // []<>state's
  int[] AEAction;    // []<>act's

  public final String toString(LiveExprNode[] checkState,
			       LiveExprNode[] checkAction) {
    StringBuffer sb = new StringBuffer();
    this.toString(sb, "", checkState, checkAction);
    return sb.toString();
  }

  public final void toString(StringBuffer sb,
			     String padding,
			     LiveExprNode[] checkState,
			     LiveExprNode[] checkAction) {
    String padding1 = padding + "       ";
    for (int i = 0; i < this.EAAction.length; i++) {
      int idx = this.EAAction[i];
      sb.append(padding + "/\\ <>[]");
      checkAction[idx].toString(sb, padding1);
      sb.append("\n");
    }
    for (int i = 0; i < this.AEState.length; i++) {
      int idx = this.AEState[i];
      sb.append(padding + "/\\ []<>");
      checkState[idx].toString(sb, padding1);
      sb.append("\n");
    }
    for (int i = 0; i < this.AEAction.length; i++) {
      int idx = this.AEAction[i];
      sb.append(padding + "/\\ []<>");
      checkAction[idx].toString(sb, padding1);
      sb.append("\n");
    }
  }

}

