// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import org.zambrovski.tla.tlasany.semantic.ErrorsContainer;

import tlasany.parser.Operators;
import tlasany.parser.SyntaxTreeNode;
import tlasany.st.SyntaxTreeConstants;
import tlasany.st.TreeNode;
import tlasany.utilities.Stack;
import tlasany.utilities.Strings;
import tlasany.utilities.Vector;
import util.Assert;
import util.UniqueString;

// This class generates a semantic graph from a parse tree. It also uses
// the list of modules to access contexts to instantiate or extend.

// The construction of the graph follows from the grammar and the API.

// The first invocation of the translation of a module is going to be
// done with the global context, which contains external symbols. The
// embedded modules will use the context of the englobing module.

// SymbolTable and Context must be suitably initialized *before*
// generateModule is invoked, since we do not know in which context we
// are working from the inside.

public class Generator implements ASTConstants, SyntaxTreeConstants {

  private Context     context;               // current context, not very useful.
  private SymbolTable symbolTable;           // symbol table used throughout the spec.
                                             //   except for embedded modules
  private ExternalModuleTable moduleTable;
  public  ErrorsContainer      errors;
  private Stack       excStack;              // Holds stack of OpApplNodes for $Except
                                             //   operators; used for @
  private Stack       excSpecStack;          // Holds stack of OpApplNode for @Pair
                                             //   operators representing ExceptSpecs;
                                             //   also used for @

  // dummy definitions; used during the creation of the "-- TLA+ BUILTINS --" phony module,
  // before real modules are processed; also used somewhat inconsistently to avoid returning
  // null values, and to allow semantic analysis to proceed when an error is detected
  private SubstInNode       nullSubstIn;
  private FormalParamNode[] nullParam;
  private OpDefNode         nullODN;
  protected OpApplNode      nullOAN;
  protected OpArgNode       nullOpArg;

  private final static UniqueString S_e     = UniqueString.uniqueStringOf("\\E");
  private final static UniqueString S_f     = UniqueString.uniqueStringOf("\\A");
  private final static UniqueString S_te    = UniqueString.uniqueStringOf("\\EE");
  private final static UniqueString S_tf    = UniqueString.uniqueStringOf("\\AA");
  private final static UniqueString S_a     = UniqueString.uniqueStringOf("<<");
  private final static UniqueString S_brack = UniqueString.uniqueStringOf("[");
  private final static UniqueString S_sf    = UniqueString.uniqueStringOf("SF_");
  private final static UniqueString S_wf    = UniqueString.uniqueStringOf("WF_");
  private final static UniqueString S_at    = UniqueString.uniqueStringOf("@");

  class Function {

    class pair{
      UniqueString a;
      OpApplNode   b;

      pair(UniqueString uniqueString, OpApplNode oan) { a = uniqueString; b = oan; }
      UniqueString uniqueString() { return a; }
      OpApplNode   oan() { return b; }
    }

    Stack funcStack = new Stack();

    void push(UniqueString uniqueString, OpApplNode oan) {
      funcStack.push( new pair(uniqueString, oan) );
    }

    void pop() { funcStack.pop(); }

    // If same function found farther down on stack, then this is a recursive
    // function definition--change the operator to indicate so.
    boolean recursionCheck(UniqueString uniqueString) {
      for (int lvi = funcStack.size()-1; lvi>=0; lvi-- ) { 
        if (uniqueString.equals( ((pair)funcStack.elementAt( lvi )).uniqueString())) {
           // OA-rfs = recursive func spec
           ((pair)funcStack.elementAt(lvi)).oan().resetOperator(OP_rfs); 
           return true;
        }
      }
      return false;
    }

  } // end class Function

  // This is the only instance of class Function
  Function functions = new Function();

  // This class represents a generalized identifier, e.g. a syntactic phrase such as
  // A(2)!B(x,y)!C!D(u,v,w)  In this case the compoundID would be A!B!C!D and the
  // args array would contain [2,x,y]  (i.e. not including u,v,and w, because they are 
  // args to the main operator, and not part of the GenID
  private class GenID {

    private TreeNode          treeNode;          // The syntax tree node holding this GenID
    private StringBuffer      compoundID;        // The string name of the compound op, with "!"'s, if any
    private Vector            argsVector = new Vector();        
                                                 // Vector of arguments (ExprNodes and OpArgNodes)
                                                 // that are embedded in the generalized identifier

    // The next three fields are null until the finalAppend method has been called
    private UniqueString      compoundIDUS;      // UniqueString version of compoundID
    private SymbolNode        fullyQualifiedOp;  // SymbolNode for compoundID
    private ExprOrOpArgNode[] args;              // Array with same contents as argsVector

    // Constructor
    public GenID(TreeNode node) {
      treeNode         = node;
      compoundID       = new StringBuffer("");
      compoundIDUS     = null;
      fullyQualifiedOp = null;
      args             = null;
    }

    public final UniqueString getCompoundIDUS() { return compoundIDUS;}

    public final SymbolNode getFullyQualifiedOp() { return fullyQualifiedOp; }

    public final ExprOrOpArgNode[] getArgs() { return args; }

    public final Vector getArgsVector() { return argsVector; }

    /** Append a new segment to the compound name of the operator */
    public final void append(String s) { 
      compoundID.append(s);
    }

    /** Add a new argument to the vector of arguments being constructed */
    public final void addArg(ExprOrOpArgNode arg) {
      argsVector.addElement(arg);
    }

    /**
     * Appends the final element of the fully-qualified name to the
     * GenID that has been being built using addArg() and
     * append(). Since it signals the completion of the construction
     * of the name, this method converts the name from StringBuffer to
     * UniqueString, resolves it to a SymbolNode, and converts the
     * argument list from vector to array form.
     */
    public final void finalAppend(String s, boolean unaryNegKludge) {
      // append the final segment of the compound name
      if (unaryNegKludge && s.equals("-")) {
        compoundID.append("-.");
      }
      else {
        compoundID.append(s);
      }

      // convert the full name to a UniqueString
      compoundIDUS = UniqueString.uniqueStringOf(compoundID.toString());

      // look up the full name in the SymbolTable (may return null)
      fullyQualifiedOp = symbolTable.resolveSymbol(Operators.resolveSynonym(compoundIDUS));

      if (fullyQualifiedOp == null && compoundIDUS != S_at) {
	// if not in the symbol table and not "@", then it is an unresolved symbol
        errors.addError(treeNode.getLocation(),
			"Could not find declaration or definition of symbol '" +
			UniqueString.uniqueStringOf(compoundID.toString()) + "'.");
      }
    }

    public final void finalizeID() {
      // copy argsVector contents into args array
      args = new ExprOrOpArgNode[argsVector.size()];

      for (int i = 0; i < args.length; i++) {
        args[i] = (ExprOrOpArgNode)argsVector.elementAt(i);
      }
    }

    /**
     * Special kluge to append a "." to the name of this ID; 
     * should be used ONLY to change unary "-" to "-."
     */
    public final void appendDot() {
      compoundIDUS = UniqueString.uniqueStringOf(compoundIDUS.toString() + ".");
    }

    public final String toString(int n) {
      String ret = "compound ID: " + compoundID.toString() + "\nargs: " + args.length + "\n";
      for (int i = 0; i < args.length; i++) {
        ret += Strings.indent(2,args[i].toString(n));
      }
      return ret;
    }

  } // end GenID

  // Constructor
  public Generator(ExternalModuleTable moduleTable, ErrorsContainer errs) {
    nullParam = new FormalParamNode[0];
    nullODN   = new OpDefNode(UniqueString.uniqueStringOf("nullODN"));
    nullOAN   = new OpApplNode(nullODN);
    nullOpArg = new OpArgNode(UniqueString.uniqueStringOf("nullOpArg"));
    this.errors       = errs;
    this.moduleTable  = moduleTable;
    this.symbolTable  = new SymbolTable(moduleTable, errors);
    this.excStack     = new Stack(); 
    this.excSpecStack = new Stack();
  }

  public final SymbolTable getSymbolTable() { return symbolTable; }

  public final ModuleNode generate(TreeNode treeNode) throws AbortException {
    if (treeNode.isKind( N_Module )) {
      this.context = symbolTable.getContext();
      return this.generateModule(treeNode, null);
    } 
    return null;
  }

  private final Context getContext(UniqueString us) {
    ModuleNode symbolNode = symbolTable.resolveModule(us);

    if (symbolNode == null) {
      return moduleTable.getContext(us);
    }
    return symbolNode.getContext();
  }

  private final ModuleNode generateModule(TreeNode treeNode, ModuleNode parent)
  throws AbortException {
    TreeNode[] children    = treeNode.heirs();     // Array of heirs of the module root
    TreeNode[] definitions = null;                 // Array of definitions in the module
    TreeNode[] ss          = children[0].heirs();  // Array of heirs of the module header
                                                   // ss[1] is always the module name
    String  moduleName     = ss[1].getImage();     // the module name
    ModuleNode currentModule = new ModuleNode(ss[1].getUS(), context, treeNode);

    // if this is an internal module, add its ModuleNode to the end of
    // the list of definitions for the parent
    if (parent != null) {
      parent.appendDef(currentModule);
    }

    symbolTable.setModuleNode( currentModule );
    // children[1] == extends
    // children[1] is always the Extend's list (even if none)
    processExtendsList(children[1].heirs(), currentModule); 

    // children[2] == body
    // children[2] is always the module body note that this line
    // redefines "children" to be the array of definitions in the
    // module
    definitions = children[2].heirs();

    // for each declaration, op definition, etc. in the body...
    for (int lvi = 0; lvi < definitions.length; lvi++) {
      switch (definitions[lvi].getKind()) {
      case N_VariableDeclaration :
        processVariables(definitions[lvi].heirs(), currentModule);
        break;

      case N_ParamDeclaration :
        processParameters(definitions[lvi].heirs(), currentModule);
        break;

      case N_OperatorDefinition :
        processOperator(definitions[lvi], null, currentModule);
        break;

      case N_FunctionDefinition :
        processFunction(definitions[lvi], null, currentModule);
        break;

      case N_ModuleDefinition :
        processModuleDefinition(definitions[lvi], null, null, currentModule);
        break;

      case N_Module :
	// Modules can be nested, but inner ones need to keep a 
	// separate SymbolTable of their own.
        SymbolTable oldSt = symbolTable; 
        symbolTable = new SymbolTable(moduleTable, errors, oldSt);
        context = new Context(moduleTable, errors);
        symbolTable.pushContext(context);
	ModuleNode mn = generateModule(definitions[lvi], currentModule);
        symbolTable.popContext();
        symbolTable = oldSt;

	// Add the inner module's name to the context of the outer module
        symbolTable.addModule(mn.getName(), mn);

        // RuntimeConfiguration.get().getErrStream().println(mn.getName() + " added to SymbolTable for " + currentModule.getName()); 
        break;

      case N_Instance :
        generateInstance(definitions[lvi], currentModule);
        break;

      case N_Proof :
	break;

      case N_Theorem : 
        processTheorem(definitions[lvi], currentModule);
        break; 

      case N_Assumption : 
        processAssumption(definitions[lvi], currentModule);
        break;

      case 35 :
	// Intended to handle "---------". Kludge for a parser bug.
	break;
	
      default :
        errors.addAbort(definitions[ lvi ].getLocation(),
			"Internal error: Syntax node of kind " + definitions[ lvi ].getKind() +
			" unsupported " + definitions[ lvi ].getImage(), true);
        break;
      }
    }
    return currentModule;
  }

  // This method must be extended so that the Extends list hangs off of 
  // the ModuleNode to support a getExtends() method that might reasonably 
  // be in the API.
  private final void processExtendsList(TreeNode treeNodes[], ModuleNode cm)
  throws AbortException{
    Vector extendeeVector = new Vector(2);

    if (treeNodes != null) {
      // module names in the EXTENDS list are separated by commas; hence incr by 2
      for (int lvi = 1; lvi < treeNodes.length; lvi += 2) {
        // Try to find the ModuleNode for the module being EXTENDED in the symbolTable
        UniqueString extendeeID = treeNodes[lvi].getUS();
        ModuleNode extendee = symbolTable.resolveModule(extendeeID);

        // It must be an external module if it isn't in the symbolTable;
	// try to find it in moduleTable (it cannot be in both places)
        if (extendee == null) {
	  extendee = moduleTable.getModuleNode(extendeeID);
          if (extendee == null) {
            errors.addAbort(treeNodes[lvi].getLocation(), 
                            "Could not find module " + extendeeID,
			    false);
          }
	}

        extendeeVector.addElement(extendee);

        Context context = this.getContext(extendeeID);
        if (context != null) {
          symbolTable.getContext().mergeExtendContext(context);
	}
        else {
          errors.addError(treeNodes[lvi].getLocation(),
			  "Couldn't find context for module " + extendeeID);
	}

	// copy nonlocal Assumes and Theorems
        cm.copyAssumes(extendee);
	cm.copyTheorems(extendee);
      }
    }
    cm.createExtendeeArray(extendeeVector);
  }

  // This method must be extended so that the variable declarations list 
  // hangs off of the module node to support the getVariableDecls method 
  // of the API.
  private final void processVariables(TreeNode treeNodes[], ModuleNode cm) {
    for (int lvi = 1; lvi < treeNodes.length; lvi += 2) {
      UniqueString us = treeNodes[ lvi ].getUS();

      if (us == S_at) {
	errors.addError(treeNodes[lvi].getLocation(),
			"Attempted to declare '@' as a variable.");
      }

      // The next line has its side-effects in the constructor; in particular,
      // the new OpDeclNode is placed in the symbolTble there.
      new OpDeclNode(us, VariableDeclKind, 1, 0, cm, symbolTable, treeNodes[lvi]);
    }
  }

  private final void buildParameter( TreeNode treeNode, ModuleNode cm ) {
    UniqueString us = null;
    int arity = 0;
    TreeNode[] ss = treeNode.heirs();

    if      ( treeNode.isKind( N_IdentDecl ) ) {
      us = ss[0].getUS();
      arity = (ss.length - 1) / 2;
    }
    else if ( treeNode.isKind( N_PrefixDecl ) ) {
      us = ss[0].getUS();
      arity = 1;
    }
    else if ( treeNode.isKind( N_InfixDecl ) ) {
      us = ss[1].getUS();
      arity = 2;
    }
    else if ( treeNode.isKind( N_PostfixDecl ) ) {
      us = ss[1].getUS();
      arity = 1;
    }
    else {
      errors.addError(treeNode.getLocation(),
		      "Unknown parameter declaration " + treeNode.getUS());
    }
    SymbolNode symbolNode = 
      new OpDeclNode(us, ConstantDeclKind, 0, arity, cm, symbolTable, treeNode);
  }

  private final void processParameters(TreeNode treeNodes[], ModuleNode cm) {
    for (int lvi = 1; lvi < treeNodes.length; lvi +=2 ) {
      if (treeNodes[lvi].getUS() == S_at) {
	errors.addError(treeNodes[lvi].getLocation(),
			"Attempted to declare '@' as a constant.");
      }
      buildParameter( treeNodes[lvi], cm );
    }
  }

  /**
   * Processes the LHS of an operator definition, in several ways,
   * depending on whether it is a prefix, infix, postfix, or a
   * parameter-free or parameter-ful function-notation operator. Also
   * creates context entries for the operator and its parameters.
   */
  private final void processOperator(TreeNode treeNode, Vector defs, ModuleNode cm) 
  throws AbortException {
    TreeNode           syntaxTreeNode = treeNode;
    UniqueString       name     = null;
    int                arity    = 0;
    boolean            local    = syntaxTreeNode.zero() != null;
    TreeNode []        children = syntaxTreeNode.one();
    TreeNode []        ss       = children[0].heirs();
    FormalParamNode [] params   = null;
    Context            ctxt     = new Context(moduleTable, errors);
  
    // New context needed because parameter symbols may have to be
    // added if the operator being defined takes params
    symbolTable.pushContext( ctxt );
  
    // If the operator is an identifier (possibly with params), as
    // opposed to a prefix, infix, or postfix symbol
    if ( children[ 0 ].isKind( N_IdentLHS ) ) {
      if ( ss.length > 2 ) {
	// If the operator has arguments
        params = new FormalParamNode[ (ss.length-2) / 2 ];
        for ( int lvi = 2; lvi < ss.length; lvi += 2 ) {
          TreeNode sss[] = ss[ lvi ].heirs();
          if        ( ss[ lvi ].isKind( N_IdentDecl ) ) {
	    // parameter is simple identifier
            name = sss[0].getUS();
            arity = (sss.length - 1 )/ 2;
          }
	  else if ( ss[ lvi ].isKind( N_InfixDecl ) ) {
	    // parameter is infix operator
            name = sss[1].getUS();
            arity = 2;
          }
	  else if ( ss[ lvi ].isKind( N_PrefixDecl ) ) {
	    // parameter is prefix operator
            name = sss[0].getUS();
            arity = 1;
          }
	  else {
	    // parameter must be postfix operator
	    Assert.check( ss[ lvi ].isKind( N_PostfixDecl ) ); 
            name = sss[1].getUS();
            arity = 1;
          }
          params[ (lvi-2)/2 ] = 
	    new FormalParamNode(name, arity, ss[lvi], symbolTable, cm);
        }
      }
      else {
	// The operator has no arguments
        params = new FormalParamNode[0];
      }
      name = ss[0].getUS();
    }
    else if (children[ 0 ].isKind(N_PrefixLHS)) { // operator is a prefix operator
      // Process the parameter
      params = new FormalParamNode[1];
      params[0] = new FormalParamNode(ss[1].getUS(), 0, ss[1], symbolTable, cm);
  
      // Process the operator
      name = Operators.resolveSynonym(ss[0].getUS());
    }
    else if (children[ 0 ].isKind(N_InfixLHS)) { // operator is an infix operator
      params = new FormalParamNode[2];
      // Process the first param
      params[0] = new FormalParamNode(ss[0].getUS(), 0, ss[0], symbolTable, cm);
  
      // Process the second param
      params[1] = new FormalParamNode(ss[2].getUS(), 0, ss[2], symbolTable, cm);
  
      // Process the operator
      name = Operators.resolveSynonym(ss[1].getUS());
    }
    else if (children[ 0 ].isKind(N_PostfixLHS)) { // operator is a postfix operator
      // Process the parameter
      params = new FormalParamNode[1];
      params[0] = new FormalParamNode(ss[0].getUS(), 0, ss[0], symbolTable, cm);
  
      // Process the operator
      name = Operators.resolveSynonym(ss[1].getUS());
    }
    else {
      errors.addError(children[0].getLocation(),
		      "Unknown parameter declaration " + children[0].getUS() );
    }
  
    // Generate expression that is the body of the operator
    ExprNode exp = generateExpression( children[2], cm );
  
    // Restore old context, popping off the context containing the parameters
    symbolTable.popContext();
  
    // Create OpDefNode using symbolTable whose top context contains the parameter symbols
    SymbolNode symbolNode = new OpDefNode(name, UserDefinedOpKind, params, local, 
					  exp, cm, symbolTable, syntaxTreeNode);
    cm.appendDef(symbolNode);
  
    // defs is non-null iff this definition is in the Let part of a Let-In expression
    if (defs != null) defs.addElement(symbolNode);
  }
    
  private final void processQuantBoundArgs(TreeNode[]treeNodeA, // node whose children include the bounded quants
                                           int offset,          // nodes to skip to get to first "quantifier"
                                           OpDeclNode[][] odna, 
                                           boolean []bt,        // set to true if arg is a tuple; otherwise false
                                           ExprNode[] ena, 
                                           ModuleNode cm)
  throws AbortException {
    // For each quantifier, evaluate the bound in the context of the
    // current symbol table, i.e. before the quantified variables are
    // added to the new context, since the quantified vars may not
    // appear in the bounds.
    for (int lvi = 0; lvi < bt.length; lvi++ ) {
      // Make ss point to each N_QuantBound node in turn.
      TreeNode[] ss = treeNodeA[offset + 2 * lvi].heirs();
      // the last element in ss is expression for the quantifier bound
      ena[lvi] = generateExpression( ss[ ss.length - 1 ], cm );
    }

    // Now for each quantifier, process the variable names
    for (int lvi = 0; lvi < bt.length; lvi++ ) {
      TreeNode treeNode = treeNodeA[offset + 2 * lvi];  

      // The variable bound to the "quantifier"
      TreeNode[] ss = treeNode.heirs();

      if (ss[0].isKind(N_IdentifierTuple)) { // three elements only, go into node
        bt[lvi] = true;
        TreeNode[] sss = ss[0].heirs();
        odna[lvi] = new OpDeclNode[ sss.length / 2 ];

        for (int lvj = 0; lvj < sss.length / 2; lvj++ ) {
          odna[lvi][lvj] = new OpDeclNode(sss[ 2*lvj+1 ].getUS(), BoundSymbolKind, 0,
                                          0, cm, symbolTable, sss[ 2*lvj+1 ]);
        }
      }
      else { // gotta be N_Identifier
        bt[lvi] = false;
        odna[lvi] = new OpDeclNode[ (ss.length - 1)/2 ];

        for ( int lvj = 0; lvj <  (ss.length - 1)/2 ; lvj++ ) {
          odna[lvi][lvj] = new OpDeclNode(ss[ 2*lvj ].getUS(), BoundSymbolKind, 0,
                                          0, cm, symbolTable, ss[ 2*lvj ]);
        }
      }
    }
  }

  // Process a function definition
  private final void processFunction(TreeNode treeNode, Vector defs, ModuleNode cm)
  throws AbortException {
    TreeNode        syntaxTreeNode = treeNode;
    boolean         local      = syntaxTreeNode.zero() != null;
    TreeNode[]      ss         = syntaxTreeNode.one();  // Heirs to N_FunctionDefinition node
    int             ql         = (ss.length-4)/2;       // number of QuantBound's
    OpApplNode      oan;
    OpDefNode       odn;
    OpDeclNode [][] quants     = new OpDeclNode[ql][0];
    OpDeclNode []   fcnDeclForRecursion = new OpDeclNode[1];
    boolean []      tuples     = new boolean[ql];
    ExprNode []     domains    = new ExprNode[ql];
    ExprNode []     lhs        = new ExprNode[1];
    Context         newContext = new Context(moduleTable, errors);

    // Fill arrays with quantifier-related information; must be called
    // in scope of *new* context, since it adds parameter symbols to
    // the context.
    symbolTable.pushContext(newContext);
    processQuantBoundArgs( ss, 2, quants, tuples, domains, cm ); 

    // This is in anticipation of the possibility that the function is
    // recursive.  We are creating a new bound symbol of the same name
    // as the function to stand in the body for the function.  The
    // arity of the bound symbol is 0 because a function formally has
    // arity 0 as an operator, even if it has several arguments as a
    // function.
    fcnDeclForRecursion[0] = new OpDeclNode(ss[0].getUS(), BoundSymbolKind, 0,
					    0, cm, symbolTable, treeNode);
    symbolTable.popContext();

    // Create OpApplNode to hold the function body; type is assumed to
    // be non-recursive function (OP_nrfs); if the body is recursive,
    // this will be discovered during generateExpression() for the
    // body
    oan = new OpApplNode(OP_nrfs, fcnDeclForRecursion, new ExprNode[0], 
                         quants, tuples, domains, syntaxTreeNode, cm);

    // Ceate OpDefNode to hold the function definition, including
    // reference to the function body ("oan")
    odn = new OpDefNode(ss[0].getUS(), UserDefinedOpKind, nullParam, 
			local, oan, cm, symbolTable, syntaxTreeNode);
    cm.appendDef(odn);

    // defs is non-null iff this function definition is in the Let
    // part of a Let-In expression.  If so, then we have to accumulate
    // these defs in a vector.
    if (defs != null) defs.addElement(odn);

    // Function body must be processed in the scope of the new
    // context, including the parms
    symbolTable.pushContext( newContext );

    // Keep stack of nested function defs to enable detection of recursion
    functions.push( ss[0].getUS(), oan );

    // Create semantic graph function body in the inner context including parameters
    lhs[0] = generateExpression( ss[ss.length - 1], cm ); 
    functions.pop();
    oan.setArgs( lhs );

    // Restore old context
    symbolTable.popContext();

    // if the function body turned out to be non-recursive, then we
    // should null-out the fcnDefForRecursion ref put in place above,
    // since it is unnecessary for a nonrecursive func
    if (oan.getOperator().getName() == OP_nrfs) {
      oan.makeNonRecursive();
    }
  } // end processFunction()

  private final ExprNode processLetIn(TreeNode treeNode, TreeNode[] children, ModuleNode cm)
  throws AbortException {
    TreeNode[] syntaxTreeNode = children[1].heirs(); // extract LetDefinitions
    Vector   defVec = new Vector(4);
    Vector   instVec = new Vector(1);

    symbolTable.pushContext(new Context(moduleTable, errors));

    for (int lvi = 0; lvi < syntaxTreeNode.length; lvi++) {
      int kind = syntaxTreeNode[lvi].getKind();

      if (kind == N_OperatorDefinition) {
        processOperator(syntaxTreeNode[ lvi ], defVec, cm);
      }
      else if (kind == N_FunctionDefinition) {
        processFunction(syntaxTreeNode[ lvi ], defVec, cm );
      }
      else {
	// Assert.check(kind == N_ModuleDefinition);
        processModuleDefinition(syntaxTreeNode[lvi], defVec, instVec, cm);
      }
    }

    ExprNode body = generateExpression(children[3], cm);

    // Convert from Vector to array of OpDefNode 
    OpDefNode[] opDefs = new OpDefNode[defVec.size()];
    for (int i = 0; i < opDefs.length; i++) {
      opDefs[i] = (OpDefNode)defVec.elementAt(i);
    }

    InstanceNode[] insts = new InstanceNode[instVec.size()];
    for (int i = 0; i < insts.length; i++) {
      insts[i] = (InstanceNode)instVec.elementAt(i);
    }
    LetInNode letIn = new LetInNode(treeNode, opDefs, insts, body);
    symbolTable.popContext();
    return letIn;
  }

  private final ExprNode generateExpression(TreeNode treeNode, ModuleNode cm)
  throws AbortException {
    TreeNode[]        children = treeNode.heirs();
    TreeNode[]        ss       = null; // grandchildren
    SymbolNode        opn      = null;
    TreeNode          op       = null;
    GenID             genID;
    ExprOrOpArgNode[] sns;             // a ExprNode list used for arguments
   
    switch (treeNode.getKind()) {

    case N_Real :
      return new DecimalNode(children[0].getImage(),children[2].getImage(), treeNode);

    case N_Number :
      return new NumeralNode( children[0].getImage(), treeNode);

    case N_String :
      return new StringNode( treeNode, true);

    case N_ParenExpr :
      return generateExpression( children[1], cm );

    case N_InfixExpr :
      genID = generateGenID(children[1], cm);

      sns = new ExprOrOpArgNode[2];
      opn = symbolTable.resolveSymbol(Operators.resolveSynonym(genID.getCompoundIDUS()));
      if ( opn == null ) {
	errors.addError(treeNode.getLocation(),
			"Couldn't resolve infix operator symbol " + genID.getCompoundIDUS());
	return null;
      }

      sns[0] = generateExpression( children[0], cm );
      sns[1] = generateExpression( children[2], cm );
      return new OpApplNode(opn, sns, treeNode, cm);

    case N_PrefixExpr :
      // 1 get gen operator node
      ss = children[0].heirs();

      // 2 get rightmost part of the possibly compound Op itself;
      op = ss[1];
      genID = generateGenID(children[0], cm, true);
      sns = new ExprOrOpArgNode[1];
      opn = symbolTable.resolveSymbol(Operators.resolveSynonym(genID.getCompoundIDUS()));

      if ( opn == null ) {
	errors.addError(treeNode.getLocation(), "Couldn't resolve prefix operator symbol " + 
			genID.getCompoundIDUS() + "." );
	return null;
      } 

      sns[0] = generateExpression( children[1], cm );
      return new OpApplNode(opn, sns, treeNode, cm);    // constructor 2 

    case N_PostfixExpr :
      genID = generateGenID(children[1], cm);

      sns = new ExprNode[1];
      opn = symbolTable.resolveSymbol(Operators.resolveSynonym(genID.getCompoundIDUS()));
      if ( opn == null ) {
	errors.addError(treeNode.getLocation(), "Couldn't resolve postfix " +
			"operator symbol " + genID.getCompoundIDUS() + ".");
	return null;
      }

      sns[0] = generateExpression( children[0], cm );
      return new OpApplNode(opn, sns, treeNode, cm);  // constructor 2 

    case N_Times : // or cartesian product
      sns = new ExprNode[ (children.length+1)/2 ];  

      for (int lvi = 0; lvi < sns.length; lvi ++ ) {
	sns[ lvi ]  = generateExpression( children[ 2 * lvi ], cm );
      }
      return new OpApplNode(OP_cp, sns, treeNode, cm);  // constructor 3 

    case N_SetEnumerate :
      int size = (children.length-1) / 2;
      sns = new ExprNode [size];
      for ( int lvi = 0; lvi < size; lvi++ ) {
	sns[lvi] = generateExpression( children[ 2 * lvi + 1 ], cm );
      }
      return new OpApplNode(OP_se, sns, treeNode, cm);  // constructor 3 

    case N_GeneralId :
      // This is a zero-ary operator; it should show in the syntax
      // tree as an OpApp, but it does not.  Hence, an OpApplication
      // node with zero arguments must be constructed for it
      // if we get here, the GeneralID really is an OpApplication with 0
      // primary arguments, but with any number of prefix arguments

      // process the generalized identifier, complete with its
      // embedded argument lists (if any)
      genID = generateGenID(treeNode, cm);

      // if the symbol is "@" then check for errors and return an AtNode if none.
      if (genID.getCompoundIDUS() == S_at) {
	if (excStack.empty() || excSpecStack.empty()) {
	  // if either stack is empty, then @ used in improper EXCEPT context
	  errors.addError(treeNode.getLocation(),
			  "@ used outside proper scope of EXCEPT construct.");
	}
	else { 
          // So, the context for @ is proper, then construct the AtNode and return it
	  return new AtNode((OpApplNode)excStack.peek(),
			    (OpApplNode)excSpecStack.peek());
	}
      }
      else if (genID.getFullyQualifiedOp() == null || genID.getArgs() == null) {
	// If it is not an "@" symbol, it may still be an unresolved symbol
	return nullOAN;
      }
      else if (genID.getFullyQualifiedOp().getKind() == ModuleKind) {
	errors.addError(treeNode.getLocation(),
			"Module name '" + genID.getFullyQualifiedOp().getName() +
			"' used as operator.");
	return nullOAN;
      }
      else {
	// but if there are no problems then we are in a situation in
	// which return the appropriate OpApplNode an N_GenID node in
	// the syntax tree really stands for an OpApplication
	return new OpApplNode(genID.getFullyQualifiedOp(), genID.getArgs(), 
			      treeNode, cm);
      }

    case N_OpApplication:
      // for an operator with arguments
      // Note: in neither case can this be an operator passed as an argument; 
      // the opAppl argument forces the return of an Operator application
      // operators passed as arguments generate OpArg nodes, and that 
      // can happen only in certain contexts, not in every context where an 
      // expression can occur, which is the context we are in here
      return generateOpAppl(treeNode, cm);

    case N_Tuple :
      size = (children.length - 1) / 2;
      sns = new ExprNode [size];
      for ( int lvi = 0; lvi < size; lvi++ ) {
	sns[lvi] = generateExpression( children[ 2 * lvi + 1 ], cm );
      }
      return new OpApplNode(OP_tup, sns, treeNode, cm); // Constructor 3 

    case N_FcnAppl :  // Apparent function application
      // Number of arguments to the apparent func app
      int numArgs = (children.length - 2) / 2;

      // Function appl involves two semantic nodes: 1 for function,
      // and 1 for arg or args tuple.
      sns = new ExprNode[2];

      // Generate expression tree for the function itself
      sns[0] = generateExpression( children[0], cm );  

      // If the function is an OpApplNode (and could it be otherwise?)
      if (sns[0].getKind() == OpApplKind) { 
	// Note if this is a recursive function, and change the top level
	functions.recursionCheck(((OpApplNode)sns[0]).getOperator().getName());
      }

      // We next check that the number of arguments to a user-defined
      // function is correct, if possible.

      // Retrieve the expression that represents the function being
      // applied, i.e. the 1st arg to $FcnApply
      ExprOrOpArgNode fcn = sns[0];

      // The entire next conditional is for one purpose: to make sure
      // that a function symbol is applied to the right number of
      // arguments, when it is possible to do that during semantic
      // analysis.  This means that if a function is declared with,
      // say, 3 parameters, e.g. "f[a,b,c] == {a,b,c}", then it is
      // never used with 2 or 4 arguments.  However, it can appear
      // with zero arguments as the expression "f" (not as "f[]"), and
      // it can appear with one argument, as in "f[e]", because e
      // might be a 3-tuple value.  (Whether it always is or not
      // cannot be determined at the time of semantic analysis.)
      // Furthermore a function declared with exactly one parameter
      // can appear with any number of argument expressions because,
      // e.g. f[1,2,3,4] is considered just an alternate way of
      // writing f[<<1,2,3,4>>], so there is really just one argument
      // value.

      // If it is an OpApplNode (as opposed to, say, an OpDeclNode)
      if ( fcn instanceof OpApplNode ) {
	// Retrieve the function being applied
	SymbolNode funcOperator = ((OpApplNode)fcn).getOperator();

	// If the function being applied is a user-defined function
	// (as opposed to OpDeclNode, FormalParamNode, builtin
	// operator, or expression)
	if (funcOperator instanceof OpDefNode &&  
	    funcOperator.getKind() == UserDefinedOpKind) {
	  // Retrieve the function body expression
	  ExprOrOpArgNode funcBody = ((OpDefNode)funcOperator).getBody();

	  // if the function body is an OpApplNode (as opposed to,
	  // say, NumeralNode, DecimalNode, etc.)
	  if (funcBody instanceof OpApplNode && 
	      (((OpApplNode)funcBody).getOperator().getName()==OP_nrfs  ||
	       ((OpApplNode)funcBody).getOperator().getName()==OP_rfs )) {

	    // find out how many arguments it is SUPPOSED to have
	    int numParms = ((OpApplNode)funcBody ).getNumberOfBoundedBoundSymbols();
                                                  
	    // If the function appears with numArgs >= 2 argument
	    // expressions, it must be declared with exactly numArgs
	    // parameters; and a function with numParms parameters in
	    // its definition should be applied to exactly numParms
	    // expressions, or 1 expression (representing arguments in
	    // tuple form), or 0 expressions (representing the
	    // function itself).  Note: one cannot define a function
	    // with 0 arguments in TLA+.
	    if ( numArgs >= 2 && numParms != numArgs ) {
	      errors.addError(treeNode.getLocation(), 
			      "Function '" + ((OpApplNode)sns[0]).getOperator().getName() +
			      "' is defined with " + numParms +
			      " parameters, but is applied to " + numArgs + " arguments.");
	      return nullOAN;
	    } // end if
	  } // end if
	} // end if
      } // end if

      // Assert.check(numArgs > 0);
      if (numArgs == 1) {
	sns[1] = generateExpression(children[2], cm);
      }
      else {
	// If there is more than one arg we have to create a tuple for the arguments.
	ExprOrOpArgNode[] exprs = new ExprNode[ numArgs ];  // One for each of the arguments

	// For each argument...
	for (int lvi = 0; lvi < numArgs; lvi++) {
	  // Create the expression for that argument
	  exprs[lvi] = generateExpression( children[ 2+2*lvi ], cm );
	}
	// Create an application of $Tuple
	sns[1] = new OpApplNode(OP_tup, exprs, treeNode, cm);
      }
      // Create the function application node.
      return new OpApplNode(OP_fa, sns, treeNode, cm);

    case N_UnboundOrBoundChoose :
      return processChoose(treeNode, children, cm );

    case N_BoundQuant :
      return processBoundQuant(treeNode, children, cm);

    case N_UnboundQuant :
      return processUnboundQuant( treeNode, children, cm );

    case N_IfThenElse :
      sns = new ExprNode[3];
      sns[0] = generateExpression( children[1], cm );
      sns[1] = generateExpression( children[3], cm );
      sns[2] = generateExpression( children[5], cm );
      return new OpApplNode(OP_ite, sns, treeNode, cm);

    case N_Case :
      return processCase(treeNode, children, cm);

    case N_DisjList:
    case N_ConjList:
      sns = new ExprNode[ children.length ];
      for (int lvi = 0; lvi< sns.length; lvi++ ) {
	sns[lvi] = generateExpression( children[lvi].heirs()[1], cm ); 
      }
      if ( treeNode.isKind(N_DisjList ) )
	return new OpApplNode(OP_dl, sns, treeNode, cm);
      else
	return new OpApplNode(OP_cl, sns, treeNode, cm);

    case N_RecordComponent : // really RcdSelect in the API
      sns = new ExprNode[2];
      sns[0] = generateExpression( children[0], cm );
      sns[1] = new StringNode(children[2], false);
      return new OpApplNode(OP_rs, sns, treeNode, cm);

    case N_SetOfFcns:
      sns = new ExprNode[2];
      sns[0] = generateExpression( children[1], cm );
      sns[1] = generateExpression( children[3], cm );

      return new OpApplNode(OP_sof, sns, treeNode, cm);

    case N_SubsetOf :
      return processSubsetOf( treeNode, children, cm );

    case N_SetOfAll :
      return processSetOfAll( treeNode, children, cm );

    case N_RcdConstructor:
      return processRcdForms( OP_rc, treeNode, children, cm );

    case N_SetOfRcds:
      return processRcdForms( OP_sor, treeNode, children, cm );

    case N_FcnConst:
      return processFcnConst( treeNode, children, cm );

    case N_ActionExpr:
    case N_FairnessExpr:
      return processAction( treeNode, children, cm );

    case N_Except:
      return processExcept( treeNode, children, cm );

    case N_LetIn:
      return processLetIn( treeNode, children, cm );

    default:
      errors.addError(treeNode.getLocation(),
		      "Unsupported expression type " + treeNode.getImage());
      return null;

    } // end switch

  } // end generateExpression()

  private final ExprNode processChoose(TreeNode treeNode, TreeNode[] children, ModuleNode cm) 
  throws AbortException {
    ExprNode[]   semanticNode   = new ExprNode[1];
    TreeNode[]   syntaxTreeNode = children[2].heirs();
    OpApplNode   result;

    symbolTable.pushContext( new Context(moduleTable, errors) );

    if (syntaxTreeNode == null || syntaxTreeNode.length == 0) {
      // unbounded case
      OpDeclNode[] odn;
      boolean      tuple;

      // either Tuple or single identifier
      if (children[1].isKind( N_IdentifierTuple)) {
        syntaxTreeNode = children[1].heirs();
        odn = new OpDeclNode[ syntaxTreeNode.length / 2 ];
        for (int lvj = 0; lvj < syntaxTreeNode.length / 2; lvj++ ) {
          odn[lvj] = new OpDeclNode(syntaxTreeNode[ 2*lvj+1 ].getUS(),
				    BoundSymbolKind, 0, 0, cm, symbolTable,
				    syntaxTreeNode[ 2*lvj+1 ]);
        }
        tuple = true;
      }
      else {
        odn = new OpDeclNode[1];
        odn[0] = new OpDeclNode(children[1].getUS(), BoundSymbolKind, 0, 0, 
                                cm, symbolTable, children[0]);
        tuple = false;
      } 
      semanticNode[0] = generateExpression( children[4], cm );
      result =  new OpApplNode(OP_uc, semanticNode, odn, tuple,
                               treeNode, cm);
    }
    else {
      // bounded case
      OpDeclNode[][] odna   = new OpDeclNode[1][0];
      boolean[]      tuples = new boolean[1];
      ExprNode[]     exprs  = new ExprNode[1]; 

      // syntaxTreeNode can be reused further down.
      exprs[0] = generateExpression(syntaxTreeNode[1], cm);

      if ( children[1].isKind( N_IdentifierTuple ) ) {
        syntaxTreeNode = children[1].heirs();
        odna[0] = new OpDeclNode[ syntaxTreeNode.length / 2 ];
        for (int lvj = 0; lvj < syntaxTreeNode.length / 2; lvj++ ) {
          odna[0][lvj] = 
	    new OpDeclNode(syntaxTreeNode[ 2*lvj+1 ].getUS(), BoundSymbolKind, 0,
			   0, cm, symbolTable, syntaxTreeNode[2*lvj+1]);
        }
        tuples[0] = true;
      }
      else {
        odna[0] = new OpDeclNode[1];
        odna[0][0] = new OpDeclNode(children[1].getUS(), BoundSymbolKind, 0,
                                    0, cm, symbolTable, children[1]);
        tuples[0] = false;
      } 
      semanticNode[0] = generateExpression( children[4], cm );
      result = new OpApplNode(OP_bc, null, semanticNode, odna, 
                              tuples, exprs, treeNode, cm);
    }
    symbolTable.popContext();
    return result;
  }

  private final ExprNode processBoundQuant(TreeNode treeNode, TreeNode[] children,
					   ModuleNode cm) 
  throws AbortException {
    // Create data structures for all parameters
    int            length = (children.length - 2) / 2;
    OpDeclNode[][] odna   = new OpDeclNode[length][0];
    boolean[]      bt     = new boolean[length];
    ExprNode[]     ea     = new ExprNode[ length ];

    // then process parameters
    symbolTable.pushContext( new Context(moduleTable, errors) );
    processQuantBoundArgs( children, 1, odna, bt, ea, cm );

    // process expression
    ExprNode semanticNode[] = new ExprNode[1];
    semanticNode[0] = generateExpression( children[ children.length - 1 ], cm );
    symbolTable.popContext();

    // then return new node.
    // which variety? look under first child.
    boolean isExists = children[0].getUS().equals( S_e );
    if (isExists) {
      return new OpApplNode(OP_be, null, semanticNode, odna, 
                            bt, ea, treeNode, cm);
    }
    else {
      return new OpApplNode(OP_bf, null, semanticNode, odna, 
                            bt, ea, treeNode, cm);
    }
  }

  private final ExprNode processUnboundQuant(TreeNode treeNode, TreeNode[] children,
					     ModuleNode cm)
  throws AbortException  {
   // which variety? look under first child.
   UniqueString us = children[0].getUS();
   UniqueString r_us;
   int level;

   if      ( us.equals (S_e ) ) { r_us = OP_ue; level = 0; } // \E
   else if ( us.equals (S_f ) ) { r_us = OP_uf; level = 0; } // \A
   else if ( us.equals (S_te) ) { r_us = OP_te; level = 1; } // \EE
   else                         { r_us = OP_tf; level = 1; } // \AA

   // Process all identifiers bound by thus quantifier
   int length = ( children.length - 2 ) / 2;
   OpDeclNode odn[] = new OpDeclNode[ length ];
   symbolTable.pushContext( new Context(moduleTable, errors) );

   for ( int lvi = 0; lvi < length; lvi ++ ) {
     odn[lvi] = new OpDeclNode(children[2*lvi +1].getUS(), BoundSymbolKind, level,
                               0, cm, symbolTable, children[2*lvi +1]);
   }

   // now the expression
   ExprNode semanticNode[] = new ExprNode[1];
   semanticNode[0] = generateExpression(children[children.length-1], cm);

   // wrap up.
   symbolTable.popContext();
   return new OpApplNode(r_us, semanticNode, odn, false, treeNode, cm);
  }

  private final ExprNode processCase(TreeNode treeNode, TreeNode[] children, ModuleNode cm) 
  throws AbortException {
    // number of arms to CASE-expr, not counting the CASE nodse itself
    // or the []-separators
    int armCount = children.length/2;          
    ExprNode[] casePairs = new ExprNode[armCount];
    
    for (int lvi = 0; lvi < armCount; lvi++) {
      TreeNode caseArm = children[2*lvi+1];
      TreeNode[] ss = caseArm.heirs();
      ExprNode[] sops = new ExprNode[2];
      if (!caseArm.isKind(N_OtherArm)) {
	sops[0] = this.generateExpression(ss[0], cm);
      }
      sops[1] = this.generateExpression(ss[2], cm);
      casePairs[lvi] = new OpApplNode(OP_pair, sops, caseArm, cm);
    }
    return new OpApplNode(OP_case, casePairs, treeNode, cm);
  }

  private final ExprNode processSubsetOf(TreeNode treeNode, TreeNode children[],
					 ModuleNode cm ) 
  throws AbortException { 
    // cfr. unbounded choose
    ExprNode[]     ops    = new ExprNode[1];
    OpDeclNode[][] odna   = new OpDeclNode[1][0];
    boolean[]      tuples = new boolean[1];
    ExprNode[]     exprs  = new ExprNode[1];

    exprs[0] = generateExpression( children[3], cm  );

    symbolTable.pushContext( new Context(moduleTable, errors) );

    if ( children[1].isKind( N_IdentifierTuple ) ) {
      TreeNode[] ss = children[1].heirs();
      odna[0] = new OpDeclNode[ ss.length / 2 ];
      for (int lvj = 0; lvj < ss.length / 2; lvj++ ) {
        odna[0][lvj] = new OpDeclNode(ss[ 2*lvj+1 ].getUS(), BoundSymbolKind, 0, 0, 
                                      cm, symbolTable, ss[ 2*lvj+1 ]);
      }
      tuples[0] = true;
    }
    else {
      odna[0] = new OpDeclNode[1];
      odna[0][0] = new OpDeclNode(children[1].getUS(), BoundSymbolKind, 0, 0, 
                                  cm, symbolTable, children[1]);
      tuples[0] = false;
    }

    ops[0] = generateExpression( children[5], cm  );
    symbolTable.popContext();
    return new OpApplNode(OP_sso, null, ops, odna, tuples, exprs, 
                          treeNode, cm);
  }

  private final ExprNode processSetOfAll(TreeNode treeNode,TreeNode children[],
					 ModuleNode cm) 
  throws AbortException {
    ExprNode[]     ops    = new ExprNode[1];
    int            length = (children.length - 3) / 2;
    OpDeclNode[][] odna   = new OpDeclNode[length][0];
    boolean[]      tuples = new boolean[length];
    ExprNode[]     exprs  = new ExprNode[length];

    symbolTable.pushContext(new Context(moduleTable, errors));
    processQuantBoundArgs(children, 3, odna, tuples, exprs, cm);

    ops[0] = generateExpression( children[1], cm  );
    symbolTable.popContext();

    return new OpApplNode(OP_soa, null, ops, odna, tuples, exprs,
			  treeNode, cm);
  }

  private final ExprNode processFcnConst(TreeNode treeNode,TreeNode children[], ModuleNode cm) 
  throws AbortException { 
    ExprNode[]     ops    = new ExprNode[1];
    int            length = (children.length - 3) / 2;    // number of args to function constant
    OpDeclNode[][] odna   = new OpDeclNode[length][0];
    boolean[]      tuples = new boolean[length];
    ExprNode[]     exprs  = new ExprNode[length];

    symbolTable.pushContext( new Context(moduleTable, errors) );
    processQuantBoundArgs( children, 1, odna, tuples, exprs, cm );

    ops[0] = generateExpression( children[children.length-2], cm  );
    symbolTable.popContext();

    return new OpApplNode(OP_fc, null, ops, odna, tuples, exprs,
			  treeNode, cm);
  }

  /**
   * This method processes both the RecordConstructor construct and the
   * SetOfRecords operator.  The two are essentially identical except for 
   * which builtin operator is used.
   */
  private final ExprNode processRcdForms(UniqueString operator,
					 TreeNode treeNode,
					 TreeNode children[],
					 ModuleNode cm) 
  throws AbortException {
    // handles RcdConstructor or SetOfRcds 
    int length = (children.length - 1) / 2;

    // Create an array of pairs to handle all of the fields mentioned in the form
    ExprNode[] fieldPairs = new ExprNode[length];

    // For each field in the RcdConstructor or SetOfRecords
    for ( int lvi = 0; lvi < length; lvi++ ) {
      TreeNode syntaxTreeNode[] = children[ 2*lvi + 1].heirs();

      // Create a pair of SemanticNodes to represent one record component
      ExprNode sops[] = new ExprNode[2];

      // The first one gets a new StringNode indicating the field name
      sops[0] = new StringNode(syntaxTreeNode[0], false);

      // The second one gets the expression indicating the field value (or set of values)
      sops[1] = generateExpression( syntaxTreeNode[2], cm  );

      // Put the $Pair OpApplNode into the fieldPairs array
      fieldPairs[lvi] = new OpApplNode(OP_pair, sops, children[2*lvi+1], cm);
    }
    // Create the top-level OpApplNode, for either the SetOfRecords op
    // or the RcdConstructor op.
    return new OpApplNode(operator, fieldPairs, treeNode, cm);
  }

  private final ExprNode processAction(TreeNode treeNode, TreeNode children[], ModuleNode cm) 
  throws AbortException {
    UniqueString match = children[0].getUS();
    if      ( match.equals( S_a ) )
      match = OP_aa;
    else if ( match.equals( S_brack ) )
      match = OP_sa;
    else if ( match.equals( S_sf) )
      match = OP_sf;
    else if ( match.equals( S_wf) )
      match = OP_wf;

    ExprNode ops[] = new ExprNode[2];
    ops[0] = generateExpression( children[1], cm  );
    ops[1] = generateExpression( children[3], cm  );
    return new OpApplNode( match, ops, treeNode, cm);
  }

  private final ExprNode processExcept(TreeNode treeNode, TreeNode[] children, ModuleNode cm) 
  throws AbortException {
    int              numExcepts = (children.length-3)/2 ;     // number of ExceptSpec's;
    ExprNode[]       operands   = new ExprNode[numExcepts+1]; // 1 for each ExceptionSpec +  first expr
    OpApplNode       excNode;                                 // Holds OpApplNode for $Except operator
    OpApplNode       excSpecNode;                             // Holds $Pair node for ExceptSpec

    // The first operand of the $Except operator is the expression to
    // which the exceptions apply Note this first operand is generated
    // BEFORE the $Except node is stacked in the next couple of lines,
    // because an @ in the first expression does NOT refer to the
    // $Except node currently being generated, but to the next outer
    // $Except node.
    operands[0] = generateExpression( children[ 1 ], cm  );  

    // Create the $Except OpApplNode that will be returned by this
    // method.  We create it now, and fill out its contents later,
    // because we need a reference to it in order to process @ properly.
    excNode = new OpApplNode( OP_exc, operands, treeNode, cm);

    // for each of the ExceptSpecs produce another element of the operands array
    for ( int excSpecIx = 0; excSpecIx < numExcepts; excSpecIx++ ) {
      TreeNode[] syntaxTreeNode = children[3 + 2*excSpecIx].heirs();  // extract ExceptSpec 
      ExprNode[] sops           = new ExprNode[2];                    // Each ExceptionSpec is a $Pair
      int        slength        = syntaxTreeNode.length - 3;          // # of ExceptComponents in ExceptSpec
      ExprNode[] ssops          = new ExprNode[ slength ];            // to store ExceptComponents

      // Process the LHS of the ExceptSpec

      // for each ExceptComponent of the form .h or [i] or [i,j,k] add
      // an arg to $SEQ node and add build up the syntax tree for
      // exceptionTarget
      for ( int excCompIx = 0; excCompIx < slength ; excCompIx++ ) {
	// the heirs of an ExceptComponent
        TreeNode subSyntaxTreeNode[] = syntaxTreeNode[ 1 + excCompIx ].heirs();

	if (subSyntaxTreeNode[0].getUS().equals( S_brack ) ) {
	  // The first heir is "[" , indicates one or more fcn args;
	  // add expressions as function args
          if ( subSyntaxTreeNode.length > 3 ) {
	    // if so, must generate a tuple for comma list of fcn args
	    int        sslength = (subSyntaxTreeNode.length-1)/2; // number of func args in comma list
            ExprNode[] sssops   = new ExprNode[ sslength ];       // holds the comma list of fcn args

            // for each of multiple function args in the ExceptComponent
            for (int fArgIx=0; fArgIx < sslength; fArgIx++ ) {
              sssops[ fArgIx ] = generateExpression( subSyntaxTreeNode[1+ 2*fArgIx], cm);
            }

            // add one ExceptComponent to vector of ExceptComponents
            ssops[ excCompIx ] = 
	      new OpApplNode(OP_tup, sssops, syntaxTreeNode[2*excCompIx+1], cm);
          }
	  else {
            // add one ExceptComponent to vector of ExceptComponents
            ssops[ excCompIx ] = generateExpression( subSyntaxTreeNode[1], cm );
          }
        }
	else {
	  // otherwise a "." indicates record selection; add a
          // StringNode operand as record selector add one
          // ExceptComponent to vector of ExceptComponents
          ssops[ excCompIx ] = new StringNode(subSyntaxTreeNode[1], false);
        }
      } // end for (each ExceptionComponent)

      // Create OpAppl for $SEQ applied to array of ExceptComponents
      // following record or func expr or !
      // This is the LHS of an ExceptionSpec
      sops[0] = new OpApplNode(OP_seq, ssops, children[3 + 2*excSpecIx], cm);

      // Process the RHS of the ExceptionSpec

      // Create exceptSpec node now, so that it can be available in
      // case the RHS expression of this ExceptSpec contains an @
      excSpecNode = new OpApplNode(OP_pair, sops, children[3+2*excSpecIx], cm);

      // Push the except node and the except spec node on stacks so
      // that the RHS of the ExceptSpec, which might contain an @, can
      // be evaluated in their "context".
      excSpecStack.push(excSpecNode);
      excStack.push(excNode);

      // Generate the expression constituting the RHS of the
      // ExceptionSpec allow @ in the context of this expression.
      sops[1] = generateExpression(syntaxTreeNode[syntaxTreeNode.length-1], cm );

      // Pop them back off
      excSpecStack.pop();
      excStack.pop();

      // Store excSpecNode as another operand of $Except
      operands[ excSpecIx+1 ] = excSpecNode;
    } // end for (each ExceptionSpec)

    return excNode;
  } // end processExcept()

  /**
   * This method generates an expression tree or an OpArgNode as an
   * argument to an OpApplNode
   * 
   *    mainOp      is the operator under that node
   *    mainSTN     is the "parent" OpApplNode, used in case an error message must be generated
   *    argPosition is the argument position (counting from 0) that treeNode represents
   *    argRoot     is the argument syntax tree that either becomes an ExprNode or an OpArgNode
   *    mn          is the module these expressions are part of
   */
  private ExprOrOpArgNode generateExprOrOpArg(SymbolNode mainOp, 
					      TreeNode   mainSTN, 
					      int        argPosition,
					      TreeNode   argRoot, 
					      ModuleNode mn) 
  throws AbortException {
    SymbolNode argOp  = null;      // the SymbolNode that heads the argRoot expression
    int        argArity;           // number of actual arguments under argRoot
    int        arityExpected;      // arity that mainOp expects of argument number <argPosition>

    if ( mainOp == null) {
      errors.addError(mainSTN.getLocation(),
                      "Unable to generate expression or operator argument; " + 
                      "this is probably because of previously reported errors." );
      return nullOAN;
    }

    // Are we sure the "operator" is not a ModuleNode?
    if (mainOp instanceof ModuleNode) {
      errors.addError(mainSTN.getLocation(), "Module name '" + mainOp.getName() + "' used as operator.");
      return nullOAN;
    }

    // Are there too many arguments to mainOp?
    if (argPosition+1 > mainOp.getArity()) {
      errors.addError(mainSTN.getLocation(), "Too many arguments for operator '" +
		      mainOp.getName() + "'.  There should be only " + mainOp.getArity() + "." ); 

      return nullOAN;
    }

    // For user-defined mainOp check the FormalParamNodes array
    // associated with the mainOp to find out whether it expects an
    // operator argument or an ordinary expression argument in
    // position number argPosition.  (Only UserDefined ops can have
    // other multi-arg ops passed to them as params, so we can just
    // assign arityExpected = 0 in other cases.)
    if ( mainOp.getKind() == UserDefinedOpKind ) {
      arityExpected = ((OpDefNode)mainOp).getParams()[argPosition].getArity();
    } else {
      arityExpected = 0;
    }

    // if mainOp expects zero arguments, then it expects an ordinary
    // expression arg in this position, so generate an expression.
    // Any errors will be found in the generateExpression method.
    if ( arityExpected == 0 ) {
      return generateExpression( argRoot, mn );
    }
    else {
      // otherwise, we are expecting an OpArg
      if (argRoot.getImage().equals("N_OpApplication")) {
        errors.addError(argRoot.getLocation(),
                        "An expression appears as argument number " + (argPosition+1) +
                        " (counting from 1) to operator '" + mainOp.getName() +
                        "', in a position an operator is required.");
        return nullOAN;
      }
      
      GenID genID = generateGenID(argRoot, mn);
      argOp = genID.getFullyQualifiedOp();

      // If the symbol has not been defined, then indicate an error and
      // return a nullOAN, allowing semantic analysis to continue
      if (argOp == null) return nullOAN;

      // retrieve the arity of argOp
      argArity = argOp.getArity();
      if ( arityExpected == argArity && genID.getArgs().length == 0) {
        return new OpArgNode(genID.getFullyQualifiedOp(), argRoot, mn);
      }
      else if (genID.getArgs().length > 0) {
        // expression (with or without correct number of args) being used where operator should be	
        errors.addError(mainSTN.getLocation(), "Expression used in argument position " +
                        (argPosition+1) + " (counting from 1) of operator `" + mainOp.getName() +
                        "', whereas an operator of arity " + arityExpected + " is required.");
        return nullOpArg;
      }
      else {
	// operator of the wrong arity is being passed as argument
        errors.addError(mainSTN.getLocation(),
			"Operator with incorrect arity passed as argument. " + 
                        "Operator '" + argOp.getName() + "' of arity " + argArity + 
                        " is argument number " + (argPosition+1) + 
                        " (counting from 1) to operator `" + mainOp + 
                        "', but an operator of arity " + arityExpected + 
                        " was expected.");
        return nullOpArg;
      }
    }
  } // end generateExprOrOpArg

  /**
   *  Process the General ID syntax tree, i.e. the part that contains
   *  an expression of the form A(x,y)!B!C(u,v,w)!D (with no params to
   *  D) and represents an operator.  A General ID occurs in the
   *  syntax in only a few places: as an operator in an operator
   *  application; as an operator used as an operand to another
   *  operator, and as an operator being substituted for a suitable
   *  constant in module instantiation.
   *
   *  Returns a GenID object, which must be further processed.
   */
  private GenID generateGenID(TreeNode syntaxTreeNode, ModuleNode mn) throws AbortException {
    return generateGenID(syntaxTreeNode, mn, false);
  }

  private GenID generateGenID(TreeNode syntaxTreeNode, ModuleNode mn, boolean unaryNegKludge)
  throws AbortException {
    GenID      genID;             // Holds components of the generalized ID for this operator
    TreeNode[] children = syntaxTreeNode.heirs(); // To contain N_IdPrefix node and the main operator
    TreeNode[] prefix = null;     // Contains array of prefix elements
                                  // This is the array of N_IdPrefixElements for the operator, 
                                  // i.e. A!B(3)!C has 2 N_IdPrefixElements, A and B(3).
    TreeNode[] prefixElt;         // a prefixElement; 2- or 3-elem array: [op, (args), "!"]
    TreeNode[] allArgs  = null;   // to collect arg arrays from prefix
    TreeNode[] argsList = null;   // Will hold an arg list tree

    if (children == null || children.length <= 0) {
      // almost certainly an @ used outside of EXCEPT, which is detected elsewhere
      return null;
    }

    prefix = children[0].heirs();

    // Allocate object to hold the Generized ID that is part of this OpAppl
    genID = new GenID(syntaxTreeNode);

    // Number of elements in the prefix
    int len = prefix.length;

    // Allocate array of SyntaxTreeNodes, one for each prefix element
    allArgs = new SyntaxTreeNode[ len ]; 

    // Process all of the prefix elements; construct the compound
    // identifier (with embedded !-characters), and also accumulate an
    // array of argument arrays (allArgs) for each prefix element
    for (int i = 0; i < len; i++ ) {
      // prefixElt becomes a 2- or 3-element array containing
      // [operator, args (optional), and "!"]
      prefixElt = prefix[i].heirs();    
        
      // Append the next part of compound identifier name and a "!"
      genID.append(prefixElt[0].getImage());
      genID.append("!");

      // allArgs[i] = array of arg syntax trees for next prefix
      // element (if any) or "!" (if not)
      allArgs[i] = prefixElt[1];          // Note: whether this is args or a "!" is checked below
    }

    // Append the primary (rightmost) operator name to compoundID;
    // calling "finalAppend" signals that the appending is finished,
    // i.e. that the string can be converted to a UniqueString and to
    // a SymbolNode inside the genID object
    genID.finalAppend(children[1].getImage(), unaryNegKludge);

    // for each argument in each potential argument list in the prefix, 
    // generate the expression or opArg corresponding to it

    // for each argument list in prefix
    int iarg = 0;
    for (int i = 0; i < allArgs.length ; i++ ) {
      // if there is an actual arg list here (instead of a "!" or null)
      if ( allArgs[i] != null && allArgs[i].isKind( N_OpArgs ) ) {
        // pick up array of arg list syntax elements
        argsList = allArgs[ i ].heirs();

        // The odd numbered syntax elements are the args expressions;
        // the even numbered ones are parens and commas.
        for (int ia = 1; ia < argsList.length; ia += 2) {
          // Each arg may be an ordinary expression, or it may be an OpArg; 
          //   produce appropriate semantic tree or node for it.
          // Note that operators can be used in place of expressions in only two contexts:
          //   as argument to suitable user-defined ops, and in the RHS of a substitution 
          //   in module instantiation
          genID.addArg(generateExprOrOpArg(genID.getFullyQualifiedOp(), syntaxTreeNode,
					   iarg, argsList[ia], mn));
          iarg++;
        }
      }
    }

    // "finalize the GenID object (i.e. convert argument vector to array)
    genID.finalizeID();
    return genID;
  }

  /**
   * Generates an OpApplNode or an OpArgNode for a SyntaxTreeNode,
   * according to whether the value of "typeExpected" is either opAppl
   * or opArg.
   */
  private final ExprNode generateOpAppl(TreeNode syntaxTreeNode, ModuleNode cm)
  throws AbortException {
    TreeNode     primaryArgs = null;     // points to syntax tree of primary arg list (if any); otherwise null
    boolean      isOpApp     = syntaxTreeNode.isKind( N_OpApplication ) ;
                                         // true ==> to indicate this is an OpAppl; must have primary args
                                         // false ==> operator used as an argument (OpArg); has no primary args
    int          primaryArgCount = 0;    // total # of arguments, including those of prefix, if any
    int          len;                    // number of prefix elements
    TreeNode[]   children    = syntaxTreeNode.heirs(); // temp used for finding the prefix
    TreeNode []  prefix;                 // array of prefix elements
    TreeNode[]   allArgs     = null;     // to collect arg arrays from both prefix and the main op (if any)
    TreeNode []  prefixElt;              // a prefixElement; 2 or 3 elem array: [op, (args), "!"]
    UniqueString symbol;                 // UniqueString name of the fully-qualified operator
    SymbolNode   fullOperator;           // The SymbolNode for the fully-qualified operator
    int          iarg        = 0;        // loop counter for actual args (as opposed to 
                                         //   arg syntax elements like commas and parens)
    TreeNode[]   argsList    = null;     // Will hold an array of arg syntax trees for primary operator
    ExprOrOpArgNode[] args   = null;     // Will hold an array of arg semantic trees for primary operator

    // Process the Generized ID that is the operator for this OpAppl
    GenID genID = generateGenID(children[0], cm);

    // Set up pointers to OpAppl's primary args
    primaryArgs = children[1];  // Array of argument list syntax elements for the main
                                // (rightmost) operator, including parens and commas;
                                //   should be an N_OpArgs node

    // calc number of primary args; args are interspersed w/ parens & commas--hence the /2
    primaryArgCount = primaryArgs.heirs().length / 2; 

    if (genID == null || genID.getFullyQualifiedOp() == null) {
      // if operator is @ or an unresolved symbol; error has already
      // been generated inside genID
      return nullOAN;
    }

    args = new ExprOrOpArgNode[primaryArgCount]; // Array to hold semantic trees for primary args

    // pick up array of arg list syntax elements
    argsList = primaryArgs.heirs();

    // The odd numbered syntax elements are the args expressions; the
    // even numbered ones are parens and commas.
    // for each arg in this arg list ...
    for ( int ia = 1; ia < argsList.length; ia += 2 ) {
      // Each arg may be an ordinary expression, or it may be an OpArg; 
      //   produce appropriate semantic tree or node for it.
      // Note that operators can be used in place of expressions in only two contexts:
      //   as argument to suitable user-defined ops, and in the RHS of a substitution 
      //   in module instantiation
      args[iarg] = generateExprOrOpArg(genID.getFullyQualifiedOp(), syntaxTreeNode,
				       iarg, argsList[ia], cm);
      iarg++;   // count the actual args
    } // end for

    // Concatenate the list of args in the GenID object to the primary arg list just 
    // created
    Vector            genIDArgList = genID.getArgsVector();
    ExprOrOpArgNode[] finalArgList = new ExprOrOpArgNode[genIDArgList.size() + iarg];

    // Copy the args from the prefix
    for (int i = 0; i < genIDArgList.size(); i++) {
      finalArgList[i] = (ExprOrOpArgNode)(genIDArgList.elementAt(i));
    }
    // Copy the primary args
    for (int i = 0, j = genIDArgList.size(); i < iarg; i++, j++ ) {
      finalArgList[j] = args[i];
    }

    // return an OpApplNode constructed from the fully-qualified
    // operator and the final arg list
    return new OpApplNode(genID.getFullyQualifiedOp(), finalArgList, 
			  syntaxTreeNode, cm);
  } // end generateOpAppl()

  /**
   * Process a named, parameterixed instantiation, e.g. of the form
   * D(p1,...pn) = INSTANCE M WITH a1 <- e1 ,..., ar <- er
   */
  private final OpDefNode processModuleDefinition(TreeNode treeNode,
						  Vector defs,
						  Vector insts,
                                                  ModuleNode cm)
  throws AbortException {
    // Start with a LHS for an instance: we must extract from it name
    // and possibly parameters. Then we need the external Context of
    // the module and to extract all non-local, non-builtin
    // symbols. Then build the proper symbol list and add it.
  
    // We must remember to identify explicitly whether or not the new
    // nodes would be local.
  
    // assert treeNode.isKind(N_ModuleDefinition)    
    // 
    // Note that this code does nothing about THEOREMS and ASSUMES in
    // modules being instantiated
    boolean      localness = treeNode.zero() != null;
    TreeNode[]   children  = treeNode.one()[0].heirs(); // heirs of IdentLHS
    UniqueString name      = children[0].getUS();

    // processing of LHS of the definition, i.e. the name and parameters
    FormalParamNode[] args = nullParam;
    Context parmCtxt = null;

    // If the operator being defined as a module instance has any parameters
    if (children.length > 1) {
      // Create new array of FormalParamNodes for the new operator
      args = new FormalParamNode[ children.length /2 -1 ];

      // Push a new context in current module's SymbolTable
      parmCtxt = new Context(moduleTable, errors);
      symbolTable.pushContext(parmCtxt);

      // For each formal parameter declared for the op being defined
      for (int i = 0; i < args.length; i++) {
        TreeNode     child = children[2 + 2 * i];
        UniqueString id    = null;
        int          count = 0;
  
        if ( child.isKind(N_IdentDecl)) {
          id = child.heirs()[0].getUS();
          count = (child.heirs().length -1)/ 2;
        }
	else if ( child.isKind(N_InfixDecl)) {
          id = child.heirs()[1].getUS();
          count = 2;
        }
	else if ( child.isKind(N_PrefixDecl)) {
          id = child.heirs()[0].getUS();
          count = 1;
        }
	else if ( child.isKind(N_PostfixDecl)) {
          id = child.heirs()[1].getUS();
          count = 1;
        }
	else {
          errors.addAbort(treeNode.getLocation(),
			  "Internal error: Error in formal params part of parse tree.",
			  true);
	}
 
        // If there was no error
        if ( id != null ) {
          // Create a new FormalParamNode for the defined Op and put 
          // it in the SymbolTable
          args[i] = new FormalParamNode(id, count, child, symbolTable, cm);
        } // end if
      } // end for
    } // end if

    // processing RHS of the definition, starting with identification
    // of module being instantiated, followed by processing of the
    // WITH clause (if any)
    children = treeNode.one()[2].heirs(); // heirs of NonLocalInstance

    // Find the Context and ModuleNode for the module being instantiated
    Context instanceeCtxt = this.getContext(children[1].getUS());
    ModuleNode instanceeModule = symbolTable.resolveModule(children[1].getUS());

    if (instanceeCtxt == null) {
      errors.addError(children[1].getLocation(),
		      "Module " + children[1].getImage() + " does not have a context.");
      return nullODN;
    }

    if (instanceeModule == null) {
      errors.addError(children[1].getLocation(),
		      "Module name " + children[1].getImage() + " is not known" +
		      " in current context.");
      return nullODN;
    }

    // Create a SubstInNode that will be used to wrap each definition
    // body in the module being defined. "children" is the array of
    // explicit substitution clauses used in the module definition.
    // Both instanceeCtxt and symbolTable are involved here since for
    // each substitution c <- e, c must be resolved in the
    // instanceeCtxt, and e must be interpreted in symbolTable
    SubstInNode substIn = processSubst(treeNode, children, symbolTable,
				       instanceeCtxt, instanceeModule, cm);

    // We are done with the local context (if one was created because
    // of parameters)
    if (parmCtxt != null) symbolTable.popContext();
  
    // Create a vector of all of the OpDefNodes in the instancee module
    Vector elts = instanceeCtxt.getByClass( OpDefNode.class );

    // For each definition in the instancee module, create a 
    // corresponding definition in the instancer module
    for (int i = 0; i < elts.size(); i++) {
      // Find the OpDefNode to be instantiated
      OpDefNode odn = (OpDefNode)elts.elementAt(i);

      // Ignore it if it is local or builtin defs
      if (!odn.isLocal() && (odn.getKind() == UserDefinedOpKind)) { 
        // Create the new name prepended with "name!"
        String compoundID = name + "!" + odn.getName();
        UniqueString qualifiedName = UniqueString.uniqueStringOf(compoundID);

        // Copy parameters for the op being defined
        FormalParamNode [] fpn    = odn.getParams();
        FormalParamNode [] params = new FormalParamNode[fpn.length + args.length];
        System.arraycopy(args, 0, params, 0, args.length);
        System.arraycopy(fpn, 0, params, args.length, fpn.length);

        OpDefNode newOdn;
        if (substIn.getSubsts().length > 0) {
	  // If there are substitutions, then the body of the new
	  // definition instance must be wrapped in a SUBST-IN node.
          // Create the "wrapping" SubstInNode as a clone of "subst"
          // above, but with a body from the OpDefNode in the module
          // being instantiated
          SubstInNode substInNode = 
	    new SubstInNode(treeNode, substIn.getSubsts(), odn.getBody(),
			    cm, instanceeModule);

          // Create the OpDefNode for the new instance of this
          // definition; because of the new operator name, cm is the
          // module of origin for purposes of deciding of two defs are
          // "the same" or "different"
          newOdn = new OpDefNode(qualifiedName, UserDefinedOpKind, params, 
				 localness, substInNode, cm, symbolTable, 
				 treeNode);   
        }
	else {
	  // no SUBST-IN node required; but because of the new
	  // operator name, cm is the module of origin for purposes of
	  // deciding of two defs are "the same" or "different"
          newOdn = new OpDefNode(qualifiedName, UserDefinedOpKind, params, 
				 localness, odn.getBody(), cm, symbolTable, 
				 treeNode);   
        }
  
        // defs is non-null iff this module definition is in the Let
        // part of a Let-In expression.  Add this newly created OpDef
        // to either the LET list or the module cm's definition list.
	if (defs == null) {
	  cm.appendDef(newOdn);
	}
	else {
	  defs.addElement(newOdn);
	}
      }
    }

    // Create a new InstanceNode to represent this INSTANCE stmt in the current module
    InstanceNode inst = new InstanceNode(name, args, instanceeModule,
					 substIn.getSubsts(), treeNode);

    // Append this new InstanceNode to the vector of InstanceNodes
    // being accumulated for this module
    if (insts == null) {
      cm.appendInstance(inst);
    }
    else {
      insts.addElement(inst);
    }

    // Create new OpDefNode with ModuleInstanceKind.  The reason for
    // doing this is to get the name in the symbol table so the name
    // cannot be re-used later in this module for a user-defined
    // operator.
    return new OpDefNode(name, args, localness, cm, symbolTable, treeNode);
  } // end processModuleDefinition()
  
  /**
   * From a particular explicit substitution (substTarget <- substValue)
   * check the legality of the substTarget, and if OK, generate the
   * appropriate type of node for substValue
   */
  private ExprOrOpArgNode generateSubst(Context instanceeCtxt, TreeNode substTarget,
                                        TreeNode substValue, ModuleNode mn) 
  throws AbortException {
    SymbolNode targetSymbol = instanceeCtxt.getSymbol(substTarget.getUS());

    // if the targetSymbol cannot be found in the instancee context, or if it
    // does not correspond to a declaration, then it is an illegal substitution target

    if (targetSymbol == null || ! (targetSymbol instanceof OpDeclNode) ) {
      errors.addError(substTarget.getLocation(),
		      "Identifier '" + substTarget.getUS() + "' is not a legal" +
		      " target of a substitution. A legal target must be a declared" +
		      " CONSTANT or VARIABLE in the module being instantiated." +
		      " (Also, check for warnings about multiple declarations of" +
		      " this same identifier.)");
      return nullOAN;
    }
    
    // but if the symbol is found, then if it has arity 0, the RHS should be an expression, 
    //whereas if arity > 0, the the RHS should be an OpArg
    ExprOrOpArgNode returnObject;

    if ( targetSymbol.getArity() == 0 ) {
      // if the target of the substitution has arity 0,
      // then we expect an expression to be substituted for it
      returnObject = generateExpression(substValue, mn);
    }
    else { 
      // if the target of the substitution has arity > 0,
      // then and operator must be substituted for it
      returnObject = generateOpArg(targetSymbol, substValue, mn);

      // and it better have the same arity as the target
      if ( ((OpArgNode)returnObject).getArity() != targetSymbol.getArity() ) {
        errors.addError(substValue.getLocation(),
			"An operator must be substituted for symbol '" +
			targetSymbol.getName() + "', and it must have arity " +
			targetSymbol.getArity() + ".");
      }
    }
    return returnObject;
  } // end generateSubst()

  /**
   * Return an OpArgNode constructed from a GeneralId tree to be used
   * in the RHS of a substitution
   */
  private OpArgNode generateOpArg(SymbolNode targetSymbol, TreeNode opArgSyntaxNode,
				  ModuleNode mn) 
  throws AbortException {
    // First, make sure that an operator ID is present, and not an expression
    if ( ! ( opArgSyntaxNode.isKind(N_GeneralId)   ||
             opArgSyntaxNode.isKind(N_GenInfixOp)  ||
             opArgSyntaxNode.isKind(N_GenPrefixOp) ||
             opArgSyntaxNode.isKind(N_GenPostfixOp)
           )
       ) {
      errors.addError(opArgSyntaxNode.getLocation(), "Arity " + targetSymbol.getArity() + 
                      " operator (not an expression) is expected" + 
                      " to substitute for CONSTANT '" + targetSymbol.getName() + "'." );
      return nullOpArg;
    }

    // Assemble the (possibly compound) generalized identifier, and resolve it.
    GenID genID = generateGenID(opArgSyntaxNode, mn);

    // If the fully-qualified op is undefined, then a message has already been
    // put in errors, but we must insert a nullOpArgNode in the tree.
    if (genID.getFullyQualifiedOp() != null && genID.getArgs().length == 0 ) {
      // Create an OpArgNode from it.
      return new OpArgNode(genID.getFullyQualifiedOp(), opArgSyntaxNode, mn);
    }
    else if ( genID.getArgs().length > 0 ) { 
      // Expression being used where Operator is required
      errors.addError(opArgSyntaxNode.getLocation(), "Arity " + targetSymbol.getArity() + 
		      " operator (not an expression) is expected" + 
		      " to substitute for CONSTANT '" + targetSymbol.getName() + "'." );
      return nullOpArg;      
    }
    else {
      return nullOpArg;
    }
  } // end generateOpArg()

  /**
   * Process the substitutions clause of a module definition or instantiation;
   * returns a SubstInNode that can be used as a template for the wrapper that
   * must be present around each OpDefNode body created from the module
   * instantiation or module definition.
   */
  private SubstInNode processSubst(TreeNode    treeNode,
                                   TreeNode[]  substNodes,     // array of subst nodes [c1 <- e1, ... ,cn <- en]
                                   SymbolTable instancerST,    // SymbolTable in which the ei must be resolved
                                   Context     instanceeCtxt,  // Context in which the ci must be resolved
                                   ModuleNode  instanceeModule,// the ModuleNode of the module in which ci must be resolved
                                   ModuleNode  mn) throws AbortException {
    TreeNode[] children;    // find the substitution part of the syntax tree

    // Create a vector of all declarations of CONSTANTS and VARIABLES 
    // in the context of the module being instantiated (instancee).
    // These are all the symbols that must have substitutions defined
    // for them in the instantiation, either explictly or implicitly.
    Vector decls = instanceeCtxt.getByClass( OpDeclNode.class );

    // Create a SubstInNode to be used as a template for the SubstInNodes in the body of 
    // every newly instantiated OpDef in the module.  The substitutions in the returned 
    // SubstInNode object will be the implicit substitutions of the form c<-c for all 
    // CONSTANTS and VARIABLES c that are BOTH declared in instancee and for which the 
    // same name is declared or defined in instancer.  
    // Note the instancerST must be passed to this constructor because with a
    // default substitution LHS<-RHS the LHS is resolved in the instanceeCtxt, (done
    // in the previous line) and the RHS is resolved in the instancerST.
    SubstInNode substIn = new SubstInNode(treeNode, instancerST, decls, mn, instanceeModule);

    // For each explicit substitution in the syntax tree, overwrite or add
    // the corresponding default entry in SubstInNode just created
    for (int i = 3; i < substNodes.length; i += 2) {
      // pick up array of syntax elements for one substitution, e.g. ["c","<-",expr]
      TreeNode sc[] = substNodes[i].heirs(); 

      // substRHS is the expression "exp" in "c <- exp"; this stmt first checks that 
      // c is properly declared in the instancee context, and then generates an
      // ExprOrOpArgNode.  If c is a constant with parameters, then an OpArgNode is
      // returned; otherwise it is an ExprNode.
      ExprOrOpArgNode substRHS = generateSubst(instanceeCtxt, sc[0], sc[2], mn);

      // Overwrite an implicit substitution if there is one, or add a new one, 
      // checking for duplicate substitutions for the same symbol
      substIn.addExplicitSubstitute(instanceeCtxt, sc[0].getUS(), sc[2], substRHS );
    }

    // Check if substitution is complete, i.e. that all constants and vars 
    // have been substituted for.  
    substIn.matchAll( decls );  
    return substIn;
  } // end processSubst

  /**
   * This method treats *unnamed* INSTANCE stmts
   * NOTE: this code does nothing with ASSUMES or THEOREMS
   */
  private final void generateInstance(TreeNode treeNode, ModuleNode cm)
  throws AbortException {
     TreeNode[] children = treeNode.one()[0].heirs(); 
                        // skip one generation below NonLocalInstance
                        // because we know zero defines local

     // id of module being instanced
     UniqueString moduleId = children[1].getUS();  
                        
     // If this module instance is declared "LOCAL" then all of the 
     // definitions in it must be instanced as if they were "LOCAL"
     boolean localness = treeNode.local();

     // Create a list of all declarations for module moduleId.
     // Match them against either something in the substitutions or 
     // something in the current context (symbol table) for the 
     // substitution.  Check that the symbol does occur in the module 
     // and as a declaration.
     Context instanceeCtxt = this.getContext(moduleId);

     if (instanceeCtxt == null) {
       errors.addAbort(children[1].getLocation(),
                       "Internal error: No context available for module " +
                       moduleId.toString(), true);
     }

     // Try to find the ModuleNode for the module being instanced in
     // the symbolTable
     ModuleNode instanceeModuleNode = symbolTable.resolveModule(moduleId);

     // It must be an external module if it isn't in the symbolTable;
     // try to find it in moduleTable (it cannot be in both places, or
     // a name conflict would have resulted)
     if (instanceeModuleNode == null) { 
       instanceeModuleNode = moduleTable.getModuleNode(moduleId);
     }

     if (instanceeModuleNode == null) {
        errors.addAbort(children[1].getLocation(), 
                        "Could not find module " + moduleId.toString(),
			false);
     }

     // Create the SubstInNode that will act as a template "wrapper"
     // for each definition in the module being instantiated; this
     // SubstInNode itself gets discarded after being used as template
     // however many times is necessary
     SubstInNode subst = processSubst(treeNode, children, symbolTable,
				      instanceeCtxt, instanceeModuleNode, cm);

     // Create a vector of all of the OpDefNodes in the module being 
     // instantiated
     Vector defs = instanceeCtxt.getByClass(OpDefNode.class);

     OpDefNode odn;                // OpDefNode in module being instantiated (instancee)
     OpDefNode newOdn;             // Its counterpart current module (instancer)
     SubstInNode substInTemplate;  // Template to be used foe any SubstInNode wrappers required
     
     // Duplicate the OpDef records from the module being INSTANCE'd 
     for (int i = 0; i < defs.size(); i++) {
       odn = (OpDefNode)defs.elementAt(i); // OpDefNode in module being instantiated (instancee)

       // Do not instantiate built-in or local operators, or those OpDefNodes created 
       // solely to prevent a ModuleName from being used as an operator node.
       if (odn.getKind() == BuiltInKind ||
	   odn.getKind() == ModuleInstanceKind ||
	   odn.isLocal()) {
	 continue;
       }

       // If there are parameters to the module being instantiated, then a SubstInNode is 
       // required, and possibly a different module of origin should be indicated
       if (!instanceeModuleNode.isParameterFree()) {
         // Create the OpDefNode for the new instance of this definition
         // Note that the new OpDefNode shares the array of FormalParamNodes 
         //   with the old OpDefNode, as well as large parts of its body (all but the SubstInNode).
         //   Hence, changes by a tool to an Original OpDefNode will likely be reflected
         //   in all instances of it.
         if ( odn.getOriginallyDefinedInModuleNode().isParameterFree() ) {  
           newOdn = odn;
	   /*
             new OpDefNode( odn.getName(), UserDefinedOpKind, odn.getParams(),  
	     localness, substInTemplate, odn.getOriginallyDefinedInModuleNode(), symbolTable, 
	     treeNode );   
	   */
         }
	 else {        
	   // Create the "wrapping" SubstInNode as a clone of "subst" above, 
	   // but with a body from the OpDefNode in the module being instantiated
	   substInTemplate = new SubstInNode(treeNode, subst.getSubsts(),
					     odn.getBody(), cm, instanceeModuleNode);
	   newOdn = new OpDefNode(odn.getName(), UserDefinedOpKind, odn.getParams(),  
				  localness, substInTemplate, cm, symbolTable, treeNode);   
         }
       }
       else { 
         // There are no parameters to the instancee module; this
         // means that a SubstInNode is not necessary, and also that
         // the new operator should be considered to be "originally
         // defined in" the same module as the old one for purposes of
         // telling whether they are "the same" or different definitions

         // Create an OpDefNode whose body is the same as the instancer's.
         newOdn = 
           new OpDefNode(odn.getName(), UserDefinedOpKind, odn.getParams(),  
                         localness, odn.getBody(), odn.getOriginallyDefinedInModuleNode(), 
			 symbolTable, treeNode);
       }
       cm.appendDef(newOdn);
     } // end for

     // Create a new InstanceNode to represent this INSTANCE stmt in the current module
     InstanceNode inst = new InstanceNode(null /*no name*/, null /*no parms*/, 
					  instanceeModuleNode, subst.getSubsts(), treeNode);

     // Append this new InstanceNode to the vector of InstanceNodes
     // being accumulated for this module
     cm.appendInstance(inst);

     // There should be code here for dealing with THEOREMs and ASSUMEs.
  }

  private final void processTheorem(TreeNode stn, ModuleNode cm) throws AbortException {
    boolean local = stn.zero() != null;
    ExprNode expr = generateExpression(stn.one()[1], cm);

    cm.addTheorem(stn, expr, local);
    return;
  }

  private final void processAssumption(TreeNode stn, ModuleNode cm) throws AbortException {
    boolean local = stn.zero() != null;    
    ExprNode expr = generateExpression(stn.one()[1], cm);

    cm.addAssumption(stn, expr, local, symbolTable);
    return;
  }

}
