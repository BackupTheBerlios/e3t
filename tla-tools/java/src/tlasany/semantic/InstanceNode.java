// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import tlasany.st.TreeNode;
import tlasany.utilities.Strings;
import util.Assert;
import util.UniqueString;

public class InstanceNode extends LevelNode {

  /**
   * This class represents a TLA module instantiation whose general form is
   *
   *    I(param[1], ... , param[p]) ==                                    
   *       INSTANCE M WITH mparam[1] <- mexp[1], ... , mparam[r] <- mexp[r]
   *
   * or simply
   *
   *   INSTANCE M WITH mparam[1] <- mexp[1], ... , mparam[r] <- mexp[r]
   * 
   *  where I         instance name
   *        param[i]  instance paramater
   *        M         moodule being instantiated
   *        mparam[i] constant and variable declared in M
   *        mexp[i]   expression substituted for mexp[i] in this instance
   *
   *  The parameters param[1] ... params[p] may be missing if there
   *  are no instance params; the name I (and the "==") may also be
   *  missing if this is an unnamed instance.  The substitutions WITH
   *  mparam[1] <- mexp[1], ... , mparam[r] <- mexp[r] may be missing if
   *  module M does not declare any constants or variables; and any
   *  particular substitution mparam[i] <- mexp[i] if the expression
   *  mexp[i] is a simply a reference to a constant or variable
   *  declared in the current module that has the same name as mparam[i]
   *  declared in M (or a module M extends).
   */      

  UniqueString      name;     // The name of this instance, e.g. "I" in the example above;
                              //   null if this is an unnamed instance.
  FormalParamNode[] params;   // The instance parameters, e.g. param[1]  ... param[n] in the
                              // example above. Have length zero if there is no param.
  ModuleNode        module;   // Reference to the module, M, being instantiated
  Subst[]           substs;   // The substitutions mparam[1] <- mexp[1], ... , mparam[r] <- mexp[r]
                              // This includes substitutions not explicitly mentioned in the
                              // surface syntax because they are of the form c <- c or x <- x
                              // Have length 0 if there is no substitution.

  public InstanceNode(UniqueString name, FormalParamNode[] params,
		      ModuleNode module, Subst[] substs, TreeNode stn) {
    super(InstanceKind, stn);
    this.name = name;
    this.params = (params != null ? params : new FormalParamNode[0]);
    this.module = module;
    this.substs = (substs != null ? substs : new Subst[0]);
  }

  /* Level checking */
  private boolean levelCorrect;
  private SetOfLevelConstraints levelConstraints;
  private SetOfArgLevelConstraints argLevelConstraints;
  private HashSet argLevelParams;
  
  public final boolean levelCheck() {
    if (this.levelConstraints != null) return this.levelCorrect;
    
    // First, level check the components.
    this.levelCorrect = true;
    if (!this.module.levelCheck()) {
      this.levelCorrect = false;
    }
    for (int i = 0; i < this.substs.length; i++ ) {
      if (!this.substs[i].getExpr().levelCheck()) {
	this.levelCorrect = false;
      }
    }

    // Check constraints on the substitution.
    if (!this.module.isConstant()) {
      for (int i = 0; i < this.substs.length; i++ ) {
	SymbolNode mparam = substs[i].getOp();
	ExprOrOpArgNode mexp = substs[i].getExpr();	
	if (mexp.getLevel() > mparam.getLevel()) {
	  if (mexp.levelCheck() &&
	      mparam.levelCheck()) {
	    errors.addError(this.stn.getLocation(), 
			    "Level error in instantiating module '" + module.getName() +
			    "':\nThe level of the expression or operator substituted for '" +
			    mparam.getName() + "' must be at most " + mparam.getLevel() + ".");
	  }
          this.levelCorrect = false;
	}
      }
    }

    SetOfLevelConstraints lcSet = this.module.getLevelConstraints();
    SetOfArgLevelConstraints alcSet = this.module.getArgLevelConstraints();    
    for (int i = 0; i < this.substs.length; i++ ) {
      SymbolNode mparam = substs[i].getOp();
      ExprOrOpArgNode mexp = substs[i].getExpr();
      Integer plevel = (Integer)lcSet.get(mparam);
      if (plevel != null &&
	  mexp.getLevel() > plevel.intValue()) {
	if (mexp.levelCheck()) {
	  errors.addError(this.stn.getLocation(), 
			  "Level error in instantiating module '" + module.getName() +
			  "':\nThe level of the expression or operator substituted for '" +
			  mparam.getName() + "' must be at most " + plevel + ".");
	}
	this.levelCorrect = false;
      }
      
      int alen = mparam.getArity();
      if (alen > 0 && ((OpArgNode)mexp).getOp() instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)((OpArgNode)mexp).getOp();
	for (int j = 0; j < alen; j++) {
	  ParamAndPosition pap = new ParamAndPosition(mparam, j);
	  Integer alevel = (Integer)alcSet.get(pap);
	  if (alevel != null &&
	      opDef.getMaxLevel(j) < alevel.intValue()) {
	    if (opDef.levelCheck()) {
	      errors.addError(this.stn.getLocation(), 
			      "Level error in instantiating module '" + module.getName() +
			      "':\nThe level of the argument " + j + " of the operator " +
			      opDef.getName() + " must be at least " + plevel + ".");
	    }
	    this.levelCorrect = false;
	  }
	}
      }
    }

    Iterator iter = this.module.getArgLevelParams().iterator();
    while (iter.hasNext()) {
      ArgLevelParam alp = (ArgLevelParam)iter.next();
      for (int i = 0; i < this.substs.length; i++) {
	SymbolNode pi = this.substs[i].getOp();
	for (int j = 0; j < this.substs.length; j++) {
	  if (alp.op == pi &&
	      alp.param == this.substs[j].getOp()) {
	    SymbolNode op = ((OpArgNode)this.substs[i].getExpr()).getOp();
	    if (op instanceof OpDefNode && 
		this.substs[j].getExpr().getLevel() > ((OpDefNode)op).getMaxLevel(alp.i)) {
	      if (op.levelCheck() &&
		  this.substs[j].getExpr().levelCheck()) {
		errors.addError(this.stn.getLocation(), 
				"Level error when instantiating module '" + module.getName() +
				"':\nThe level of the argument " + alp.i + " of the operator " +
				pi.getName() + "' must be at most " +
				((OpDefNode)op).getMaxLevel(alp.i) + ".");
	      }
	      this.levelCorrect = false;
	    }
	  }
	}
      }
    }
    
    // Calculate level information.
    this.levelConstraints = new SetOfLevelConstraints();
    lcSet = Subst.getSubLCSet(this.module, this.substs, this.module.isConstant());
    iter = lcSet.keySet().iterator();
    while (iter.hasNext()) {
      SymbolNode param = (SymbolNode)iter.next();
      if (!param.occur(this.params)) {
	this.levelConstraints.put(param, lcSet.get(param));
      }
    }
    for (int i = 0; i < this.substs.length; i++) {
      lcSet = this.substs[i].getExpr().getLevelConstraints();
      iter = lcSet.keySet().iterator();
      while (iter.hasNext()) {
	SymbolNode param = (SymbolNode)iter.next();
	if (!param.occur(this.params)) {
	  this.levelConstraints.put(param, lcSet.get(param));
	}
      }
    }
    
    this.argLevelConstraints = new SetOfArgLevelConstraints();
    alcSet = Subst.getSubALCSet(this.module, this.substs);
    iter = alcSet.keySet().iterator();
    while (iter.hasNext()) {
      ParamAndPosition pap = (ParamAndPosition)iter.next();
      if (!pap.param.occur(this.params)) {
	this.argLevelConstraints.put(pap, alcSet.get(pap));
      }
    }
    for (int i = 0; i < this.substs.length; i++) {
      alcSet = this.substs[i].getExpr().getArgLevelConstraints();
      iter = alcSet.keySet().iterator();
      while (iter.hasNext()) {
	ParamAndPosition pap = (ParamAndPosition)iter.next();
	if (!pap.param.occur(this.params)) {
	  this.argLevelConstraints.put(pap, alcSet.get(pap));
	}
      }
    }
    
    this.argLevelParams = new HashSet();
    HashSet alpSet = Subst.getSubALPSet(this.module, this.substs);
    iter = alpSet.iterator();
    while (iter.hasNext()) {
      ArgLevelParam alp = (ArgLevelParam)iter.next();
      if (!alp.occur(this.params)) {
	this.argLevelParams.add(alp);
      }
    }
    for (int i = 0; i < this.substs.length; i++) {
      alpSet = this.substs[i].getExpr().getArgLevelParams();
      iter = alpSet.iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	if (!alp.occur(this.params)) {
	  this.argLevelParams.add(alp);
	}
      }
    }
    return this.levelCorrect;
  }

  public final int getLevel() {
    Assert.fail("Internal Error: Should never call InstanceNode.getLevel().");
    return -1;    // make compiler happy
  }

  public final HashSet getLevelParams() {
    Assert.fail("Internal Error: Should never call InstanceNode.getLevelParams().");
    return null;    // make compiler happy
  }
  
  public final SetOfLevelConstraints getLevelConstraints() { 
    return this.levelConstraints;
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() { 
    return this.argLevelConstraints;
  }
  
  public final HashSet getArgLevelParams() { return this.argLevelParams; }

  public final String levelDataToString() {
    return "LevelConstraints: "    + this.levelConstraints    + "\n" +
           "ArgLevelConstraints: " + this.argLevelConstraints + "\n" +
           "ArgLevelParams: "      + this.argLevelParams      + "\n";
  }

  public final void walkGaph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);
    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(new Integer(myUID), this);

    for (int i = 0; i < params.length; i++) {
      params[i].walkGraph(semNodesTable);
    }
    module.walkGraph(semNodesTable);
  }

  public final String toString(int depth) {
    if (depth <= 0) return "";

    String ret = "\n*InstanceNode " + super.toString(depth) + 
                 "  InstanceName = " + (name == null ? "(none)" : name.toString()) +
                 Strings.indent(2, "\nModule: " + module.getName());
    if (params.length > 0) {
      ret += Strings.indent(2,"\nInstance parameters:");
      for ( int i = 0; i < params.length; i++ ) {
        ret += Strings.indent(4,params[i].toString(depth-1));
      }
    }

    if (substs.length > 0) {
      ret += Strings.indent(2, "\nSubstitutions:");
      for (int i = 0; i < substs.length; i++) {
        ret += Strings.indent(2,
			      Strings.indent(2, "\nSubst:" +
					     (substs[i] != null ?
					      Strings.indent(2, substs[i].toString(depth-1)) :
					      "<null>")));
      }
    }
    return ret;
  }

}
