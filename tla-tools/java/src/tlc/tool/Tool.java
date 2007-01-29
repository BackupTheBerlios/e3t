// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Jan  4 22:46:57 PST 2002 by yuanyu 

package tlc.tool;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.semantic.AtNode;
import tlasany.semantic.ExprNode;
import tlasany.semantic.ExprOrOpArgNode;
import tlasany.semantic.FormalParamNode;
import tlasany.semantic.LetInNode;
import tlasany.semantic.ModuleNode;
import tlasany.semantic.OpApplNode;
import tlasany.semantic.OpArgNode;
import tlasany.semantic.OpDeclNode;
import tlasany.semantic.OpDefNode;
import tlasany.semantic.SemanticNode;
import tlasany.semantic.Subst;
import tlasany.semantic.SubstInNode;
import tlasany.semantic.SymbolNode;
import tlc.TLCGlobals;
import tlc.util.Context;
import tlc.util.Vect;
import tlc.value.Applicable;
import tlc.value.BoolValue;
import tlc.value.Enumerable;
import tlc.value.FcnLambdaValue;
import tlc.value.FcnParams;
import tlc.value.FcnRcdValue;
import tlc.value.LazyValue;
import tlc.value.MVPerm;
import tlc.value.MethodValue;
import tlc.value.OpLambdaValue;
import tlc.value.OpValue;
import tlc.value.RecordValue;
import tlc.value.Reducible;
import tlc.value.SetCapValue;
import tlc.value.SetCupValue;
import tlc.value.SetDiffValue;
import tlc.value.SetEnumValue;
import tlc.value.SetOfFcnsValue;
import tlc.value.SetOfRcdsValue;
import tlc.value.SetOfTuplesValue;
import tlc.value.SetPredValue;
import tlc.value.StringValue;
import tlc.value.SubsetValue;
import tlc.value.TupleValue;
import tlc.value.UnionValue;
import tlc.value.Value;
import tlc.value.ValueConstants;
import tlc.value.ValueEnumeration;
import tlc.value.ValueExcept;
import tlc.value.ValueVec;
import util.Assert;
import util.UniqueString;

/**
 * This class provides useful methods for tools like model checker
 * and simulator.
 */
public class Tool 
	extends Spec 
	implements ValueConstants, ToolGlobals, TraceApp 
{
  protected Action[] actions;     // the list of TLA actions.
  private CallStack callStack;    // the call stack.

  public Tool(String specDir, String specFile, String configFile) 
  {
    super(specDir, specFile, configFile);
    this.actions = null;
    this.callStack = null;
  }

  /* Initialization. Any Tool object must call it before doing anything. */
  public final void init(boolean preprocess) {
    // Parse and process this spec. It takes care of all overrides.
    this.processSpec();

    // Initialize state.
    if (TLCGlobals.coverage) 
    {
      TLCStateMutSource.init(this);
    } else {
      TLCStateMut.init(this);
    }

    // Pre-evaluate all the definitions in the spec that are constants.
    if (preprocess) 
    {
      this.processConstantDefns();
    }

    // Finally, process the config file.
    this.processConfig();
  }

  public final void setCallStack() { this.callStack = new CallStack(); }

  public final void printCallStack() {
    if (this.callStack.size() > 0) {
      RuntimeConfiguration.get().getErrStream().println(this.callStack);
    }
    else {
      RuntimeConfiguration.get().getErrStream().println("    The error call stack is empty.\n");
    }
  }
   
  /**
   * This method returns the set of all possible actions of the 
   * spec, and sets the actions field of this object. In fact, we
   * could simply treat the next predicate as one "giant" action.
   * But for efficiency, we preprocess the next state predicate by 
   * splitting it into a set of actions for the maximum prefix
   * of disjunction and existential quantification.
   */
  public final Action[] getActions() {
    if (this.actions == null) {
      Action next = this.getNextStateSpec();
      if (next == null) {
	this.actions = new Action[0];
      }
      else {
	this.getActions(next.pred, next.con);
	int sz = this.actionVec.size();
	this.actions = new Action[sz];
	for (int i = 0; i < sz; i++) {
	  this.actions[i] = (Action)this.actionVec.elementAt(i);
	}
      }
    }
    return this.actions;
  }

  private Vect actionVec = new Vect(10);
  
  private final void getActions(SemanticNode next, Context con) {
    switch (next.getKind()) {
    case OpApplKind:
      {
	OpApplNode next1 = (OpApplNode)next;
	this.getActionsAppl(next1, con);
	return;
      }
    case LetInKind:
      {
	LetInNode next1 = (LetInNode)next;
	this.getActions(next1.getBody(), con);
	return;
      }
    case SubstInKind:
      {
	SubstInNode next1 = (SubstInNode)next;
	Subst[] substs = next1.getSubsts();
	if (substs.length == 0) {
	  this.getActions(next1.getBody(), con);
	}
	else {
	  Action action = new Action(next1, con);
	  this.actionVec.addElement(action);
	}
	return;
      }
    default:
      {
	Assert.fail("The next state relation is not a boolean expression.\n" + next);
      }
    }
  }

  private final void getActionsAppl(OpApplNode next, Context con) {
    ExprOrOpArgNode[] args = next.getArgs();
    SymbolNode opNode = next.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    if (opcode == 0) {
      Object val = this.lookup(opNode, con, false);

      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	opcode = BuiltInOPs.getOpCode(opDef.getName());
	if (opcode == 0) {
	  try {
	    FormalParamNode[] formals = opDef.getParams();
	    int alen = args.length;
	    Context con1 = con;
	    for (int i = 0; i < alen; i++) {
	      Value aval = this.eval(args[i], con, TLCState.Empty);
	      con1 = con1.cons(formals[i], aval);
	    }
	    this.getActions(opDef.getBody(), con1);
	    return;
	  }
	  catch (Throwable e) { /*SKIP*/ }
	}
      }
      if (opcode == 0) {
	Action action = new Action(next, con);
	this.actionVec.addElement(action);
	return;
      }
    }

    switch (opcode) {
    case OPCODE_be:     // BoundedExists
      {
	int cnt = this.actionVec.size();
	try {
	  ContextEnumerator enum =
	    this.contexts(next, con, TLCState.Empty, TLCState.Empty, EvalControl.Clear);
	  Context econ;
	  while ((econ = enum.nextElement()) != null) {
	    this.getActions(args[0], econ);
	  }
	}
	catch (Throwable e) {
	  Action action = new Action(next, con);
	  this.actionVec.removeAll(cnt);
	  this.actionVec.addElement(action);
	}
	return;
      }
    case OPCODE_dl:     // DisjList
    case OPCODE_lor:      
      {
	for (int i = 0; i < args.length; i++) {
	  this.getActions(args[i], con);
	}
	return;
      }
    default:
      {
	// We handle all the other builtin operators here.
	Action action = new Action(next, con);
	this.actionVec.addElement(action);
	return;
      }
    }
  }

  /*
   * This method returns the set of possible initial states that
   * satisfies the initial state predicate. Initial state predicate
   * can be under-specified.  Too many possible initial states will
   * probably make tools like TLC useless.
   */
  public final StateVec getInitStates() {
    Vect init = this.getInitStateSpec();
    ActionItemList acts = ActionItemList.Empty;
    StateVec initStates = new StateVec(0);
    for (int i = 1; i < init.size(); i++) {
      Action elem = (Action)init.elementAt(i);
      acts = acts.cons(elem.pred, elem.con, -1);
    }
    if (init.size() != 0) {
      Action elem = (Action)init.elementAt(0);
      TLCState ps = TLCState.Empty.createEmpty();
      this.getInitStates(elem.pred, acts, elem.con, ps, initStates);
    }
    return initStates;
  }

  /* Create the state specified by pred.  */
  public final TLCState makeState(SemanticNode pred) {
    ActionItemList acts = ActionItemList.Empty;
    TLCState ps = TLCState.Empty.createEmpty();
    StateVec states = new StateVec(0);
    this.getInitStates(pred, acts, Context.Empty, ps, states);
    if (states.size() != 1) {
      Assert.fail("The predicate does not specify a unique state." + pred);
    }
    TLCState state = states.elementAt(0);
    if (!this.isGoodState(state)) {
      Assert.fail("The state specified by the predicate is not complete." + pred);
    }
    return state;
  }

  private final void getInitStates(SemanticNode init, ActionItemList acts,
				   Context c, TLCState ps, StateVec states) {
    switch (init.getKind()) {
    case OpApplKind:
      {
	OpApplNode init1 = (OpApplNode)init;
	this.getInitStatesAppl(init1, acts, c, ps, states);
	return;
      }
    case LetInKind:
      {
	LetInNode init1 = (LetInNode)init;
	this.getInitStates(init1.getBody(), acts, c, ps, states);
	return;
      }
    case SubstInKind:
      {
	SubstInNode init1 = (SubstInNode)init;
	Subst[] subs = init1.getSubsts();
	Context c1 = c;
	for (int i = 0; i < subs.length; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, false));
	}
	this.getInitStates(init1.getBody(), acts, c1, ps, states);
	return;
      }
    default:
      {
	Assert.fail("The init state relation is not a boolean expression.\n" + init);
      }
    }
  }

  private final void getInitStates(ActionItemList acts, TLCState ps, StateVec states) {
    if (acts.isEmpty()) {
      TLCState newState = ps.copy();
      states.addElement(newState);
    }
    else {
      // Assert.check(act.kind > 0 || act.kind == -1);
      ActionItemList acts1 = acts.cdr();
      this.getInitStates(acts.carPred(), acts1, acts.carContext(), ps, states);
    }
  }

  private final void getInitStatesAppl(OpApplNode init, ActionItemList acts,
				       Context c, TLCState ps, StateVec states) {
    ExprOrOpArgNode[] args = init.getArgs();
    int alen = args.length;
    SymbolNode opNode = init.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    if (opcode == 0) {
      // This is a user-defined operator with one exception: it may
      // be substed by a builtin operator. This special case occurs
      // when the lookup returns an OpDef with opcode # 0.
      Object val = this.lookup(opNode, c, ps, false);

      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	opcode = BuiltInOPs.getOpCode(opDef.getName());
	if (opcode == 0) {
	  Context c1 = this.getOpContext(opDef, args, c, false);
	  this.getInitStates(opDef.getBody(), acts, c1, ps, states);
	  return;
	}
      }

      if (val instanceof LazyValue) {
	LazyValue lv = (LazyValue)val;
	if (lv.val == null || lv.val == ValUndef) {
	  this.getInitStates(lv.expr, acts, lv.con, ps, states);
	  return;
	}
	val = lv.val;
      }

      Object bval = val;
      if (alen == 0) {
	if (val instanceof MethodValue) {
	  bval = ((MethodValue)val).apply(EmptyValueArgs, EvalControl.Clear);
	}
      }
      else {
	if (val instanceof OpValue) {
	  Applicable opVal = (Applicable)val;
	  Value[] argVals = new Value[alen];
	  // evaluate the actuals:
	  for (int i = 0; i < alen; i++) {
	    argVals[i] = this.eval(args[i], c, ps);
	  }
	  // apply the operator:
	  bval = opVal.apply(argVals, EvalControl.Clear);
	}
      }

      if (opcode == 0) {
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing initial states, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + init);
	}
	if (((BoolValue)bval).val) {
	  this.getInitStates(acts, ps, states);
	}
	return;
      }
    }

    switch (opcode) {
    case OPCODE_dl:     // DisjList
    case OPCODE_lor:
      {
	for (int i = 0; i < alen; i++) {
	  this.getInitStates(args[i], acts, c, ps, states);
	}
	return;
      }
    case OPCODE_cl:     // ConjList
    case OPCODE_land:
      {
	for (int i = alen-1; i > 0; i--) {
	  acts = acts.cons(args[i], c, i);
	}
	this.getInitStates(args[0], acts, c, ps, states);
	return;
      }
    case OPCODE_be:     // BoundedExists
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(init, c, ps, TLCState.Empty, EvalControl.Clear);
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  this.getInitStates(body, acts, c1, ps, states);
	}
	return;
      }
    case OPCODE_bf:     // BoundedForall
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(init, c, ps, TLCState.Empty, EvalControl.Clear);
	Context c1 = enum.nextElement();
	if (c1 == null) {
	  this.getInitStates(acts, ps, states);
	}
	else {
	  ActionItemList acts1 = acts;
	  Context c2;
	  while ((c2 = enum.nextElement()) != null) {
	    acts1 = acts1.cons(body, c2, -1);
	  }
	  this.getInitStates(body, acts1, c1, ps, states);
	}
	return;
      }
    case OPCODE_ite:    // IfThenElse
      {
	Value guard = this.eval(args[0], c, ps);
	if (!(guard instanceof BoolValue)) {
	  Assert.fail("In computing initial states, a non-boolean expression(" +
		      guard.getKindString() + ") was used as the condition " +
		      "of an IF.\n" + init);
	}
	int idx = (((BoolValue)guard).val) ? 1 : 2;
	this.getInitStates(args[idx], acts, c, ps, states);
	return;
      }
    case OPCODE_case:   // Case
      {
	SemanticNode other = null;
	for (int i = 0; i < alen; i++) {
	  OpApplNode pair = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pairArgs = pair.getArgs();
	  if (pairArgs[0] == null) {
	    other = pairArgs[1];
	  }
	  else {
	    Value bval = this.eval(pairArgs[0], c, ps);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("In computing initial states, a non-boolean expression(" +
			  bval.getKindString() + ") was used as a guard condition" +
			  " of a CASE.\n" + pairArgs[1]);
	    }
	    if (((BoolValue)bval).val) {
	      this.getInitStates(pairArgs[1], acts, c, ps, states);
	      return;
	    }
	  }
	}
	if (other == null) {
	  Assert.fail("In computing initial states, TLC encountered a CASE with no" +
		      " conditions true.\n" + init);
	}
	this.getInitStates(other, acts, c, ps, states);
	return;
      }
    case OPCODE_fa:     // FcnApply
      {
	Value fval = this.eval(args[0], c, ps);
	if (fval instanceof FcnLambdaValue) {
	  FcnLambdaValue fcn = (FcnLambdaValue)fval;
	  if (fcn.fcnRcd == null) {
	    Context c1 = this.getFcnContext(fcn, args, c, ps, TLCState.Empty, EvalControl.Clear);
	    this.getInitStates(fcn.body, acts, c1, ps, states);
	    return;
	  }
	  fval = fcn.fcnRcd;
	}
	if (!(fval instanceof Applicable)) {
	  Assert.fail("In computing initial states, a non-function \n(" +
		      fval.getKindString() + ") was applied as a function.\n" + init);
	}
	Applicable fcn = (Applicable)fval;
	Value[] argVals = new Value[alen-1];
	for (int i = 0; i < argVals.length; i++) {
	  argVals[i] = this.eval(args[i+1], c, ps);
	}
	Value bval = fcn.apply(argVals, EvalControl.Clear);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing initial states, TLC expected a boolean" +
		      " expression,\nbut did not find one.\n" + init);
	}
	if (((BoolValue)bval).val) {
	  this.getInitStates(acts, ps, states);
	}
	return;
      }
    case OPCODE_eq:
      {
	SymbolNode var = this.getVar(args[0], c, false);
	if (var == null || var.getName().getVarLoc() < 0) {
	  Value bval = this.eval(init, c, ps);
	  if (!((BoolValue)bval).val) return;
	}
	else {
	  UniqueString varName = var.getName();	  
	  Value lval = ps.lookup(varName);
	  Value rval = this.eval(args[1], c, ps);
	  if (lval == null) {
	    ps = ps.bind(varName, rval, init);
	    this.getInitStates(acts, ps, states);
	    ps.unbind(varName);
	    return;
	  }
	  else {
	    if (!lval.equals(rval)) return;
	  }
	}
	this.getInitStates(acts, ps, states);
	return;
      }
    case OPCODE_in:
      {
	SymbolNode var = this.getVar(args[0], c, false);
	if (var == null || var.getName().getVarLoc() < 0) {
	  Value bval = this.eval(init, c, ps);
	  if (!((BoolValue)bval).val) return;
	}
	else {
	  UniqueString varName = var.getName();
	  Value lval = ps.lookup(varName);
	  Value rval = this.eval(args[1], c, ps);
	  if (lval == null) {
	    if (!(rval instanceof Enumerable)) {
	      Assert.fail("In computing initial states, the right side of \\IN" +
			  " is not enumerable.\n" + init);
	    }
	    ValueEnumeration enum = ((Enumerable)rval).elements();
	    Value elem;
	    while ((elem = enum.nextElement()) != null) {
	      ps.bind(varName, elem, init);
	      this.getInitStates(acts, ps, states);
	      ps.unbind(varName);
	    }
	    return;
	  }
	  else {
	    if (!rval.member(lval)) return;
	  }
	}
	this.getInitStates(acts, ps, states);
	return;
      }
    case OPCODE_implies:
      {
	Value lval = this.eval(args[0], c, ps);
	if (!(lval instanceof BoolValue)) {
	  Assert.fail("In computing initial states of a predicate of form" +
		      " P => Q, P was " + lval.getKindString() + "\n." + init);
	}
	if (((BoolValue)lval).val) {
	  this.getInitStates(args[1], acts, c, ps, states);
	}
	else {
	  this.getInitStates(acts, ps, states);
	}
	return;
      }
    default:
      {
	// For all the other builtin operators, simply evaluate:
	Value bval = this.eval(init, c, ps);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing initial states, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + init);
	}
	if (((BoolValue)bval).val) {
	  this.getInitStates(acts, ps, states);
	}
	return;
      }
    }
  }

  /**
   * This method returns the set of next states when taking the action
   * in the given state.
   */
  public final StateVec getNextStates(Action action, TLCState state) {
    ActionItemList acts = ActionItemList.Empty;
    TLCState s1 = TLCState.Empty.createEmpty();
    StateVec nss = new StateVec(0);
    this.getNextStates(action.pred, acts, action.con, state, s1, nss);
    return nss;
  }

  private final void getNextStates(SemanticNode pred, ActionItemList acts, Context c,
				   TLCState s0, TLCState s1, StateVec nss) {
    switch (pred.getKind()) {
    case OpApplKind:
      {
	OpApplNode pred1 = (OpApplNode)pred;
	this.getNextStatesAppl(pred1, acts, c, s0, s1, nss);
	return;
      }
    case LetInKind:
      {
	LetInNode pred1 = (LetInNode)pred;
	this.getNextStates(pred1.getBody(), acts, c, s0, s1, nss);
	return;
      }
    case SubstInKind:
      {
	SubstInNode pred1 = (SubstInNode)pred;
	Subst[] subs = pred1.getSubsts();
	int slen = subs.length;
	Context c1 = c;
	for (int i = 0; i < slen; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, false));
	}
	this.getNextStates(pred1.getBody(), acts, c1, s0, s1, nss);
	return;
      }
    default:
      {
	Assert.fail("The next state relation is not a boolean expression.\n" + pred);
      }
    }
  }

  private final void getNextStates(ActionItemList acts, TLCState s0, TLCState s1,
				   StateVec nss) {
    if (acts.isEmpty()) {
      TLCState newState = s1.copy();
      nss.addElement(newState);
    }
    else {
      int kind = acts.carKind();
      SemanticNode pred = acts.carPred();
      Context c = acts.carContext();
      ActionItemList acts1 = acts.cdr();
      if (kind > 0) {
	if (this.callStack != null) this.callStack.push(pred);
	this.getNextStates(pred, acts1, c, s0, s1, nss);
	if (this.callStack != null) this.callStack.pop();	
      }
      else if (kind == -1) {
	this.getNextStates(pred, acts1, c, s0, s1, nss);
      }
      else if (kind == -2) {
	this.processUnchanged(pred, acts1, c, s0, s1, nss);
      }
      else {
	Value v1 = this.eval(pred, c, s0);
	Value v2 = this.eval(pred, c, s1);
	if (!v1.equals(v2)) {
	  this.getNextStates(acts1, s0, s1, nss);
	}
      }
    }
  }
  
  private final void getNextStatesAppl(OpApplNode pred, ActionItemList acts, Context c,
				       TLCState s0, TLCState s1, StateVec nss) {
    ExprOrOpArgNode[] args = pred.getArgs();
    int alen = args.length;
    SymbolNode opNode = pred.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    if (opcode == 0) {
      // This is a user-defined operator with one exception: it may
      // be substed by a builtin operator. This special case occurs
      // when the lookup returns an OpDef with opcode # 0.
      Object val = this.lookup(opNode, c, s0, false);

      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	opcode = BuiltInOPs.getOpCode(opDef.getName());
	if (opcode == 0) {
	  Context c1 = this.getOpContext(opDef, args, c, false);
	  this.getNextStates(opDef.getBody(), acts, c1, s0, s1, nss);
	  return;
	}
      }

      if (val instanceof LazyValue) {
	LazyValue lv = (LazyValue)val;
	if (lv.val == null || lv.val == ValUndef) {
	  this.getNextStates(lv.expr, acts, lv.con, s0, s1, nss);
	  return;
	}
	val = lv.val;
      }

      Object bval = val;
      if (alen == 0) {
	if (val instanceof MethodValue) {
	  bval = ((MethodValue)val).apply(EmptyValueArgs, EvalControl.Clear);
	}
      }
      else {
	if (val instanceof OpValue) {
	  Applicable opVal = (Applicable)val;
	  Value[] argVals = new Value[alen];
	  // evaluate the actuals:
	  for (int i = 0; i < alen; i++) {
	    argVals[i] = this.eval(args[i], c, s0, s1, EvalControl.Clear);
	  }
	  // apply the operator:
	  bval = opVal.apply(argVals, EvalControl.Clear);
	}
      }

      if (opcode == 0) {
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing next states, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  this.getNextStates(acts, s0, s1, nss);
	}
	return;
      }
    }
    
    switch (opcode) {
    case OPCODE_cl:     // ConjList
    case OPCODE_land:      
      {
	ActionItemList acts1 = acts;
	for (int i = alen - 1; i > 0; i--) {
	  acts1 = acts1.cons(args[i], c, i);
	}
	if (this.callStack != null) this.callStack.push(args[0]);
	this.getNextStates(args[0], acts1, c, s0, s1, nss);
	if (this.callStack != null) this.callStack.pop();
	return;
      }
    case OPCODE_dl:     // DisjList
    case OPCODE_lor:      
      {
	for (int i = 0; i < alen; i++) {
	  if (this.callStack != null) this.callStack.push(args[i]);	  
	  this.getNextStates(args[i], acts, c, s0, s1, nss);
	  if (this.callStack != null) this.callStack.pop();	  
	}
	return;
      }
    case OPCODE_be:     // BoundedExists
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(pred, c, s0, s1, EvalControl.Clear);
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  this.getNextStates(body, acts, c1, s0, s1, nss);
	}
	return;	
      }
    case OPCODE_bf:     // BoundedForall
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(pred, c, s0, s1, EvalControl.Clear);
	Context c1 = enum.nextElement();
	if (c1 == null) {
	  this.getNextStates(acts, s0, s1, nss);
	}
	else {
	  ActionItemList acts1 = acts;
	  Context c2;
	  while ((c2 = enum.nextElement()) != null) {
	    acts1 = acts1.cons(body, c2, -1);
	  }
	  this.getNextStates(body, acts1, c1, s0, s1, nss);
	}
	return;
      }
    case OPCODE_fa:     // FcnApply
      {
	Value fval = this.eval(args[0], c, s0, s1, EvalControl.KeepLazy);
	if (fval instanceof FcnLambdaValue) {
	  FcnLambdaValue fcn = (FcnLambdaValue)fval;
	  if (fcn.fcnRcd == null) {
	    Context c1 = this.getFcnContext(fcn, args, c, s0, s1, EvalControl.Clear);
	    this.getNextStates(fcn.body, acts, c1, s0, s1, nss);
	    return;
	  }
	  fval = fcn.fcnRcd;
	}
	if (!(fval instanceof Applicable)) {
	  Assert.fail("In computing next states, a non-function \n(" +
		      fval.getKindString() + ") was applied as a function.\n" + pred);
	}
	Applicable fcn = (Applicable)fval;
	Value[] argVals = new Value[alen-1];
	for (int i = 0; i < argVals.length; i++) {
	  argVals[i] = this.eval(args[i+1], c, s0, s1, EvalControl.Clear);
	}
	Value bval = fcn.apply(argVals, EvalControl.Clear);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing next states, TLC expected a boolean" +
		      " expression,\nbut did not find one.\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  this.getNextStates(acts, s0, s1, nss);
	}
	return;
      }
    case OPCODE_aa:     // AngleAct <A>_e
      {
	ActionItemList acts1 = acts.cons(args[1], c, -3);
	this.getNextStates(args[0], acts1, c, s0, s1, nss);
	return;
      }
    case OPCODE_sa:	// [A]_e
      {
	this.getNextStates(args[0], acts, c, s0, s1, nss);
	this.processUnchanged(args[1], acts, c, s0, s1, nss);
	return;
      }
    case OPCODE_ite:    // IfThenElse
      {
	Value guard = this.eval(args[0], c, s0, s1, EvalControl.Clear);
	if (!(guard instanceof BoolValue)) {
	  Assert.fail("In computing next states, a non-boolean expression(" +
		      guard.getKindString() + ") was used as the condition of" +
		      " an IF." + pred);
	}
	if (((BoolValue)guard).val) {
	  this.getNextStates(args[1], acts, c, s0, s1, nss);
	}
	else {
	  this.getNextStates(args[2], acts, c, s0, s1, nss);
	}
	return;
      }
    case OPCODE_case:   // Case
      {
	SemanticNode other = null;
	for (int i = 0; i < alen; i++) {
	  OpApplNode pair = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pairArgs = pair.getArgs();
	  if (pairArgs[0] == null) {
	    other = pairArgs[1];
	  }
	  else {
	    Value bval = this.eval(pairArgs[0], c, s0, s1, EvalControl.Clear);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("In computing next states, a non-boolean expression(" +
			  bval.getKindString() + ") was used as a guard condition" +
			  " of a CASE.\n" + pairArgs[1]);
	    }
	    if (((BoolValue)bval).val) {
	      this.getNextStates(pairArgs[1], acts, c, s0, s1, nss);
	      return;
	    }
	  }
	}
	if (other == null) {
	  Assert.fail("In computing next states, TLC encountered a CASE with no" +
		      " conditions true.\n" + pred);
	}
	this.getNextStates(other, acts, c, s0, s1, nss);
	return;
      }
    case OPCODE_eq:
      {
	SymbolNode var = this.getPrimedVar(args[0], c, false);
	// Assert.check(var.getName().getVarLoc() >= 0);
	if (var == null) {
	  Value bval = this.eval(pred, c, s0, s1, EvalControl.Clear);
	  if (!((BoolValue)bval).val) return;
	}
	else {
	  UniqueString varName = var.getName();
	  Value lval = s1.lookup(varName);
	  Value rval = this.eval(args[1], c, s0, s1, EvalControl.Clear);
	  if (lval == null) {
	    s1.bind(varName, rval, pred);
	    this.getNextStates(acts, s0, s1, nss);
	    s1.unbind(varName);
	    return;
	  }
	  else {
	    if (!lval.equals(rval)) return;
	  }
	}
	this.getNextStates(acts, s0, s1, nss);
	return;
      }
    case OPCODE_in:
      {
	SymbolNode var = this.getPrimedVar(args[0], c, false);
	// Assert.check(var.getName().getVarLoc() >= 0);	
	if (var == null) {
	  Value bval = this.eval(pred, c, s0, s1, EvalControl.Clear);
	  if (!((BoolValue)bval).val) return;
	}
	else {
	  UniqueString varName = var.getName();
	  Value lval = s1.lookup(varName);
	  Value rval = this.eval(args[1], c, s0, s1, EvalControl.Clear);
	  if (lval == null) {
	    if (!(rval instanceof Enumerable)) {
	      Assert.fail("In computing next states, the right side of \\IN" +
			  " is not enumerable.\n" + pred);
	    }
	    ValueEnumeration enum = ((Enumerable)rval).elements();
	    Value elem;
	    while ((elem = enum.nextElement()) != null) {
	      s1.bind(varName, elem, pred);
	      this.getNextStates(acts, s0, s1, nss);
	      s1.unbind(varName);
	    }
	    return;
	  }
	  else {
	    if (!rval.member(lval)) return;
	  }
	}
	this.getNextStates(acts, s0, s1, nss);
	return;
      }
    case OPCODE_implies:
      {
	Value bval = this.eval(args[0], c, s0, s1, EvalControl.Clear);
	if (!(bval instanceof BoolValue)) {
	    Assert.fail("In computing next states of a predicate of the form" +
			" P => Q, P was\n" + bval.getKindString() + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  this.getNextStates(args[1], acts, c, s0, s1, nss);
	}
	else {
	  this.getNextStates(acts, s0, s1, nss);
	}
	return;
      }
    case OPCODE_unchanged:
      {
	this.processUnchanged(args[0], acts, c, s0, s1, nss);
	return;
      }
    case OPCODE_cdot:
      {
	Assert.fail("The current version of TLC does not support action composition.");
	/***
	TLCState s01 = TLCStateFun.Empty;
	StateVec iss = new StateVec(0);
	this.getNextStates(args[0], ActionItemList.Empty, c, s0, s01, iss);
	int sz = iss.size();
	for (int i = 0; i < sz; i++) {
	  s01 = iss.elementAt(i);
	  this.getNextStates(args[1], acts, c, s01, s1, nss);
	}
	***/
	return;
      }
    default:
      {
	// We handle all the other builtin operators here.
	Value bval = this.eval(pred, c, s0, s1, EvalControl.Clear);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing next states, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  this.getNextStates(acts, s0, s1, nss);
	}
	return;
      }
    }
  }

  private final void processUnchanged(SemanticNode expr, ActionItemList acts, Context c,
				      TLCState s0, TLCState s1, StateVec nss) {
    SymbolNode var = this.getVar(expr, c, false);
    if (var != null) {
      // expr is a state variable:
      UniqueString varName = var.getName();
      Value val0 = s0.lookup(varName);
      Value val1 = s1.lookup(varName);
      if (val1 == null) {
	s1.bind(varName, val0, expr);
	this.getNextStates(acts, s0, s1, nss);
	s1.unbind(varName);
      }
      else if (val0.equals(val1)) {
	this.getNextStates(acts, s0, s1, nss);
      }
      else {
	Assert.printWarning(TLCGlobals.warn,
			    "The variable " + varName + " was changed while" +
			    " it is specified as UNCHANGED at\n" + expr);
      }
      return;
    }
      
    if (expr instanceof OpApplNode) {
      OpApplNode expr1 = (OpApplNode)expr;
      ExprOrOpArgNode[] args = expr1.getArgs();
      int alen = args.length;
      SymbolNode opNode = expr1.getOperator();
      UniqueString opName = opNode.getName();
      int opcode = BuiltInOPs.getOpCode(opName);

      if (opcode == OPCODE_tup) {
	// a tuple:
	if (alen != 0) {
	  ActionItemList acts1 = acts;
	  for (int i = alen-1; i > 0; i--) {
	    acts1 = acts1.cons(args[i], c, -2);
	  }
	  this.processUnchanged(args[0], acts1, c, s0, s1, nss);
	  return;
	}
	this.getNextStates(acts, s0, s1, nss);
	return;
      }

      if (opcode == 0 && alen == 0) {
	// a 0-arity operator:
	Object val = this.lookup(opNode, c, false);

	if (val instanceof OpDefNode) {
	  this.processUnchanged(((OpDefNode)val).getBody(), acts, c, s0, s1, nss);
	  return;
	}
	else if (val instanceof LazyValue) {
	  LazyValue lv = (LazyValue)val;
	  this.processUnchanged(lv.expr, acts, lv.con, s0, s1, nss);
	  return;
	}
	else {
	  Assert.fail("In computing next states, TLC found the identifier\n" +
		      opName + " undefined in an UNCHANGED expression at\n" + expr);
	}
	this.getNextStates(acts, s0, s1, nss);
	return;
      }
    }
    
    Value v0 = this.eval(expr, c, s0);
    Value v1 = this.eval(expr, c, s1, null, EvalControl.Clear);
    if (v0.equals(v1)) {
      this.getNextStates(acts, s0, s1, nss);
    }
    return;
  }

  /* Special version of eval for state expressions. */
  public final Value eval(SemanticNode expr, Context c, TLCState s0) {
    return this.eval(expr, c, s0, TLCState.Empty, EvalControl.Clear);
  }
  
  /*
   * This method evaluates the expression expr in the given context,
   * current state, and partial next state.
   */
  public final Value eval(SemanticNode expr, Context c, TLCState s0,
			  TLCState s1, int control) {
    switch (expr.getKind()) {
    case OpApplKind:
      {
	OpApplNode expr1 = (OpApplNode)expr;
	return this.evalAppl(expr1, c, s0, s1, control);
      }
    case LetInKind:
      {
	LetInNode expr1 = (LetInNode)expr;
	OpDefNode[] letDefs = expr1.getLets();
	int letLen = letDefs.length;
	Context c1 = c;
	for (int i = 0; i < letLen; i++) {
	  OpDefNode opDef = letDefs[i];
	  if (opDef.getArity() == 0) {
	    Value rhs = new LazyValue(opDef.getBody(), c1);
	    c1 = c1.cons(opDef, rhs);
	  }
	}
	return this.eval(expr1.getBody(), c1, s0, s1, control);
      }
    case SubstInKind:
      {
	SubstInNode expr1 = (SubstInNode)expr;
	Subst[] subs = expr1.getSubsts();
	int slen = subs.length;
	Context c1 = c;
	for (int i = 0; i < slen; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, true));
	}
	return this.eval(expr1.getBody(), c1, s0, s1, control);
      }
    case NumeralKind:
    case DecimalKind:
    case StringKind:
      {
	return Value.getValue(expr);
      }
    case AtNodeKind:
      {
	AtNode expr1 = (AtNode)expr;
	Value atVal = (Value)c.lookup(EXCEPT_AT);
	return atVal;
      }
    case OpArgKind:
      {
	OpArgNode expr1 = (OpArgNode)expr;
	SymbolNode opNode = expr1.getOp();
	Object val = this.lookup(opNode, c, false);

	if (val instanceof OpDefNode) {
	  return new OpLambdaValue((OpDefNode)val, this, c, s0, s1);
	}
	return (Value)val;
      }
    default:
      {
	Assert.fail("Attempted to evaluate an expression that cannot be evaluated.\n" +
		    expr);
	return null;     // make compiler happy
      }
    }
  }

  public final Value evalAppl(OpApplNode expr, Context c, TLCState s0,
			      TLCState s1, int control) {
    ExprOrOpArgNode[] args = expr.getArgs();
    SymbolNode opNode = expr.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    if (opcode == 0) {
      // This is a user-defined operator with one exception: it may
      // be substed by a builtin operator. This special case occurs
      // when the lookup returns an OpDef with opcode # 0.
      if (this.callStack != null) this.callStack.push(expr);
      Object val = this.lookup(opNode, c, s0, EvalControl.isPrimed(control));

      // First, unlazy if it is a lazy value. We cannot use the cached
      // value when s1 == null or isEnabled(control).
      if (val instanceof LazyValue) {
	LazyValue lv = (LazyValue)val;
	if (s1 == null ||
	    lv.val == ValUndef ||
	    EvalControl.isEnabled(control)) {
	  val = this.eval(lv.expr, lv.con, s0, s1, control);
	}
	else {
	  if (lv.val == null) {
	    lv.val = this.eval(lv.expr, lv.con, s0, s1, control);
	  }
	  val = lv.val;
	}
      }

      Value res = null;
      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	opcode = BuiltInOPs.getOpCode(opDef.getName());
	if (opcode == 0) {
	  Context c1 = this.getOpContext(opDef, args, c, true);
	  res = this.eval(opDef.getBody(), c1, s0, s1, control);
	}
      }
      else if (val instanceof Value) {
	res = (Value)val;
	int alen = args.length;
	if (alen == 0) {
	  if (val instanceof MethodValue) {
	    res = ((MethodValue)val).apply(EmptyValueArgs, EvalControl.Clear);
	  }
	}
	else {
	  if (val instanceof OpValue) {
	    Applicable opVal = (Applicable)val;
	    Value[] argVals = new Value[alen];
	    // evaluate the actuals:
	    for (int i = 0; i < alen; i++) {
	      argVals[i] = this.eval(args[i], c, s0, s1, control);
	    }
	    // apply the operator:
	    res = opVal.apply(argVals, control);
	  }
	}
      }
      else {
	Assert.fail("The identifier " + opNode.getName() + " is either" +
		    " undefined or not an operator.\n" + expr);
      }
      if (this.callStack != null) this.callStack.pop();
      if (opcode == 0) return res;
    }

    switch (opcode) {
    case OPCODE_bc:     // BoundedChoose
      {
	// Assert.check(args.length == 1);
	SemanticNode pred = args[0];
	SemanticNode inExpr = expr.getBdedQuantBounds()[0];
	Value inVal = this.eval(inExpr, c, s0, s1, control);
	if (!(inVal instanceof Enumerable)) {
	  Assert.fail("Attempted to compute the value of an expression of\n" +
		      "form CHOOSE x \\in S: P, but S was not enumerable.\n" + expr);
	}
	inVal.normalize();	
	ValueEnumeration enumSet = ((Enumerable)inVal).elements();
	OpDeclNode[] bvars = expr.getBdedQuantSymbolLists()[0];	
	boolean isTuple = expr.isBdedQuantATuple()[0];
	if (isTuple) {
	  // Identifier tuple case:
	  int cnt = bvars.length;
	  Value val;
	  while ((val = enumSet.nextElement()) != null) {
	    TupleValue tv = TupleValue.convert(val);
	    if (tv == null || tv.size() != cnt) {
	      Assert.fail("Attempted to compute the value of an expression of form\n" +
			  "CHOOSE <<x1, ... , xN>> \\in S: P, but S was not a set\n" +
			  "of N-tuples.\n" + expr);
	    }
	    Context c1 = c;
	    for (int i = 0; i < cnt; i++) {
	      c1 = c1.cons(bvars[i], tv.elems[i]);
	    }
	    Value bval = this.eval(pred, c1, s0, s1, control);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("Expected a boolean expression, but did not find one.\n" +
			  expr);
	    }
	    if (((BoolValue)bval).val) return val;
	  }
	}
	else {
	  // Simple identifier case:
	  SymbolNode name = bvars[0];
	  Value val;
	  while ((val = enumSet.nextElement()) != null) {
	    Context c1 = c.cons(name, val);
	    Value bval = this.eval(pred, c1, s0, s1, control);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("Expected a boolean expression, but did not find one.\n" +
			  expr);
	    }
	    if (((BoolValue)bval).val) return val;
	  }
	}
	Assert.fail("Attempted to compute the value of an expression of form\n" +
		    "CHOOSE x \\in S: P, but no element of S satisfied P.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_be:     // BoundedExists
      {
	// Assert.check(args.length == 1);
	ContextEnumerator enum = this.contexts(expr, c, s0, s1, control);
	SemanticNode body = args[0];
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  Value bval = this.eval(body, c1, s0, s1, control);
	  if (!(bval instanceof BoolValue)) {
	    Assert.fail("Expected a boolean expression, but did not find one.\n" +
			expr);
	  }
	  if (((BoolValue)bval).val) return ValTrue;
	}
	return ValFalse;	
      }
    case OPCODE_bf:     // BoundedForall
      {
	// Assert.check(args.length == 1);
	ContextEnumerator enum = this.contexts(expr, c, s0, s1, control);
	SemanticNode body = args[0];
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  Value bval = this.eval(body, c1, s0, s1, control);
	  if (!(bval instanceof BoolValue)) {
	    Assert.fail("Expected a boolean expression, but did not find one.\n" +
			expr);
	  }
	  if (!((BoolValue)bval).val) return ValFalse;
	}
	return ValTrue;
      }
    case OPCODE_case:   // Case
      {
	int alen = args.length;
	SemanticNode other = null;
	for (int i = 0; i < alen; i++) {
	  OpApplNode pairNode = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pairArgs = pairNode.getArgs();
	  if (pairArgs[0] == null) {
	    other = pairArgs[1];
	  }
	  else {
	    Value bval = this.eval(pairArgs[0], c, s0, s1, control);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("A non-boolean expression (" + bval.getKindString() +
			  ") was used as a condition of a CASE. " + pairArgs[0]);
	    }
	    if (((BoolValue)bval).val) {
	      return this.eval(pairArgs[1], c, s0, s1, control);
	    }
	  }
	}
	if (other == null) {
	  Assert.fail("Attempted to evaluate a CASE with no conditions true.\n" + expr);
	}
	return this.eval(other, c, s0, s1, control);
      }
    case OPCODE_cp:     // CartesianProd
      {
	int alen = args.length;
	Value[] sets = new Value[alen];
	for (int i = 0; i < alen; i++) {
	  sets[i] = this.eval(args[i], c, s0, s1, control);
	}
	return new SetOfTuplesValue(sets);
      }
    case OPCODE_cl:     // ConjList
      {
	int alen = args.length;
	for (int i = 0; i < alen; i++) {
	  if (this.callStack != null) this.callStack.push(args[i]);
	  Value bval = this.eval(args[i], c, s0, s1, control);
	  if (!(bval instanceof BoolValue)) {
	    Assert.fail("A non-boolean expression (" + bval.getKindString() + ") was" +
			" used as a formula in a conjunction.\n" + args[i]);
	  }
	  if (this.callStack != null) this.callStack.pop();
	  if (!((BoolValue)bval).val) return ValFalse;
	}
	return ValTrue;
      }
    case OPCODE_dl:     // DisjList
      {
	int alen = args.length;
	for (int i = 0; i < alen; i++) {
	  if (this.callStack != null) this.callStack.push(args[i]);
	  Value bval = this.eval(args[i], c, s0, s1, control);
	  if (!(bval instanceof BoolValue)) {
	    Assert.fail("A non-boolean expression (" + bval.getKindString() + ") was" +
			" used as a formula in a disjunction.\n" + args[i]);
	  }
	  if (this.callStack != null) this.callStack.pop();
	  if (((BoolValue)bval).val) return ValTrue;
	}
	return ValFalse;
      }
    case OPCODE_exc:    // Except
      {
	int alen = args.length;
	Value result = this.eval(args[0], c, s0, s1, control);
	ValueExcept[] expts = new ValueExcept[alen-1];
	int exCnt = 0;
	for (int i = 1; i < alen; i++) {
	  OpApplNode pairNode = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pairArgs = pairNode.getArgs();
	  SemanticNode[] cmpts = ((OpApplNode)pairArgs[0]).getArgs();

	  Value[] lhs = new Value[cmpts.length];
	  for (int j = 0; j < lhs.length; j++) {
	    lhs[j] = this.eval(cmpts[j], c, s0, s1, control);
	  }
	  Value atVal = result.select(lhs);
	  if (atVal == null) {
	    // Do nothing but warn:
	    Assert.printWarning(TLCGlobals.warn,
				"The EXCEPT was applied to non-existing fields of the" +
				" value at\n" + args[0]);
	  }
	  else {
	    Context c1 = c.cons(EXCEPT_AT, atVal);
	    Value rhs = this.eval(pairArgs[1], c1, s0, s1, control);
	    ValueExcept vex = new ValueExcept(lhs, rhs);
	    result = result.takeExcept(vex);
	  }
	}
	return result;
      }
    case OPCODE_fa:     // FcnApply
      {
	if (this.callStack != null) this.callStack.push(expr);
	Value res = null;
	Value fval = this.eval(args[0], c, s0, s1, EvalControl.setKeepLazy(control));
	if ((fval instanceof FcnRcdValue) ||
	    (fval instanceof FcnLambdaValue)) {
	  Applicable fcn = (Applicable)fval;
	  Value[] argVals = new Value[args.length-1];
	  for (int i = 0; i < argVals.length; i++) {
	    argVals[i] = this.eval(args[i+1], c, s0, s1, control);
	  }
	  res = fcn.apply(argVals, control);
	}
	else if ((fval instanceof TupleValue) ||
		 (fval instanceof RecordValue)) {
	  Applicable fcn = (Applicable)fval;
	  if (args.length != 2) {
	    Assert.fail("Attempted to evaluate an expression of form f[e1, ... , eN]" +
			"\nwith f a tuple or record and N > 1.\n" + expr);
	  }
	  Value aval = this.eval(args[1], c, s0, s1, control);
	  res = fcn.apply(aval, control);
	}
	else {
	  Assert.fail("A non-function (" + fval.getKindString() + ") was applied" +
		      " as a function.\n" + expr);
	}
	if (this.callStack != null) this.callStack.pop();
	return res;
      }
    case OPCODE_fc:     // FcnConstructor
    case OPCODE_nrfs:   // NonRecursiveFcnSpec
    case OPCODE_rfs:    // RecursiveFcnSpec
      {
	OpDeclNode[][] formals = expr.getBdedQuantSymbolLists();
	boolean[] isTuples = expr.isBdedQuantATuple();
	ExprNode[] domains = expr.getBdedQuantBounds();

	Value[] dvals = new Value[domains.length];
	boolean isFcnRcd = true;
	for (int i = 0; i < dvals.length; i++) {
	  dvals[i] = this.eval(domains[i], c, s0, s1, control);
	  isFcnRcd = isFcnRcd && (dvals[i] instanceof Reducible);
	}
	FcnParams params = new FcnParams(formals, isTuples, dvals);

	SemanticNode fbody = args[0];
	FcnLambdaValue fval = new FcnLambdaValue(params, fbody, this, c, s0, s1, control);
	if (opcode == OPCODE_rfs) {
	  SymbolNode fname = expr.getUnbdedQuantSymbols()[0];
	  fval.makeRecursive(fname);
	  isFcnRcd = false;
	}
	if (isFcnRcd && !EvalControl.isKeepLazy(control)) {
	  return fval.toFcnRcd();
	}
	return fval;
      }
    case OPCODE_ite:    // IfThenElse
      {
	Value bval = this.eval(args[0], c, s0, s1, control);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("A non-boolean expression (" + bval.getKindString() +
		      ") was used as the condition of an IF.\n" + expr);
	}
	if (((BoolValue)bval).val) {
	  return this.eval(args[1], c, s0, s1, control);
	}
	return this.eval(args[2], c, s0, s1, control);
      }
    case OPCODE_rc:     // RcdConstructor
      {
	int alen = args.length;
	UniqueString[] names = new UniqueString[alen];
	Value[] vals = new Value[alen];
	for (int i = 0; i < alen; i++) {
	  OpApplNode pairNode = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pair = pairNode.getArgs();
	  names[i] = ((StringValue)Value.getValue(pair[0])).getVal();
	  vals[i] = this.eval(pair[1], c, s0, s1, control);
	}
	return new RecordValue(names, vals, false);
      }
    case OPCODE_rs:     // RcdSelect
      {
	Value rval = this.eval(args[0], c, s0, s1, control);
	Value sval = Value.getValue(args[1]);
	if (rval instanceof RecordValue) {
	  Value res = ((RecordValue)rval).select(sval);
	  if (res == null) {
	    Assert.fail("Attempted to select nonexistent field " + sval + " from the" +
			" record\n" + Value.ppr(rval.toString()) + "\n" + expr);
	  }
	  return res;
	}
	else {
	  FcnRcdValue fcn = FcnRcdValue.convert(rval);
	  if (fcn == null) {
	    Assert.fail("Attempted to select field " + sval + " from a non-record" +
			" value " + Value.ppr(rval.toString()) + "\n" + expr);
	  }
	  return fcn.apply(sval, control);
	}
      }
    case OPCODE_se:     // SetEnumerate
      {
	int alen = args.length;
	ValueVec vals = new ValueVec(alen);
	for (int i = 0; i < alen; i++) {
	  vals.addElement(this.eval(args[i], c, s0, s1, control));
	}
	return new SetEnumValue(vals, false);
      }
    case OPCODE_soa:    // SetOfAll
      {
	ValueVec vals = new ValueVec();
	ContextEnumerator enum = this.contexts(expr, c, s0, s1, control);
	SemanticNode body = args[0];
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  Value val = this.eval(body, c1, s0, s1, control);
	  // vals.addElement(val);
	  vals.addElement1(val);	  
	}
	return new SetEnumValue(vals, true);
      }
    case OPCODE_sor:    // SetOfRcds
      {
	int alen = args.length;
	UniqueString names[] = new UniqueString[alen];
	Value vals[] = new Value[alen];
	for (int i = 0; i < alen; i++) {
	  OpApplNode pairNode = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pair = pairNode.getArgs();
	  names[i] = ((StringValue)Value.getValue(pair[0])).getVal();
	  vals[i] = this.eval(pair[1], c, s0, s1, control);
	}
	return new SetOfRcdsValue(names, vals, false);
      }
    case OPCODE_sof:    // SetOfFcns
      {
	// Assert.check(args.length == 2);
	Value lhs = this.eval(args[0], c, s0, s1, control);
	Value rhs = this.eval(args[1], c, s0, s1, control);
	return new SetOfFcnsValue(lhs, rhs);
      }
    case OPCODE_sso:    // SubsetOf
      {
	// Assert.check(args.length == 1);
	SemanticNode pred = args[0];
	SemanticNode inExpr = expr.getBdedQuantBounds()[0];
	Value inVal = this.eval(inExpr, c, s0, s1, control);
	boolean isTuple = expr.isBdedQuantATuple()[0];
	OpDeclNode[] bvars = expr.getBdedQuantSymbolLists()[0];
	if (inVal instanceof Reducible) {
	  ValueVec vals = new ValueVec();
	  ValueEnumeration enumSet = ((Enumerable)inVal).elements();
	  Value elem;
	  if (isTuple) {
	    while ((elem = enumSet.nextElement()) != null) {
	      Context c1 = c;
	      Value[] tuple = ((TupleValue)elem).elems;
	      for (int i = 0; i < bvars.length; i++) {
		c1 = c1.cons(bvars[i], tuple[i]);
	      }
	      Value bval = this.eval(pred, c1, s0, s1, control);
	      if (((BoolValue)bval).val) {
		vals.addElement(elem);
	      }
	    }
	  }
	  else {
	    SymbolNode idName = bvars[0];
	    while ((elem = enumSet.nextElement()) != null) {
	      Context c1 = c.cons(idName, elem);
	      Value bval = this.eval(pred, c1, s0, s1, control);
	      if (((BoolValue)bval).val) {
		vals.addElement(elem);
	      }
	    }
	  }
	  return new SetEnumValue(vals, inVal.isNormalized());
	}
	else if (isTuple) {
	  return new SetPredValue(bvars, inVal, pred, this, c, s0, s1, control);
	}
	else {
	  return new SetPredValue(bvars[0], inVal, pred, this, c, s0, s1, control);
	}
      }
    case OPCODE_tup:    // Tuple
      {
	int alen = args.length;
	Value[] vals = new Value[alen];
	for (int i = 0; i < alen; i++) {
	  vals[i] = this.eval(args[i], c, s0, s1, control);
	}
	return new TupleValue(vals);
      }
    case OPCODE_uc:     // UnboundedChoose
      {
	Assert.fail("TLC attempted to evaluate an unbounded CHOOSE.\n" + expr);
	return null;    // make compiler happy
      }      
    case OPCODE_ue:     // UnboundedExists
      {
	Assert.fail("TLC attempted to evaluate an unbounded \\E.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_uf:     // UnboundedForall
      {
	Assert.fail("TLC attempted to evaluate an unbounded \\A.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_lnot:
      {
	// Assert.check(args.length == 1);
	Value arg = this.eval(args[0], c, s0, s1, control);
	if (!(arg instanceof BoolValue)) {
	  Assert.fail("Attempted to apply the operator ~ to a non-boolean\n(" +
		      arg.getKindString() + ")\n" + expr);
	}
	return (((BoolValue)arg).val) ? ValFalse : ValTrue;
      }
    case OPCODE_subset:
      {
	// Assert.check(args.length == 1);	
	Value arg = this.eval(args[0], c, s0, s1, control);
	return new SubsetValue(arg);
      }
    case OPCODE_union:
      {
	// Assert.check(args.length == 1);	
	Value arg = this.eval(args[0], c, s0, s1, control);
	return UnionValue.union(arg);	
      }
    case OPCODE_domain:
      {
	// Assert.check(args.length == 1);	
	Value arg = this.eval(args[0], c, s0, s1, control);
	if (!(arg instanceof Applicable)) {
	  Assert.fail("Attempted to apply the operator DOMAIN to a non-function\n(" +
		      arg.getKindString() + ")\n" + expr);
	}
	return ((Applicable)arg).getDomain();
      }
    case OPCODE_enabled:
      {
	// Assert.check(args.length == 1);
	TLCState sfun = TLCStateFun.Empty;
	Context c1 = Context.branch(c);
	sfun = this.enabled(args[0], ActionItemList.Empty, c1, s0, sfun);
	return (sfun != null) ? ValTrue : ValFalse;
      }
    case OPCODE_eq:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	return (arg1.equals(arg2)) ? ValTrue : ValFalse;
      }
    case OPCODE_land:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	if (!(arg1 instanceof BoolValue)) {
	  Assert.fail("Attempted to evaluate an expression of form P /\\ Q" +
		      " when P was\n" + arg1.getKindString() + ".\n" + expr);
	}
	if (((BoolValue)arg1).val) {
	  Value arg2 = this.eval(args[1], c, s0, s1, control);
	  if (!(arg2 instanceof BoolValue)) {
	    Assert.fail("Attempted to evaluate an expression of form P /\\ Q" +
			" when Q was\n" + arg2.getKindString() + ".\n" + expr);
	  }
	  return arg2;
	}
	return ValFalse;
      }
    case OPCODE_lor:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	if (!(arg1 instanceof BoolValue)) {
	  Assert.fail("Attempted to evaluate an expression of form P \\/ Q" +
		      " when P was\n" + arg1.getKindString() + ".\n" + expr);
	}
	if (((BoolValue)arg1).val) return ValTrue;
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (!(arg2 instanceof BoolValue)) {
	  Assert.fail("Attempted to evaluate an expression of form P \\/ Q" +
		      " when Q was\n" + arg2.getKindString() + ".\n" + expr);
	}
	return arg2;
      }
    case OPCODE_implies:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	if (!(arg1 instanceof BoolValue)) {
	  Assert.fail("Attempted to evaluate an expression of form P => Q" +
		      " when P was\n" + arg1.getKindString() + ".\n" + expr);
	}
	if (((BoolValue)arg1).val) {
	  Value arg2 = this.eval(args[1], c, s0, s1, control);
	  if (!(arg2 instanceof BoolValue)) {
	    Assert.fail("Attempted to evaluate an expression of form P => Q" +
			" when Q was\n" + arg2.getKindString() + ".\n" + expr);
	  }
	  return arg2;
	}
	return ValTrue;
      }
    case OPCODE_equiv:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (!(arg1 instanceof BoolValue) || !(arg2 instanceof BoolValue)) {
	  Assert.fail("Attempted to evaluate an expression of form P <=> Q" +
		      " when P or Q was not a boolean.\n" + expr);
	}
	BoolValue bval1 = (BoolValue)arg1;
	BoolValue bval2 = (BoolValue)arg2;
	return (bval1.val == bval2.val) ? ValTrue : ValFalse;
      }
    case OPCODE_noteq:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	return arg1.equals(arg2) ? ValFalse : ValTrue;
      }
    case OPCODE_subseteq:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (!(arg1 instanceof Enumerable)) {
	  Assert.fail("TLC was trying to compute the value of an expression of\n" +
		      "form S \\subseteq T, but S was not enumerable.\n" + expr);
	}
	ValueEnumeration enum = ((Enumerable)arg1).elements();
	Value elem;
	while ((elem = enum.nextElement()) != null) {
	  if (!arg2.member(elem)) return ValFalse;
	}
	return ValTrue;
      }
    case OPCODE_in:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	boolean res = arg2.member(arg1);
	return (res) ? ValTrue : ValFalse;
      }
    case OPCODE_notin:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	boolean res = arg2.member(arg1);
	return (res) ? ValFalse : ValTrue;
      }
    case OPCODE_setdiff:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (arg1 instanceof Reducible) {
	  return ((Reducible)arg1).diff(arg2);
	}
	return new SetDiffValue(arg1, arg2);
      }
    case OPCODE_cap:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (arg1 instanceof Reducible) {
	  return ((Reducible)arg1).cap(arg2);
	}
	else if (arg2 instanceof Reducible) {
	  return ((Reducible)arg2).cap(arg1);
	}
	return new SetCapValue(arg1, arg2);
      }
    case OPCODE_cup:
      {
	// Assert.check(args.length == 2);
	Value arg1 = this.eval(args[0], c, s0, s1, control);
	Value arg2 = this.eval(args[1], c, s0, s1, control);
	if (arg1 instanceof Reducible) {
	  return ((Reducible)arg1).cup(arg2);
	}
	else if (arg2 instanceof Reducible) {
	  return ((Reducible)arg2).cup(arg1);
	}
	return new SetCupValue(arg1, arg2);
      }
    case OPCODE_prime:
      {
	// Assert.check(args.length == 1);
	if (EvalControl.isEnabled(control)) {
	  // We are now in primed and enabled.
	  return this.eval(args[0], c, s1, null, EvalControl.setPrimed(control));
	}
	return this.eval(args[0], c, s1, null, control);
      }
    case OPCODE_unchanged:
      {
	// Assert.check(args.length == 1);
	Value v0 = this.eval(args[0], c, s0, TLCState.Empty, control);
	if (EvalControl.isEnabled(control)) {
	  // We are now in primed and enabled.
	  control = EvalControl.setPrimed(control);
	}
	Value v1 = this.eval(args[0], c, s1, null, control);
	return (v0.equals(v1)) ? ValTrue : ValFalse;
      }
    case OPCODE_aa:     // AngleAct <A>_e	  
      {
	// Assert.check(args.length == 2);
	Value res = this.eval(args[0], c, s0, s1, control);
	if (!(res instanceof BoolValue)) {
	  Assert.fail("Expected a boolean expression, but did not find one.\n" + expr);
	}
	if (!((BoolValue)res).val) return ValFalse;
	Value v0 = this.eval(args[1], c, s0, TLCState.Empty, control);
	if (EvalControl.isEnabled(control)) {
	  // We are now in primed and enabled. 
	  control = EvalControl.setPrimed(control);
	}
	Value v1 = this.eval(args[1], c, s1, null, control);
	return v0.equals(v1) ? ValFalse : ValTrue;
      }
    case OPCODE_sa:	// [A]_e
      {
	Value res = this.eval(args[0], c, s0, s1, control);
	if (!(res instanceof BoolValue)) {
	  Assert.fail("Expected a boolean expression, but did not find one.\n" + expr);
	}
	if (((BoolValue)res).val) return ValTrue;
	Value v0 = this.eval(args[1], c, s0, TLCState.Empty, control);
	if (EvalControl.isEnabled(control)) {
	  // We are now in primed and enabled.
	  control = EvalControl.setPrimed(control);
	}
	Value v1 = this.eval(args[1], c, s1, null, control);
	return (v0.equals(v1)) ? ValTrue : ValFalse;
      }
    case OPCODE_cdot:
      {
	// Assert.check(args.length == 2);
	Assert.fail("The current version of TLC does not support action composition.");
	/***
	TLCState s01 = TLCStateFun.Empty;
	StateVec iss = new StateVec(0);
	this.getNextStates(args[0], ActionItemList.Empty, c, s0, s01, iss);
	int sz = iss.size();
	for (int i = 0; i < sz; i++) {
	  s01 = iss.elementAt(i);
	  this.eval(args[1], c, s01, s1, control);
	}
	***/
	return null;    // make compiler happy
      }
    case OPCODE_sf:     // SF
      {
	Assert.fail("TLC encountered a temporal formula (SF) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_wf:     // WF
      {
	Assert.fail("TLC encountered a temporal formula (WF) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_te:     // TemporalExists
      {
	Assert.fail("TLC encountered a temporal formula (\\EE) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_tf:     // TemporalForAll
      {
	Assert.fail("TLC encountered a temporal formula (\\AA) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_leadto:
      {
	// Assert.check(args.length == 2);
	Assert.fail("TLC encountered a temporal formula (a ~> b) when" +
		    " evaluating a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_arrow:
      {
	// Assert.check(args.length == 2);
	Assert.fail("TLC encountered a temporal formula (a -+-> formula)" +
		    " when evaluating a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_box:
      {
	// Assert.check(args.length == 1);	
	Assert.fail("TLC encountered a temporal formula ([]A) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    case OPCODE_diamond:
      {
	// Assert.check(args.length == 1);	
	Assert.fail("TLC encountered a temporal formula (<>A) when evaluating" +
		    " a predicate or action.\n" + expr);
	return null;    // make compiler happy
      }
    default:
      {
	Assert.fail("TLC BUG: could not evaluate this expression.\n" + expr);
	return null;
      }
    }
  }

  /**
   * This method determines if the argument is a valid state.  A state
   * is good iff it assigns legal explicit values to all the global
   * state variables.
   */
  public final boolean isGoodState(TLCState state) {
    return state.allAssigned();
  }

  /* This method determines if a state satisfies the model constraints. */
  public final boolean isInModel(TLCState state) throws EvalException {
    ExprNode[] constrs = this.getModelConstraints();
    for (int i = 0; i < constrs.length; i++) {
      Value bval = this.eval(constrs[i], Context.Empty, state);
      if (!(bval instanceof BoolValue)) {
	Assert.fail("TLC expected a boolean expression, but did not find one.\n" +
		    constrs[i]);
      }
      if (!((BoolValue)bval).val) return false;
    }
    return true;
  }

  /* This method determines if a pair of states satisfy the action constraints. */
  public final boolean isInActions(TLCState s1, TLCState s2) throws EvalException {
    ExprNode[] constrs = this.getActionConstraints();
    for (int i = 0; i < constrs.length; i++) {
      Value bval = this.eval(constrs[i], Context.Empty, s1, s2, EvalControl.Clear);
      if (!(bval instanceof BoolValue)) {
	Assert.fail("TLC expected a boolean expression, but did not find one.\n" +
		    constrs[i]);
      }
      if (!((BoolValue)bval).val) return false;
    }
    return true;
  }

  /**
   * This method determines if an action is enabled in the given state.
   * More precisely, it determines if (act.pred /\ (sub' # sub)) is
   * enabled in the state s and context act.con.
   */
  public final TLCState enabled(SemanticNode pred, ActionItemList acts,
				Context c, TLCState s0, TLCState s1) {
    switch (pred.getKind()) {
    case OpApplKind:
      {
	OpApplNode pred1 = (OpApplNode)pred;
	return this.enabledAppl(pred1, acts, c, s0, s1);
      }
    case LetInKind:
      {
	LetInNode pred1 = (LetInNode)pred;
	OpDefNode[] letDefs = pred1.getLets();
	Context c1 = c;
	for (int i = 0; i < letDefs.length; i++) {
	  OpDefNode opDef = letDefs[i];
	  if (opDef.getArity() == 0) {
	    Value rhs = new LazyValue(opDef.getBody(), c1);
	    c1 = c1.cons(opDef, rhs);
	  }
	}
	return this.enabled(pred1.getBody(), acts, c1, s0, s1);
      }
    case SubstInKind:
      {
	SubstInNode pred1 = (SubstInNode)pred;
	Subst[] subs = pred1.getSubsts();
	int slen = subs.length;
	Context c1 = c;
	for (int i = 0; i < slen; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, false));
	}
	return this.enabled(pred1.getBody(), acts, c1, s0, s1);
      }
    default:
      {
	// We should not compute enabled on anything else.
	Assert.fail("Attempted to compute ENABLED on a non-boolean expression.\n" + pred);
	return null;    // make compiler happy
      }
    }
  }

  private final TLCState enabled(ActionItemList acts, TLCState s0, TLCState s1) {
    if (acts.isEmpty()) return s1;

    int kind = acts.carKind();
    SemanticNode pred = acts.carPred();
    Context c = acts.carContext();
    ActionItemList acts1 = acts.cdr();
    if (kind > 0) {
      if (this.callStack != null) this.callStack.push(acts.carPred());
      TLCState res = this.enabled(pred, acts1, c, s0, s1);
      if (this.callStack != null) this.callStack.pop();
      return res;
    }
    else if (kind == -1) {
      return this.enabled(pred, acts1, c, s0, s1);
    }
    if (kind == -2) {
      return this.enabledUnchanged(pred, acts1, c, s0, s1);
    }

    Value v1 = this.eval(pred, c, s0, TLCState.Empty, EvalControl.Enabled);
    // We are now in ENABLED and primed state.
    Value v2 = this.eval(pred, c, s1, null, EvalControl.Primed);

    if (v1.equals(v2)) return null;
    return this.enabled(acts1, s0, s1);
  }

  private final TLCState enabledAppl(OpApplNode pred, ActionItemList acts,
				     Context c, TLCState s0, TLCState s1) {
    ExprOrOpArgNode[] args = pred.getArgs();
    int alen = args.length;
    SymbolNode opNode = pred.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    if (opcode == 0) {
      // This is a user-defined operator with one exception: it may
      // be substed by a builtin operator. This special case occurs
      // when the lookup returns an OpDef with opcode # 0.
      Object val = this.lookup(opNode, c, s0, false);

      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	opcode = BuiltInOPs.getOpCode(opDef.getName());
	if (opcode == 0) {
	  Context c1 = this.getOpContext(opDef, args, c, false);
	  return this.enabled(opDef.getBody(), acts, c1, s0, s1);
	}
      }

      if (val instanceof LazyValue) {
	LazyValue lv = (LazyValue)val;
	return this.enabled(lv.expr, acts, lv.con, s0, s1);
      }

      Object bval = val;
      if (alen == 0) {
	if (val instanceof MethodValue) {
	  bval = ((MethodValue)val).apply(EmptyValueArgs, EvalControl.Clear);
	}
      }
      else {
	if (val instanceof OpValue) {
	  Applicable op = (Applicable)val;
	  Value[] argVals = new Value[alen];
	  // evaluate the actuals:
	  for (int i = 0; i < alen; i++) {
	    argVals[i] = this.eval(args[i], c, s0, s1, EvalControl.Enabled);
	  }
	  // apply the operator:
	  bval = op.apply(argVals, EvalControl.Enabled);
	}
      }

      if (opcode == 0) {
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing ENABLED, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  return this.enabled(acts, s0, s1);
	}
	return null;
      }
    }

    switch (opcode) {
    case OPCODE_aa:     // AngleAct <A>_e
      {
	ActionItemList acts1 = acts.cons(args[1], c, -3);
	return this.enabled(args[0], acts1, c, s0, s1);
      }
    case OPCODE_be:     // BoundedExists
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(pred, c, s0, s1, EvalControl.Enabled);
	Context c1;
	while ((c1 = enum.nextElement()) != null) {
	  TLCState s2 = this.enabled(body, acts, c1, s0, s1);
	  if (s2 != null) return s2;
	}
	return null;
      }
    case OPCODE_bf:     // BoundedForall
      {
	SemanticNode body = args[0];
	ContextEnumerator enum = this.contexts(pred, c, s0, s1, EvalControl.Enabled);	
	Context c1 = enum.nextElement();
	if (c1 == null) {
	  return this.enabled(acts, s0, s1);
	}
	ActionItemList acts1 = acts;
	Context c2;
	while ((c2 = enum.nextElement()) != null) {
	  acts1 = acts1.cons(body, c2, -1);
	}
	return this.enabled(body, acts1, c1, s0, s1);
      }
    case OPCODE_case:   // Case
      {
	SemanticNode other = null;
	for (int i = 0; i < alen; i++) {
	  OpApplNode pair = (OpApplNode)args[i];
	  ExprOrOpArgNode[] pairArgs = pair.getArgs();
	  if (pairArgs[0] == null) {
	    other = pairArgs[1];
	  }
	  else {
	    Value bval = this.eval(pairArgs[0], c, s0, s1, EvalControl.Enabled);
	    if (!(bval instanceof BoolValue)) {
	      Assert.fail("In computing ENABLED, a non-boolean expression(" +
			  bval.getKindString() + ") was used as a guard condition" +
			  " of a CASE.\n" + pairArgs[1]);
	    }
	    if (((BoolValue)bval).val) {
	      return this.enabled(pairArgs[1], acts, c, s0, s1);
	    }
	  }
	}
	if (other == null) {
	  Assert.fail("In computing ENABLED, TLC encountered a CASE with no" +
		      " conditions true.\n" + pred);
	}
	return this.enabled(other, acts, c, s0, s1);
      }
    case OPCODE_cl:     // ConjList
    case OPCODE_land:
      {
	ActionItemList acts1 = acts;
	for (int i = alen-1; i > 0; i--) {
	  acts1 = acts1.cons(args[i], c, i);
	}
	if (this.callStack != null) this.callStack.push(args[0]);
	TLCState res = this.enabled(args[0], acts1, c, s0, s1);
	if (this.callStack != null) this.callStack.pop();
	return res;
      }
    case OPCODE_dl:     // DisjList 
    case OPCODE_lor:
      {
	for (int i = 0; i < alen; i++) {
	  if (this.callStack != null) this.callStack.push(args[i]);
	  TLCState s2 = this.enabled(args[i], acts, c, s0, s1);
	  if (this.callStack != null) this.callStack.pop();
	  if (s2 != null) return s2;
	}
	return null;
      }
    case OPCODE_fa:     // FcnApply
      {
	Value fval = this.eval(args[0], c, s0, s1, EvalControl.Enabled);
	if (fval instanceof FcnLambdaValue) {
	  FcnLambdaValue fcn = (FcnLambdaValue)fval;
	  if (fcn.fcnRcd == null) {
	    Context c1 = this.getFcnContext(fcn, args, c, s0, s1, EvalControl.Enabled);
	    return this.enabled(fcn.body, acts, c1, s0, s1);
	  }
	  fval = fcn.fcnRcd;
	}
	if (fval instanceof Applicable) {
	  Applicable fcn = (Applicable)fval;
	  Value[] argVals = new Value[alen-1];
	  for (int i = 0; i < argVals.length; i++) {
	    argVals[i] = this.eval(args[i+1], c, s0, s1, EvalControl.Enabled);
	  }
	  Value bval = fcn.apply(argVals, EvalControl.Enabled);
	  if (!(bval instanceof BoolValue)) {
	    Assert.fail("In computing ENABLED, TLC expected a boolean" +
			" expression,\nbut did not find one.\n" + pred);
	  }
	  if (!((BoolValue)bval).val) return null;
	}
	else {
	  Assert.fail("In computing ENABLED, a non-function (" + fval.getKindString() +
		      ") was applied as a function.\n" + pred);
	}
	return this.enabled(acts, s0, s1);
      }
    case OPCODE_ite:    // IfThenElse
      {
	Value guard = this.eval(args[0], c, s0, s1, EvalControl.Enabled);
	if (!(guard instanceof BoolValue)) {
	  Assert.fail("In computing ENABLED, a non-boolean expression(" +
		      guard.getKindString() + ") was used as the guard condition" +
		      " of an IF.\n" + pred);
	}
	int idx = (((BoolValue)guard).val) ? 1 : 2;
	return this.enabled(args[idx], acts, c, s0, s1);
      }
    case OPCODE_sa:     // SquareAct [A]_e
      {
	TLCState s2 = this.enabled(args[0], acts, c, s0, s1);
	if (s2 != null) return s2;
	return this.enabledUnchanged(args[1], acts, c, s0, s1);
      }
    case OPCODE_sf:     // SF
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula (SF).\n" + pred);
	return null;    // make compiler happy
      }
    case OPCODE_te:     // TemporalExists
    case OPCODE_tf:     // TemporalForAll
      {
	Assert.fail("In computing ENABLED, TLC encountered temporal quantifier.\n" + pred);
	return null;    // make compiler happy
      }
    case OPCODE_uc:     // UnboundedChoose
      {
	Assert.fail("In computing ENABLED, TLC encountered unbounded CHOOSE. " + pred);
	return null;    // make compiler happy
      }
    case OPCODE_ue:     // UnboundedExists
    case OPCODE_uf:     // UnboundedForall
      {
	Assert.fail("In computing ENABLED, TLC encountered unbounded quantifier. " + pred);
	return null;    // make compiler happy
      }
    case OPCODE_wf:     // WF
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula (WF).\n" + pred);
	return null;    // make compiler happy
      }
    case OPCODE_box:
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula ([]).\n" + pred);
	return null;    // make compiler happy
      }
    case OPCODE_diamond:
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula (<>).\n" + pred);
	return null;    // make compiler happy
      }
    case OPCODE_unchanged:
      {
	return this.enabledUnchanged(args[0], acts, c, s0, s1);
      }
    case OPCODE_eq:
      {
	SymbolNode var = this.getPrimedVar(args[0], c, true);
	if (var == null) {
	  Value bval = this.eval(pred, c, s0, s1, EvalControl.Enabled);
	  if (!((BoolValue)bval).val) return null;
	}
	else {
	  UniqueString varName = var.getName();
	  Value lval = s1.lookup(varName);
	  Value rval = this.eval(args[1], c, s0, s1, EvalControl.Enabled);
	  if (lval == null) {
	    TLCState s2 = s1.bind(var, rval, pred);
	    return this.enabled(acts, s0, s2);
	  }
	  else {
	    if (!lval.equals(rval)) return null;
	  }
	}
	return this.enabled(acts, s0, s1);
      }
    case OPCODE_implies:
      {
	Value bval = this.eval(args[0], c, s0, s1, EvalControl.Enabled);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("While computing ENABLED of an expression of the form" +
		      " P => Q, P was " + bval.getKindString() + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  return this.enabled(args[1], acts, c, s0, s1);
	}
	return this.enabled(acts, s0, s1);
      }
    case OPCODE_cdot:
      {
	Assert.fail("The current version of TLC does not support action composition.");
	/***
	TLCState s01 = TLCStateFun.Empty;
	StateVec iss = new StateVec(0);
	this.getNextStates(args[0], ActionItemList.Empty, c, s0, s01, iss);
	int sz = iss.size();
	for (int i = 0; i < sz; i++) {
	  s01 = iss.elementAt(i);
	  TLCState s2 = this.enabled(args[1], acts, c, s01, s1);
	  if (s2 != null) return s2;
	}
	***/
	return null;   // make compiler happy
      }
    case OPCODE_leadto:
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula" +
		    " (a ~> b).\n" + pred);
	return null;   // make compiler happy
      }
    case OPCODE_arrow:
      {
	Assert.fail("In computing ENABLED, TLC encountered a temporal formula" +
		    " (a -+-> formula).\n" + pred);
	return null;   // make compiler happy
      }
    case OPCODE_in:
      {
	SymbolNode var = this.getPrimedVar(args[0], c, true);
	if (var == null) {
	  Value bval = this.eval(pred, c, s0, s1, EvalControl.Enabled);
	  if (!((BoolValue)bval).val) return null;
	}
	else {
	  UniqueString varName = var.getName();
	  Value lval = s1.lookup(varName);
	  Value rval = this.eval(args[1], c, s0, s1, EvalControl.Enabled);
	  if (lval == null) {
	    if (!(rval instanceof Enumerable)) {
	      Assert.fail("The right side of \\IN is not enumerable.\n" + pred);
	    }	      
	    ValueEnumeration enum = ((Enumerable)rval).elements();
	    Value val;
	    while ((val = enum.nextElement()) != null) {
	      TLCState s2 = s1.bind(var, val, pred);
	      s2 = this.enabled(acts, s0, s2);
	      if (s2 != null) return s2;
	    }
	    return null;
	  }
	  else {
	    if (!rval.member(lval)) return null;
	  }
	}
	return this.enabled(acts, s0, s1);
      }
    default:
      {
	// We handle all the other builtin operators here.
	Value bval = this.eval(pred, c, s0, s1, EvalControl.Enabled);
	if (!(bval instanceof BoolValue)) {
	  Assert.fail("In computing ENABLED, TLC expected a boolean expression," +
		      "\nbut instead found " + bval + ".\n" + pred);
	}
	if (((BoolValue)bval).val) {
	  return this.enabled(acts, s0, s1);
	}
	return null;
      }
    }
  }

  private final TLCState enabledUnchanged(SemanticNode expr, ActionItemList acts,
					  Context c, TLCState s0, TLCState s1) {
    SymbolNode var = this.getVar(expr, c, true);
    if (var != null) {
      // a state variable:
      UniqueString varName = var.getName();
      Value v0 = this.eval(expr, c, s0, s1, EvalControl.Enabled);
      Value v1 = s1.lookup(varName);
      if (v1 == null) {
	s1 = s1.bind(var, v0, expr);
	return this.enabled(acts, s0, s1);
      }
      if (v1.equals(v0)) {
	return this.enabled(acts, s0, s1);
      }
      Assert.printWarning(TLCGlobals.warn,
			  "The variable " + varName + " was changed while" +
			  " it is specified as UNCHANGED at\n" + expr);
      return null;
    }
      
    if (expr instanceof OpApplNode) {
      OpApplNode expr1 = (OpApplNode)expr;
      ExprOrOpArgNode[] args = expr1.getArgs();
      int alen = args.length;
      SymbolNode opNode = expr1.getOperator();
      UniqueString opName = opNode.getName();
      int opcode = BuiltInOPs.getOpCode(opName);

      if (opcode == OPCODE_tup) {
	// a tuple:
	if (alen != 0) {
	  ActionItemList acts1 = acts;
	  for (int i = 1; i < alen; i++) {
	    acts1 = acts1.cons(args[i], c, -2);
	  }
	  return this.enabledUnchanged(args[0], acts1, c, s0, s1);
	}
	return this.enabled(acts, s0, s1);	
      }

      if (opcode == 0 && alen == 0) {
	// a 0-arity operator:
	Object val = this.lookup(opNode, c, false);

	if (val instanceof LazyValue) {
	  LazyValue lv = (LazyValue)val;
	  return this.enabledUnchanged(lv.expr, acts, lv.con, s0, s1);
	}
	else if (val instanceof OpDefNode) {
	  return this.enabledUnchanged(((OpDefNode)val).getBody(), acts, c, s0, s1);
	}
	else if (val == null) {
	  Assert.fail("In computing ENABLED, TLC found the undefined identifier\n" +
		      opName + " in an UNCHANGED expression at\n" + expr);
	}
	return this.enabled(acts, s0, s1);
      }
    }

    Value v0 = this.eval(expr, c, s0, TLCState.Empty, EvalControl.Enabled);
    Value v1 = this.eval(expr, c, s1, TLCState.Empty, EvalControl.Primed);
    if (!v0.equals(v1)) return null;
    return this.enabled(acts, s0, s1);
  }

  /* This method determines if the action predicate is valid in (s0, s1). */
  public final boolean isValid(Action act, TLCState s0, TLCState s1) {
    Value val = this.eval(act.pred, act.con, s0, s1, EvalControl.Clear);
    if (!(val instanceof BoolValue)) {
      Assert.fail("TLC expected a boolean expression, but did not find one.\n" +
		  act.pred);
    }
    return ((BoolValue)val).val;
  }
  
  /* Returns true iff the predicate is valid in the state. */
  public final boolean isValid(Action act, TLCState state) {
    return this.isValid(act, state, TLCState.Empty);
  }

  /* Returns true iff the predicate is valid in the state. */
  public final boolean isValid(Action act) {
    return this.isValid(act, TLCState.Empty, TLCState.Empty);
  }

  public final boolean isValid(ExprNode expr) {
    Value val = this.eval(expr, Context.Empty, TLCState.Empty);
    if (!(val instanceof BoolValue)) {
      Assert.fail("TLC expected a boolean expression, but did not find one.\n" +
		  expr);
    }
    return ((BoolValue)val).val;
  }
  
  /* Reconstruct the initial state whose fingerprint is fp. */
  public final TLCStateInfo getState(long fp) {
    StateVec initStates = this.getInitStates();
    for (int i = 0; i < initStates.size(); i++) {
      TLCState state = initStates.elementAt(i);
      long nfp = state.fingerPrint();
      if (fp == nfp) {
	String info = "<Initial predicate>";
	return new TLCStateInfo(state, info);
      }
    }
    return null;
  }
  
  /* Reconstruct the next state of state s whose fingerprint is fp. */
  public final TLCStateInfo getState(long fp, TLCState s) {
    for (int i = 0; i < this.actions.length; i++) {
      Action curAction = this.actions[i];
      StateVec nextStates = this.getNextStates(curAction, s);
      for (int j = 0; j < nextStates.size(); j++) {
	TLCState state = nextStates.elementAt(j);
	long nfp = state.fingerPrint();
	if (fp == nfp) {
	  return new TLCStateInfo(state, curAction.getLocation());
	}
      }
    }
    return null;
  }

  /* Reconstruct the info for s1.   */
  public final TLCStateInfo getState(TLCState s1, TLCState s) {
    for (int i = 0; i < this.actions.length; i++) {
      Action curAction = this.actions[i];
      StateVec nextStates = this.getNextStates(curAction, s);
      for (int j = 0; j < nextStates.size(); j++) {
	TLCState state = nextStates.elementAt(j);
	if (s1.equals(state)) {
	  return new TLCStateInfo(state, curAction.getLocation());
	}
      }
    }
    return null;
  }

  /* Return the set of all permutations under the symmetry assumption. */
  public final MVPerm[] getSymmetryPerms() {
    String name = this.config.getSymmetry();
    if (name.length() == 0) return null;
    Object symm = this.defns.get(name);
    if (symm == null) {
      Assert.fail("The symmetry function " + name + " specified by the" +
		  " configuration file\nis not defined in the specification.");
    }
    if (!(symm instanceof OpDefNode)) {
      Assert.fail("The symmetry function " + name + " must specify a set of permutations.");
    }
    Value fcns = this.eval(((OpDefNode)symm).getBody(), Context.Empty, TLCState.Empty);
    if (!(fcns instanceof Enumerable)) {
      Assert.fail("The symmetry operator must specify a set of functions.");
    }
    ValueEnumeration enum = ((Enumerable)fcns).elements();
    return MVPerm.permutationSubgroup(enum);
  }

  public final Context getFcnContext(FcnLambdaValue fcn, ExprOrOpArgNode[] args,
				     Context c, TLCState s0, TLCState s1,
				     int control) {
    Context c1 = c;
    FcnParams params = fcn.params;
    int plen = params.length();
    int alen = args.length;
    OpDeclNode[][] formals = params.formals;
    Value[] domains = params.domains;
    boolean[] isTuples = params.isTuples;
    if (plen == 1 && alen > 1) {
      Value[] elems = new Value[alen];
      for (int i = 0; i < alen; i++) {
	elems[i] = this.eval(args[i], c, s0, s1, control);
      }
      Value argVal = new TupleValue(elems);
      c1 = c1.cons(formals[0][0], argVal);
    }
    else if (plen > 1 && alen == 1) {
      int argn = 0;      
      Value argVal = this.eval(args[0], c, s0, s1, control);
      TupleValue tv = TupleValue.convert(argVal);
      if (tv == null) {
	Assert.fail("Attempted to apply a function to an argument not in its" +
		    " domain.\n" + args[0]);
      }
      Value[] elems = tv.elems;
      for (int i = 0; i < formals.length; i++) {
	OpDeclNode[] ids = formals[i];
	Value domain = domains[i];	  
	if (isTuples[i]) {
	  if (!domain.member(elems[argn])) {
	    Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			",\nthe argument number " + (argn+1) + " is not in its domain.\n" +
			args[0]);
	  }
	  TupleValue tv1 = TupleValue.convert(elems[argn++]);
	  if (tv1 == null || tv1.size() != ids.length) {
	    Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			",\nthe argument number " + argn + " does not match its" +
			" formal parameter.\n" + args[0]);
	  }
	  Value[] avals = tv1.elems;
	  for (int j = 0; j < ids.length; j++) {
	    c1 = c1.cons(ids[j], avals[j]);
	  }	  
	}
	else {
	  for (int j = 0; j < ids.length; j++) {
	    if (!domain.member(elems[argn])) {
	      Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			  ",\nthe argument number " + (argn+1) + " is not in its domain.\n" +
			  args[0]);
	    }
	    c1 = c1.cons(ids[j], elems[argn++]);
	  }
	}
      }
    }
    else {
      int argn = 0;
      for (int i = 0; i < formals.length; i++) {
	OpDeclNode[] ids = formals[i];
	Value domain = domains[i];	  
	if (isTuples[i]) {
	  Value argVal = this.eval(args[argn++], c, s0, s1, control);
	  if (!domain.member(argVal)) {
	    Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			",\nthe argument number " + argn + " is not in its domain.\n" +
			args[0]);
	  }
	  TupleValue tv = TupleValue.convert(argVal);
	  if (tv == null || tv.size() != ids.length) {
	    Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			",\nthe argument number " + argn + " does not match its" +
			" formal parameter.\n" + args[0]);
	  }
	  Value[] avals = tv.elems;
	  for (int j = 0; j < ids.length; j++) {
	    c1 = c1.cons(ids[j], avals[j]);
	  }	  
	}
	else {
	  for (int j = 0; j < ids.length; j++) {
	    Value argVal = this.eval(args[argn++], c, s0, s1, control);
	    if (!domain.member(argVal)) {
	      Assert.fail("In applying the function\n" + Value.ppr(fcn.toString()) +
			  ",\nthe argument number " + argn + " is not in its domain.\n" +
			  args[0]);
	    }
	    c1 = c1.cons(ids[j], argVal);
	  }
	}
      }
    }
    return c1;
  }

  /* A context enumerator for an operator application. */
  public final ContextEnumerator contexts(OpApplNode appl, Context c, TLCState s0,
					  TLCState s1, int control) {
    OpDeclNode[][] formals = appl.getBdedQuantSymbolLists();
    boolean[] isTuples = appl.isBdedQuantATuple();
    ExprNode[] domains = appl.getBdedQuantBounds();

    int flen = formals.length;
    int alen = 0;
    for (int i = 0; i < flen; i++) {
      alen += (isTuples[i]) ? 1 : formals[i].length;
    }
    Object[] vars = new Object[alen];
    ValueEnumeration[] enums = new ValueEnumeration[alen];
    int idx = 0;
    for (int i = 0; i < flen; i++) {
      Value boundSet = this.eval(domains[i], c, s0, s1, control);
      if (!(boundSet instanceof Enumerable)) {
	Assert.fail("TLC encountered a non-enumerable quantifier bound\n" +
		    Value.ppr(boundSet.toString()) + ".\n" + domains[i]);
      }
      OpDeclNode[] farg = formals[i];
      if (isTuples[i]) {
	vars[idx] = farg;
	enums[idx++] = ((Enumerable)boundSet).elements();
      }
      else {
	for (int j = 0; j < farg.length; j++) {
	  vars[idx] = farg[j];
	  enums[idx++] = ((Enumerable)boundSet).elements();
	}
      }
    }
    return new ContextEnumerator(vars, enums, c);
  }

  /**
   * This method converts every definition that is constant into TLC
   * value. By doing this, TLC avoids evaluating the same expression
   * multiple times.
   */
  private final void processConstantDefns() {
    ModuleNode[] mods = this.moduleTbl.getModuleNodes();
    for (int i = 0; i < mods.length; i++) {
      this.processConstantDefns(mods[i]);
    }
  }

  private final void processConstantDefns(ModuleNode mod) {
    OpDeclNode[] consts = mod.getConstantDecls();
    for (int i = 0; i < consts.length; i++) {
      Object val = consts[i].getToolObject(TLCGlobals.ToolId);
      if (val != null && val instanceof Value) {
	((Value)val).deepNormalize();
	// RuntimeConfiguration.get().getErrStream().println(consts[i].getName() + ": " + val);	
      }
    }

    OpDefNode[] opDefs = mod.getOpDefs();
    for (int i = 0; i < opDefs.length; i++) {
      OpDefNode opDef = opDefs[i]; 
      if (opDef.getArity() == 0) {
	Object realDef = this.lookup(opDef, Context.Empty, false);
	if (realDef instanceof OpDefNode) {
	  opDef = (OpDefNode)realDef;
	  if (this.getLevel(opDef.getBody(), Context.Empty) == 0) {
	    try {
	      UniqueString opName = opDef.getName();
	      Value val = this.eval(opDef.getBody(), Context.Empty, TLCState.Empty);
	      val.deepNormalize();
	      // RuntimeConfiguration.get().getErrStream().println(opName + ": " + val);
	      opDef.setToolObject(TLCGlobals.ToolId, val);
	      Object def = this.defns.get(opName);
	      if (def == opDef) {
		this.defns.put(opName, val);
	      }
	    }
	    catch (Throwable e) { /*SKIP*/ }
	  }
	}
      }
    }

    ModuleNode[] imods = mod.getInnerModules();
    for (int i = 0; i < imods.length; i++) {
      this.processConstantDefns(imods[i]);
    }
  }

}
