// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.parser;

import tlasany.utilities.Vector;
import util.UniqueString;

public class OperatorStack implements tlasany.st.SyntaxTreeConstants {
  private Vector StackOfStack = new Vector (10);
  private SyntaxTreeNode VoidSTNode = new SyntaxTreeNode( );

  private Vector CurrentTop = null;
  private ParseErrors PErrors;

  private Operator fcnOp;

  public OperatorStack( ParseErrors pe ) {
    PErrors = pe;
    fcnOp = Operators.getOperator( UniqueString.uniqueStringOf("[") ); }

// could be optimized to reuse memory
  public final void newStack() {
    CurrentTop = new Vector( 20 );
    StackOfStack.addElement( CurrentTop );
  }

// could be optimized to reuse memory
  public final void popStack() {
    StackOfStack.removeElementAt( StackOfStack.size()-1 );
    if (StackOfStack.size() > 0 )
      CurrentTop = (Vector) StackOfStack.elementAt( StackOfStack.size() - 1 );
    else
      CurrentTop = null;
  }

/*
  We use the prec and succ relations, as described in the formal specs of the grammar.
  Their definition is embedded in the Operator class.
  One way to look at prec and succ is in term of their effect on the stack:
  if left \prec right, then shift; if left \succ right, then reduce.
  There are caveats in the case of prefix or postfix operators.
*/

// returns true if the top of the stack holds a prefix or infix op
// used to distinguish [] from [] - or vice versa ?
// also used to distinguish junction from \/ or /\
  final public boolean preInEmptyTop() {
    if (CurrentTop == null) return true; // empty or holding nothing - lookahead compliant
    if (CurrentTop.size() == 0)
      return true;
    else {
      Operator op  = ((OSelement) CurrentTop.elementAt( CurrentTop.size()-1 )).getOperator();
      if (op != null)
        return op.isPrefix() || op.isInfix();
      else
        return false;
    }
  }

  final private void reduceInfix( Operator op ) {
// RuntimeConfiguration.get().getErrStream().println("infix reduction");
    int n = CurrentTop.size()-1;
//    SyntaxTreeNode localTN = new InfixExprNode();
    if (n>=3) {
      SyntaxTreeNode opNode  = ((OSelement) CurrentTop.elementAt( n-2)).getNode();
      SyntaxTreeNode leftOp  = ((OSelement) CurrentTop.elementAt( n-3)).getNode();
      SyntaxTreeNode rightOp = ((OSelement) CurrentTop.elementAt( n-1)).getNode();
      CurrentTop.removeElementAt(n-1);
      CurrentTop.removeElementAt(n-2);
      SyntaxTreeNode lSTN;

      if (op.isNfix() && leftOp.isKind( N_Times ) ) {
        SyntaxTreeNode children[] = (SyntaxTreeNode[])leftOp.heirs();
        SyntaxTreeNode newC[] = new SyntaxTreeNode[ children.length + 2];
        System.arraycopy(children, 0, newC, 0, children.length);
        newC[ newC.length-2 ] = opNode;
        newC[ newC.length-1 ] = rightOp;
        lSTN = new SyntaxTreeNode( N_Times, newC );
      } else if (op.isNfix()) { // first \X encountered
        lSTN = new SyntaxTreeNode(N_Times, leftOp , opNode, rightOp );
      } else { // inFix
        lSTN = new SyntaxTreeNode(N_InfixExpr, leftOp , opNode, rightOp );
      }
      CurrentTop.setElementAt(new OSelement(lSTN), n-3);
    }
  }

  final private void reducePrefix() {
// Log.log(evalStackLog, "--- reduce prefix");
// RuntimeConfiguration.get().getErrStream().print("prefix reduction ");
    int n = CurrentTop.size()-1;
//    SyntaxTreeNode localTN = new PrefixExprNode();
    if (n>=2) {
      Operator op = ((OSelement) CurrentTop.elementAt( n-2)).getOperator();
      SyntaxTreeNode opNode = ((OSelement) CurrentTop.elementAt( n-2)).getNode();
// RuntimeConfiguration.get().getErrStream().println( op.getIdentifier() );
//      if ( op.isInfix() )
//        ((GenOpNode)opNode).register( op.getIdentifier() + ".", STable);
//      else
//        ((GenOpNode)opNode).register( op.getIdentifier(), STable);
        SyntaxTreeNode lSTN = new SyntaxTreeNode(N_PrefixExpr,
          ((OSelement) CurrentTop.elementAt( n-2)).getNode(),
          ((OSelement) CurrentTop.elementAt( n-1)).getNode());
//      localTN.addChild( ((OSelement) CurrentTop.elementAt( n-2)).getNode() ) ;
//      localTN.addChild( ((OSelement) CurrentTop.elementAt( n-1)).getNode() ) ;
      CurrentTop.removeElementAt(n-1);
      CurrentTop.setElementAt(new OSelement(lSTN) , n-2);
    }
  }

  final private void reducePostfix() {
// Log.log(evalStackLog, "--- reduce postfix");
// RuntimeConfiguration.get().getErrStream().println("postfix reduction");
    int n = CurrentTop.size()-1;
    SyntaxTreeNode lSTN;
//    SyntaxTreeNode localTN = new PostfixExprNode();
    if (n>=2) {
      Operator op = ((OSelement) CurrentTop.elementAt( n-1)).getOperator();
      SyntaxTreeNode opNode = ((OSelement) CurrentTop.elementAt( n-1)).getNode();
      if (op != fcnOp ) {
//      ((GenOpNode)opNode).register( op.getIdentifier(), STable);
        lSTN = new SyntaxTreeNode(N_PostfixExpr,
          ((OSelement) CurrentTop.elementAt( n-2)).getNode(),
          opNode);
//      localTN.addChild( ((OSelement) CurrentTop.elementAt( n-2)).getNode() ) ;
//      localTN.addChild( opNode ) ;
      } else {
// RuntimeConfiguration.get().getErrStream().println("postfix reduction : FcnOp");
        SyntaxTreeNode eSTN = ((OSelement) CurrentTop.elementAt( n-2)).getNode();
        lSTN = new SyntaxTreeNode( eSTN.getFN(), N_FcnAppl, eSTN, (SyntaxTreeNode[]) (opNode.heirs()) );
      }
      CurrentTop.removeElementAt(n-1);
      CurrentTop.setElementAt(new OSelement(lSTN) , n-2);
    }
  }


// we follow case by case the definitions here.
// returns true if reduction succeeded, or was without effect.
  final public void reduceStack () throws ParseException {
    int n;
    Operator oR, oL;    // left and right operator
    Operator LastOp = null; // used to remember what we last reduced
    OSelement tm0, tm1, tm2;
    int a1, a2;

// RuntimeConfiguration.get().getErrStream().println("reduce Stack called");
    do {
      n = CurrentTop.size()-1; // note !!! n is used as index, not size. XXX lousy identifier.
// RuntimeConfiguration.get().getErrStream().println("loop, size " + n);
      tm0 = (OSelement)CurrentTop.elementAt( n );

      if ( ! tm0.isOperator() ) break;
      oR = tm0.getOperator();
// RuntimeConfiguration.get().getErrStream().println("tm0 est " + oR.toString() + printStack() );
      if        ( oR.isPostfix() ) {
        if ( n == 0 ) {
          throw new ParseException("\n  Encountered postfix op " + oR.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " on empty stack");
        } else {
          tm1 = (OSelement)CurrentTop.elementAt( n-1 );
          if ( tm1.isOperator()) {
            oL = tm1.getOperator();
// RuntimeConfiguration.get().getErrStream().println("tm1 est " + oL.toString() );
            if ( oL.isInfix() || oL.isPrefix() ) {
              throw new ParseException("\n  Encountered postfix op " + oR.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " following prefix or infix op " + oL.getIdentifier() + ".");
            } else { // isPostfixOp - must reduce.
              reducePostfix();
            }
          } else { // tm1 is Expression - what's below ?
            if ( n > 1 ) {
              tm2 = (OSelement)CurrentTop.elementAt( n-2 );
              if ( tm2.isOperator() ) { 
                oL = tm2.getOperator();
                if ( Operator.succ( oL, oR ) ) { // prefix or infix ?
                   if ( oL.isInfix() ) reduceInfix( oL ); else reducePrefix();
                } else if ( ! Operator.prec( oL, oR ) ) {
                  throw new ParseException("Precedence conflict between ops " + oL.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " and " + oR.getIdentifier() + ".");
		} else
		  break;
              } else {
                throw new ParseException(
         "Expression at location " + tm2.getNode().getLocation().toString() +
" and expression at location " + tm1.getNode().getLocation().toString() +
" follow each other without any intervening operator.");
              }
            } else
              break; // nothing this time around.
          }
        }
      } else if ( oR.isPrefix() ) {
        if ( n == 0 ) break; // can't do anything yet
        else {
          tm1 = (OSelement)CurrentTop.elementAt( n-1 );
          if ( tm1.isOperator()) { // prefix, or infix
            oL = tm1.getOperator();
            if ( oL.isPostfix() ) {
              throw new ParseException("\n  Encountered prefix op " + oR.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " following postfix op " + oL.getIdentifier() + ".");
            } else // nothing to do
              break;
          } else { // can't be expression 
            throw new ParseException("\n  Encountered prefix op " + oR.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " following an expression.");
          }
        }
      } else { // oR.isInfix()
        if ( n == 0 ) {
            Operator mixR  = Operators.getMixfix( oR );
            if ( mixR == null )
              throw new ParseException("\n  Encountered infix op " + oR.getIdentifier() + " in block " + tm0.getNode().getLocation().toString() + " on empty stack.");
            else
              break;
        } else {
// RuntimeConfiguration.get().getErrStream().println("infix case: " + oR.getIdentifier());
          tm1 = (OSelement)CurrentTop.elementAt( n-1 );
          if ( tm1.isOperator()) {
            oL = tm1.getOperator();
            if ( oL.isInfix() || oL.isPrefix() ) { // new case for mixfix XXX this is not exhaustive.
              Operator mixR  = Operators.getMixfix( oR );
              if ( mixR == null ) { // is infix
                if (oR == Operator.VoidOperator )
                  throw new ParseException("\n  Missing expression in block " + tm1.getNode().getLocation().toString() + " following prefix or infix op " + oL.getIdentifier() + ".");
                else
                  throw new ParseException("\n  Encountered infix op " + oR.getIdentifier() + " in block " + tm1.getNode().getLocation().toString() + " following prefix or infix op " + oL.getIdentifier() + ".");
              } else if ( Operator.succ( oL, mixR ) || ( oL == mixR && oL.assocLeft() ) ) {
                throw new ParseException("\n  Precedence conflict between ops " + oL.getIdentifier() + " in block " + tm1.getNode().getLocation().toString() + " and " + mixR.getIdentifier() + ".");
              } // else, skip
            } else // no choice
              reducePostfix();
          } else { // tm1 is Expression - what's below ?
// RuntimeConfiguration.get().getErrStream().println("tm1 is expression");
            if ( n > 1 ) {
// RuntimeConfiguration.get().getErrStream().println("deep stack");
              tm2 = (OSelement)CurrentTop.elementAt( n-2 );
              if ( tm2.isOperator() ) {
                oL = tm2.getOperator();
// RuntimeConfiguration.get().getErrStream().println("tm2 is operator: " + oL.getIdentifier());
                Operator mixL = Operators.getMixfix( oL );
                if ( mixL != null && ((n==2) || ((OSelement)CurrentTop.elementAt( n-3 )).isOperator() ) ) {
//  RuntimeConfiguration.get().getErrStream().println("identified prefix");
                  oL = mixL;
                } 
                if ( Operator.succ( oL, oR ) ||
                     ( oL == oR && oL.assocLeft() ) ) { // prefix or infix ?
                   if      ( oL.isInfix() ) reduceInfix(oL);
                   else if ( oL.isPrefix() ) reducePrefix();
                   else {
                     if ( (tm2.getNode().getLocation().beginLine() < tm0.getNode().getLocation().beginLine() )
                        && (oR.getIdentifier() == UniqueString.uniqueStringOf("=") ))
                       throw new ParseException("\n  *** Hint *** You may have mistyped ==\n  Illegal combination of operators " + oL.getIdentifier() + " in block " + tm2.getNode().getLocation().toString() + " and " + oR.getIdentifier() + ".");
                     else
                       throw new ParseException("\n  Illegal combination of operators " + oL.getIdentifier() + " in block " + tm2.getNode().getLocation().toString() + " and " + oR.getIdentifier() + ".");
                  }
                } else if ( !( Operator.prec( oL, oR ) ||
                          (oL==oR && oL.assocRight())) ) {
                    throw new ParseException("\n  Precedence conflict between ops " + oL.getIdentifier() + " in block " + tm2.getNode().getLocation().toString() + " and " + oR.getIdentifier() + ".");
                } else
                  break;
              } else {
                throw new ParseException(
         "Expression at location " + tm2.getNode().getLocation().toString() +
" and expression at location " + tm1.getNode().getLocation().toString() +
" follow each other without any intervening operator.");
              }
            } else
              break; // nothing this time around.
          }
        }
      }
    } while (n != CurrentTop.size()-1);
// RuntimeConfiguration.get().getErrStream().println("exit at size: " + (CurrentTop.size()-1));
  }

  final public SyntaxTreeNode finalReduce() throws ParseException {
// RuntimeConfiguration.get().getErrStream().println("final reduce called");

    int n=0;
    pushOnStack( null, Operator.VoidOperator );
    reduceStack();
// RuntimeConfiguration.get().getErrStream().println("end of final reduction");
    if ( isWellReduced() )
      return ((OSelement) CurrentTop.elementAt(0)).getNode();
    else {
      StringBuffer msg = new StringBuffer("Couldn't properly parse expression");
      int l[];
      do {
//  ((OSelement)CurrentTop.elementAt(n)).getNode().printTree(new java.io.PrintWriter(RuntimeConfiguration.get().getErrStream()));  n++;
        msg.append("-- incomplete expression at ");
        msg.append( ((OSelement)CurrentTop.elementAt(n)).getNode().getLocation().toString() ) ;
        msg.append(".\n");
        n++;
      } while (n < CurrentTop.size()-1);
      PErrors.push( new ParseError( msg.toString(), "-- --" ));
      return null;
    }
  }

  public boolean isWellReduced () {
     return CurrentTop.size() == 2;
  }

  final public void pushOnStack( SyntaxTreeNode n, Operator o ) {
    /* XXX could be optimized to reuse OSelements */
// RuntimeConfiguration.get().getErrStream().print("pushing");
// if ( n != null )
// RuntimeConfiguration.get().getErrStream().print( " " + n.getImage() );
// if ( o != null )
// RuntimeConfiguration.get().getErrStream().print(" " + o.toString() );
// RuntimeConfiguration.get().getErrStream().println();
    CurrentTop.addElement( new OSelement( n, o) );
//    RuntimeConfiguration.get().getErrStream().println(printStack());
  }

  public SyntaxTreeNode bottomOfStack() {
    return ((OSelement) CurrentTop.elementAt(0)).getNode();
  }

  public final void reduceRecord( SyntaxTreeNode middle, SyntaxTreeNode right ) throws ParseException {
    int index;
    OSelement oselt;
    int a1, a2;

    index = CurrentTop.size()-1;
    if (index < 0)
      throw new ParseException("\n    ``.'' has no left hand side at " + middle.getLocation().toString() + "." );
    oselt = (OSelement)CurrentTop.elementAt( index );

    if ( oselt.isOperator() ) {
      OSelement ospelt = (OSelement)CurrentTop.elementAt( index - 1 );
      if ( oselt.getOperator().isPostfix() && !ospelt.isOperator() ) {
        CurrentTop.addElement( null ); // humour reducePostfix
        reducePostfix();
        index = CurrentTop.size()-1;             // fix humoring
        CurrentTop.removeElementAt(index);
        index--;
        oselt = (OSelement)CurrentTop.elementAt( index );
      } else
        throw new ParseException("\n    ``.'' follows operator " + oselt.getNode().getLocation().toString() + "." );
    } 
    SyntaxTreeNode left = ((OSelement) CurrentTop.elementAt(index )).getNode();
    SyntaxTreeNode rcd = new SyntaxTreeNode(N_RecordComponent, left, middle, right);
    CurrentTop.setElementAt(new OSelement(rcd) , index);
}

// simple utility

  final private String printStack() {
    String str = new String( "stack dump, " + StackOfStack.size() + " levels, " + CurrentTop.size() + " in top one: ");
     for (int i = 0; i< CurrentTop.size(); i++ ) {
       SyntaxTreeNode tn = ((OSelement)CurrentTop.elementAt( i )).getNode();
       if (tn != null)
         str = str.concat(tn.getImage() + " " );
     }
   return str;     
  }

}
