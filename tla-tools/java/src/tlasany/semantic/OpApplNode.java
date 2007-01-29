// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import tlasany.explorer.ExploreNode;
import tlasany.parser.SyntaxTreeNode;
import tlasany.st.TreeNode;
import tlasany.utilities.Strings;
import util.UniqueString;

/** 
 * OpApplNodes represent all kinds of operator applications in TLA+,
 * including the application of builtin operators and user-defined
 * operators, operators with a variable number of arguments or a fixed
 * number (including, of course, 0), and including quantifiers and
 * choose, which involve bound variables with or without bounding sets.
 *
 * We distinguish three different uses of OpApplNode:
 *  o Basic case: getOperator, getArgs, setArgs
 *  o unbounded parameters: getOperator, getArgs, setArgs,
 *                          getUnbdedQuantSymbols, isUnbdedQuantATuple
 *  o messy kind: getOperator, getArgs, setArgs, getBdedQuantSymbolLists,
 *                isBdedQuantATuple, getBdedQuantBounds
 */
public class OpApplNode extends ExprNode implements ExploreNode {

  protected SymbolNode        operator;              // operator being applied to the operands
  protected ExprOrOpArgNode[] operands;              // the operands. For an op with no operands 
                                                     //   this is a zero-length array
  protected boolean           isATuple;              // indicates whether bound vars are in form of a tuple,
                                                     //   e.g. surrounded by << >>
  protected OpDeclNode[]      unboundedBoundSymbols; // bound symbols introduced without restricted range
  protected OpDeclNode[][]    boundedBoundSymbols;   // bound symbols introduced with a restricted range
  protected ExprNode[]        ranges;                // ranges of bounded bound symbols
  protected boolean[]         tupleOrs;              // true if bound variable is in a tuple

  /**
   * Used only for creating "null" OpApplNode, nullOAN in Generator class.
   */
  public OpApplNode(SymbolNode sn) {
    super(-1, SyntaxTreeNode.nullSTN);
    this.operator = sn; 
    this.operands = null;
    this.unboundedBoundSymbols = null; 
    this.isATuple = false; 
    this.boundedBoundSymbols = null; 
    this.ranges = new ExprNode[0]; 
    this.tupleOrs = null; 
  }

  /**
   * Constructor for base case; used in SubstInNode and many cases in
   * Generator.
   */
  public OpApplNode(SymbolNode op, ExprOrOpArgNode[] oprands, TreeNode stn, 
                    ModuleNode mn) throws AbortException {
    super(OpApplKind, stn);
    this.operator = op;
    this.operands = oprands;
    this.unboundedBoundSymbols = null;
    this.isATuple = false;
    this.boundedBoundSymbols= null; 
    this.tupleOrs = null; 
    this.ranges = new ExprNode[0];

    // Call the match method for the operator in this op application,
    // with this OpApplNode as argument
    op.match( this, mn );
  }

  /*
   * Constructor for builtins --- matching is very specific in this case.
   * This is also used for the "@" construct, which somehow gets treated 
   * as an OpApplNode for now
   */
  public OpApplNode(UniqueString us, ExprOrOpArgNode[] ops, TreeNode stn, 
                    ModuleNode mn) {
    super(OpApplKind, stn);
    this.operands = ops;
    this.unboundedBoundSymbols = null;
    this.isATuple = false;
    this.boundedBoundSymbols= null; 
    this.tupleOrs = null; 
    this.ranges = new ExprNode[0];
    this.operator = Context.getGlobalContext().getSymbol(us);
    // operator.match( this, mn );
  }

  /**
   * Constructor used in the case of unbounded quantifiers, and the
   * first arg, "us", indicates which quantifier it is.  constructor
   * for unbounded builtins --- matching is very syntax-specific, and
   * we skip it.
   */
  public OpApplNode(UniqueString us, ExprOrOpArgNode[] ops, OpDeclNode[] odns, 
                    boolean t, TreeNode stn, ModuleNode mn) {
    super(OpApplKind, stn);
    this.operands = ops;
    this.unboundedBoundSymbols = odns;
    this.isATuple = t;
    this.boundedBoundSymbols= null; 
    this.tupleOrs = null; 
    this.ranges = new ExprNode[0];
    this.operator = Context.getGlobalContext().getSymbol(us);  
    // operator.match( this, mn );
  }

  /**
   * constructor for builtins & bounded quantifiers, including fcn defs-- 
   * matching is very syntax-specific in this case and we skip it.
   */
  public OpApplNode(UniqueString us, OpDeclNode[] funcName, ExprOrOpArgNode[] ops,
		    OpDeclNode[][] pars, boolean[] isT, ExprNode[] rs, TreeNode stn, 
                    ModuleNode mn) {
    super(OpApplKind, stn);
    this.operands = ops;
    this.unboundedBoundSymbols = funcName; // Will be null except for function defs.
                                           // In that case it will be non-null initially
                                           // and will be changed to null if the function
                                           // turns out to be non-recursive.
    this.isATuple = false;
    this.boundedBoundSymbols= pars; 
    this.tupleOrs = isT; 
    this.ranges = rs;
    this.operator = Context.getGlobalContext().getSymbol(us);
    // operator.match( this, mn );   
  }

  /**
   *  Returns the node identifying the operator of the operator
   *  application.  For example, for the expression A \cup B, this
   *  points to the OpDefOrDeclNode for \cup.
   */
  public final SymbolNode getOperator() { return this.operator; }

  /**
   * Changes the operator field of this OpApplNode; used only to
   * change nonrecursive function definition operator to recursive
   * when occurrence of operator being defined is encountered on the
   * RHS of the def.
   */
  final void resetOperator( UniqueString us ) {
    this.operator = Context.getGlobalContext().getSymbol(us);
  }

  /**
   * Sets the unBoundedBound symbols vector for THIS OpApplNode to null,
   * once it is discoved that a function def is in fact nonrecursive.
   */
  final void makeNonRecursive() { this.unboundedBoundSymbols = null; }

  /**
   * Returns the array of arguments (including operator arguments, but
   * not bound symbols or bounding sets) in the expression.  For
   * example, for the OpApplNode representing the expression
   *                                                                    
   *    \E x \in S : P                                                  
   *                                                                    
   * it returns a one-element array whose single element is a ref to
   * the ExprNode representing the expression P. The setArgs method
   * sets the value.
   */
  public final ExprOrOpArgNode[] getArgs() { return this.operands; }

  /**
   * Sets the operands array that is returned by getArgs()
   */
  public final void setArgs(ExprOrOpArgNode[] args) { this.operands = args; }

  final int getNumberOfBoundedBoundSymbols() {
    if (this.boundedBoundSymbols == null) return 0;

    int num = 0;
    for (int i = 0; i < this.boundedBoundSymbols.length; i++) {
      if (this.tupleOrs[i]) {
	num++;
      }
      else {
	num += this.boundedBoundSymbols[i].length;
      }
    }
    return num;
  }

  /**
   * These methods identify the OpApplNode's unbounded quantifier
   * symbols. For example, the x, y, and z in                            
   *                                                                    
   *     \E x, y, z : P    or   \E <<x, y, z>> : P                      
   *                                                                    
   * The method getUnbdedQuantSymbols() returns an array of refs to
   * the OpDeclNodes for x, y, z; and isUnbdedQuantATuple() indicates
   * whether or not there is a << >> around them.
   */
  public final OpDeclNode[] getUnbdedQuantSymbols() {
    return this.unboundedBoundSymbols;
  }

  /**
   * For the OpApplNode representing                                    
   *                                                                    
   *    \E u \in V,  x, y \in S,  <<z, w>> \in R  :  P                  
   *                                                                    
   *  - getBdedQuantSymbolLists returns the array of arrays of nodes    
   *       [ [u], [x, y], [z, w] ]                                      
   *                                                                    
   *  - isBdedQuantATuple() returns the array of booleans               
   *       [ false, false, true ]                                       
   *                                                                    
   *  - getBdedQuantBounds() returns the array of nodes                 
   *       [ V, S, R ]                                                  
   */
  public final OpDeclNode[][] getBdedQuantSymbolLists() {
    return boundedBoundSymbols;
  }
  
  /** 
   * See documentation for getUnbdedQuantSymbols and getBdedQuantSymbolLists()
   */
  public final boolean[] isBdedQuantATuple() { return this.tupleOrs; }

  /** 
   * See documentation for getUnbdedQuantSymbols and getBdedQuantSymbolLists()
   */
  public final boolean isUnbdedQuantATuple() { return this.isATuple; }

  /**
   * Returns array of the bound expressions for quantified variables that
   * are bounded in this operator application.
   */
  public final ExprNode[] getBdedQuantBounds() { return this.ranges; }

  private final ExprOrOpArgNode getArg(SymbolNode param) {
    OpDefNode opDef = (OpDefNode)this.operator;
    FormalParamNode[] formals = opDef.getParams();
    for (int i = 0; i < this.operands.length; i++) {
      if (formals[i] == param) {
	return this.operands[i];
      }
    }
    return null;
  }
  
  /* Level Checking */
  private boolean levelCorrect;
  private int level;
  private HashSet levelParams;
  private SetOfLevelConstraints levelConstraints;
  private SetOfArgLevelConstraints argLevelConstraints;
  private HashSet argLevelParams;

  public final boolean levelCheck() {
    if (this.levelConstraints != null) return this.levelCorrect;
    
    // Level check components:
    this.levelCorrect = true;
    for (int i = 0; i < this.operands.length; i++) {
      if (this.operands[i] != null &&
	  !this.operands[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    for (int i = 0; i < this.ranges.length; i++) {
      if (this.ranges[i] != null &&
	  !this.ranges[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }

    if (this.operator instanceof OpDefNode) {
      // Application of a builtin or user defined operator
      // Level correctness conditions
      OpDefNode opDef = (OpDefNode)this.operator;
      for (int i = 0; i < this.operands.length; i++) {
	ExprOrOpArgNode opd = this.operands[i];
	if (opd != null) {
	  if (opd.getLevel() > opDef.getMaxLevel(i)) {
	    if (opDef.levelCheck() &&
		opd.levelCheck()) {
	      errors.addError(this.stn.getLocation(),
			      "Level error in applying operator " + opDef.getName() +
			      ":\nThe level of argument " + (i+1) + " exceeds the" +
			      " maximum level allowed by the operator.");
	    }
	    this.levelCorrect = false;
	  }
	  if (opd instanceof OpArgNode &&
	      ((OpArgNode)opd).getOp() instanceof OpDefNode) {
	    OpDefNode opdDef = (OpDefNode)((OpArgNode)opd).getOp();
	    int alen = opdDef.getArity();
	    for (int j = 0; j < alen; j++) {
	      if (opdDef.getMaxLevel(j) < opDef.getMinMaxLevel(i, j)) {
		if (opDef.levelCheck() &&
		    opd.levelCheck()) {
		  errors.addError(this.stn.getLocation(),
				  "Level error in applying operator " + opDef.getName() + ":\n" +
				  "The level of argument " + (j+1) + " of the operator argument " +
				  (i+1) + " must be at least " + opDef.getMinMaxLevel(i, j) + ".");
		}
		this.levelCorrect = false;
	      }
	    }
	    for (int j = 0; j < this.operands.length; j++) {
	      for (int k = 0; k < alen; k++) {
		if (opDef.getOpLevelCond(i, j, k) &&
		    this.operands[j].getLevel() > opdDef.getMaxLevel(k)) {
		  if (opd.levelCheck() &&
		      this.operands[j].levelCheck()) {
		    errors.addError(this.stn.getLocation(),
				    "Level error in applying operator " + opDef.getName() +
				    ":\nThe level of argument " + (j+1) + " exceeds the" +
				    " maximum level allowed by the operator.");
		  }
		  this.levelCorrect = false;
		}
	      }
	    }
	  }
	}
      }

      for (int i = 0; i < this.ranges.length; i++) {
	ExprNode range = this.ranges[i];
	if (range != null) {
	  if (range.getLevel() > ActionLevel) {
	    if (range.levelCheck()) {
	      errors.addError(this.stn.getLocation(),
			      "Level error in applying operator " + opDef.getName() +
			      ":\nThe level of the range for the bounded variable " +
			      boundedBoundSymbols[i][0] + " exceeds the maximum " +
			      "level allowed by the operator.");
	    }
	    this.levelCorrect = false;
	  }
	}
      }

      // Calculate level information
      this.level = opDef.getLevel();
      for (int i = 0; i < this.operands.length; i++) {
	if (this.operands[i] != null &&
	    opDef.getWeight(i) == 1) {
	  this.level = Math.max(this.level, this.operands[i].getLevel());
	}
      }
      for (int i = 0; i < this.ranges.length; i++) {
	this.level = Math.max(this.level, this.ranges[i].getLevel());
      }

      this.levelParams = new HashSet();
      this.levelParams.addAll(opDef.getLevelParams());
      for (int i = 0; i < this.operands.length; i++) {
	if (this.operands[i] != null &&
	    opDef.getWeight(i) == 1) {
	  this.levelParams.addAll(this.operands[i].getLevelParams());
	}
      }
      for (int i = 0; i < this.ranges.length; i++) {
	this.levelParams.addAll(this.ranges[i].getLevelParams());
      }      

      this.levelConstraints = new SetOfLevelConstraints();
      this.levelConstraints.putAll(opDef.getLevelConstraints());
      for (int i = 0; i < this.operands.length; i++) {
	if (this.operands[i] != null) {
	  this.levelConstraints.putAll(this.operands[i].getLevelConstraints());
	}
      }
      for (int i = 0; i < this.ranges.length; i++) {
	this.levelConstraints.putAll(this.ranges[i].getLevelConstraints());
      }    
      for (int i = 0; i < this.operands.length; i++) {
	Integer mlevel = new Integer(opDef.getMaxLevel(i));
	if (this.operands[i] != null) {
	  Iterator iter = this.operands[i].getLevelParams().iterator();
	  while (iter.hasNext()) {
	    this.levelConstraints.put(iter.next(), mlevel);
	  }
	}
      }
      for (int i = 0; i < this.operands.length; i++) {
	ExprOrOpArgNode opdi = this.operands[i];
	if (opdi != null &&
	    opdi instanceof OpArgNode &&
	    ((OpArgNode)opdi).getOp() instanceof OpDefNode) {
	  OpDefNode argDef = (OpDefNode)((OpArgNode)opdi).getOp();
	  int alen = argDef.getArity();
	  for (int j = 0; j < this.operands.length; j++) {
	    for (int k = 0; k < alen; k++) {
	      if (opDef.getOpLevelCond(i, j, k)) {
		Integer mlevel = new Integer(argDef.getMaxLevel(k));		
		Iterator iter = this.operands[j].getLevelParams().iterator();
		while (iter.hasNext()) {
		  this.levelConstraints.put(iter.next(), mlevel);
		}
	      }
	    }
	  }
	}
      }
      HashSet alpSet = opDef.getArgLevelParams();
      Iterator iter = alpSet.iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	ExprOrOpArgNode arg = this.getArg(alp.op);
	if (arg != null &&
	    arg instanceof OpArgNode &&
	    ((OpArgNode)arg).getOp() instanceof OpDefNode) {
	  OpDefNode argDef = (OpDefNode)((OpArgNode)arg).getOp();
	  Integer mlevel = new Integer(argDef.getMaxLevel(alp.i));
	  this.levelConstraints.put(alp.param, mlevel);
	}
      }

      this.argLevelConstraints = new SetOfArgLevelConstraints();
      this.argLevelConstraints.putAll(opDef.getArgLevelConstraints());
      for (int i = 0; i < this.operands.length; i++) {
	if (this.operands[i] != null) {
	  this.argLevelConstraints.putAll(this.operands[i].getArgLevelConstraints());
	}
      }
      for (int i = 0; i < this.ranges.length; i++) {
	this.argLevelConstraints.putAll(this.ranges[i].getArgLevelConstraints());
      } 
      for (int i = 0; i < this.operands.length; i++) {
	ExprOrOpArgNode opdi = this.operands[i];
	if (opdi != null &&
	    opdi instanceof OpArgNode &&
	    ((OpArgNode)opdi).getOp().isParam()) {
	  SymbolNode opArg = ((OpArgNode)opdi).getOp();
	  int alen = opArg.getArity();
	  for (int j = 0; j < alen; j++) {
	    ParamAndPosition pap = new ParamAndPosition(opArg, j);
	    Integer mlevel = new Integer(opDef.getMinMaxLevel(i, j));
	    this.argLevelConstraints.put(pap, mlevel);
	  }
	  for (int j = 0; j < this.operands.length; j++) {
	    for (int k = 0; k < alen; k++) {
	      if (opDef.getOpLevelCond(i, j, k)) {
		ParamAndPosition pap = new ParamAndPosition(opArg, k);
		Integer mlevel = new Integer(this.operands[j].getLevel());
		this.argLevelConstraints.put(pap, mlevel);
	      }
	    }
	  }
	}
      }
      iter = alpSet.iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	ExprOrOpArgNode arg = this.getArg(alp.op);
	if (arg != null) {
	  ParamAndPosition pap = new ParamAndPosition(alp.op, alp.i);
	  this.argLevelConstraints.put(pap, new Integer(arg.getLevel()));
	}
      }
      
      this.argLevelParams = new HashSet();
      for (int i = 0; i < this.operands.length; i++) {
	if (this.operands[i] != null) {
	  this.argLevelParams.addAll(this.operands[i].getArgLevelParams());
	}
      }
      for (int i = 0; i < this.ranges.length; i++) {
	this.argLevelParams.addAll(this.ranges[i].getArgLevelParams());
      }
      iter = alpSet.iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	ExprOrOpArgNode arg = this.getArg(alp.op);
	if (arg == null) {
	  arg = this.getArg(alp.param);
	  if (arg == null) {
	    this.argLevelParams.add(alp);
	  }
	  else {
	    Iterator iter1 = arg.getLevelParams().iterator();
	    while (iter1.hasNext()) {
	      SymbolNode param = (SymbolNode)iter1.next();
	      this.argLevelParams.add(new ArgLevelParam(alp.op, alp.i, param));
	    }
	  }
	}
	else {
	  if (arg instanceof OpArgNode &&
	      ((OpArgNode)arg).getOp().isParam()) {
	    SymbolNode argOp = ((OpArgNode)arg).getOp();
	    this.argLevelParams.add(new ArgLevelParam(argOp, alp.i, alp.param));
	  }
	}
      }
      for (int i = 0; i < this.operands.length; i++) {
	ExprOrOpArgNode opdi = this.operands[i];
	if (opdi != null &&
	    opdi instanceof OpArgNode &&
	    ((OpArgNode)opdi).getOp().isParam()) {
	  SymbolNode opArg = ((OpArgNode)opdi).getOp();
	  int alen = opArg.getArity();
	  for (int j = 0; j < this.operands.length; j++) {
	    for (int k = 0; k < alen; k++) {
	      if (opDef.getOpLevelCond(i, j, k)) {
		Iterator iter1 = this.operands[j].getLevelParams().iterator();
		while (iter1.hasNext()) {
		  SymbolNode param = (SymbolNode)iter1.next();
		  this.argLevelParams.add(new ArgLevelParam(opArg, k, param));
		}
	      }
	    }
	  }
	}
      }
    }
    else {
      // Application of a declared operator
      this.level = this.operator.getLevel();
      for (int i = 0; i < this.operands.length; i++) {
	this.level = Math.max(this.level, this.operands[i].getLevel());
      }

      this.levelParams = new HashSet();
      this.levelParams.add(this.operator);
      for (int i = 0; i < this.operands.length; i++) {
	this.levelParams.addAll(this.operands[i].getLevelParams());
      }

      this.levelConstraints = new SetOfLevelConstraints();
      for (int i = 0; i < this.operands.length; i++) {
	this.levelConstraints.putAll(this.operands[i].getLevelConstraints());
      }

      this.argLevelConstraints = new SetOfArgLevelConstraints();
      for (int i = 0; i < this.operands.length; i++) {
	this.argLevelConstraints.put(this.operator, i, this.operands[i].getLevel());
	this.argLevelConstraints.putAll(this.operands[i].getArgLevelConstraints());
      }
      
      this.argLevelParams = new HashSet();
      for (int i = 0; i < this.operands.length; i++) {
	HashSet lpSet = this.operands[i].getLevelParams();
	Iterator iter = lpSet.iterator();
	while (iter.hasNext()) {
	  SymbolNode param = (SymbolNode)iter.next();
	  this.argLevelParams.add(new ArgLevelParam(this.operator, i, param));
	}
	this.argLevelParams.addAll(this.operands[i].getArgLevelParams());
      }
    }
    return this.levelCorrect;
  }
  
  public final int getLevel() { return this.level; }

  public final HashSet getLevelParams() { return this.levelParams; }

  public final SetOfLevelConstraints getLevelConstraints() {
    return this.levelConstraints;
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    return this.argLevelConstraints;
  }

  public final HashSet getArgLevelParams() { return this.argLevelParams; }
  
  /**
   * toString, levelDataToString, and walkGraph methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    return "Level: "               + this.level               + "\n" +
           "LevelParams: "         + this.levelParams         + "\n" +
           "LevelConstraints: "    + this.levelConstraints    + "\n" +
           "ArgLevelConstraints: " + this.argLevelConstraints + "\n" +
           "ArgLevelParams: "      + this.argLevelParams      + "\n" ;
  }

  /**
   * walkGraph finds all reachable nodes in the semantic graph
   * and inserts them in the Hashtable semNodesTable for use by the Explorer tool.
   */
  public void walkGraph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);
    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(uid, this);

    if (operator != null) {
      operator.walkGraph(semNodesTable);
    }

    if (unboundedBoundSymbols != null && unboundedBoundSymbols.length > 0) {
      for (int i = 0; i < unboundedBoundSymbols.length; i++) 
        if (unboundedBoundSymbols[i] != null) 
           unboundedBoundSymbols[i].walkGraph(semNodesTable);
    }

    if (operands != null && operands.length > 0) {
      for (int i = 0; i < operands.length; i++) 
        if (operands[i] != null) operands[i].walkGraph(semNodesTable);
    }
  
    if (ranges.length > 0) {
      for (int i = 0; i < ranges.length; i++) 
        if (ranges[i] != null) ranges[i].walkGraph(semNodesTable);
    }

    if (boundedBoundSymbols != null && boundedBoundSymbols.length > 0) {
      for (int i = 0; i < boundedBoundSymbols.length; i++) {
        if (boundedBoundSymbols[i] != null && boundedBoundSymbols[i].length > 0) {
          for (int j = 0; j < boundedBoundSymbols[i].length; j++) {
            if (boundedBoundSymbols[i][j] != null) 
               boundedBoundSymbols[i][j].walkGraph(semNodesTable);
          }
        }
      }
    }
  }

  // Used in implementation of toString() below
  private String toStringBody(int depth) {
    if (depth <= 1) return "";

    String ret;
    if (operator == null) {
      ret = "\nOperator: null";
    }
    else {
      ret = "\nOperator: " + operator.getName().toString() + "  " 
	    + operator.getUid() + "  ";
    }

    if (unboundedBoundSymbols!=null && unboundedBoundSymbols.length > 0) {
      ret += "\nUnbounded bound symbols:  ";
      for (int i = 0; i < unboundedBoundSymbols.length; i++) {
        ret += Strings.indent(2,unboundedBoundSymbols[i].toString(depth-1));
      }
    }

    if (boundedBoundSymbols != null && boundedBoundSymbols.length > 0) {
      ret += "\nBounded bound symbols: " + getNumberOfBoundedBoundSymbols();
      for (int i = 0; i < boundedBoundSymbols.length; i++) {
	if (boundedBoundSymbols[i] != null && boundedBoundSymbols[i].length > 0) {
	  for (int j = 0; j < boundedBoundSymbols[i].length; j++) {
            ret += Strings.indent(2, "\n[" + i + "," + j + "]" +
                      Strings.indent(2,boundedBoundSymbols[i][j].toString(depth-1)));
          }
        }
      }
    }

    if (ranges.length > 0) {
      ret += "\nRanges: ";
      for (int i = 0; i < ranges.length; i++) 
        ret += Strings.indent(2,(ranges[i] != null ? 
                                     ranges[i].toString(depth-1) : "null" ));
    }

    if (tupleOrs != null && tupleOrs.length > 0 && tupleOrs[0]) {
      ret += "\nTupleOrs: \n  ";
      for (int i = 0; i < tupleOrs.length; i++) {
        ret += Strings.indent(2, (tupleOrs[i] ? "true" : "false"));
      }
    }

    if (operands != null) {
      if (operands.length > 0) {
        ret += "\nOperands: " + operands.length;
        for (int i = 0; i < operands.length; i++) {
          ret += Strings.indent(2,
                    (operands[i] == null ? "\nnull" : operands[i].toString(depth-1)));
        }
      }
    }
    else {
      ret += "\nOperands: null";
    }
    return Strings.indent(2, ret);  
  }

  /**
   * Displays this node as a String, implementing ExploreNode interface; depth
   * parameter is a bound on the depth of the portion of the tree that is displayed.
   */
  public String toString(int depth) {
    if (depth <= 0) return "";
    return "\n*OpApplNode: " + operator.getName() + "  " + super.toString(depth+1) 
           + "  errors: " + (errors != null ? "non-null" : "null")
           + toStringBody(depth);
  }

}
