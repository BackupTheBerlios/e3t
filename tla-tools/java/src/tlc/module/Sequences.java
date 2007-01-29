// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Jun 29 23:58:36 PDT 2001 by yuanyu

package tlc.module;

import tlc.tool.EvalControl;
import tlc.tool.EvalException;
import tlc.tool.TLARegistry;
import tlc.value.Applicable;
import tlc.value.BoolValue;
import tlc.value.IntValue;
import tlc.value.ModelValue;
import tlc.value.OpLambdaValue;
import tlc.value.OpRcdValue;
import tlc.value.TupleValue;
import tlc.value.UserObj;
import tlc.value.UserValue;
import tlc.value.Value;
import tlc.value.ValueConstants;
import tlc.value.ValueVec;
import util.Assert;

public class Sequences extends UserObj implements ValueConstants {
  private Value range;
  private int size;

  public Sequences(Value range, int size) {
    this.range = range;
    this.size = size;
  }

  static {
    Assert.check(TLARegistry.put("Concat", "\\o") == null);
  }
    
  /* The set of all sequences of value range. */
  public static Value Seq(Value range) {
    UserObj obj = new Sequences(range, Integer.MAX_VALUE);
    return new UserValue(obj);
  }

  public static IntValue Len(Value s) {
    TupleValue seq = TupleValue.convert(s);
    if (seq != null) {
      return IntValue.gen(seq.size());
    }
    String msg = "The argument of Len should be a sequence; but " +
      "instead it is:\n" + Value.ppr(s.toString());
    throw new EvalException(msg);
  }

  public static Value Head(Value s) {
    TupleValue seq = TupleValue.convert(s);
    if (seq != null) {
      if (seq.size() == 0) {
	throw new EvalException("Attempted to apply Head to the empty sequence.");
      }
      return seq.elems[0];
    }
    String msg = "The argument of Head should be a sequence; but instead" +
      " it is:\n" + Value.ppr(s.toString()); 
    throw new EvalException(msg);
  }
  
  public static Value Tail(Value s) {
    TupleValue seq = TupleValue.convert(s);
    if (seq != null) {
      if (seq.size() == 0) {
	throw new EvalException("Applying Tail to the empty sequence.");
      }
      int len = seq.size();
      Value[] vals = new Value[len-1];
      System.arraycopy(seq.elems, 1, vals, 0, vals.length);
      return new TupleValue(vals);
    }
    String msg = "The argument of Tail should be a sequence; but instead" +
      " it is:\n" + Value.ppr(s.toString()); 
    throw new EvalException(msg);
  }

  public static Value Cons(Value v, Value s) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "Evaluating an expression of the form Cons(v, s)" +
	" when s is \nnon-sequence:\n" + Value.ppr(s.toString()); 
      throw new EvalException(msg);
    }
    int len = seq.size();
    Value[] values = new Value[len+1];
    values[0] = v;
    System.arraycopy(seq.elems, 0, values, 1, len);
    return new TupleValue(values);
  }

  public static Value Append(Value s, Value v) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "Evaluating an expression of the form Append(s, v)" +
	" when s is the\nnon-sequence:\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    int len = seq.size();
    Value[] values = new Value[len+1];
    System.arraycopy(seq.elems, 0, values, 0, len);
    values[len] = v;
    return new TupleValue(values);
  }
  
  public static Value Concat(Value s1, Value s2) {
    TupleValue seq1 = TupleValue.convert(s1);
    TupleValue seq2 = TupleValue.convert(s2);
    if (seq1 == null) {
      String msg = "Evaluating an expression of the form s \\o t when" +
	" s is the\nnon-sequence:\n" + Value.ppr(s1.toString());
      throw new EvalException(msg);
    }
    if (seq2 == null) {
      String msg = "Evaluating an expression of the form s \\o t when" +
	" t is the\nnon-sequence:\n" + Value.ppr(s2.toString());
      throw new EvalException(msg);
    }
    int len1 = seq1.size();
    int len2 = seq2.size();
    if (len1 == 0) return seq2;
    if (len2 == 0) return seq1;
    Value[] values = new Value[len1+len2];
    for (int i = 0; i < len1; i++) {
      values[i] = seq1.elems[i];
    }
    for (int i = 0; i < len2; i++) {
      values[i+len1] = seq2.elems[i];
    }
    return new TupleValue(values);
  }

  /**
   * Returns the index (starting from 1) of the first element to match.
   * If no match, return 0.
   */
  public static Value SelectInSeq(Value s, Value test) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "The first argument of SelectInSeq must be " +
	"a sequence, but instead it is\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    if (!(test instanceof Applicable)) {
      String msg = "The second argument of SelectInSeq must be " +
	"a function, but instead it is\n" + Value.ppr(test.toString());
      throw new EvalException(msg);
    }
    int len = seq.size();
    Applicable ftest = (Applicable)test;
    Value[] args = new Value[1];
    for (int i = 0; i < len; i++) {
      args[0] = seq.elems[i];
      Value val = ftest.apply(args, EvalControl.Clear);
      if (!(val instanceof BoolValue)) {
	String msg = "The second argument of SelectInSeq must be a boolean" +
	  " valued function, but instead it is\n" + Value.ppr(test.toString());
	throw new EvalException(msg);
      }
      if (((BoolValue)val).val) return IntValue.gen(i+1);
    }
    return ValZero;
  }

  /**  Not in the standard interface.
  public static Value Remove(Value s, Value index) {
    TupleValue seq = TupleValue.convert(s);
    if (seq != null) {
      if (index instanceof IntValue) {
	int ridx = ((IntValue)index).val;
	int len = seq.size();
	if (ridx > 0 && ridx <= len) {
	  Value[] values = new Value[len-1];
	  for (int i = 0; i < ridx - 1; i++) {
	    values[i] = seq.elems[i];
	  }
	  for (int j = ridx; j < len; j++) {
	    values[j-1] = seq.elems[j];
	  }
	  return new TupleValue(values);
	}
	else {
	  String msg = "The second argument of Remove must be in the " +
	    "domain of its first argument:\n" + Value.ppr(s.toString()) +
	    "\n, but instead it is\n" + Value.ppr(index.toString());
	  throw new EvalException(msg);
	}
      }
      else {
	String msg = "The second argument of Remove must be a natural " +
	  "number,\nbut instead it is\n" + Value.ppr(index.toString());
	throw new EvalException(msg);
      }
    }
    String msg = "The first argument of Remove must be " +
      "a sequence, but instead it is\n" + Value.ppr(s.toString());
    throw new EvalException(msg);
  }
  **/

  public static Value SubSeq(Value s, Value m, Value n) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "The first argument of SubSeq should be a sequence;" +
	" but instead it is:\n" + Value.ppr(s.toString()); 
      throw new EvalException(msg);
    }
    if (!(m instanceof IntValue)) {
      String msg = "The second argument of SubSeq must be a natural number;" +
	" but instead it is:\n" + Value.ppr(m.toString()); 
      throw new EvalException(msg);
    }
    if (!(n instanceof IntValue)) {
      String msg = "The third argument of SubSeq must be a natural number;" +
	" but instead it is:\n" + Value.ppr(n.toString()); 
      throw new EvalException(msg);
    }
    int beg = ((IntValue)m).val;
    int end = ((IntValue)n).val;
    if (beg > end) return EmptyTuple;
    int len = seq.size();
    int sublen = end - beg + 1;
    if (beg < 1 || beg > len) {
      String msg = "The second argument of SubSeq must be in the domain" +
	" of its first argument:\n" + Value.ppr(s.toString()) + "\n, but" +
	" instead it is\n" + Value.ppr(m.toString());
      throw new EvalException(msg);
    }
    if (end < 1 || end > len) {
      String msg = "The third argument of SubSeq must be in the domain" +
	" of its first argument:\n" + Value.ppr(s.toString()) + "\nbut instead" +
	" it is\n" + Value.ppr(n.toString());
      throw new EvalException(msg);
    }
    Value[] elems = new Value[sublen];
    for (int i = 0; i < sublen; i++) {
      elems[i] = seq.elems[beg+i-1];
    }
    return new TupleValue(elems);
  }

  public static Value SelectSeq(Value s, Value test) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "The first argument of SelectSeq must be a sequence," +
	"\nbut instead it is\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    int len = seq.size();
    if (len == 0) return EmptyTuple;
    if (!(test instanceof OpLambdaValue) &&
	!(test instanceof OpRcdValue)) {
      String msg = "The second argument of SelectSeq must be an operator," +
	"\nbut instead it is\n" + Value.ppr(test.toString());
      throw new EvalException(msg);
    }
    ValueVec vals = new ValueVec();
    Applicable ftest = (Applicable)test;
    Value[] args = new Value[1];
    for (int i = 0; i < len; i++) {
      args[0] = seq.elems[i];
      Value val = ftest.apply(args, EvalControl.Clear);
      if (val instanceof BoolValue) {
	if (((BoolValue)val).val)
	  vals.addElement(args[0]);
      }
      else {
	String msg = "The second argument of SelectSeq must be a boolean" +
	  " valued operator, but instead it is\n" + Value.ppr(test.toString());
	throw new EvalException(msg);
      }
    }
    Value[] elems = new Value[vals.size()];
    for (int i = 0; i < elems.length; i++) {
      elems[i] = vals.elementAt(i);
    }
    return new TupleValue(elems);
  }

  public final int compareTo(Value s) {
    if ((s instanceof UserValue) &&
	(((UserValue)s).userObj instanceof Sequences)) {
      Sequences seq = (Sequences)((UserValue)s).userObj;
      int cmp = this.size - seq.size;
      if (cmp == 0) {
	cmp = this.range.compareTo(seq.range);
      }
      return cmp;
    }
    if (s instanceof ModelValue) return 1;    
    String msg = "Comparing the value\n" + Value.ppr(this.toString()) +
      "\nwith the value:\n" + Value.ppr(s.toString());
    throw new EvalException(msg);
  }

  public final boolean member(Value s) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      if (s instanceof ModelValue) return false;
      String msg ="Attempted to check if the value\n" + Value.ppr(s.toString()) +
	"\nis an element of\n" + Value.ppr(this.toString());
      throw new EvalException(msg);
    }
    int len = seq.size();
    if (len > this.size) return false;
    for (int i = 0; i < seq.elems.length; i++) {
      if (!this.range.member(seq.elems[i]))
	return false;
    }
    return true;
  }

  public final boolean isFinite() {
    return this.size != Integer.MAX_VALUE;
  }
  
  public final StringBuffer toString(StringBuffer sb, int offset) {
    if (this.size == Integer.MAX_VALUE) {
      sb = sb.append("Seq(");
      sb = this.range.toString(sb, offset);
      sb = sb.append(")");
    }
    else {
      sb = sb.append("BSeq(");
      sb = this.range.toString(sb, offset);
      sb = sb.append(", ");
      sb = sb.append(this.size);
      sb = sb.append(")");
    }
    return sb;
  }

  public static Value Insert(Value s, Value v, Value test) {
    TupleValue seq = TupleValue.convert(s);
    if (seq == null) {
      String msg = "The first argument of Insert must be a sequence," +
	" but instead it is\n" + Value.ppr(s.toString());
      throw new EvalException(msg);
    }
    if (!(test instanceof Applicable)) {
      String msg = "The second argument of SelectInSeq must be a" +
	" function, but instead it is\n" + Value.ppr(test.toString());
      throw new EvalException(msg);
    }
    int len = seq.size();
    Applicable ftest = (Applicable)test;
    Value[] args = new Value[2];
    args[0] = v;
    Value[] values = new Value[len+1];
    int idx = len;
    while (idx > 0) {
      args[1] = seq.elems[idx-1];
      Value val = ftest.apply(args, EvalControl.Clear);
      if (!(val instanceof BoolValue)) {
	String msg = "The third argument of Insert must be a boolean" +
	  " valued operator, but instead it is\n" + Value.ppr(test.toString());
	throw new EvalException(msg);
      }
      if (((BoolValue)val).val && v.compareTo(args[1]) < 0) {
	values[idx] = args[1];
	idx--;
      }
      else {
	values[idx] = v;
	break;
      }
    }
    if (idx == 0) {
      values[0] = v;
    }
    else {
      for (int i = idx-1; i >= 0; i--) {
	values[i] = seq.elems[i];
      }
    }
    return new TupleValue(values);
  }
  
}
