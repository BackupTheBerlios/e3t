// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import java.util.Enumeration;
import java.util.Hashtable;

import org.zambrovski.tla.tlasany.semantic.ErrorsContainer;

import tlasany.explorer.ExploreNode;
import tlasany.st.Location;
import tlasany.utilities.Strings;
import tlasany.utilities.Vector;
import util.UniqueString;

// A context contains def/declNodes only.
// Implements a simple context for symbol decls and defs. Also
// supports the intial (or global) context of built-in operators.

// The pair construct is used to maintain a linked list of
// definitions.  This in turn will allow the extraction of a full
// context from anywhere in the tree, from the englobing definition.

// Merging: different forms of merging should be offered, depending
// what is being merged.  We always start with a clone of the
// initialContext.  Alterwards, we can Extend or Instantiate.  In one
// case, we import defs and decls; in the other, only defs; but we
// need to keep track of substitutions.

/**
 * A Context is an assignment of meanings to module names and to
 * symbol names, where a meaning is a SemNode.  At the moment, there
 * seems to be no need to provide methods to manipulate or do anything
 * with contexts except pass them to the Front End.
 */
final public class Context implements ExploreNode {

  class Pair {
    Pair       link;
    SymbolNode info;

    // Note: Does not set lastPair
    Pair(Pair lnk, SymbolNode inf) {
      this.link = lnk;
      this.info = inf;
    }     

    // Note: Does set lastPair
    Pair(SymbolNode inf) {
      this.link = lastPair;
      this.info = inf;
      lastPair = this;
    }
    
    public final SymbolNode getSymbol() { return this.info; }
  }

  public class InitialSymbolEnumeration {

    Enumeration e = initialContext.content();

    public final boolean hasMoreElements() { return e.hasMoreElements(); }

    public final SymbolNode nextElement() {
      return (SymbolNode)(((Pair)(e.nextElement())).getSymbol());
    }
  }

  public class ContextSymbolEnumeration {

    Enumeration e = Context.this.content();

    public final boolean hasMoreElements() { return e.hasMoreElements(); }

    public final SymbolNode nextElement() {
      return ((Pair)(e.nextElement())).getSymbol();
    }
  }

  private static Context initialContext = new Context(null, new ErrorsContainer());
                                      // the one, static unique Context with builtin operators
                                      // null ModuleTable arg because this is shared by all modules

  private ExternalModuleTable exMT;   // The external ModuleTable that this context's SymbolTable
                                      // belongs to is null for global contex shared by all modules.

  private ErrorsContainer         errors;      // Object in which to register errors
  private Hashtable      table;       // Mapping from symbol name to Pair's that include SymbolNode's
  private Pair           lastPair;    // Pair added last to the this.table

  /**
   * exMT is the ExternalModuleTable containing the module whose
   * SymbolTable this Context is part of (or null).
   */
  public Context(ExternalModuleTable mt, ErrorsContainer errs) { 
    table = new Hashtable(); 
    this.exMT = mt;
    this.errors = errs;
    this.lastPair = null;
  }

  /*
   * Reinitialize the initialContext module so that a new spec file
   * can be parsed; must be called at the beginning of each root file
   * of a spec.
   */
  public static void reInit() {
    initialContext =  new Context(null, new ErrorsContainer()); // null because outside of any module
  }

  /**
   * This method returns a copy of the context that contains
   * declarations only of the built-in operators of TLA+.  This
   * context assigns no meanings to module names.
   */
  public static final Context getGlobalContext() { 
    return initialContext; 
  }

  public final ErrorsContainer getErrors() { return errors; }

  // Adds a symbol to the (unique) initialContext; aborts if already there
  public static final void addGlobalSymbol(UniqueString name, SymbolNode sn, ErrorsContainer errors) 
  throws AbortException {
    if (initialContext.getSymbol(name) != null) {
      errors.addAbort(Location.nullLoc,
		      "Error building initial context: Multiply-defined builtin operator " +
		      name + " at " + sn, false );
    }
    else {
      initialContext.addSymbolToContext( name, sn );
      return;
    }
  }

  /**
   * Returns symbol node associated with "name" in this Context, if
   * one exists; else returns null
   */
  public final SymbolNode getSymbol(Object name) {
    Pair r = (Pair)table.get(name);
    if (r != null) {
      return r.info;
    }
    return null;
  }

  /**
   * Adds a (UniqueString, SymbolNode) pair to this context; no-op if
   * already present
   */
  final void addSymbolToContext(Object name, SymbolNode s) {
    table.put(name, new Pair(s));    // Links to & updates lastPair
  }

  // Tests whether a name is present in this context
  final boolean occurSymbol(Object name) { 
    return table.containsKey(name); 
  }

  /**
   * Returns Enumeration of the elements of the Hashtable "Table",
   * which are pair of the form (Pair link, SymbolNode sn)
   */
  final Enumeration content() { 
    return table.elements(); 
  }

  /**
   * Returns a new ContextSymbolEnumeration object, which enumerates
   * the SymbolNodes of THIS context
   */
  public final ContextSymbolEnumeration getContextSymbolEnumeration() {
    return new ContextSymbolEnumeration();
  }

  /**
   * Returns a Vector of those SymbolNodes in this Context that are
   * instances of class "template" (or one of its subclasses)
   */
  final Vector getByClass( Class template ) {
    Vector result = new Vector();
    Enumeration list = table.elements();
    while (list.hasMoreElements()) {
      Pair elt = (Pair)list.nextElement();
      if (template.isInstance(elt.info)) {
        result.addElement( elt.info );
      }
    }
    return result;
  }

  /** 
   * Returns a Vector of those SymbolNodes in this Context that are
   * instances of class OpDefNode and that are NOT of kind BuiltInKind
   * or ModuleInstanceKind
   */
  public final Vector getOpDefs() {
    Class template = OpDefNode.class;
    Pair nextPair = lastPair;

    Vector result = new Vector();
    while (nextPair != null) {
      if ( nextPair.info instanceof OpDefNode &&     // true for superclasses too.
           ((OpDefNode)nextPair.info).getKind() != ASTConstants.ModuleInstanceKind &&
           ((OpDefNode)nextPair.info).getKind() != ASTConstants.BuiltInKind  )
        result.addElement( (OpDefNode)(nextPair.info) );
      nextPair = nextPair.link;
    }
    return result;
  }

  /** 
   * Returns vector of OpDeclNodes that represent CONSTANT declarations 
   */
  public final Vector getConstantDecls() {
    Class templateClass = OpDeclNode.class;
    Enumeration list = table.elements();

    Vector result = new Vector();
    while (list.hasMoreElements()) {
      Pair elt = (Pair)list.nextElement();
      if (templateClass.isInstance(elt.info) &&     // true for superclasses too.
         ((OpDeclNode)elt.info).getKind() == ASTConstants.ConstantDeclKind  )
        result.addElement( (SemanticNode)(elt.info) );

    }
    return result;
  }

  /* Returns vector of OpDeclNodes that represent CONSTANT declarations  */
  public final Vector getVariableDecls() {
    Class templateClass = OpDeclNode.class;
    Enumeration list = table.elements();

    Vector result = new Vector();
    while (list.hasMoreElements()) {
      Pair elt = (Pair)list.nextElement();
      if (templateClass.isInstance(elt.info) &&     // true for superclasses too.
           ((OpDeclNode)elt.info).getKind() == ASTConstants.VariableDeclKind  )
        result.addElement( (SemanticNode)(elt.info) );
    }
    return result;
  }

  /** 
   * Returns a Vector of those SymbolNodes in this Context that are
   * instances of class ModuleNode
   */
  public final Vector getModDefs() {
    Class template = ModuleNode.class;
    Enumeration list = table.elements();

    Vector result = new Vector();
    while (list.hasMoreElements()) {
      Pair elt = (Pair)list.nextElement();
      if (template.isInstance(elt.info))    // true for superclasses too.
        result.addElement( (SemanticNode)(elt.info) );
    }
    return result;
  }

  /**
   * Restricted Context merge.  Invoked once in Generator class.
   * Merges Context "ct" into THIS Context, except for local symbols
   * in "ct" Two symbols clash if they have the same name AND the same
   * class; that is an error.  If they only have the same name, they
   * are considered to be two inheritances of the same(or at least
   * compatible) declarations, and there is only a warning.  Returns
   * true if there is no error or there are only warnings; returns
   * false if there is an error
   */
  final boolean mergeExtendContext(Context ct) {
    boolean erc = true;

    // check locality, and multiplicity
    Pair p = ct.lastPair;
    while (p != null) {
      // Walk back along the list of pairs added to Context "ct"
      SymbolNode sn = p.info;

      // Ignore local symbols in Context "ct"
      if (!sn.isLocal()) {
	Object sName;
	if (sn instanceof ModuleNode) {
	  sName = new SymbolTable.ModuleName(sn.getName());
	}
	else {
	  sName = sn.getName();
	}

        if (!table.containsKey(sName)) {
	  // If this context DOES NOT contain this name, add it:
	  table.put(sName, new Pair(sn));
        }
	else {
	  // If this Context DOES contain this name
          SymbolNode symbol = ((Pair)table.get(sName)).info;
          if (symbol != sn) {
	    // if the two SymbolNodes with the same name are distinct nodes,
	    // We issue a warning if they are instances of the same Java class
	    // i.e. FormalParamNode, OpDeclNode, OpDefNode, or ModuleNode.
	    // otherwise, it is considered to be an error.
	    if (symbol.getClass() == sn.getClass()) {
              errors.addWarning( sn.getTreeNode().getLocation(),
                                 "Warning: multiply-defined symbol '" + 
                                 sName.toString() + 
                                 "' conflicts with declaration at " + 
                                 symbol.getTreeNode().getLocation() + ".");

            }
	    else {
              errors.addError( sn.getTreeNode().getLocation(),
                               "Incompatible multiple definitions of symbol '" + 
                               sName.toString() + 
                               "'; the conflicting declaration is at " + 
                               symbol.getTreeNode().getLocation()+ ".");
              erc = false;
            } //end else
          } // end if
        } // end else
      }
      p  = p.link;
    }
    return erc;
  }

  /**
   * Returns a duplicate of this Context.  Called once from
   * SymbolTable class.  The tricky part is duplicating the
   * linked-list of Pairs starting from this.lastpair. 
   */
  final Context duplicate(ExternalModuleTable exMT) {    // Added argument exMT (DRJ)
    Context dup       = new Context(exMT, errors);
    Pair    p         = this.lastPair;
    Pair    current   = null;
    boolean firstTime = true;

    while (p != null) {
      if (firstTime) {
        current = new Pair(null, p.info);     // Does NOT link to or update this.lastPair
        dup.lastPair = current;
        firstTime = false;
      }
      else {
	current.link = new Pair(null,p.info); // Note: causes sharing of reference in link.info
        current = current.link;
      }
      dup.table.put(current.info.getName(), current);
      p = p.link;
    }
    return dup;
  }

  /**
   * toString, levelDataToString, and walkGraph methods to implement
   * ExploreNode interface
   */
  public final String levelDataToString() { return "Dummy level string"; }

  public final String toString(int depth) {
    return "Please use Context.getContextEntryStringVector()" +
      " instead of Context.toString()";
  }  

  /* Returns a vector of strings */
  public final Vector getContextEntryStringVector(int depth, boolean b) {
    Vector ctxtEntries = new Vector(100);  // vector of Strings
    Context naturalsContext = 
               exMT.getContext(UniqueString.uniqueStringOf("Naturals"));

    if (depth <= 0) return ctxtEntries;

    Pair p = lastPair;
    while (p != null) {
      UniqueString key = p.info.getName();

      // If b is false, don't bother printing the initialContext--too long--
      // and, don't bother printing elements of the Naturals module either
      if (b || (!initialContext.table.containsKey(key) &&
		(naturalsContext == null || 
		 !naturalsContext.table.containsKey(key)))) {
        SymbolNode symbNode  = ((Pair)(table.get(key))).info;
	ctxtEntries.addElement("\nContext Entry: " + key.toString() + "  " 
                    + String.valueOf(((SemanticNode)symbNode).myUID).toString() + " " 
                    + Strings.indentSB(2,(symbNode.toString(depth-1))));
      }
      p = p.link;
    }

    // Reverse the order of elements in the vector so they print properly
    Object obj;
    int n = ctxtEntries.size();
    for (int i = 0; i < n/2; i++) {
      obj = ctxtEntries.elementAt(i);
      ctxtEntries.setElementAt(ctxtEntries.elementAt(n-1-i),i);
      ctxtEntries.setElementAt(obj, n-1-i);
    }
    return ctxtEntries;
  }

  public final void walkGraph(Hashtable semNodesTable) {
    UniqueString key;
    Enumeration  varEnum = table.keys();

    while (varEnum.hasMoreElements()) {
      key = (UniqueString)varEnum.nextElement();
      ((Pair)table.get(key)).info.walkGraph(semNodesTable);
    }
  }

}
