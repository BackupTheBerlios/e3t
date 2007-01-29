// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.error;

import org.zambrovski.tla.RuntimeConfiguration;

public class Log implements LogCategories {

  private static boolean logIndex[];

  public static void initialize () {
    logIndex = new boolean[ lastCategory ];

    for (int i = 0; i < logIndex.length; i++) logIndex[i] = false;
  }

  public static void removeFromLog( int logClass ) {
    if (logClass == allLog) {
       for (int i = 0; i < logIndex.length; i++) logIndex[i] = false;
    } else if (logClass < logIndex.length) {
      logIndex[logClass] = false;
    }
  }

  public static void addToLog( int logClass ) {
    if (logIndex == null) initialize();
    if (logClass == allLog) {
       for (int i = 0; i < logIndex.length; i++) logIndex[i] = true;
    } else if (logClass < logIndex.length) {
      logIndex[logClass] = true;
    }
  }

  public static void log( int kind, String msg ) {
    if (logIndex == null) initialize();
    if (kind < logIndex.length && logIndex[kind] )
      RuntimeConfiguration.get().getOutStream().println( msg );
  }
  
  public static void logLine( int kind, String msg ) {
    if (logIndex == null) initialize();
    if (kind < logIndex.length && logIndex[kind] )
        RuntimeConfiguration.get().getOutStream().print( msg );
  }
}
