// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Last modified on Wed Aug 23 13:22:02 PDT 2000 by yuanyu
//      modified on Wed Jun 16 14:36:34 EDT 1999 by tuttle

package tlc.pprint;

import org.zambrovski.tla.RuntimeConfiguration;

public class PrettyPrint {

  public static String mypp(String value, int width) {
    try {
      Node   tree   = Parse.parse(value,0);
      String format = Format.format(tree, width, 0, "");
      return format;
    }
    catch (Exception e) { return value; }
  }
  
  public static String pp(String value, int width) {
    return pp(value, width, "");
  }

  public static String pp(String value, int width, String padding) {
    try {
      Node   tree   = Parse.parse(value,0);
      String format = Format.format(tree,width,0,padding);
      return format;
    }
    catch (ParseException e) {
      RuntimeConfiguration.get().getErrStream().println("TLC Bug: error while parsing " + value + "\n" + 
			 e.getMessage());
      return value;
    }
    catch (FormatException e) {
      RuntimeConfiguration.get().getErrStream().println("TLC Bug: error while formating " + value + "\n" + 
			 e.getMessage()); 
      return value;
    }
  }

  public static void main(String[] argv) {
    String value = argv[0];
    int    width = Integer.parseInt(argv[1]);
    String padding;

    if (argv.length > 2) {
      padding = argv[2];
    } else {
      padding = "";
    }

    for (int i = 0; i < width; i++) {
      RuntimeConfiguration.get().getOutStream().print("*");
    }
    RuntimeConfiguration.get().getOutStream().println("");
    RuntimeConfiguration.get().getOutStream().println(pp(value,width,padding));
    for (int i = 0; i < width; i++) {
      RuntimeConfiguration.get().getOutStream().print("*");
    }
    RuntimeConfiguration.get().getOutStream().println("");

    return;
  }

}
