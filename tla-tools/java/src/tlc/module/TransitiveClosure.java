// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue Aug 22 16:03:24 PDT 2000 by yuanyu

package tlc.module;

import java.util.Hashtable;

import tlc.tool.EvalException;
import tlc.util.Vect;
import tlc.value.Enumerable;
import tlc.value.SetEnumValue;
import tlc.value.TupleValue;
import tlc.value.Value;
import tlc.value.ValueConstants;
import tlc.value.ValueEnumeration;
import tlc.value.ValueVec;

public class TransitiveClosure implements ValueConstants {

  /* Implement the Warshall algorithm for transitive closure. */
  public static Value Warshall(Value rel) {
    if (!(rel instanceof Enumerable)) {
      String msg = "Applying TransitiveClosure to the following value," +
	"\nwhich is not an enumerable set:\n" + Value.ppr(rel.toString());
      throw new EvalException(msg);
    }
    int maxLen = 2 * rel.size();
    boolean[][] matrix = new boolean[maxLen][maxLen];
    ValueEnumeration elems = ((Enumerable)rel).elements();
    Vect elemList = new Vect();
    Hashtable fps = new Hashtable();
    int cnt = 0;
    Value elem = null;
    while ((elem = elems.nextElement()) != null) {
      TupleValue tv = TupleValue.convert(elem);
      if (tv == null || tv.size() != 2) {
	String msg = "Applying TransitiveClosure to a set containing\n" +
	  "the following value:\n" + Value.ppr(elem.toString());
	throw new EvalException(msg);
      }
      Value elem1 = tv.elems[0];
      Value elem2 = tv.elems[1];
      int num1 = cnt;
      Integer num = (Integer)fps.get(elem1);
      if (num == null) {
	fps.put(elem1, new Integer(cnt));
	elemList.addElement(elem1);
	cnt++;
      }
      else {
	num1 = num.intValue();
      }
      int num2 = cnt;
      num = (Integer)fps.get(elem2);
      if (num == null) {
	fps.put(elem2, new Integer(cnt));
	elemList.addElement(elem2);	
	cnt++;
      }
      else {
	num2 = num.intValue();
      }
      matrix[num1][num2] = true;
    }

    for (int y = 0; y < cnt; y++) {
      for (int x = 0; x < cnt; x++) {
	if (matrix[x][y]) {
	  for (int z = 0; z < cnt; z++) {
	    if (matrix[y][z]) matrix[x][z] = true;
	  }
	}
      }
    }

    ValueVec newElems = new ValueVec();
    for (int i = 0; i < cnt; i++) {
      for (int j = 0; j < cnt; j++) {
	if (matrix[i][j]) {
	  Value elem1 = (Value)elemList.elementAt(i);
	  Value elem2 = (Value)elemList.elementAt(j);	  
	  Value newElem = new TupleValue(elem1, elem2);
	  newElems.addElement(newElem);
	}
      }
    }
    return new SetEnumValue(newElems, false);
  }
  
}
