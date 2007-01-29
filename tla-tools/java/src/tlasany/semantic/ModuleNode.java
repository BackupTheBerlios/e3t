// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.st.TreeNode;
import tlasany.utilities.Strings;
import tlasany.utilities.Vector;
import util.Assert;
import util.UniqueString;

public class ModuleNode extends SymbolNode {

  private Context      ctxt;     // The (flat) context with all names known in this module,
                                 //   including builtin ops, and ops declared as CONSTANT
                                 //   or VARIABLE, ops imported and made visible via EXTENDS,
                                 //   and ops created through INSTANCE, names from modules outer
                                 //   to this one, as well as the names of internal modules (but
                                 //   not names declared or defined in internal modules of this one).
                                 // It does NOT include ops declared or defined in internal modules,
                                 //   ops defined in LETs, local from modules EXTENDed or INSTANCEd, 
                                 //   formal params, nor names bound by quantifier, CHOOSE, or 
                                 //   recursive function definition.

  private ModuleNode[]  extendees    = new ModuleNode[0]; 
                                             // Modules directly extended by this one.
  private OpDeclNode[] constantDecls = null; // CONSTANTs declared in this module
  private OpDeclNode[] variableDecls = null; // VARIABLEs declared in this module
  private Vector definitions         = new Vector(8); 
                                             // AssumeNodes, internal ModuleNodes, OpDefNodes, and 
                                             // TheoremNodes, in the exact order they were defined 
                                             // in this module
  private OpDefNode[]    opDefs         = null; // operators defined in this module, in order defined
  private ModuleNode[]   modDefs        = null; // inner modules defined in this module
  private InstanceNode[] instantiations = null; // top level module instantiations in this module  
  private AssumeNode[]   assumptions    = null; // unnamed assumptions in this module
  private TheoremNode[]  theorems       = null; // unnamed theorems in this module

  // The next two vectors hold the unnamed ASSUMEs and THEOREMs
  // declared in this module or inherited via EXTENDS
  // NOTE: For the moment, no ASSUMEs or THEOREMs are named, nor are
  // they inherited via INSTANCing
  private Vector assumptionVec = new Vector();  // Vector of AssumeNodes
  private Vector theoremVec    = new Vector();  // Vector of TheoremNodes
  private Vector instanceVec   = new Vector();  // Vector of InstanceNodes
  
  // Invoked only in Generator
  public ModuleNode(UniqueString us, Context ct, TreeNode stn) {
    super(ModuleKind, stn, us); 
    this.ctxt = ct; 
  }

  // Required for SymbolNode interface.
  public final int getArity() { return -2; }

  public final SymbolTable getSymbolTable() { return null; }

  public final Context getContext() { return this.ctxt; }

  // Meaningless--just here for compatibility with SymbolNode interface
  public final boolean isLocal() { return false; }

  // Returns true iff this module has no parmeters, i.e. CONSTANT or
  // VARIABLE decls, so that INSTANCEing it is the same as EXTENDing it.
  final boolean isParameterFree() {
    return (getConstantDecls().length == 0 &&
            getVariableDecls().length == 0);
  }

  public final void createExtendeeArray(Vector extendeeVec) {
    extendees = new ModuleNode[extendeeVec.size()];

    for ( int i = 0; i < extendees.length; i++ ) {
      extendees[i] = (ModuleNode)extendeeVec.elementAt(i);
    }
  }
    
  /**
   * Returns vector of the OpDeclNode's in the current module
   * representing CONSTANT declarations, including operator constants
   * and constants defined via EXTENDS and INSTANCE, but excluding
   * CONSTANTS from internal modules.
   */
  public final OpDeclNode[] getConstantDecls() {
    if (constantDecls != null) return constantDecls;

    Vector contextVec = ctxt.getConstantDecls();
    constantDecls = new OpDeclNode[contextVec.size()];
    for (int i = 0, j = constantDecls.length - 1; i < constantDecls.length; i++) {
      constantDecls[j--] = (OpDeclNode)contextVec.elementAt(i);
    }
    return constantDecls;
  }

  /**
   * Returns a vector of the OpDeclNode's in the current module
   * representing VARIABLE declarations, including those defined via
   * EXTENDS and INSTANCE, but excluding VARIABLES from internal modules.
   */
   public final OpDeclNode[] getVariableDecls() {
    if (variableDecls != null) return variableDecls;

    Vector contextVec = ctxt.getVariableDecls();
    variableDecls = new OpDeclNode[contextVec.size()];
    for (int i = 0, j = variableDecls.length - 1; i < variableDecls.length; i++) {
      variableDecls[j--] = (OpDeclNode)contextVec.elementAt(i);
    }
    return variableDecls;
  }

  /**
   * Returns array of the OpDefNode's created in the current module,
   * including function defs, and operators those defined via EXTENDS
   * and INSTANCE, but excluding built-in operators, operators in the
   * LET-clause of a let-expression, formal parameters, bound
   * variables, and operators defined in internal modules.
   *
   * The OpDefNodes are ordered such that if B is defined in terms of
   * A, the B has a HIGHER index than A in the returned array.
   */
  public final OpDefNode[] getOpDefs() {
    if (opDefs != null) return opDefs;

    Vector contextVec = ctxt.getOpDefs();
    opDefs = new OpDefNode[contextVec.size()];
    for (int i = 0, j = opDefs.length - 1; i < opDefs.length; i++) {
        opDefs[j--] = (OpDefNode)contextVec.elementAt(i);
    }
    return opDefs;
  }

  /** 
   * Appends to vector of definitions in this module; should only be
   * called with AssumeNodes, ModuleNodes, OpDefNodes and TheoremNodes
   * as arguments.
   */
  public final void appendDef(SemanticNode s) {
    definitions.addElement(s);
  }

  /**
   * Returns array of the InstanceNode's representing module
   * instantiations in the current module, including those inherited
   * via EXTENDS, but excluding those in the LET-clause of a
   * let-expression and in internal modules
   */
  public final InstanceNode[] getInstances() {
    if (instantiations != null) return instantiations;

    instantiations = new InstanceNode[instanceVec.size()];
    for (int i = 0; i < instantiations.length; i++) {
      instantiations[i] = (InstanceNode)(instanceVec.elementAt(i));
    }
    return instantiations;
  }

  /** 
   * Appends to vector of instantiations in this module
   */
  public final void appendInstance(InstanceNode s) {
    instanceVec.addElement(s);
  }

  /**
   * Returns an array of all the top-level inner modules that appear
   * in this module.  Their submodules in turn are retrieved by again
   * applying this method to the ModuleNode's in the returned vector,
   * etc.
   */
  public final ModuleNode[] getInnerModules() {
    if ( modDefs != null ) return modDefs;

    Vector v = ctxt.getModDefs();
    modDefs = new ModuleNode[v.size()];
    for (int i = 0; i < modDefs.length; i++) {
      modDefs[i] = (ModuleNode)v.elementAt(i);
    }
    return modDefs;
  }

  /**
   * Returns the array of unnamed AssumeNodes that are part of this module
   */
  public final AssumeNode[] getAssumptions() {
    if (assumptions != null) return assumptions;

    assumptions = new AssumeNode[assumptionVec.size()];
    for (int i = 0; i< assumptions.length; i++) {
      assumptions[i] = (AssumeNode)assumptionVec.elementAt(i);
    }
    return assumptions;
  }

  /**
   * Returns the array of unnamed TheoremNodes that are part of this module
   */
  public final TheoremNode[] getTheorems() {
    if (theorems != null) return theorems;

    theorems = new TheoremNode[theoremVec.size()];
    for (int i = 0; i < theorems.length; i++) {
      theorems[i] = (TheoremNode)(theoremVec.elementAt(i));
    }
    return theorems;
  }

  final void addAssumption(TreeNode stn, ExprNode ass, boolean localness, SymbolTable st) {
    assumptionVec.addElement( new AssumeNode( stn, ass, localness, this ) );
  }

  final void addTheorem( TreeNode stn, ExprNode thm, boolean localness) {
    theoremVec.addElement( new TheoremNode( stn, thm, localness, this ) );
  }

  final void copyAssumes(ModuleNode extendee) {
    for (int i = 0; i < extendee.assumptionVec.size(); i++) {
      AssumeNode assume = (AssumeNode)extendee.assumptionVec.elementAt(i);
      if ( ! assume.isLocal() ) {
        assumptionVec.addElement(assume);    
      }
    }
  }

  final void copyTheorems(ModuleNode extendee) {
    for (int i = 0; i < extendee.theoremVec.size(); i++) {
      TheoremNode theorem = (TheoremNode)extendee.theoremVec.elementAt(i);
      if (!theorem.isLocal()) {
        theoremVec.addElement(theorem);
      }
    }
  }

  public final ModuleNode[] getExtendedModules() {
    return new ModuleNode[0];
  }

  /** 
   * Just a stub method; one cannot resolve against a ModuleNode.
   * This method is here only to satisfy the SymbolNode interface.
   */
  public final boolean match( OpApplNode sn, ModuleNode mn ) { return false; } 

  /**
   * Returns an array of all the theorems that appear in this module,   
   * along with their proofs (if they have them).  It includes theorems 
   * obtained from extended and instantiated modules.  Note that if     
   * module M has ASSUME statements A and B, then                       
   *                                                                    
   *    Foo(x, y) == INSTANCE M WITH ...                                
   *                                                                    
   * introduces, for each theorem T in module M, the theorem            
   *                                                                    
   *    ASSUME 1. LEVELDECL x                                           
   *           2. LEVELDECL y                                           
   *           3. A                                                     
   *           4. B                                                     
   *    PROVE  T                                                        
   *                                                                    
   * where LEVELDECL denotes some appropriate level declaration based   
   * on the maximum levels of expressions that can be substituted       
   * for the formal parameters x and y.   
   *
   * Not implemented -- see getTheorems()                              
   */
  public final TheoremNode[] getThms() { return null; }

  /**
   * Returns an array of all the assumptions (the expressions in ASSUME 
   * statements).  An assumption in an ordinary specification has the   
   * form                                                               
   *                                                                    
   *    ASSUME A == expr                                                
   *                                                                    
   * where expr is a constant-level expression.  However, the grammar   
   * allows assumptions such as                                         
   *                                                                    
   *    ASSUME A == ASSUME B                                            
   *                PROVE  C                                            
   *                                                                    
   * Hence, an assumption must be represented by an AssumeProveNode.    
   *                                                                    
   * An assumption like this produces a ref in getAssumes() to the      
   * AssumeProveNode that represents the assumption, and a ref in       
   * getOpDefs to the OpDefNode that represents the definition of A. 
   *
   * Not implemented -- see getAssumptions()   
   */
  public final AssumeProveNode[] getAssumes() { return null; }

  /* Level checking */
  private boolean levelCorrect;
  private HashSet levelParams;
  private SetOfLevelConstraints levelConstraints;
  private SetOfArgLevelConstraints argLevelConstraints;
  private HashSet argLevelParams;
  
  public final boolean levelCheck() {
    if (this.levelConstraints != null) return this.levelCorrect;
    
    // Level check everything in this module
    this.levelCorrect = true;
    ModuleNode[] mods = this.getInnerModules();
    for (int i = 0; i < mods.length; i++) {
      if (!mods[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    OpDefNode[] opDefs = this.getOpDefs();
    for (int i = 0; i < opDefs.length; i++) {
      if (!opDefs[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    TheoremNode[] thms = this.getTheorems();
    for (int i = 0; i < thms.length; i++) {
      if (!thms[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    AssumeNode[] assumps = this.getAssumptions();
    for (int i = 0; i < assumps.length; i++) {
      if (!assumps[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
    InstanceNode[] insts = this.getInstances();
    for (int i = 0; i < insts.length; i++) {
      if (!insts[i].levelCheck()) {
	this.levelCorrect = false;
      }
    }
  
    // Calculate level information.
    this.levelParams = new HashSet();
    OpDeclNode[] decls = this.getConstantDecls();
    for (int i = 0; i < decls.length; i++) {
      this.levelParams.add(decls[i]);
    }
    
    this.levelConstraints = new SetOfLevelConstraints();
    this.argLevelConstraints = new SetOfArgLevelConstraints();
    this.argLevelParams = new HashSet();
    if (!this.isConstant()) {
      for (int i = 0; i < decls.length; i++) {
	this.levelConstraints.put(decls[i], Levels[ConstantLevel]);
      }
    }
    for (int i = 0; i < opDefs.length; i++) {
      this.levelConstraints.putAll(opDefs[i].getLevelConstraints());
      this.argLevelConstraints.putAll(opDefs[i].getArgLevelConstraints());
      Iterator iter = opDefs[i].getArgLevelParams().iterator();
      while (iter.hasNext()) {
	ArgLevelParam alp = (ArgLevelParam)iter.next();
	if (!alp.occur(opDefs[i].getParams())) {
	  this.argLevelParams.add(alp);
	}
      }
    }
    for (int i = 0; i < thms.length; i++) {
      this.levelConstraints.putAll(thms[i].getLevelConstraints());
      this.argLevelConstraints.putAll(thms[i].getArgLevelConstraints());
      this.argLevelParams.addAll(thms[i].getArgLevelParams());      
    }
    for (int i = 0; i < insts.length; i++) {
      this.levelConstraints.putAll(insts[i].getLevelConstraints());
      this.argLevelConstraints.putAll(insts[i].getArgLevelConstraints());
      this.argLevelParams.addAll(insts[i].getArgLevelParams());
    }
    for (int i = 0; i < assumps.length; i++) {
      this.levelConstraints.putAll(assumps[i].getLevelConstraints());
      this.argLevelConstraints.putAll(assumps[i].getArgLevelConstraints());
      this.argLevelParams.addAll(assumps[i].getArgLevelParams());      
    }
    return this.levelCorrect;
  }

  public final int getLevel() {
    Assert.fail("Internal Error: Should never call ModuleNode.getLevel()");
    return -1;    // make compiler happy
  }
  
  public final HashSet getLevelParams() { return this.levelParams; }
  
  public final SetOfLevelConstraints getLevelConstraints() { 
    return this.levelConstraints; 
  }

  public final SetOfArgLevelConstraints getArgLevelConstraints() { 
    return this.argLevelConstraints; 
  }

  public final HashSet getArgLevelParams() { return this.argLevelParams; }

  /**
   * Returns true iff the module is a constant module. See the
   * discussion of constant modules in the ExprNode interface.
   *
   * A module is a constant module iff the following conditions are
   * satisfied:
   *
   * 1. It contains no VARIABLE declarations (or other nonCONSTANT
   *    declarations in an ASSUME). 
   *
   * 2. It contains no nonconstant operators such as prime ('),
   *    ENABLED, or [].
   *
   * 3. It extends and instantiates only constant modules.
   *
   * NOTE: Can only be called after calling levelCheck().
   */
  public final boolean isConstant() {
    // if the module contains any VARIABLE declarations, it is not a
    // constant module
    if (this.getVariableDecls().length > 0) return false;

    // If the module contains any non-constant operators, it is not a
    // constant module.  We test this by checking the level of the
    // bodies of the opDefs.  We enumerate this module's Context
    // object rather than using the opDefs array, because we must
    // include all operators not only defined in this module, but also
    // inherited through extention and instantiation
    OpDefNode[] opDefs = this.getOpDefs();
    for (int i = 0; i < opDefs.length; i++) {
      if (opDefs[i].getKind() != ModuleInstanceKind &&
          opDefs[i].getBody().getLevel() != ConstantLevel)
        return false;
    }

    // If the module contains any nonconstant expressions as Theorems
    // it is nonconstant module.  (Assumptions can only be of level 0
    // anyway, so no additional test for them is necessary here.)
    for (int i = 0; i < theoremVec.size(); i++) {
      if (((TheoremNode)(theoremVec.elementAt(i))).getLevel() != ConstantLevel) {
        return false;
      }
    }

    // Otherwise this module is a constant module
    return true;
  }

  /**  
   * walkGraph, levelDataToString, and toString methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { 
    return "LevelParams: "         + getLevelParams()         + "\n" +
           "LevelConstraints: "    + getLevelConstraints()    + "\n" +
           "ArgLevelConstraints: " + getArgLevelConstraints() + "\n" +
           "ArgLevelParams: "      + getArgLevelParams()      + "\n";
  }

  public final void walkGraph (Hashtable semNodesTable) {
    Integer uid = new Integer(myUID);

    if (semNodesTable.get(uid) != null) return;

    semNodesTable.put(uid, this);
    if (ctxt != null) {
      ctxt.walkGraph(semNodesTable);
    }
    for (int i = 0; i < instanceVec.size(); i++) {
      ((InstanceNode)(instanceVec.elementAt(i))).walkGraph(semNodesTable);
    }
    for (int i = 0; i < theoremVec.size(); i++) {
      ((TheoremNode)(theoremVec.elementAt(i))).walkGraph(semNodesTable);
    }
    for (int i = 0; i < assumptionVec.size(); i++) {
      ((AssumeNode)(assumptionVec.elementAt(i))).walkGraph(semNodesTable);
    }
  }

  public final void print(int indent, int depth, boolean b) {
    if (depth <= 0) return;

    RuntimeConfiguration.get().getOutStream().print(
      "*ModuleNode: " + name + "  " + super.toString(depth) 
      + "  errors: " + (errors == null 
                           ? "null" 
                           : (errors.getNumErrors() == 0 
                                 ? "none" 
                                 : "" +errors.getNumErrors())));

    Vector contextEntries = ctxt.getContextEntryStringVector(depth-1, b);
    for (int i = 0; i < contextEntries.size(); i++) {
      RuntimeConfiguration.get().getOutStream().print(Strings.indent(2+indent, (String)contextEntries.elementAt(i)) );
    }
  }

  public final String toString(int depth) {
    if (depth <= 0) return "";

    String ret =
      "*ModuleNode: " + name + "  " + super.toString(depth) + 
      "  constant module: " + this.isConstant() +
      "  errors: " + (errors == null 
                        ? "null" 
                        : (errors.getNumErrors() == 0 
                              ? "none" 
                              : "" + errors.getNumErrors()));

    Vector contextEntries = ctxt.getContextEntryStringVector(depth-1,false);
    if (contextEntries != null) {
      for (int i = 0; i < contextEntries.size(); i++) {
        if (contextEntries.elementAt(i) != null) {
          ret += Strings.indent(2, (String)contextEntries.elementAt(i));
        }
	else {
          ret += "*** null ***";
        }
      }
    }

    if ( instanceVec.size() > 0 ) {
      ret += Strings.indent(2, "\nInstantiations:");
      for (int i = 0; i < instanceVec.size(); i++) {
        ret += Strings.indent(4, ((InstanceNode)(instanceVec.elementAt(i))).toString(depth));
      }
    }

    if ( assumptionVec.size() > 0 ) {
      ret += Strings.indent(2, "\nAssumptions:");
      for (int i = 0; i < assumptionVec.size(); i++) {
        ret += Strings.indent(4, ((AssumeNode)(assumptionVec.elementAt(i))).toString(depth));
      }
    }

    if ( theoremVec.size() > 0 ) {
      ret += Strings.indent(2, "\nTheorems:");
      for (int i = 0; i < theoremVec.size(); i++) {
        ret += Strings.indent(4, ((TheoremNode)(theoremVec.elementAt(i))).toString(depth));
      }
    }
    return ret;
  }

}

