// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.modanalyzer;

// A ParseUnit is, roughly, a wrapper for a parser and the file it originates from.

// The parser contains the root syntactic node

// At creation time, ParseUnit is given a NamedInputStream as parameter, 
// which supplies it with the input stream as well as other relevant information.

// ParseUnit keeps track of whether or not a syntactic unit needs to be 
// reparsed.  This can be tested with isLoaded(), and done with parseFile().

import org.zambrovski.tla.RuntimeConfiguration;
import org.zambrovski.tla.tlasany.semantic.ErrorsContainer;

import tlasany.semantic.AbortException;
import tlasany.st.Location;
import tlasany.st.ParseTree;
import tlasany.st.TreeNode;
import tlasany.utilities.Vector;


/**
 * This class represents a parse unit, i.e. a file containing a single top-level TLA+ module and its
 * inner modules.
 */
public class ParseUnit {

  private SpecObj            spec;                   // Back-reference to the SpecObj that this ParseUnit is part of
  private NamedInputStream   nis        = null;      // nis contains the file name, top-level module name, and 
                                                     //   source File object for a module
  private ParseTree          parseTree  = null;      // Parse tree for the contents of "nis"; ParseTree is an 
                                                     // interface.  This is how one gets at the root of the 
                                                     // parse tree for a ParseUnit
  private long               parseStamp = 0;         // time stamp of the source file last parsed.
  private ModulePointer      rootModule = null;      // The ModulePointer for the top level module of this ParseUnit 
  private String             rootModuleName;         // The String name of the ParseUnit, and also of its root module
  private ParseUnitRelatives parseUnitRelatives = new ParseUnitRelatives();
                                                     // The ParseUnits this one is related to by EXTENTION and INSTANCE
  // Constructor
  public ParseUnit( SpecObj spec, NamedInputStream source ) {
    this.spec = spec;
    this.nis = source;
    rootModuleName = (source != null ? nis.getModuleName() : null);
  }

  // Simple toString that just prints the ParseUnit's name
  public final String toString() {
    return "[ ParseUnit: " + rootModuleName + " ]";
  }

  /** 
   *  Returns true the source file for the nis still exists (i.e. has
   *  not been deleted) and if the parseStamp of THIS ParseUnit is
   *  more recent than the modification time of the source file.
   */
  public final boolean isLoaded() {
    return (nis != null &&  
	    nis.sourceFile().exists() &&
	    parseStamp > nis.sourceFile().lastModified());
  }

  // Get-methods
  public final String        getName()        { return rootModuleName; } 

  public final SpecObj       getSpec()        { return spec; }

  public final String        getFileName()    { return (nis != null ? nis.getFileName() : null); } 

  public final TreeNode      getParseTree()   { return (parseTree != null ? parseTree.rootNode() : null); }

  public final ModulePointer getRootModule()  { return rootModule; }

  final        Vector        getExtendees()   { return parseUnitRelatives.extendees; }

  final        Vector        getExtendedBy()  { return parseUnitRelatives.extendedBy; }

  final        Vector        getInstancees()  { return parseUnitRelatives.instancees; }
             
  final        Vector        getInstancedBy() { return parseUnitRelatives.instancedBy; }

  // Add-methods
  final        void          addExtendee(ParseUnit pu)    { parseUnitRelatives.extendees.addElement(pu); }

  final        void          addExtendedBy(ParseUnit pu)  { parseUnitRelatives.extendedBy.addElement(pu); }

  final        void          addInstancee(ParseUnit pu)   { parseUnitRelatives.instancees.addElement(pu); }

  final        void          addInstancedBy(ParseUnit pu) { parseUnitRelatives.instancedBy.addElement(pu); }

  final ModuleRelatives getRelatives(ModulePointer module) {
    return module.getRelatives();
  }

  final ParseUnitRelatives getRelatives() { return parseUnitRelatives; }

  final ModuleContext getContext(ModuleRelatives relatives) {
    return relatives.context;
  }

  final ModuleContext getContext(ModulePointer module) {
    return module.getRelatives().context;
  }

  final ModulePointer getParent(ModulePointer module) {
    return module.getRelatives().outerModule;
  }

  final Vector getPeers(ModulePointer module) {
    if ( module.getRelatives().outerModule != null ) {
      return module.getRelatives().outerModule.getRelatives().directInnerModules;
    }
    return null;
  }

  private final void writeParseTreeToFile(ErrorsContainer errors) throws AbortException {
    // Construct the name with a .jcg extension (Jean-Charles Gregoire)
    String sinkName = nis.getModuleName() + ".jcg";
    java.io.File compiled = new java.io.File( sinkName );

    // if that file already exists, we are about to overwrite it, so delete it first
    if ( compiled.exists() ) compiled.delete();
       
    // Go ahead and write the tree out
    try {
      java.io.FileOutputStream output = new java.io.FileOutputStream( compiled );
      java.io.PrintWriter pw = new java.io.PrintWriter(output); 

      // This is different from parseTree.SyntaxTreeNode.printST()
      SyntaxTreePrinter.print( parseTree, pw );
      pw.flush(); 
      pw.close();
    }
    catch ( java.io.IOException e ) {
      errors.addAbort("Error: Failed to open output file " + sinkName +
		      "\n" + e.getMessage());
    }
  }

  private final void verifyEquivalenceOfFileAndModuleNames(ErrorsContainer errors) 
  	throws AbortException 
  {
    // Retrieve the full file name
    String fName = getFileName();

    // Remove all of the pathname up to the final "/"
    String  newName1 = fName.substring(fName.lastIndexOf("/")+1);
    String  newName2 = fName.substring(fName.lastIndexOf("\\")+1);
    
    //TODO: refactor
    if (!fName.equals(newName1) && fName.equals(newName2))
    {
        fName = newName1;
    } else if (!fName.equals(newName2) && fName.equals(newName1))
    {
        fName = newName2;        
    } else if(fName.equals(newName2) && fName.equals(newName1) )
    {
        
    } else {
        errors.addAbort("File name '" + fName + "' couldn't be analysed ");
    }

    // Remove any extention from file name
    fName = fName.substring(0,fName.lastIndexOf("."));

    // mName = name of top-level module declared in the file
    String mName = getParseTree().heirs()[0].heirs()[1].getImage();

    // Make sure the module named in the file matches the name of the file, at least
    // with a case-independent test.
    if (!mName.equalsIgnoreCase(fName)) {
      errors.addAbort("File name '" + fName + "' does not match the name '" +
		      mName + "' of the top level module it contains.");
    }
  }

  /**
   * This method parses the source in THIS.nis, if it has not been
   * parsed already; It then proceeds to analyze the resulting parse
   * tree to see what other external modules must be found and parsed.
   * Finally, it writes result to a file if required by a command line
   * switch.
   */
  public final void parseFile(ErrorsContainer errors, boolean firstCall) throws AbortException {
    // Has it already been parsed since last modified?  If yes, then no need to parse again
    if ( parseStamp > nis.sourceFile().lastModified() ) return;

    // Does the file exist?  If not abort cleanly.  Of course the file could be deleted 
    // during the next few lines, and execution would also abort, but not so cleanly.  
    // We ignore that possibility.
    if (! nis.sourceFile().exists() ) {
      errors.addAbort("Error: source file '" + nis.getName() + 
                      "' has apparently been deleted.");
    }

    // Print user feedback
    RuntimeConfiguration.get().getErrStream().println("Parsing file " + nis.sourceFile());

    // create parser object
    parseTree = new tlasany.parser.TLAplusParser(nis);

    // Here is the one true REAL call to the parseTree.parse() for a file; 
    // The root node of the parse tree is left in parseTree.
    boolean parseSuccess = parseTree.parse();
    if ( !parseSuccess ) { // if parsing the contents of "nis" failed...
      errors.addAbort(Location.nullLoc,
		      "Could not parse module " + nis.getModuleName() + 
                      " from file " + nis.getName(),true );
    }

    parseStamp = System.currentTimeMillis();

    // if the is the very first time parseFile() is called
    if (firstCall) {
    // We don't know the name of the specification until this moment!
      spec.setName(getParseTree().heirs()[0].heirs()[1].getImage());
    }

    rootModule = new ModulePointer(spec, this, getParseTree());

    // Determine which modules extend or include which others
    determineModuleRelationships(rootModule, /* parent */ null); 

    /*
    // Debugging
    RuntimeConfiguration.get().getErrStream().println("ModuleRelationships for ParseUnit " + this.getName() + "\n" +  
                       spec.getModuleRelationships().toString() ); 
    */

    // Make sure file contains module of the same name
    verifyEquivalenceOfFileAndModuleNames(errors);

    // Use system property to decide whether to "print" the parse tree to a file
    if ( System.getProperty( "TLA-Print", "no").equals("yes") ) {
      writeParseTreeToFile(errors);  
    }
  }

  private void handleExtensions(ModulePointer currentModule, ModulePointer otherModule) {
    /*
    // Debugging
    RuntimeConfiguration.get().getErrStream().println("Entering HandleExtensions for (" + 
                       currentModule.getName() + ", " + 
                       ( otherModule.getName() != null ? otherModule.getName() : "null" ) + 
                       ")");
    RuntimeConfiguration.get().getErrStream().println(currentModule.getContext().toString());
    */

    if (otherModule == null) {
      // RuntimeConfiguration.get().getErrStream().println("Leaving handleExtensions because otherModule == null");
      return;
    }

    ModuleContext currentContext = getContext(currentModule);
    Vector        extendeeNames  = otherModule.getNamesOfModulesExtended();

    // For all modules extended by otherModule
    for (int i = 0; i < extendeeNames.size(); i++) {
      String        extendeeName = (String)extendeeNames.elementAt(i);
      ModulePointer extendeeResolvant = currentContext.resolve(extendeeName);

      // if extendeeName is resolved in currentContext, then
      // incorporate all of its inner modules into currentContext And
      // recursively add to currentContext all of the inner modules in
      // modules extended by resolvant
      if ( extendeeResolvant != null ) {
        Vector directInnerModules = extendeeResolvant.getDirectInnerModules();
        for (int j = 0; j < directInnerModules.size(); j++) {
          ModulePointer upperInnerMod = (ModulePointer)directInnerModules.elementAt(j);
          currentContext.bindIfNotBound(upperInnerMod.getName(), upperInnerMod);
          handleExtensions(currentModule,extendeeResolvant); // recursive call
	}
      }
    }

    /*
    // Debugging
    if ( currentModule.getName().equals("BB") ) {
      RuntimeConfiguration.get().getErrStream().println("Leaving HandleExtensions for " + currentModule.getName() + "; context is:");
      RuntimeConfiguration.get().getErrStream().println(currentModule.getContext().toString());
    }
    */
  }

  /**
   * Calculates the bindings (context) for module names known at the
   * location where currentModule is defined, and bound to modules
   * defined within this ParseUnit.  Does NOT include currentModule
   * itself, but DOES include peers defined earlier in the parent
   * module, and peers of the parent defined earlier than the parent,
   * and peers of the grandparent defined earlier than the
   * grandparent, etc.
   *
   * This method must be called before "peer" modules (other inner
   * modules at the same level) defined later are encountered, because
   * peer modules defined earlier are in the context of the current
   * module, but peer modules defined later are not.
   */
  private void calculateContextWithinParseUnit(ModulePointer currentModule) {
    // RuntimeConfiguration.get().getErrStream().println("Context before adding parents: " + currentModule.getContext());
    ModuleContext currentContext = getContext(currentModule);

    // A module's context includes that of parent, grandparent,
    // etc. at point where it is defined
    if ( getParent(currentModule) != null) {
      currentContext.union(getParent(currentModule).getContext());
    }

    // A module's context also contains the names of direct inner
    // modules of ancestors defined before this one
    ModulePointer ancestorModule = currentModule;
    while ( getParent(ancestorModule) != null ) {
      // A module's context includes that of parent, grandparent,
      // etc. at point where it is defined
      currentContext.union(getParent(ancestorModule).getContext());

      Vector peers = getPeers(ancestorModule);
      // All peers defined so far are in the currentContext
      for (int i = 0; i < peers.size() - 1; i++) {
        ModulePointer nextPeer = (ModulePointer)peers.elementAt(i);
        currentContext.bindIfNotBound(nextPeer.getName(), nextPeer);
      }
      ancestorModule = getParent(ancestorModule);
    } // end while

    // A module's context also includes inner modules of modules
    // extended by currentModule's parent, etc.  directly or
    // indirectly, are in this module's context.
    handleExtensions(currentModule, currentModule );

    // RuntimeConfiguration.get().getErrStream().println("Context after handling extentions: \n" + getContext(currentModule));
  }

  // Analyzes various sets of modules related to the module at
  // "currentNode" (which must be an "N_Module" node).  The second
  // argument is the ModulePointer for the immediate outer module for
  // currentNode (or null if none).
  //
  // For the module "currentModule" do the following:
  //   1) Create a ModuleRelatives object associated with it 
  //   2) Fill in the outerModule field for currentNode
  //   3) Calculate the module context as known within this ParseUnit
  //   4) Fill its directlyExtendedModules Vector with the String names
  //      of all of the modules it extends
  //   5) Fill its directlyInstantiatedModules Vector with the String
  //      names of all modules it instances
  //   6) Fill in its directInnerModules Vector with references to the
  //      module name TreeNodes of all direct inner modules it defines
  //   7) For all direct inner modules defined, call
  //      determineModuleRelationships recursively
  void determineModuleRelationships(ModulePointer currentModule, ModulePointer parent) {
    /***
    RuntimeConfiguration.get().getErrStream().println("< Entering determineModuleRelationships for currentModule " +
		       currentModule.getName() + ", with parent " +
		       (parent != null ? parent.getName() : "null"));  
    ***/

    // Allocate ModuleRelatives object for the "current" module
    ModuleRelatives currentRelatives = new ModuleRelatives(this, currentModule);

    // Create Association between currentModule and its new ModuleRelatives object
    currentModule.putRelatives(currentRelatives);

    // indicate outerModule relationship
    currentRelatives.outerModule = parent;

    // Find the N_Extends node of the currentModule
    TreeNode extendNode = currentModule.getExtendsDecl();
    
    // Loop through the EXTENDS decl for the odd nodes, which are the
    // names of the modules being extended, and add an entry in the
    // directlyExtendsModules vector for each module mentioned
    for (int i = 1; i < extendNode.heirs().length; i += 2) {
      String extendedModuleName = extendNode.heirs()[i].getImage();
      currentRelatives.directlyExtendedModuleNames.addElement(extendedModuleName);
    }

    // Calculate the module context for current module, i.e. the names
    // and bindings of all modules within the current parseUnit that
    // are in the context of this module
    calculateContextWithinParseUnit(currentModule);

    // Find the body part of module tree
    TreeNode body = currentModule.getBody();

    // loop through the top level definitions in the body of the module
    // looking for embedded modules instantiations, and module definitions
    for (int i = 0; i < body.heirs().length; i++) {
      TreeNode def = body.heirs()[i];

      if (def.getImage().equals("N_Module")) {
	// We encounter an new (inner) module
        // Pick off name of inner module
        ModulePointer innerModule = new ModulePointer(spec, this,def);

        // Indicate that the inner module is inner to the current module
        currentRelatives.directInnerModules.addElement( innerModule );

        // Recursive call to determine module relationships for the inner module
        determineModuleRelationships(innerModule, currentModule); 
      }
      else if (def.getImage().equals("N_Instance")) {
	// We encounter an INSTANCE decl
        TreeNode[] instanceHeirs = def.heirs();
        int nonLocalInstanceNodeIX = 0;

        // The modifier "LOCAL" may or may not appear in the syntax
        // tree; if so, offset by 1.
        if (instanceHeirs[0].getImage().equals("LOCAL")) {
          nonLocalInstanceNodeIX = 1;
	}

        // Find the name of the module being instantiated
        String instanceModuleName = instanceHeirs[nonLocalInstanceNodeIX].heirs()[1].getImage();

        // Append it to the Vector of instantiated modules
        currentRelatives.directlyInstantiatedModuleNames.addElement(instanceModuleName);
      }
      else if ( def.getImage().equals("N_ModuleDefinition") ) {
	// We encounter a module definition (i.e. D(x,y) == INSTANCE Modname WITH ...)
	// that counts as an instance also
        TreeNode[] instanceHeirs = def.heirs();
        int nonLocalInstanceNodeIX = 2;

        // The modifier "LOCAL" may or may not appear in the syntax
        // tree; if so, offset by 1.
        if (instanceHeirs[0].getImage().equals("LOCAL")) {
          nonLocalInstanceNodeIX = 3;
	}

        // Find the name of the module being instantiated
        String instanceModuleName = instanceHeirs[nonLocalInstanceNodeIX].heirs()[1].getImage();

        // Append it to the Vector of instantiated modules
        currentRelatives.directlyInstantiatedModuleNames.addElement(instanceModuleName);
      }
      else {
	// We also need to look for INSTANCE in Let constructs
	this.getInstanceInLet(def, currentRelatives);
      }
    } // end for

    /***
    RuntimeConfiguration.get().getErrStream().println("  Leaving determineModuleRelationships for currentModule " +
		       currentModule.getName() +  ", with parent " +
		       (parent != null ? parent.getName() : "null") + "\n>");
    RuntimeConfiguration.get().getErrStream().println("ModuleRelationships =" +  spec.getModuleRelationships());
    ***/
  }

  private void getInstanceInLet(TreeNode treeNode, ModuleRelatives currentRelatives) {
    TreeNode[] children = treeNode.heirs();  

    if (treeNode.getImage().equals("N_LetIn")) {
      TreeNode[] syntaxTreeNode = children[1].heirs();

      for (int i = 0; i < syntaxTreeNode.length; i++) {
	TreeNode def = syntaxTreeNode[i];

	if (def.getImage().equals("N_ModuleDefinition")) {
	  // We encounter a module definition (i.e. D(x,y) == INSTANCE Modname WITH ...)
	  // that counts as an instance also
	  TreeNode[] instanceHeirs = def.heirs();
	  int nonLocalInstanceNodeIX = 2;

	  // Find the name of the module being instantiated
	  String instanceModuleName = instanceHeirs[nonLocalInstanceNodeIX].heirs()[1].getImage();

	  // Append it to the Vector of instantiated modules
	  currentRelatives.directlyInstantiatedModuleNames.addElement(instanceModuleName);
	}
	else {
	  TreeNode[] defChildren = def.heirs();
	  for (int j = 0; j < defChildren.length; j++) {
	    this.getInstanceInLet(defChildren[j], currentRelatives);
	  }
	}
      }
    }
    else {
      for (int i = 0; i < children.length; i++) {
	this.getInstanceInLet(children[i], currentRelatives);
      }
    }
  }

}

