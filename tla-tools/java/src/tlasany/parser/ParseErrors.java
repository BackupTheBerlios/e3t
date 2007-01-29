// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.parser;

import java.util.Vector;

public final class ParseErrors implements tlasany.st.ParseErrors {
  private Vector loe;

  ParseErrors() { loe = new Vector(); };
  final boolean empty() { return loe.isEmpty(); }

  final void push( ParseError pe ) {
    loe.addElement( pe );
  }

  public final tlasany.st.ParseError[] errors() {
    tlasany.st.ParseError[] pes = new tlasany.st.ParseError[ loe.size() ];
    for (int lvi = 0; lvi < pes.length; lvi++ )
      pes[ lvi ] = (ParseError)loe.elementAt( lvi );
    return pes;
  }
}
