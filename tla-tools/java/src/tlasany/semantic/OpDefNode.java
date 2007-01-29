// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import tlasany.parser.SyntaxTreeNode;
import tlasany.st.Location;
import tlasany.st.TreeNode;
import tlasany.utilities.Strings;
import util.UniqueString;

  /**
   * An OpDefNode can have one of the following kinds:                    
   *                                                                      
   *     ModuleInstanceKind                                               
   *        Represents a module instantiation name, such as the M         
   *        in  M(a, b) == INSTANCE ...                                   
   *                                                                      
   *     UserDefinedOpKind                                                
   *        Represents a user definition, for example the definition      
   *        of the symbol Foo in  Foo(A, B) == expr.                      
   *                                                                      
   *     BuiltInKind                                                      
   *        An imaginary declaration for a built-in operator of TLA+      
   *        such as \cup.                                                 
   *                                                                      
   * All TLA+ constructs such as record constructors that produce an      
   * expression are represented as OppApplNodes for special operators.    
   * There is an OpDefNode of kind BuiltInKind for each of these special  
   * operators.  The special operators are:                               
   *                                                                      
   *     $FcnApply                                                        
   *        f[x] is represented as $FcnApply(f, x), and f[x,y]            
   *        is represented as $FcnApply(f, <<x, y>>).                     
   *                                                                      
   *     $RcdSelect                                                       
   *        r.c is represented as $RcdSelect(r, "c").  Note that,         
   *        semantically, $RcdSelect(r, "c") is equivalent to             
   *        $FcnApply(r, "c").  But, a tool might want to handle          
   *        records differently from other functions for efficiency.      
   *                                                                      
   *     $NonRecursiveFcnSpec                                             
   *        The definition  f[x \in S] == exp  is represented as          
   *        $NonRecursiveFcnSpec(x, S, exp) if f does not appear in exp.  
   *                                                                      
   *     $RecursiveFcnSpec                                                
   *        Similar to $NonRecursiveFcnSpec, except for recursive         
   *        function definitions.                                         
   *                                                                      
   *     $Pair                                                            
   *     $RcdConstructor                                                  
   *        We represent [L1 |-> e1, L2 |-> e2] as                        
   *        $RcdConstructor($Pair("L1", e1), $Pair("L2", e2))             
   *                                                                      
   *     $SetOfRcds                                                          
   *        Used to represent [L1 : e1, L2 : e2] much like                
   *        $RcdConstructor.                                              
   *                                                                      
   *     $Except                                                          
   *     $Seq                                                             
   *        We represent [f EXCEPT ![a].b[q] = c, ![d,u][v] = e]          
   *        as $Except(f, $Pair( $Seq(a, "b", q), c ),                     
   *                      $Pair( $Seq(<<d,u>>, v), e )).                   
   *        We are representing the equivalent expressions                
   *        [r EXCEPT !["a"] = b] and [r EXCEPT !.a = b] the same, even   
   *        though we represent r["a"] and r.a differently.  This         
   *        inconsistency resulted from a compromise between supporting   
   *        efficient implementation and keeping the api simple.  If      
   *        consistency is desired, we should probably eliminate          
   *        $RcdSelect.                                                   
   *                                                                      
   *     $Tuple                                                           
   *        We represent <<a, b, c>> as $Tuple(a, b, c).                  
   *                                                                      
   *     $CartesianProd                                                   
   *        Represents  A \X B \X C  as  $CartesianProd(A, B, C)          
   *                                                                      
   *     $BoundedChoose                                                   
   *        Represents CHOOSE x \in S : P                                 
   *                                                                      
   *     $UnboundedChoose                                                 
   *        Represents CHOOSE x : P.                                      
   *                                                                      
   *     $BoundedForall                                                   
   *        Represents \A x \in S : P.                                    
   *                                                                      
   *     $UnboundedForall                                                 
   *        Represents \A x : P.                                          
   *                                                                      
   *     $BoundedExists                                                   
   *        Represents \E x \in S : P.                                    
   *                                                                      
   *     $UnboundedExists                                                 
   *        Represents \E x : P.                                          
   *                                                                      
   *     $SetEnumerate                                                    
   *        Represents {a, b, c}.                                         
   *                                                                      
   *     $SubsetOf                                                        
   *        Represents {x \in S : p}.                                     
   *                                                                      
   *     $SetOfAll                                                        
   *        Represents {e : x \in S}.                                     
   *                                                                      
   *     $FcnConstructor                                                  
   *        Represents [x \in S |-> e].                                   
   *                                                                      
   *     $SetOfFcns                                                       
   *        Represents [S -> T].                                          
   *                                                                      
   *     $IfThenElse                                                      
   *     $ConjList                                                        
   *     $DisjList                                                        
   *         These are fairly obvious.                                    
   *                                                                      
   *     $Case                                                            
   *        We represent CASE p1 -> e1 [] p2 -> e2 as                     
   *        $Case( $Pair(p1, e1), $Pair(p2, e2) ) and we represent        
   *        CASE p1 -> e1 [] p2 -> e2 [] OTHER -> e3  as                  
   *        $Case( $Pair(p1, e1), $Pair(p2, e2), $Pair(null, e3))         
   *                                                                      
   *     $SquareAct                                                       
   *        We represent [A]_e as $SquareAct(A, e).                       
   *                                                                      
   *     $AngleAct                                                        
   *        We represent <<A>>_e as $AngleAct(A, e).                      
   *                                                                      
   *     $WF(e, A)                                                        
   *     $SF(e, A)                                                        
   *        We represent WF_e(A) as $WF(e, A), etc.                       
   *                                                                      
   *     $TemporalExists                                                  
   *     $TemporalForall                                                  
   *        Represent \EE and \AA.                                        
   */

public class OpDefNode extends OpDefOrDeclNode {

  private boolean           local  = false;  // Is this definition local to the module?
  private ExprNode          body   = null;   // the expression that is the def'n of the operator
  private FormalParamNode[] params = null;   // Array of FormalParamNodes that this Operator takes

  /* Used only for creating nullODN */
  public OpDefNode(UniqueString us) {
    super(us, 0, -2, null, null, SyntaxTreeNode.nullSTN); 
    if (st != null) {
      st.addSymbol(us, this);
    }
  }

  /* Invoked in configuration.Configuration for built-in ops */
  public OpDefNode(UniqueString us, int k, int ar, FormalParamNode[] parms,
		   boolean localness, ExprNode exp, ModuleNode oModNode,
		   SymbolTable symbolTable, TreeNode stn) {
    super(us, k, (parms == null ? -1 : parms.length), oModNode, symbolTable, stn);
    params = parms;

    // Create phony FormalParamNodes for built-in operators
    if ( arity >= 0 ) {
      for (int i = 0; i < params.length; i++ ) {
        params[i] = new FormalParamNode(UniqueString.uniqueStringOf("Formal_" + i),
					0, null, symbolTable, oModNode);
      }
    }
    if (st != null) {
      st.addSymbol(us, this);
    }
  }

  /* Invoked by ordinary operator definition. */
  public OpDefNode(UniqueString us, int k, FormalParamNode[] parms, boolean localness, 
                   ExprNode exp, ModuleNode oModNode, SymbolTable symbolTable, 
                   TreeNode stn) {
    super(us, k, (parms != null ? parms.length : 0), oModNode, symbolTable, stn);
    this.local = localness;
    this.params = (parms != null ? parms : new FormalParamNode[0]);
    this.body = exp;
    if (st != null) {
      st.addSymbol(us, this);
    }
  }

  /* Used for ModuleInstance names */
  public OpDefNode(UniqueString us, FormalParamNode[] parms, boolean localness, 
                   ModuleNode oModNode, SymbolTable symbolTable, TreeNode stn) {
    super(us, ModuleInstanceKind, (parms == null ? -1 : parms.length), oModNode, symbolTable, stn);
    this.params = parms;
    this.local = localness;
    if (st != null) {
      st.addSymbol(us, this);
    }
  }

  /**
   * When applied to a user-defined op node or a built-in op
   * with a fixed number of params, returns an array of the formal 
   * parameter nodes associated with this operator.  For example,
   * with
   *
   *   F(A(_,_), b, c) == A(b,c)
   *
   * it returns an array of length 3.
   * 
   * When applied to a module instance node, returns (new) parameter
   * nodes introduced by that module instance. For example, with
   *
   *   D(x,y) == INSTANCE FooMod WITH c <- +
   *
   * it returns an array of length 2.
   * 
   * When applied to a builtin op with a variable number of args, returns null.
   */
  public final FormalParamNode[] getParams() { return this.params; }

  /**
   * For a UserDefinedOp node, the getBody() method returns the
   * definition.  For other kinds of OpDefNodes, the method is 
   * meaningless and should return null.  For example, if nOp is the    
   * UserDefinedOp node for the operator Op defined by                  
   *                                                                    
   *    Op(a, b) == expr                                                
   *                                                                    
   * then nOp.getBody() is a ref to the ExprNode for expr.              
   *                                                                    
   * A tool can use the setBody method to change the definition of a
   * user-defined operator.  For example, TLC can implement the
   * replacement A <- B by setting the Body of A's UserDefinedOp node
   * to equal the Body of B's UserDefinedOp node.
   *                                                                    
   * The setBody method checks that body.getParent() equals the
   * current node, and raises an exception if it doesn't.
   */
  public final ExprNode getBody() { return this.body; }

  /**
   * Sets the body of this definition to the expression in body.  See
   * documentation for getBody();
   */
  public final void setBody(ExprNode body) { this.body = body; }

  /**
   * Returns true iff this definition is declared LOCAL; definitions
   * that are in fact local, e.g. in LETs or inner modules, but that do not 
   * get declared so using the LOCAL modifier are NOT
   * local for this purpose.
   */
  public final boolean isLocal() { return this.local; }

  /**
   * Returns the arity of this operator, or -1 in the case of an operator
   * that takes a variable number of args.
   */
  public final int getArity() { return this.arity; } 

  /**
   * This method tests whether an operand is a legal instance of an
   * operator being passed as argument to another operator
   */
  private boolean matchingOpArgOperand (ExprOrOpArgNode arg, int i) {
    return ((arg instanceof OpArgNode) &&
	    params[i].getArity() == ((OpArgNode)arg).getArity());
  }

  /* This method shortens the match() method right after it */
  private boolean errReport(Location loc, String s) {
    errors.addError(loc, s);
    return false;
  }

  /**
   * This method is called at the end of OpApplNode constructors to
   * make sure the OpApplNode is correct by "matching" the argument
   * expressions against the parameter list requirements.
   *
   * The OpApplNode must have the same number of args as parameters,
   * unless the operator takes a variable number of parameters.  Also,
   * FormalParamNodes that specify operators of arity > 0 must be
   * matched by arguments that OpArgNodes of the appropriate arity.
   *
   * Constructor argument oan is an OpApplNode having THIS OpDefNode
   * as its operator part.  This method decides whether the arguments
   * to oan (i.e args[]) match the argument pattern required by THIS
   * OpDefNode in terms of arity, etc.
   */
  public final boolean match(OpApplNode oanParent, ModuleNode mn) throws AbortException {
    ExprOrOpArgNode[] args       = oanParent.getArgs();  // arg expr's that THIS operator is being applied to
    boolean           result     = true;                 // Remains true unless an error is detected
    boolean           tempResult = true;

    Location loc = (oanParent.getTreeNode() != null 
		    ? oanParent.getTreeNode().getLocation()
		    : null);

    // If THIS OpDefNode defines a module instance, then something is clearly wrong
    //   since a module instance node should not be under an OpApplNode 
    if (this.getKind() == ModuleInstanceKind) {
      errors.addError(loc, "Module instance identifier where operator should be.");
      result = false;
    }
    else if ( arity == -1 ) {
      // if THIS OpDefNode is for an operator that takes a variable number of args ...
      if ( args != null ) { // args vector may have length zero, but should not be null
	for ( int i = 0; i < args.length; i++ ) {
	  if (args[i] instanceof OpArgNode) {
	    errors.addError(loc, "Illegal expression used as argument " + (i+1) + 
			    " (counting from 1) to operator '" + this.getName() + "'.");
	    result = false;
	  }
	}
      }
      else  {// null arg vector; supposedly cannot happen
	errors.addAbort(loc, "Internal error: null args vector for operator '" +
			this.getName() + "' that should take variable number of args.",true);
      }
    }
    else {
      // It is an operator with a fixed number of params (possibly zero)
      if (args == null | params == null) { // args vector should never be null
        errors.addAbort(loc, "Internal error: Null args or params vector for operator '" + 
			this.getName() + "'.", true);
      }
      else { // Normal case: params != null & args != null
	// if the number of args does not match the number of params 
        if (params.length != args.length) {
	  errors.addError(loc, "Wrong number of arguments (" + args.length +
			  ") given to operator '" + this.getName() + "', which requires " +
			  params.length + " arguments.");
	  result = false;
        }
	else {
	  // we have the correct number of args
	  // if the operator is a built-in op... (We separate out the logic for the builtin ops 
	  // because there are no FormalParamNodes in the semantic tree to describe their arguments
          if ( this.getKind() == BuiltInKind ) {
	    // for each arg, check that an expression, not an operator, is used as argument,
	    // since no built-in operators take operators as arguments
            for ( int i = 0; i < params.length; i++ ) {
	      if (args[i] instanceof OpArgNode) {
		errors.addError(loc, "Non-expression used as argument number " + (i + 1)
				+ " (counting from 1) to BuiltIn operator '" 
				+ this.getName() + "'.");
		result = false;
              }
            }
          }
	  else if ( this.getKind() == UserDefinedOpKind ) {
	    // for each formal parameter to THIS OpDef
	    for (int i = 0; i < params.length; i++ ) {
	      // if i'th FormalParamNode shows arity == 0, then an expression is expected as argument
	      if (params[i].getArity() == 0) {
		if (args[i] instanceof OpArgNode) {
		  // No ops can be passed in this parm position
		  errors.addError(loc, "Operator used in argument number " + (i+1) 
				  + " (counting from 1) has incorrect number of arguments.");
		  result = false;
		}
	      }
	      else if (params[i].getArity() > 0) {
                // OpArgNode of correct arity must be passed in this arg position
                if (! matchingOpArgOperand(args[i],i)) {
		  errors.addError(loc, "Argument number " + (i+1) + " (counting from 1) to operator '"  
				  + this.getName() + "' should be a " + params[i].getArity() 
				  + "-parameter operator.");
		  result = false;
		}
	      } else { // if params[i].getArity() < 0
		errors.addError(loc,
				"Internal error: Operator '" + this.getName() +
				"' indicates that it requires a negative number of arguments.");
	      }
	    } // end for
          }
	  else {
            errors.addAbort(null,
			    "Internal error: operator neither BuiltIn nor UserDefined" +
			    " in call to OpDefNode.match()", true);
	  }
        }
      } // end "normal case"
    } // end "arity != -1" case

    return result;
  } // end match()

  /* Level checking */
  private boolean levelCorrect;
  private int level;
  private HashSet levelParams;
  private SetOfLevelConstraints levelConstraints;
  private SetOfArgLevelConstraints argLevelConstraints;
  private HashSet argLevelParams;
  private int[] maxLevels;
  private int[] weights;
  private int[][] minMaxLevel;
  private boolean[][][] opLevelCond;
  
  /* Set the level information for this builtin operator. */
  public final void setBuiltinLevel(BuiltInLevel.Data d) {
    if (d.arity == -1) {
      this.maxLevels = new int[1];
      this.maxLevels[0] = d.argMaxLevels[0];
      this.weights = new int[1];
      this.weights[0] = d.argWeights[0];
    }
    else {
      this.maxLevels = d.argMaxLevels;
      this.weights = d.argWeights;
    }

    this.levelCorrect        = true;
    this.level               = d.opLevel;
    this.levelParams         = EmptySet;
    this.levelConstraints    = EmptyLC;
    this.argLevelConstraints = EmptyALC;
    this.argLevelParams      = EmptySet;
  }
  
  public final boolean levelCheck() {
    if (this.levelConstraints != null) return this.levelCorrect;
    
    // Level check the body:
    this.levelCorrect = this.body.levelCheck();

    // Calculate level information:
    this.level = this.body.getLevel();

    this.maxLevels = new int[this.params.length];
    SetOfLevelConstraints lcSet = this.body.getLevelConstraints();
    for (int i = 0; i < this.params.length; i++) {
      Object plevel = lcSet.get(params[i]);
      if (plevel == null) {
	this.maxLevels[i] = MaxLevel;
      }
      else {
	this.maxLevels[i] = ((Integer)plevel).intValue();
      }
    }

    this.weights = new int[this.params.length];
    for (int i = 0; i < this.params.length; i++) {
      if (this.body.getLevelParams().contains(this.params[i])) {
	this.weights[i] = 1;
      }
    }

    this.minMaxLevel = new int[this.params.length][];
    SetOfArgLevelConstraints alcSet = this.body.getArgLevelConstraints();
    for (int i = 0; i < this.params.length; i++) {
      int alen = this.params[i].getArity();
      this.minMaxLevel[i] = new int[alen];
      for (int j = 0; j < alen; j++) {
	Object alevel = alcSet.get(new ParamAndPosition(this.params[i], j));
	if (alevel == null) {
	  this.minMaxLevel[i][j] = MinLevel;
	}
	else {
	  this.minMaxLevel[i][j] = ((Integer)alevel).intValue();
	}
      }
    }

    this.opLevelCond = new boolean[this.params.length][this.params.length][];
    HashSet alpSet = this.body.getArgLevelParams();
    for (int i = 0; i < this.params.length; i++) {
      for (int j = 0; j < this.params.length; j++) {
	this.opLevelCond[i][j] = new boolean[this.params[i].getArity()];
	for (int k = 0; k < this.params[i].getArity(); k++) {
	  ArgLevelParam alp = new ArgLevelParam(this.params[i], k, this.params[j]);
	  this.opLevelCond[i][j][k] = alpSet.contains(alp);
	}
      }
    }

    this.levelParams = (HashSet)this.body.getLevelParams().clone();
    for (int i = 0; i < this.params.length; i++) {
      this.levelParams.remove(this.params[i]);
    }

    this.levelConstraints = (SetOfLevelConstraints)lcSet.clone();
    for (int i = 0; i < this.params.length; i++) {
      this.levelConstraints.remove(this.params[i]);
    }

    this.argLevelConstraints = (SetOfArgLevelConstraints)alcSet.clone();
    for (int i = 0; i < this.params.length; i++) {
      int alen = this.params[i].getArity();
      for (int j = 0; j < alen; j++) {
	this.argLevelConstraints.remove(new ParamAndPosition(this.params[i], j));
      }
    }

    this.argLevelParams = new HashSet();
    Iterator iter = alpSet.iterator();
    while (iter.hasNext()) {
      ArgLevelParam alp = (ArgLevelParam)iter.next();
      if (!alp.op.occur(this.params) ||
	  !alp.param.occur(this.params)) {
	this.argLevelParams.add(alp);
      }
    }
    return this.levelCorrect;
  }

  public final int getLevel() {
    this.levelCheck();
    return this.level;
  }

  public final HashSet getLevelParams() {
    this.levelCheck();
    return this.levelParams;
  }

  public final SetOfLevelConstraints getLevelConstraints() {
    this.levelCheck();    
    return this.levelConstraints;
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() {
    this.levelCheck();    
    return this.argLevelConstraints;
  }

  public final HashSet getArgLevelParams() {
    this.levelCheck();    
    return this.argLevelParams;
  }

  public final int getMaxLevel(int i) {
    this.levelCheck();    
    int idx = (this.getArity() == -1) ? 0 : i;
    return this.maxLevels[idx];
  }

  public final int getWeight(int i) {
    this.levelCheck();    
    int idx = (this.getArity() == -1) ? 0 : i;
    return this.weights[idx];
  }  

  public final int getMinMaxLevel(int i, int j) {
    this.levelCheck();    
    if (this.minMaxLevel == null) {
      return ConstantLevel;
    }
    return this.minMaxLevel[i][j];
  }

  public final boolean getOpLevelCond(int i, int j, int k) {
    this.levelCheck();    
    if (this.opLevelCond == null) {
      return false;
    }
    return this.opLevelCond[i][j][k];
  }

  /**
   * toString, levelDataToString, and walkGraph methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    if (this.arity < 0) {
      return "Arity: "               + this.arity                   + "\n" +
             "Level: "               + this.getLevel()              + "\n" +
             "MaxLevel: "            + this.maxLevels[0]            + "\n";
    }
    else {
      return "Arity: "               + this.arity                    + "\n" +
             "Level: "               + this.getLevel()               + "\n" +
             "LevelParams: "         + this.getLevelParams()         + "\n" +
             "LevelConstraints: "    + this.getLevelConstraints()    + "\n" +
             "ArgLevelConstraints: " + this.getArgLevelConstraints() + "\n" +
             "ArgLevelParams: "      + this.getArgLevelParams()      + "\n" +
             "MaxLevel: "            + this.maxLevels                + "\n";	
    }
  }

  /**
   * walkGraph finds all reachable nodes in the semantic graph
   * and inserts them in the Hashtable semNodesTable for use by 
   * the Explorer tool.
   */
  public final void walkGraph(Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);
    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(uid, this);
    if (params != null && params.length > 0) {
      for (int i = 0; i < params.length; i++) {
	if (params[i] != null) params[i].walkGraph(semNodesTable);
      }
    }
    if (body != null) body.walkGraph(semNodesTable);
  }

  /**
   * Displays this node as a String, implementing ExploreNode
   * interface; depth parameter is a bound on the depth of the portion
   * of the tree that is displayed.
   */
  public final String toString(int depth) {
    if (depth <= 0) return "";

    String ret = "\n*OpDefNode: " + this.getName().toString() + "  " +
                 super.toString(depth) + "  local: " + local;
    if (params != null) {
      String tempString = "\nFormal params: " + params.length;
      for (int i = 0; i < params.length; i++) {
        tempString += Strings.indent(2, ((params[i] != null)
					 ? params[i].toString(depth-1) 
					 : "\nnull"));
      }
      ret += Strings.indent(2,tempString);
    }
    else {
      ret += "\nFormal params: null";
    }

    // Only print this stuff if user asks for a larger than necessary depth
    if (depth > 1) {
      if (st != null) {
        ret += Strings.indent(2,"\nSymbolTable: non-null");
      }
      else {
        ret += Strings.indent(2,"\nSymbolTable: null");
      }
    }
    if (body != null) {
      ret += Strings.indent(2,"\nBody:" + Strings.indent(2,body.toString(depth-1)));
    }
    else {
      ret += Strings.indent(2,"\nBody: null");
    }
    return ret;
  }

}
