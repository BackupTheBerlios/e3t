// Copyright (c) 2003 Compaq Corporation.  All rights reserved.

package tlasany;

import org.zambrovski.tla.RuntimeConfiguration;
import org.zambrovski.tla.tlasany.Runner;

/**
 * SANY is a shell class to call the main driver method of SANY
 */

public class SANY {

  public static final void main(String[] args) 
  {
      RuntimeConfiguration.get().getOutStream().println("Please use org.zambrovski.tla.tlasany.Runner");
      Runner.main(args);
  }
  
}
