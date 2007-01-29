// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu Nov 16 15:53:30 PST 2000 by yuanyu

package tlc.value;

import java.util.Enumeration;

import tlc.util.Vect;
import util.Assert;
import util.Set;

public final class MVPerm {
  private ModelValue[] elems;
  private int count;

  public MVPerm() {
    this.elems = new ModelValue[ModelValue.mvs.length];
    this.count = 0;
  }

  public final boolean equals(Object obj) {
    if (obj instanceof MVPerm) {
      MVPerm perm = (MVPerm)obj;
      for (int i = 0; i < this.elems.length; i++) {
	if (this.elems[i] == null) {
	  if (perm.elems[i] != null) {
	    return false;
	  }
	}
	else if (!this.elems[i].equals(perm.elems[i])) {
	  return false;
	}
      }
      return true;
    }
    return false;
  }

  public final int hashCode() {
    int res = 0;
    for (int i = 0; i < this.elems.length; i++) {
      ModelValue mv = this.elems[i];
      if (mv != null) {
	res = 31*res + mv.val.hashCode();
      }
    }
    return res;
  }
  
  public final int size() { return this.count; }

  public final ModelValue get(ModelValue k) {
    return this.elems[k.index];
  }

  public final void put(ModelValue k, ModelValue elem) {
    if (!k.equals(elem) && this.elems[k.index] == null) {
      this.elems[k.index] = elem;
      this.count++;
    }
  }

  public final void put(int i, ModelValue elem) {
    if (this.elems[i] == null && elem != null) {
      this.elems[i] = elem;
      this.count++;
    }
  }
  
  public final MVPerm compose(MVPerm perm) {
    MVPerm res = new MVPerm();
    for (int i = 0; i < this.elems.length; i++) {
      ModelValue mv = this.elems[i];
      if (mv == null) {
	res.put(i, perm.elems[i]);
      }
      else {
	ModelValue mv1 = perm.elems[mv.index];
	if (mv1 == null) {
	  res.put(i, mv);
	}
	else if (!ModelValue.mvs[i].equals(mv1)) {
	  res.put(i, mv1);
	}
      }
    }
    return res;
  }
  
  public final String toString() {
    StringBuffer sb = new StringBuffer("[");
    int i = 0;
    for (i = 0; i < this.elems.length; i++) {
      if (this.elems[i] != null) {
	sb.append(ModelValue.mvs[i].toString());
	sb.append(" -> ");
	sb.append(this.elems[i].toString());
	break;
      }
    }
    for (int j = i+1; j < this.elems.length; j++) {
      if (this.elems[j] != null) {
	sb.append(", ");
	sb.append(ModelValue.mvs[j].toString());
	sb.append(" -> ");
	sb.append(this.elems[j].toString());
      }
    }
    sb.append("]");    
    return sb.toString();
  }

  public static final MVPerm[] permutationSubgroup(ValueEnumeration enum) {
    Set perms = new Set(20);
    Vect permVec = new Vect(20);
    // Compute the group generators:
    Value elem;
    while ((elem = enum.nextElement()) != null) {
      FcnRcdValue fcn = FcnRcdValue.convert(elem);
      if (fcn == null) {
	Assert.fail("The symmetry operator must specify a set of functions.");
      }
      MVPerm perm = new MVPerm();
      for (int i = 0; i < fcn.domain.length; i++) {
	Value dval = fcn.domain[i];
	Value rval = fcn.values[i];
	if ((dval instanceof ModelValue) && (rval instanceof ModelValue)) {
	  perm.put((ModelValue)dval, (ModelValue)rval);
	}
	else {
	  Assert.fail("Symmetry function must have model values as domain and range.");
	}
      }
      if (perm.size() > 0 && perms.put(perm) == null) {
	permVec.addElement(perm);
      }
    }
    // Compute the group generated by the generators:
    int gsz = permVec.size();
    int sz0 = 0;
    while (true) {
      int sz1 = permVec.size();
      for (int i = 0; i < gsz; i++) {
	MVPerm perm1 = (MVPerm)permVec.elementAt(i);
	for (int j = sz0; j < sz1; j++) {
	  MVPerm perm = perm1.compose((MVPerm)permVec.elementAt(j));
	  if (perm.size() > 0 && perms.put(perm) == null) {
	    permVec.addElement(perm);
	  }
	}
      }
      if (sz1 == permVec.size()) break;
      sz0 = sz1;
    }
    // Finally, put all the elements in an array ready for use:
    MVPerm[] res = new MVPerm[permVec.size()];
    Enumeration permEnum = permVec.elements();
    for (int i = 0; i < res.length; i++) {
      res[i] = (MVPerm)permEnum.nextElement();
    }
    return res;
  }

}
