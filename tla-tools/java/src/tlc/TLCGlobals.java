// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Mar  6 23:02:34 PST 2002 by yuanyu

package tlc;

import tlasany.semantic.FrontEnd;

public class TLCGlobals {

  // The current version of TLC
  public static String versionOfTLC = "Version 2.0 of Jun 10, 2003";

  // The bound for set enumeration
  public static int enumBound = 100000;

  // The bound for the cardinality of a set
  public static int setBound = 1000000;

  // Number of concurrent workers
  private static int numWorkers = 1;

  // Enable collecting coverage information
  public static boolean coverage = false;
  public static int coverageInterval = 1800000;

  public synchronized static void setNumWorkers(int n) {
    numWorkers = n;
  }

  public synchronized static int getNumWorkers() {
    return numWorkers;
  }

  public synchronized static void incNumWorkers(int n) {
    numWorkers += n;
  }

  // Depth for depth-first iterative deepening
  public static int DFIDMax = -1;
  
  // Continue running even when invariant is violated
  public static boolean continuation = false;
  
  // Suppress warnings report if true
  public static boolean warn = true;

  // The time interval to report progress
  public static final int progressInterval = 300000;

  // The time interval to checkpoint.
  public static final long chkptDuration = 1800000;

  // The meta data root.
  public static final String metaRoot = "states";
  public static String metaDir = null;

  // The list of fingerprint servers.
  public static String[] fpServers = null;

  // The tool id number for TLC.
  public static int ToolId = FrontEnd.getToolId();

  // Various tracing options.
  public static boolean traceSAT;

}
