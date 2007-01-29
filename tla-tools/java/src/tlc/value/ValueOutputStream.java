// Copyright (c) 2003 Microsoft Corporation.  All rights reserved.

package tlc.value;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.zambrovski.tla.RuntimeConfiguration;

import util.Assert;
import util.BufferedDataOutputStream;

public final class ValueOutputStream implements ValueConstants {

  private BufferedDataOutputStream dos;
  private HandleTable handles;

  public ValueOutputStream(File file) throws IOException {
    this.dos = new BufferedDataOutputStream(file);
    this.handles = new HandleTable();
  }

  public ValueOutputStream(String fname) throws IOException {
    this.dos = new BufferedDataOutputStream(fname);
    this.handles = new HandleTable();
  }

  public final void write(Value val) throws IOException {
    switch (val.getKind()) {
    case BOOLVALUE:
      {
	this.dos.writeByte(BOOLVALUE);
	this.dos.writeBoolean(((BoolValue)val).val);
	break;
      }
    case INTVALUE:
      {
	this.dos.writeByte(INTVALUE);
	dos.writeInt(((IntValue)val).val);
	break;
      }
    case STRINGVALUE:
      {
	int index = this.handles.put(val);
	if (index == -1) {
	  this.dos.writeByte(STRINGVALUE);
	  ((StringValue)val).val.write(this.dos);
	}
	else {
	  this.dos.writeByte(DUMMYVALUE);
	  this.writeNat(index);	  
	}
	break;
      }
    case MODELVALUE:
      {
	this.dos.writeByte(MODELVALUE);
	this.dos.writeShort((short)((ModelValue)val).index);
	break;
      }
    case INTERVALVALUE:
      {
	this.dos.writeByte(INTERVALVALUE);
	this.dos.writeInt(((IntervalValue)val).low);
	this.dos.writeInt(((IntervalValue)val).high);
	break;
      }
    case RECORDVALUE:
      {
	int index = this.handles.put(val);
	if (index == -1) {
	  this.dos.writeByte(RECORDVALUE);
	  RecordValue rval = (RecordValue)val;
	  int len = rval.names.length;
	  this.dos.writeInt((rval.isNormalized()) ? len : -len);
	  for (int i = 0; i < len; i++) {
	    int index1 = this.handles.put(rval.names[i]);
	    if (index1 == -1) {
	      this.dos.writeByte(STRINGVALUE);
	      rval.names[i].write(this.dos);
	    }
	    else {
	      this.dos.writeByte(DUMMYVALUE);
	      this.writeNat(index1);
	    }
	    this.write(rval.values[i]);
	  }
	}
	else {
	  this.dos.writeByte(DUMMYVALUE);
	  this.writeNat(index);
	}
	break;
      }
    case FCNRCDVALUE:
      {
	int index = this.handles.put(val);
	if (index == -1) {
	  this.dos.writeByte(FCNRCDVALUE);	  
	  FcnRcdValue fval = (FcnRcdValue)val;
	  int len = fval.values.length;
	  this.writeNat(len);
	  if (fval.intv != null) {
	    this.dos.writeByte((byte)0);
	    this.dos.writeInt(fval.intv.low);
	    this.dos.writeInt(fval.intv.high);
	    for (int i = 0; i < len; i++) {
	      this.write(fval.values[i]);
	    }
	  }
	  else {
	    this.dos.writeByte((fval.isNormalized()) ? (byte)1 : (byte)2);
	    for (int i = 0; i < len; i++) {
	      this.write(fval.domain[i]);
	      this.write(fval.values[i]);
	    }
	  }
	}
	else {
	  this.dos.writeByte(DUMMYVALUE);
	  this.writeNat(index);
	}
	break;
      }
    case SETENUMVALUE:
      {
	int index = this.handles.put(val);
	if (index == -1) {
	  this.dos.writeByte(SETENUMVALUE);
	  SetEnumValue sval = (SetEnumValue)val;
	  int len = sval.elems.size();
	  this.dos.writeInt((sval.isNormalized()) ? len : -len);
	  for (int i = 0; i < len; i++) {
	    this.write(sval.elems.elementAt(i));
	  }
	}
	else {
	  this.dos.writeByte(DUMMYVALUE);
	  this.writeNat(index);
	}
	break;
      }
    case TUPLEVALUE:
      {
	int index = this.handles.put(val);
	if (index == -1) {
	  this.dos.writeByte(TUPLEVALUE);
	  TupleValue tval = (TupleValue)val;
	  int len = tval.elems.length;
	  this.writeNat(len);
	  for (int i = 0; i < len; i++) {
	    this.write(tval.elems[i]);
	  }
	}
	else {
	  this.dos.writeByte(DUMMYVALUE);
	  this.writeNat(index);
	}
	break;
      }
    case SETCAPVALUE:
      {
	SetCapValue cap = (SetCapValue)val;
	// Assert.check(cap.capSet != null);
	this.write(cap.capSet);
	break;
      }
    case SETCUPVALUE:
      {
	SetCupValue cup = (SetCupValue)val;
	// Assert.check(cup.cupSet != null);
	this.write(cup.cupSet);
	break;
      }
    case SETDIFFVALUE:
      {
	SetDiffValue diff = (SetDiffValue)val;
	// Assert.check(diff.diffSet != null);
	this.write(diff.diffSet);
	break;
      }
    case SUBSETVALUE:
      {
	SubsetValue pset = (SubsetValue)val;
	// Assert.check(pset.pset != null);
	this.write(pset.pset);
	break;
      }
    case UNIONVALUE:
      {
	UnionValue uv = (UnionValue)val;
	// Assert.check(uv.realSet != null);
	this.write(uv.realSet);
	break;
      }
    case SETOFRCDSVALUE:
      {
	SetOfRcdsValue rcds = (SetOfRcdsValue)val;
	// Assert.check(rcds.rcdSet != null);
	this.write(rcds.rcdSet);
	break;
      }
    case SETOFFCNSVALUE:
      {
	SetOfFcnsValue fcns = (SetOfFcnsValue)val;
	// Assert.check(fcns.fcnSet != null);
	this.write(fcns.fcnSet);
	break;
      }
    case SETOFTUPLESVALUE:
      {
	SetOfTuplesValue tuples = (SetOfTuplesValue)val;
	// Assert.check(tuples.tupleSet != null);
	this.write(tuples.tupleSet);
	break;
      }
    case SETPREDVALUE:
      {
	SetPredValue spred = (SetPredValue)val;
	// Assert.check(spred.tool == null);
	this.write(spred.inVal);
	break;
      }
    case FCNLAMBDAVALUE:
      {
	FcnLambdaValue flambda = (FcnLambdaValue)val;
	// Assert.check(flambda.fcnRcd != null);
	this.write(flambda.fcnRcd);
	break;
      }
    default:
      {
	Assert.fail("ValueOutputStream: Can not pickle the value\n" +
		    Value.ppr(val.toString()));
      }
    }
  }

  public final void writeInt(int x) throws IOException {
    this.dos.writeInt(x);
  }

  public final void writeLong(long x) throws IOException {
    this.dos.writeLong(x);
  }
  
  public final void close() throws IOException {
    this.dos.close();
  }

  /* Precondition: x is a non-negative int. */
  public final void writeNat(int x) throws IOException {
    if (x > 0x7fff) {
      this.dos.writeInt(-x);
    }
    else {
      this.dos.writeShort((short)x);
    }
  }

  /* Precondition: x is a non-negative long. */
  public final void writeLongNat(long x) throws IOException {
    if (x <= 0x7fffffff) {
      this.dos.writeInt((int)x);
    }
    else {
      this.dos.writeLong(-x);
    }
  }
  
  private static class HandleTable {
    private int[] spine;
    private int[] next;
    private Object[] values;
    private int size;
    private int threshold;
    
    HandleTable() {
      this.spine = new int[17];
      Arrays.fill(spine, -1);
      this.next = new int[16];
      this.values = new Object[16];
      this.size = 0;
      this.threshold = (int)(this.spine.length * 0.75);
    }

    final int size() { return this.size; }
    
    final int put(Object val) {
      int index = (System.identityHashCode(val) & 0x7FFFFFFF) % this.spine.length;
      // lookup:
      for (int i = spine[index]; i >= 0; i = next[i]) {
	if (values[i] == val) { return i; }
      }
      // grow if needed:
      if (this.size >= this.next.length) {
	this.growEntries();
      }
      if (this.size >= this.threshold) {
	this.growSpine();
      }
      // add val to the table:
      this.values[this.size] = val;
      this.next[this.size] = this.spine[index];
      this.spine[index] = this.size;
      this.size++;
      return -1;
    }

    private final void growEntries() {
      int newLength = this.next.length * 2;
      int[] newNext = new int[newLength];
      System.arraycopy(this.next, 0, newNext, 0, this.size);
      this.next = newNext;

      Object[] newValues = new Object[newLength];
      System.arraycopy(this.values, 0, newValues, 0, this.size);
      this.values = newValues;
    }

    private final void growSpine() {
      this.spine = new int[(this.spine.length * 2) + 1];
      int len = this.spine.length;
      this.threshold = (int)(len * 0.75);
      Arrays.fill(this.spine, -1);
      for (int i = 0; i < this.size; i++) {
	int index = (System.identityHashCode(this.values[i]) & 0x7FFFFFFF) % len;
	this.next[i] = this.spine[index];
	this.spine[index] = i;
      }
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      RuntimeConfiguration.get().getErrStream().println("Usage: java tlc.value.ValueOutputStream filename.");
      System.exit(1);
    }
    
    IntValue[] aa = new IntValue[100];
    StringValue[] bb = new StringValue[100];
      
    for (int i = 0; i < aa.length; i++) {
      aa[i] = IntValue.gen(88);
    }

    StringValue sval = new StringValue("ssssssssss");
    for (int i = 0; i < bb.length; i++) {
      bb[i] = sval;
    }

    try {
      /**
      BufferedDataOutputStream dos = new BufferedDataOutputStream(args[0]+"_1");
      for (int i = 0; i < aa.length; i++) {
	dos.writeInt(88);
      }
      dos.close();
      
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(args[0]+"_2"));
      for (int i = 0; i < bb.length; i++) {
	oos.writeObject(bb[i]);
      }
      oos.close();

      ValueOutputStream vos = new ValueOutputStream(new File(args[0]+"_3"));
      for (int i = 0; i < bb.length; i++) {
	vos.write(bb[i]);
      }
      vos.close();
      **/

      ValueOutputStream vos = new ValueOutputStream(new File(args[0]));
      long x = 1;
      for (int i = 0; i < 63; i++) {
	RuntimeConfiguration.get().getErrStream().println("write " + x);
	vos.writeLongNat(x);
	x = x * 2;
      }
      vos.close();

      ValueInputStream vis = new ValueInputStream(new File(args[0]));
      for (int i = 0; i < 63; i++) {
	RuntimeConfiguration.get().getErrStream().println("read " + vis.readLongNat());
      }
      vis.close();      
    }
    catch (Exception e) { }
  }
  
}
