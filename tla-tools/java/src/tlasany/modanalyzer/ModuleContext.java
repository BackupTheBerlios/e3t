// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.modanalyzer;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An instance of this class is the ModuleContext for a module, i.e. the mapping between
 * module String names and other ModulePointers elsewhere in the specification that they 
 * are bound to.
 * 
 * There is no distinction made between a String that is not the name of a module known
 * in this context, and an as-yet unbound name that is known.  In both cases resolve()
 * returns null.
 */
public class ModuleContext {

  Hashtable context = new Hashtable();

  /** 
   * Find the ModulePointer that the String modName resolves to;
   * Return null if either modName is not found in the context or
   * if it is found and resolves to null, i.e. is not yet resolved.
   */

  ModulePointer resolve( String modName ) {
    return (ModulePointer)context.get(modName);
  }

  /**
   * Bind a module name to a particular ModulePointer, replacing any binding
   * that is already there to the same modName.
   */
  void bind( String modName, ModulePointer modPointer) {
    context.put(modName,modPointer);
  }

 /**
   * Bind a module name to a particular ModulePointer iff that name is not
   * already bound; otherwise no-op.
   */
  void bindIfNotBound( String modName, ModulePointer modPointer) {
    if (context.get(modName) == null) context.put(modName,modPointer);
  }

  /**
   * Add elements of unionee ModuleContext to THIS ModuleContext,
   * without overwriting in cases where keys overlap between THIS and unionee
   */
  void union (ModuleContext unionee) {

    Enumeration enum = unionee.context.keys();
    while ( enum.hasMoreElements() ) {
      String key = (String)enum.nextElement();
      this.bindIfNotBound(key,unionee.resolve(key));
    }

  }

  public String toString() {
    String ret = "Context:\n";
    Enumeration enum = context.keys();

    while (enum.hasMoreElements()) {
      String key = (String)enum.nextElement();
      ModulePointer modPointer = (ModulePointer)context.get(key);

      ret = ret + "  " + key + "-->" + (modPointer != null ? modPointer.toStringAbbrev() : "null");
    }

    return ret;

  } // end toString()
    

} // end class
