// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon Jan 29 16:21:11 PST 2001 by yuanyu

package tlc.tool;

import org.zambrovski.tla.RuntimeConfiguration;

import util.Assert;

public class TLAClass {
  /* Load a class from a file. */
  private String pkg;

  public TLAClass(String pkg) {
    if (pkg.length() != 0 &&
	pkg.charAt(pkg.length()-1) != '.') {
      this.pkg = pkg + '.';
    }
    else {
      this.pkg = pkg;
    }
  }

  public synchronized Class loadClass(String name) {
    Class cl = null;
    try {
      try {
	cl = Class.forName(name);
      }
      catch (Exception e) { /*SKIP*/ }
      if (cl == null) {
	try {
	  cl = Class.forName(this.pkg + name);
	}
	catch (Exception e) { /*SKIP*/ }
      }
    }
    catch (Throwable e) {
      Assert.fail("Found a Java class for module " + name + ", but unable to read\n" +
		  "it as a Java class object. " + e.getMessage());
    }
    return cl;
  }

  public static void main(String argv[]) {
    TLAClass tc = new TLAClass("tlc.module");
    Class c = tc.loadClass("Strings");  // must set CLASSPATH correctly
    RuntimeConfiguration.get().getErrStream().println(c);
  }
  
}
