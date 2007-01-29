// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Last modified on Wed Jul 25 11:56:59 PDT 2001 by yuanyu

package util;

import org.zambrovski.tla.RuntimeConfiguration;

public class Assert extends RuntimeException {

  /* The set of warned source locations. */
  private static Set msgSet = new Set();

  public Assert(String msg) { super(msg); }
  
  public static void fail() {
    throw new RuntimeException();
  }

  public static void fail(String msg) {
    throw new RuntimeException(msg);
  }

  public static void check(boolean b) {
    if (!b) throw new RuntimeException();
  }

  public static void check(boolean b, String msg) {
    if (!b) throw new RuntimeException(msg);
  }

  public static void warn(String msg) {
    throw new Assert(msg);
  }

  public static void printWarning(boolean warn, String msg) {
    if (warn && msgSet.put(msg) == null) {
      RuntimeConfiguration.get().getErrStream().println("Warning: " + msg);
      RuntimeConfiguration.get().getErrStream().println("(Use the -nowarning option to disable this warning.)");
    }
  }

  public static void printStack(boolean b) {
    if (b) {
      Exception e = new Exception();
      e.printStackTrace();
    }
  }
  
}
