// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.parser;

import java.util.Stack;
import tlasany.st.SyntaxTreeConstants;
import tlasany.error.*;

public class BracketStack implements LogCategories, SyntaxTreeConstants {

  private Stack stack = new Stack( );
  private int [] classes = new int[ NULL_ID ];
  private int classIndex = 0;

  public void newClass() { classIndex++; }
  public void registerInCurrentClass( int k ) {
    if ( classIndex == 0) classIndex++;
    classes[k] = classIndex;
  }

  BracketStack() {
    stack.push( new StackElement(0, -1 )) ;
  }

  void newReference( int o, int kind ) {
     stack.push( new StackElement( o, classes[ kind ] )) ;
  }

   void popReference( ) {
     stack.pop();
  }

  boolean onReference( int o, int kind ) {
    StackElement se = (StackElement)  stack.peek();
Log.log(bracketStackLog, "--- onReference, " + o + " " + classes [ kind ] + "  " + se.Kind + " " + se.Offset);
    return
      classes[ kind ] == se.Kind && se.Offset == o;
  }

  boolean belowReference( int o ) {
    StackElement se = (StackElement)  stack.peek();
Log.log(bracketStackLog, "--- belowReference, " + o + " " + se.Offset);
    return
      se.Offset > o;
  }

  boolean aboveReference( int o ) {
    StackElement se = (StackElement)  stack.peek();
Log.log(bracketStackLog, "--- aboveReference, " + o + " " + se.Offset);
    return
      se.Offset-1 < o; /* careful here. o is a beginning column, while
                          Offset is the end column of the token ...\/ ou .../\
                          on utilise - 1 pour comparer au de'but de la partie
                          significative du symbole.
                          De cette manire, le comportement ne change pas si
                          on utilise uniquement la forme non prfixe des
                         symboles */
  }
}
