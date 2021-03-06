// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.parser;

import util.UniqueString;

public class Operator implements tlasany.st.SyntaxTreeConstants {

  private UniqueString Id;
  private int Low;
  private int High;
  public int Associativity;
  public int Fix;

  static public Operator VoidOperator =  new Operator(UniqueString.uniqueStringOf("$$_void"), 0, 0, Operators.assocNone, Operators.infix);

  public Operator( UniqueString id, int l, int h, int a, int f) {
    Id = id; Low = l; High = h; Associativity = a; Fix = f;
  }

  public Operator clone ( UniqueString name ) {
    return new Operator( name, Low, High, Associativity, Fix);
  }

  public String toString() {
  switch ( Fix ) {
    case 0 /* Operators.nofix   */ : return Id.toString() + ", nofix";
    case 1 /* Operators.prefix  */ : return Id.toString() + ", prefix";
    case 2 /* Operators.postfix */ : return Id.toString() + ", postfix";
    case 3 /* Operators.infix   */ : return Id.toString() + ", infix";
    case 4 /* Operators.nfix    */ : return Id.toString() + ", nfix";
  }
  return null;
}

  public final boolean isPrefix() {
    return (Fix==Operators.prefix);
  }

  public final boolean isInfix() {
    return ( Fix == Operators.infix || Fix == Operators.nfix );
  }

  public final boolean isPostfix() {
    return ( Fix == Operators.postfix );
  }

  public final boolean isNfix() {
    return Fix == Operators.nfix ;
  }

  public final boolean isPrefixDecl() {
    return ( Fix == Operators.prefix && Id.toString().endsWith(".") );
  }

  public final boolean assocLeft() {
    return Associativity == Operators.assocLeft;
  }

  public final boolean assocRight() {
    return Associativity == Operators.assocRight;
  }

/*
(* Note that we can view a prefix or postfix operator as an infix          *)
(* operator with one empty argument.)  We can eliminate the side           *)
(* conditions by defining op1 \prec op2 to be true if op2 is not a postfix *)
(* or infix operator, and defining op2 \succ op1 to be true if op2 is not  *)
(* a prefix or infix operator.                                             *)
*/

  static final boolean succ( Operator left, Operator right) {
      return left.Low > right.High;
  }

  static final boolean prec( Operator left, Operator right ) {
    return left.High < right.Low;
  }

  static final boolean samePrec( Operator left, Operator right ) {
    return ( (left.High == right.High) && (left.Low == right.Low) );
  }

  public final UniqueString getIdentifier() {
    return Id;
  }
}
