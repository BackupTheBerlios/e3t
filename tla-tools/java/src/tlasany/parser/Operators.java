// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.parser;

import java.util.Enumeration;
import java.util.Hashtable;

import org.zambrovski.tla.RuntimeConfiguration;

import util.UniqueString;

public class Operators {
  static public int assocNone = 0;
  static public int assocLeft = 1;
  static public int assocRight = 2;

  static public int nofix = 0;
  static public int prefix = 1;
  static public int postfix = 2;
  static public int infix = 3;
  static public int nfix = 4;// \X

  static Hashtable DefinitionTable = new Hashtable();
  static Hashtable BuiltinTable = new Hashtable();

  static public void addOperator( UniqueString name, Operator op ) {
    DefinitionTable.put( name, op );
  }

  static public Operator getOperator( UniqueString name ) {
    return (Operator) DefinitionTable.get( name );
  }

  static public Operator getMixfix( Operator op ) {
     if (op.isPrefix()) return op;
     else {
       UniqueString id = UniqueString.uniqueStringOf( op.getIdentifier().toString() + ".");
       return (Operator) DefinitionTable.get( id );
     }
  }
  
  static public boolean existsOperator( UniqueString name ) {
    return ( DefinitionTable.get( name ) != null );
  }

  static public void addSynonym( UniqueString template, UniqueString match ) {
    /*
       do make sure that the operator already exists.
       We make the new definition point to the other one.
    */
    Operator n = (Operator) DefinitionTable.get( match );
    if (n != null) {
      DefinitionTable.put( template, n);
    } /* else {
       error
    } */
  }
  
  static public UniqueString resolveSynonym( UniqueString name ) {
    Operator n = (Operator) DefinitionTable.get( name );
    if ( n == null ) return name;
    else return n.getIdentifier();
  }

  static public void addBuiltinAssoc( UniqueString symbol, UniqueString builtin ) {
    BuiltinTable.put( symbol, builtin );
  }

  static public UniqueString getBuiltinAssoc( UniqueString symbol ) {
    /* first, resolve synonyms */
    Operator n = (Operator) DefinitionTable.get(symbol);
    if (n != null) {
      UniqueString name = n.getIdentifier(); /* can't be null */
      /* then lookup solution */
      return (UniqueString) (BuiltinTable.get(name));
    } else
      return null;
  }

/* debugging help */
  static public void printTable() {
      RuntimeConfiguration.get().getOutStream().println("printing Operators table");
    Enumeration varEnum = DefinitionTable.keys();
    while( varEnum.hasMoreElements() ) { RuntimeConfiguration.get().getOutStream().println("-> " + ((UniqueString)varEnum.nextElement()).toString() ); }
  }

// shouldn't be necessary
//  static public Operator operatorFromASTNode ( ASTNode tn ) {
//     Operator n = (Operator) DefinitionTable.get( tn.getToken().image );
//        return n;
//  }

}
