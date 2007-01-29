// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlasany.semantic;

import tlasany.st.Location;
import tlasany.utilities.Vector;

public class Errors {

  private boolean succeed = true;

  private int    numAborts   = 0;
  private int    numErrors   = 0;
  private int    numWarnings = 0;

  private Vector warnings = new Vector();
  private Vector errors   = new Vector();
  private Vector aborts   = new Vector();
  
  
 


  public final void addWarning( Location loc, String str ) { 
    if (loc == null) loc = Location.nullLoc;

    int i;
    for (i = warnings.size()-1; i >= 0; i--) {
      if ( (loc.toString() + "\n\n" + str).equals( warnings.elementAt(i) ) ) break;
    }

    if ( i < 0) {
      warnings.addElement( loc.toString() + "\n\n"+ str );
      numWarnings++;
    }
  }


  public final void addError(Location loc, String str) {
    if (loc == null) loc = Location.nullLoc;

    int i;
    for (i = errors.size()-1; i >= 0; i--) {
      if ( (loc.toString() + "\n\n" + str).equals( errors.elementAt(i) ) )  break;
    }

    if ( i < 0) {
      errors.addElement( loc.toString() + "\n\n"+ str );
      numErrors++;
    }
    succeed = false;

  }
  

  public final void addAbort(Location loc, String str, boolean abort) throws AbortException {
    String errMsg = loc.toString() + "\n\n" + str;
    int i;
    for (i = aborts.size()-1; i >= 0; i--) {
      if (errMsg.equals(aborts.elementAt(i))) break;
    }
    if (i < 0) {
      aborts.addElement(errMsg);
      numAborts++;
    }
    succeed = false;

    if (abort){
      // RuntimeConfiguration.get().getOutStream().println(this.toString());
      throw new AbortException(); 
    }
  }

  public final void addAbort(Location loc, String str ) throws AbortException {
    addAbort(loc, str, true);
  }


  public final void addAbort(String str, boolean abort) throws AbortException {
    addAbort(Location.nullLoc, str, abort);
  }


  public final void addAbort(String str) throws AbortException {
    addAbort(Location.nullLoc, str, true);
  }

  public final boolean isSuccess()             { return succeed; }

  public final boolean isFailure()             { return !succeed; }

  public final int     getNumErrors()          { return numErrors; }

  public final int     getNumAbortsAndErrors() { return numAborts + numErrors; }

  public final int     getNumMessages()        { return numAborts + numErrors + numWarnings; }

  public final String  toString()  { 
    StringBuffer ret = new StringBuffer("");

    ret.append((numAborts > 0) ? "*** Abort messages: " + numAborts + "\n\n" : "");
    for (int i = 0; i < aborts.size(); i++)   {
      ret.append(aborts.elementAt(i) + "\n\n\n");
    }

    ret.append((numErrors > 0) ? "*** Errors: " + numErrors + "\n\n" : "");
    for (int i = 0; i < errors.size(); i++)   {
      ret.append(errors.elementAt(i) + "\n\n\n");
    }

    ret.append((numWarnings > 0) ? "*** Warnings: " + numWarnings + "\n\n" : "");
    for (int i = 0; i < warnings.size(); i++) {
      ret.append(warnings.elementAt(i) + "\n\n\n");
    }

    return ret.toString();
  }
	    
}
