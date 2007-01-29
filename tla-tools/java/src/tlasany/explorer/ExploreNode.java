// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.explorer;
import java.util.Hashtable;

/**
 * implemented by the following classes (as well as various abstract and  superclasses):
 *
 *        AssumeNode, AtNode, Context, DecimalNode, FormalParamNode, LetInNode, NumeralNode, 
 *        OpApplNode, OpArgNode, OpDeclNode, OpDefNode, StringNode, Subst, SubstInNode, TheoremNode
 */

public interface ExploreNode {

  public String toString(int depth);
  public String levelDataToString();
  public void   walkGraph(Hashtable semNodesTable);

}
