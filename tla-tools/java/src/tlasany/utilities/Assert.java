// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.utilities;

import org.zambrovski.tla.RuntimeConfiguration;

public class Assert {

  public final static void assertion(boolean b) {
    if (!b) {
      RuntimeConfiguration.get().getErrStream().println("assertion failed:");
      Throwable e = new Throwable();
      e.printStackTrace();
      System.exit(1);
    }
  }

  public final static void fail(String msg) {
    RuntimeConfiguration.get().getErrStream().println("Error: " + msg);
    Throwable e = new Throwable();
    e.printStackTrace();
    System.exit(1);
  }
  
}
