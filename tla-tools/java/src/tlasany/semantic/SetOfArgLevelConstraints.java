// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class SetOfArgLevelConstraints extends HashMap implements LevelConstants {
  // Implements a map mapping arg-level parameters (ParamAndPosition)
  // to levels (Integer). An entry <pap,x> means that the argument
  // pap.position of the operator pap.param must have a level >= x.

  /**
   * This method adds <pap, level> into this set, and "subsumes"
   * it with another one for the same parameter if one is there, or
   * ignoring the constraint if it is vacuous.
   */
  public final Object put(Object pap, Object level) {
    int newLevel = ((Integer)level).intValue();
    Object old = this.get(pap);

    int oldLevel = (old == null) ? MinLevel : ((Integer)old).intValue();
    super.put(pap, new Integer(Math.max(newLevel, oldLevel)));
    return old;
  }

  public final Object put(SymbolNode param, int position, int level) {
    ParamAndPosition pap = new ParamAndPosition(param, position);
    return this.put(pap, new Integer(level));
  }

  /**
   * This method adds all of the elements of its argument s to this
   * map, in each case "subsuming" it with any constraint already
   * present for the same parameter if one is there.
   */
  public final void putAll(Map s) {
    for (Iterator iter = s.keySet().iterator(); iter.hasNext(); ) {
      Object key = iter.next();
      put(key, s.get(key));
    }
  }

  public final String toString() {
    StringBuffer sb = new StringBuffer("{ ");
    for (Iterator iter = this.keySet().iterator(); iter.hasNext(); ) {
      Object pap = iter.next();
      sb.append(pap.toString() + " -> " + this.get(pap));
      if (iter.hasNext()) sb.append(", ");
    }
    sb.append("}");
    return sb.toString();
  }

}
