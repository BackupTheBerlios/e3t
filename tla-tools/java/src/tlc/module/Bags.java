// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue Jan  2 11:40:25 PST 2001 by yuanyu

package tlc.module;

import tlc.tool.EvalControl;
import tlc.tool.EvalException;
import tlc.tool.TLARegistry;
import tlc.util.Vect;
import tlc.value.Applicable;
import tlc.value.BoolValue;
import tlc.value.FcnRcdValue;
import tlc.value.IntValue;
import tlc.value.SetEnumValue;
import tlc.value.Value;
import tlc.value.ValueConstants;
import tlc.value.ValueVec;

public class Bags implements ValueConstants {
  
  static {
    TLARegistry.put("BagCup", "\\oplus");
    TLARegistry.put("BagDiff", "\\ominus");
    TLARegistry.put("SqSubseteq", "\\sqsubseteq");
  }

  public static final Value EmptyBag() { return EmptyFcn; }

  public static BoolValue IsABag(Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    if (fcn == null) {
      String msg = "Applying IsABag to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    Value[] vals = ((FcnRcdValue)b).values;
    for (int i = 0; i < vals.length; i++) {
      if (!(vals[i] instanceof IntValue) ||
	  ((IntValue)vals[i]).val < 0) {
	return ValFalse;
      }
    }
    return ValTrue;
  }

  public static IntValue BagCardinality(Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    if (fcn == null) {
      String msg = "Applying BagCardinality to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    int num = 0;
    Value[] vals = fcn.values;
    for (int i = 0; i < vals.length; i++) {
      if (vals[i] instanceof IntValue) {
	int cnt = ((IntValue)vals[i]).val;
	if (cnt > 0) {
	  num += ((IntValue)vals[i]).val;
	}
	else {
	  String msg = "Applying BagCardinality to the following value," +
	    " which\nis not a bag:\n" + Value.ppr(b.toString());
	  throw new EvalException(msg);
	}
      }
      else {
	String msg = "Applying BagCardinality to the following value," +
	  " which\nis not a bag:\n" + Value.ppr(b.toString());
	throw new EvalException(msg);
      }
    }
    return IntValue.gen(num);
  }

  public static BoolValue BagIn(Value e, Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    Value[] domain = fcn.domain;
    Value[] values = fcn.values;
    Value val;
    for (int i = 0; i < domain.length; i++) {
      if (e.equals(domain[i])) {
	if (values[i] instanceof IntValue) {
	  return (((IntValue)values[i]).val > 0) ? ValTrue : ValFalse;
	}
	String msg = "The second argument of BagIn should be bag, but" +
	  " instead it is:\n" + Value.ppr(b.toString());
	throw new EvalException(msg);
      }
    }
    return ValFalse;
  }

  public static IntValue CopiesIn(Value e, Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    Value[] domain = fcn.domain;
    Value[] values = fcn.values;
    Value val;
    for (int i = 0; i < domain.length; i++) {
      if (e.equals(domain[i])) {
	if (values[i] instanceof IntValue) {
	  return (IntValue)values[i];
	}
	String msg = "The second argument of CopiesIn should be a bag," +
	  " but instead it is:\n" + Value.ppr(b.toString());
	throw new EvalException(msg);
      }
    }
    return ValZero;
  }

  public static Value BagCup(Value b1, Value b2) {
    FcnRcdValue fcn1 = FcnRcdValue.convert(b1);
    FcnRcdValue fcn2 = FcnRcdValue.convert(b2);
    if (!IsABag(fcn1).val) {
      String msg = "The first argument of (+) should be a bag, but" +
	" instead it is:\n" + Value.ppr(b1.toString());
      throw new EvalException(msg);
    }
    if (!IsABag(fcn2).val) {
      String msg = "The second argument of (+) should be a bag, but" +
	" instead it is:\n" + Value.ppr(b2.toString());
      throw new EvalException(msg);
    }
    Value[] domain1 = fcn1.domain;
    Value[] values1 = fcn1.values;
    Value[] domain2 = fcn2.domain;
    Value[] values2 = fcn2.values;
    Vect dVec = new Vect(domain1.length);
    Vect vVec = new Vect(domain1.length);
    for (int i = 0; i < domain1.length; i++) {
      dVec.addElement(domain1[i]);
      vVec.addElement(values1[i]);
    }
    for (int i = 0; i < domain2.length; i++) {
      boolean found = false;
      for (int j = 0; j < domain1.length; j++) {
	if (domain2[i].equals(domain1[j])) {
	  int v1 = ((IntValue)values1[j]).val;
	  int v2 = ((IntValue)values2[i]).val;
	  vVec.setElementAt(IntValue.gen(v1+v2), j);
	  found = true;
	  break;
	}
      }
      if (!found) {
	dVec.addElement(domain2[i]);
	vVec.addElement(values2[i]);
      }
    }
    Value[] domain = new Value[dVec.size()];
    Value[] values = new Value[dVec.size()];
    for (int i = 0; i < domain.length; i++) {
      domain[i] = (Value)dVec.elementAt(i);
      values[i] = (Value)vVec.elementAt(i);
    }
    return new FcnRcdValue(domain, values, false);
  }

  public static Value BagDiff(Value b1, Value b2) {
    FcnRcdValue fcn1 = FcnRcdValue.convert(b1);
    FcnRcdValue fcn2 = FcnRcdValue.convert(b2);
    if (fcn1 == null) {
      String msg = "The first argument of (-) should be a bag, but" +
	" instead it is:\n" + Value.ppr(b1.toString());
      throw new EvalException(msg);
    }
    if (fcn2 == null) {
      String msg = "The second argument of (-) should be a bag, but" +
	" instead it is:\n" + Value.ppr(b2.toString());
      throw new EvalException(msg);
    }
    Value[] domain1 = fcn1.domain;
    Value[] values1 = fcn1.values;
    Value[] domain2 = fcn2.domain;
    Value[] values2 = fcn2.values;
    Vect dVec = new Vect(domain1.length);
    Vect vVec = new Vect(domain1.length);
    for (int i = 0; i < domain1.length; i++) {
      int v1 = ((IntValue)values1[i]).val;
      for (int j = 0; j < domain2.length; j++) {
	if (domain1[i].equals(domain2[j])) {
	  int v2 = ((IntValue)values2[j]).val;
	  v1 -= v2;
	  break;
	}
      }
      if (v1 > 0) {
	dVec.addElement(domain1[i]);
	vVec.addElement(IntValue.gen(v1));
      }
    }
    Value[] domain = new Value[dVec.size()];
    Value[] values = new Value[vVec.size()];
    for (int i = 0; i < domain.length; i++) {
      domain[i] = (Value)dVec.elementAt(i);
      values[i] = (Value)vVec.elementAt(i);
    }
    return new FcnRcdValue(domain, values, fcn1.isNormalized());
  }

  public static Value BagUnion(Value s) {
    SetEnumValue s1 = SetEnumValue.convert(s);
    if (s1 == null) {
      String msg = "Applying BagUnion to the following value, which\n" +
	"is not a finite enumerable set:\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    ValueVec elems = s1.elems;
    int sz = elems.size();
    if (sz == 0) return EmptyFcn;
    if (sz == 1) return elems.elementAt(0);
    ValueVec dVec = new ValueVec();
    ValueVec vVec = new ValueVec();
    FcnRcdValue fcn = FcnRcdValue.convert(elems.elementAt(0));
    if (fcn == null) {
      String msg = "Applying BagUnion to the following set, whose\n" +
	"element is not a bag:\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    Value[] domain = fcn.domain;
    Value[] values = fcn.values;
    for (int i = 0; i < domain.length; i++) {
      dVec.addElement(domain[i]);
      vVec.addElement(values[i]);
    }
    for (int i = 1; i < sz; i++) {
      fcn = FcnRcdValue.convert(elems.elementAt(i));
      if (fcn == null) {
	String msg = "Applying BagUnion to the following set, whose\n" +
	  "element is not a bag:\n" + Value.ppr(s.toString());
	throw new EvalException(msg);
      }
      domain = fcn.domain;
      values = fcn.values;
      for (int j = 0; j < domain.length; j++) {
	boolean found = false;
	for (int k = 0; k < dVec.size(); k++) {
	  if (domain[j].equals(dVec.elementAt(k))) {
	    int v1 = ((IntValue)vVec.elementAt(k)).val;
	    int v2 = ((IntValue)values[j]).val;	
	    vVec.setElementAt(IntValue.gen(v1+v2), k);
	    found = true;
	    break;
	  }
	}
	if (!found) {
	  dVec.addElement(domain[j]);
	  vVec.addElement(values[j]);
	}
      }
    }
    Value[] dom = new Value[dVec.size()];
    Value[] vals = new Value[vVec.size()];
    for (int i = 0; i < dom.length; i++) {
      dom[i] = dVec.elementAt(i);
      vals[i] = vVec.elementAt(i);
    }
    return new FcnRcdValue(dom, vals, false);
  }

  public static BoolValue SqSubseteq(Value b1, Value b2) {
    FcnRcdValue fcn1 = FcnRcdValue.convert(b1);
    FcnRcdValue fcn2 = FcnRcdValue.convert(b2);
    if (fcn1 == null) {
      String msg = "Applying \\sqsubseteq to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b1.toString());
      throw new EvalException(msg);
    }
    if (fcn2 == null) {
      String msg = "Applying \\sqsubseteq to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b2.toString());
      throw new EvalException(msg);
    }
    Value[] domain1 = fcn1.domain;
    Value[] values1 = fcn1.values;
    Value[] domain2 = fcn2.domain;
    Value[] values2 = fcn2.values;
    for (int i = 0; i < domain1.length; i++) {
      int v1 = ((IntValue)values1[i]).val;
      for (int j = 0; j < domain2.length; j++) {
	if (domain1[i].equals(domain2[j])) {
	  int v2 = ((IntValue)values2[j]).val;
	  v1 -= v2;
	  break;
	}
      }
      if (v1 > 0) return ValFalse;
    }
    return ValTrue;
  }

  public static Value BagOfAll(Value f, Value b) {
    if (!(f instanceof Applicable)) {
      String msg = "The fisrt argument of BagOfAll must be an operator," +
	" but instead it is\n" + Value.ppr(f.toString());
      throw new EvalException(msg);
    }
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    if (fcn == null) {
      String msg = "The second argument of BagOfAll must be a function" +
	" with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    Applicable ff = (Applicable)f;
    ValueVec dVec = new ValueVec();
    ValueVec vVec = new ValueVec();
    Value[] domain = fcn.domain;
    Value[] values = fcn.values;
    Value[] args = new Value[1];
    for (int i = 0; i < domain.length; i++) {
      args[0] = domain[i];
      Value val = ff.apply(args, EvalControl.Clear);
      boolean found = false;
      for (int j = 0; j < dVec.size(); j++) {
	if (val.equals(dVec.elementAt(j))) {
	  int v1 = ((IntValue)vVec.elementAt(j)).val;
	  int v2 = ((IntValue)values[i]).val;	
	  vVec.setElementAt(IntValue.gen(v1+v2), j);
	  found = true;
	  break;
	}
      }
      if (!found) {
	dVec.addElement(val);
	vVec.addElement(values[i]);
      }
    }
    Value[] dom = new Value[dVec.size()];
    Value[] vals = new Value[vVec.size()];
    for (int i = 0; i < dom.length; i++) {
      dom[i] = dVec.elementAt(i);
      vals[i] = vVec.elementAt(i);
    }
    return new FcnRcdValue(dom, vals, false);
  }

  /******
   // For now, we do not override SubBag. So, We are using the TLA+ definition.
  public static Value SubBag(Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    if (fcn == null) {
      String msg = "Applying SubBag to the following value, which is\n" +
	"not a function with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    throw new EvalException("SubBag is not implemented.");
  }
  ******/
  
  public static Value BagToSet(Value b) {
    FcnRcdValue fcn = FcnRcdValue.convert(b);
    if (fcn == null) {
      String msg = "Applying BagToSet to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    return fcn.getDomain();
  }

  public static Value SetToBag(Value b) {
    SetEnumValue s1 = SetEnumValue.convert(b);
    if (s1 == null) {
      String msg = "Applying BagToSet to the following value, which\n" +
	"is not a function with a finite domain:\n" + Value.ppr(b.toString());
      throw new EvalException(msg);
    }
    ValueVec elems = s1.elems;
    Value[] domain = new Value[elems.size()];
    Value[] values = new Value[elems.size()];
    for (int i = 0; i < elems.size(); i++) {
      domain[i] = elems.elementAt(i);
      values[i] = ValOne;
    }
    return new FcnRcdValue(domain, values, s1.isNormalized());
  }

}
