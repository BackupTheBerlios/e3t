// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.error;

import org.zambrovski.tla.RuntimeConfiguration;

public class ErrorRegistry {
  static private int ErrorCount = 0;
  static private int WarningCount = 0;

  static public void addError( String description ) {
    ErrorCount++;
    if (description !=null) RuntimeConfiguration.get().getOutStream().println( "\n" + "ERROR: " + description );
  }

  static public void addWarning(  String description ) {
    WarningCount++;
    if (description !=null) RuntimeConfiguration.get().getOutStream().println( "\n" + "WARNING: " + description);
  }

  static public boolean isError() {
    if ( ErrorCount > 0 ) {
        RuntimeConfiguration.get().getOutStream().print( "Parsing failed with "  + ErrorCount );
      if (ErrorCount == 1) RuntimeConfiguration.get().getOutStream().print(" error");
      else   RuntimeConfiguration.get().getOutStream().print(" errors");
      if (WarningCount > 0) {
          RuntimeConfiguration.get().getOutStream().print( " and " + WarningCount );
        if (WarningCount == 1) RuntimeConfiguration.get().getOutStream().println(" warning");
        else RuntimeConfiguration.get().getOutStream().println(" warnings");
      } else
          RuntimeConfiguration.get().getOutStream().println();
      return true;
    } else
      return false;
  }

  static public boolean isErrorAndReset() {
    if (ErrorCount > 0 ) {
      ErrorCount = 0;
      return true;
    } else
      return false;
  }
}

