// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.explorer;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Enumeration;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.semantic.*;
import tlasany.explorer.ExploreNode;
import tlasany.utilities.Vector;
import util.UniqueString;


// This class implements the explorer tool for traversing
// and examining the semantic graph and associated data structures
// of a TLA specification

public class Explorer {

    public  Generator         generator;

    // Next three vars used in reading commands from keyboard
    private InputStreamReader inStream = new InputStreamReader(System.in);
    private final int         inCapacity = 100;
    private StringBuffer      input = new StringBuffer(inCapacity);
    private int               lineLength;

    // semNodesTable contains various nodes in the semantic graph keyed 
    // by their UIDs
    private Hashtable         semNodesTable = new Hashtable();

    // variables used in parsing commands
    private int               ntokens;
    private StringTokenizer   inputTokens;
    private String            inputString;
    private String            firstToken,secondToken;
    private Integer           icmd,icmd2 = null;
    private ExploreNode       obj;

    private ExternalModuleTable       mt;


    // Constructor
    public Explorer (ExternalModuleTable mtarg) {

      mt = mtarg;

    }


    /* Reads one line from "inStream" into "input".
       Returns "false" if there is an EOF; "true" otherwise
    */

    private boolean getLine() {
      try {
        lineLength=0;
        input.setLength(inCapacity);
	do { 
          input.setCharAt(lineLength,(char)inStream.read()); 
          lineLength++;
	}
	while (input.charAt(lineLength-1) != '\n' & lineLength < inCapacity );
        input.setLength(lineLength);
      } 
      catch (EOFException e) {
	  return false;
      }
      catch (IOException e) {
	RuntimeConfiguration.get().getOutStream().println("***I/O exception on keyboard input; " + e);
	System.exit(-1);
      }
      if (lineLength >= inCapacity) {
        RuntimeConfiguration.get().getOutStream().println("Input line too long; line limited to " + inCapacity
                           + " chars.  Line ignored.");
        input.setLength(0);
      }

      return true;
    }



    // Integer command
    private void printNode(int depth) {

      // See if the object requested is already in the table
      if ((obj = (ExploreNode)semNodesTable.get(icmd)) != null) {
	//Print tree to depth of icmd2
        RuntimeConfiguration.get().getOutStream().println(((ExploreNode)obj).toString(depth));
        RuntimeConfiguration.get().getOutStream().print("\n" + ((ExploreNode)obj).levelDataToString());
      } else { 
        // object requested is not in semNodesTable 
        RuntimeConfiguration.get().getOutStream().println("No such node encountered yet");
      }
    } // end method


    private void lookUpAndPrintSyntaxTree(String symbName) {

      Vector symbolVect = new Vector(8);  // Initial room for 8 symbols with same name


      // Collect in Vector symbols all SymbolNodes in the semNodesTable whose name == symbName

      for ( Enumeration varEnum = semNodesTable.elements(); varEnum.hasMoreElements(); ) {

        Object semNode = varEnum.nextElement();

        if ( semNode instanceof SymbolNode && 
             ((SymbolNode)semNode).getName() == UniqueString.uniqueStringOf(symbName) ) {

          symbolVect.addElement(semNode);

        }
      }

      // Print them all
      for (int i = 0; i < symbolVect.size(); i++ ) {
        SymbolNode sym = (SymbolNode)(symbolVect.elementAt(i));
        ((SemanticNode)(sym)).getTreeNode().printST(0);
        RuntimeConfiguration.get().getOutStream().println();
      }

    }


    private void lookUpAndPrintDef(String symbName) {

      Vector symbolVect = new Vector(8);  // Initial room for 8 symbols with same name


      // Collect in Vector symbols all SymbolNodes in the semNodesTable whose name == symbName

      for ( Enumeration varEnum = semNodesTable.elements(); varEnum.hasMoreElements(); ) {

        Object semNode = varEnum.nextElement();

        if ( semNode instanceof SymbolNode && 
             ((SymbolNode)semNode).getName() == UniqueString.uniqueStringOf(symbName) ) {

          symbolVect.addElement(semNode);

        }
      }

      // Print them all
      for (int i = 0; i < symbolVect.size(); i++ ) {
        SymbolNode sym = (SymbolNode)(symbolVect.elementAt(i));
        if (sym instanceof OpDefOrDeclNode) {
	  if ( ((OpDefOrDeclNode)sym).getOriginallyDefinedInModuleNode() != null ) {
            RuntimeConfiguration.get().getOutStream().print( "Module: " + ((OpDefOrDeclNode)sym).getOriginallyDefinedInModuleNode().getName() + "\n" );
	  } else {
            RuntimeConfiguration.get().getOutStream().print( "Module: " + "null" + "\n");
	  }
	} else if (sym instanceof FormalParamNode) {
          RuntimeConfiguration.get().getOutStream().print( "Module: " + ((FormalParamNode)sym).getModuleNode().getName() );
	}
        RuntimeConfiguration.get().getOutStream().println( ((ExploreNode)(symbolVect.elementAt(i))).toString(100) );
        RuntimeConfiguration.get().getOutStream().println();
      }

    }

    private void levelDataPrint(String symbName) {

      Vector symbolVect = new Vector(8);  // Initial room for 8 symbols with same name


      // Collect in Vector symbols all SymbolNodes in the semNodesTable whose name == symbName

      for ( Enumeration varEnum = semNodesTable.elements(); varEnum.hasMoreElements(); ) {

        Object semNode = varEnum.nextElement();

        if ( semNode instanceof SymbolNode && 
             ((SymbolNode)semNode).getName() == UniqueString.uniqueStringOf(symbName) ) {

          symbolVect.addElement(semNode);

        }
      }

      // Print them all
      for (int i = 0; i < symbolVect.size(); i++ ) {
        SymbolNode sym = (SymbolNode)(symbolVect.elementAt(i));
        if (sym instanceof OpDefOrDeclNode) {
	  if ( ((OpDefOrDeclNode)sym).getOriginallyDefinedInModuleNode() != null ) {
            RuntimeConfiguration.get().getOutStream().print( "Module: " + ((OpDefOrDeclNode)sym).getOriginallyDefinedInModuleNode().getName() + "\n" );
	  } else {
            RuntimeConfiguration.get().getOutStream().print( "Module: " + "null" + "\n" );
	  }
	} else if (sym instanceof FormalParamNode) {
          RuntimeConfiguration.get().getOutStream().print( "Module: " + ((FormalParamNode)sym).getModuleNode().getName() + "\n" );
	}
        RuntimeConfiguration.get().getOutStream().println( ((ExploreNode)(sym)).levelDataToString() );
        RuntimeConfiguration.get().getOutStream().println();
      }

    }


    private void executeCommand() throws ExplorerQuitException {

      // At this point icmd (firsToken) may be null, but icmd2 
      // (second token) is always non-null

      // Integers as commands start printing at the node having icmd == UID; 
      // non-integer commands do something else
        
      if (icmd != null) { // first token is an integer

        printNode(icmd2.intValue());

      } else {      // the first token is not an integer

        // non-integer commands
        if (firstToken.toLowerCase().startsWith("qu")) { 
          // "quit" command
          throw new ExplorerQuitException();

        } else if (firstToken.toLowerCase().equals("mt")) {

          // Print the semantic graph, rooted in the Module Table
	  // excluding built-ins and ops defined in module Naturals
	  if (icmd2 != null) { 
            mt.printExternalModuleTable(icmd2.intValue(),false);
          } else {
            mt.printExternalModuleTable(2, false);
          }

        } else if (firstToken.toLowerCase().equals("mt*")) {

          // Print the semantic graph, rooted in the Module Table
	  // including builtins and ops defined in Naturals
	  if (icmd2 != null) { 
            mt.printExternalModuleTable(icmd2.intValue(),true);
          } else {
            mt.printExternalModuleTable(2,true);
          }

        } else if (firstToken.toLowerCase().startsWith("cst")) {
          printSyntaxTree();

        } else if (firstToken.toLowerCase().startsWith("s")) {
	  if (secondToken != null) {
            lookUpAndPrintSyntaxTree(secondToken);
	  } else {
            RuntimeConfiguration.get().getOutStream().println("***Error: You must indicate what name you want to print the syntax tree of.");
	  }

        } else if (firstToken.toLowerCase().startsWith("d")) {
	  if (secondToken != null) {
            lookUpAndPrintDef(secondToken);
	  } else {
            RuntimeConfiguration.get().getOutStream().println("***Error: You must indicate what name you want to print the definition of.");
	  }

        } else if (firstToken.toLowerCase().startsWith("l")) {
	  if (secondToken != null) {
            levelDataPrint(secondToken);
	  } else {
            RuntimeConfiguration.get().getOutStream().println("***Error: You must indicate what name you want to print the level data of.");
	  }

        } else {
	  // unknown command
          RuntimeConfiguration.get().getOutStream().println("Unknown command: " + firstToken.toString());
          return;
        }

      } // end else

    }


    private void parseAndExecuteCommand() throws ExplorerQuitException {

      icmd = null;
      icmd2 = null;
      ntokens = 0;

      // Do nothing if cmd line contains no tokens
      if (!inputTokens.hasMoreElements()) return;

      // Process first token
      ntokens++;
      firstToken = (String)(inputTokens.nextElement());

      // Try parsing first token as an Integer
      try {
        icmd = Integer.valueOf(firstToken);
      }
      catch (Exception e) { }

      //Process second token (if present)
      if (inputTokens.hasMoreElements()) {
        ntokens++;
        secondToken = (String)(inputTokens.nextElement());
  
        // Try parsing second token as an Integer
        try {
          icmd2 = Integer.valueOf(secondToken);
        }
        catch (Exception e) { }
      }

      // A single token command defaults the depth to 20, except for
      // "mt" command, which defaults to 2
      if (ntokens < 2 || (icmd2 != null && icmd2.intValue() < 0)) {
	if (firstToken.toLowerCase().startsWith("mt")) {
          icmd2 = new Integer(2);
        } else {
          icmd2 = new Integer(4);
        }
      }

      if (inputTokens.hasMoreElements()) {
        RuntimeConfiguration.get().getOutStream().println("Command has too many tokens");
        return;
      }

      executeCommand();

    } // end method


    public void printSyntaxTree () {

      Integer key;

      // Prepare to iterate over ExternalModuleTable entries
      Iterator modules = mt.moduleHashTable.values().iterator();
      ExternalModuleTable.ExternalModuleTableEntry mte;

      // For each entry ExternalModuleTableEntry mte in the ExternalModuleTable mt ... 
      while (modules.hasNext()) {
        key = new Integer(-1);

        mte = (ExternalModuleTable.ExternalModuleTableEntry)(modules.next());

        // Did the module parse correctly?
        if (mte != null) {
          if (mte.getModuleNode() != null ) {
            key = new Integer(mte.getModuleNode().getUid());

            // Make an entry in the semNodesTable for this ModuleNode
            semNodesTable.put(key,mte.getModuleNode());

            // Print the concrete syntax tree for this ExternalModuleTableEntry
            RuntimeConfiguration.get().getOutStream().println("\n*** Concrete Syntax Tree for Module " + key);

            tlasany.st.TreeNode stn = mte.getModuleNode().getTreeNode();
            stn.printST(0);    // Zero indentation level

            RuntimeConfiguration.get().getOutStream().println("\n*** End of concrete syntax tree for Module " 
                               + key);
          } else {
            RuntimeConfiguration.get().getOutStream().println("\n*** Null ExternalModuleTableEntry.  " +
                      "\n*** Next module did not parse, and cannot be printed.");
          }
        } else {
          RuntimeConfiguration.get().getOutStream().println("*** Null SemanticNode in ExternalModuleTableEntry.  " +
                      "/n*** Next module did not parse, and cannot be printed.");
        }

      }
    }


    public void main() throws ExplorerQuitException {

      if (mt==null) {
        RuntimeConfiguration.get().getOutStream().println("*** module table == null in Explorer.main() ***");
        return;
      }

      // Get all semNodes in semNodeTable
      mt.walkGraph(semNodesTable);

      // Print initial user input prompt
      RuntimeConfiguration.get().getOutStream().println("\n\n*** TLA+ semantic graph exploration tool v 1.0 (DRJ)");
      RuntimeConfiguration.get().getOutStream().print("\n>>");  

      // Main command interpreter loop
      while (getLine()) {

        inputTokens = new StringTokenizer(input.toString());

        parseAndExecuteCommand();

        // Print next user prompt
        RuntimeConfiguration.get().getOutStream().print("\n>>");

      } // end while

   } // end main() method

} // end class
