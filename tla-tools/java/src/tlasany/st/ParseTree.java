// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.st;

// Implemented only by parser.TLAPlusParser

// Outside of the parser package this is used only by 
//   drivers.ParseUnit and drivers.SyntaxTreePrinter

public  interface ParseTree {
  public String[] dependencies();
  public TreeNode rootNode();
  public String   moduleName();
  public boolean  parse();
}
