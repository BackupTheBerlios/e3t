// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// jcg wrote this.
// last revised February 1st 2000


// XXX Watch out !!!
// This code uses a static variable to hold the last SyntaxTreeNode previously generated from 
// a Token. This is necessary to properly attach the comments, but unfortunately makes the 
// code non-reentrant.
//
// Removing this variable would require modifying the signature of constructors which are 
// extensively used, or possibly use a trick to memorize these pointers for each thread.
 
package tlasany.parser;

import org.zambrovski.tla.RuntimeConfiguration;

import tlasany.semantic.ASTConstants;
import tlasany.st.Location;
import tlasany.st.SyntaxTreeConstants;
import tlasany.st.TreeNode;
import tlasany.utilities.Strings;
import util.UniqueString;

// The SyntaxTreeNode is the node of the syntax tree. It holds key information from the 
// tokens (string, position). Heirs are held in two arrays. This is a trick to facilitate 
// construction, and also to test for the presence of the LOCAL token more easily.

// all strings are resolved internally to UniqueString.

/* methods
 * various constructors
 * kind manipulation : getKind, setKind, isKind
 * heirs()
 * getLocation()
 * getFileName, and US version.
 * possibly other methods to facilitate the semantic analysis.
 */

public class SyntaxTreeNode implements TreeNode, SyntaxTreeConstants, ASTConstants {

  private static SyntaxTreeNode[] nullArray   = new SyntaxTreeNode[0];
  protected SyntaxTreeNode[]      zero, one;
  int                             kind        = 0;
  UniqueString                    image       = null;
  int []                          location    = new int[4];
  private UniqueString            fileName    = null;
  private String []               ns          = new String[0];
  private String []               preComment  = ns;
  private String []               postComment = ns;
  private static SyntaxTreeNode   lastSN      = null;


  public static SyntaxTreeNode nullSTN =
    new SyntaxTreeNode( UniqueString.uniqueStringOf("***I do not exist***") );

  public SyntaxTreeNode() {
    zero = nullArray; one = nullArray;
  }

  public SyntaxTreeNode( UniqueString fn ) {
    kind = 0; 
    image = fn;
    zero = nullArray;
    one = nullArray;
    location[0] = 0; location[1] = 0; location[2] = 0; location[3] = 0;
    fileName = UniqueString.uniqueStringOf("--TLA+ BUILTINS--");
  }


  public SyntaxTreeNode(UniqueString fn, Token t) {
    this.kind = t.kind; 
    this.image = UniqueString.uniqueStringOf( t.image );
    zero = nullArray; 
    one = nullArray;
    location[0] = t.beginLine;
    location[1] = t.beginColumn;
    location[2] = t.endLine;
    location[3] = t.endColumn;
    fileName = fn;
    preComment = comments( t );
    lastSN = this;
  }


  public SyntaxTreeNode(UniqueString fn, int kind, Token t) {
    this.kind = kind;
//  this.image = SyntaxNodeImage[ kind ];
    this.image = UniqueString.uniqueStringOf( t.image );
    zero = nullArray;
    one = nullArray;
    location[0] = t.beginLine;
    location[1] = t.beginColumn;
    location[2] = t.endLine;
    location[3] = t.endColumn;
    fileName = fn;
    preComment = comments( t );
    lastSN = this;
  }


  public SyntaxTreeNode(UniqueString fn, int kind, SyntaxTreeNode a[]) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    zero = a;
    fileName = fn;
    updateLocation();
  }


  public SyntaxTreeNode( int kind, SyntaxTreeNode a[]) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    zero = a;
    fileName = a[0].fileName;
    updateLocation();
  }

  // This constructor used only in Generator class for handling @  in EXCEPT construct
  public SyntaxTreeNode( int kind, SyntaxTreeNode a[], boolean ignored) {
    this.kind = kind;
    zero = a;
  }


  public SyntaxTreeNode(UniqueString fn, int kind, SyntaxTreeNode a, SyntaxTreeNode b[]) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    if (a != null) {
      zero = new SyntaxTreeNode[1]; 
      zero[0] = a;
    }
    one = b;
    fileName = fn;
    updateLocation();
  }


  public SyntaxTreeNode(UniqueString fn, int kind, SyntaxTreeNode a[], SyntaxTreeNode b[]) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    zero = a;
    one = b;
    fileName = fn;
    updateLocation();
  }


  public SyntaxTreeNode(int kind, SyntaxTreeNode a, SyntaxTreeNode b) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    fileName = a.fileName;
    zero = new SyntaxTreeNode[2];
    zero[0] = a;
    zero[1] = b;
    updateLocation();
  }


  public SyntaxTreeNode(int kind, SyntaxTreeNode a, SyntaxTreeNode b, SyntaxTreeNode c) {
    this.kind = kind;
    image = SyntaxNodeImage[ kind ];
    fileName = a.fileName;
    zero = new SyntaxTreeNode[3];
    zero[0] = a; 
    zero[1] = b;
    zero[2] = c;
    updateLocation();
  }


  public final int       getKind()         { return kind; }


         final void      setKind( int k )  { kind = k ; }


  public final boolean   isKind( int k )   { return kind == k; }


  public final String [] getPreComments()  { return preComment; }


  public final String [] getPostComments() { return postComment; }


  public boolean isGenOp() {
    if ( kind == N_GenPrefixOp || kind == N_GenNonExpPrefixOp || 
         kind == N_GenInfixOp || kind == N_GenPostfixOp )
       return true;
    else
      return false;
  }


  private static Token nullToken = new Token();


  private final String[] comments( Token t ) {
     Token nextPre  = nullToken;
     Token nextPost = nullToken;
     int cPre = 0;
     int cPost = 0;

     if (t.specialToken == null) {
       if ( lastSN != null ) lastSN.postComment = ns;
       return ns;
     }

     Token tmp_t = t.specialToken;
     while (tmp_t != null) {
       if ( tmp_t.image.startsWith( "(*." )) {
         cPre++;
         tmp_t.next = nextPre;
         nextPre = tmp_t;
       } else {
         cPost++;
         tmp_t.next = nextPost;
         nextPost = tmp_t;
       }
       tmp_t = tmp_t.specialToken;
     }
     String []aPre = new String[ cPre ];
     String []aPost = new String[ cPost ];
     tmp_t = nextPre; cPre = 0;
     while (tmp_t != nullToken) { aPre[ cPre ] = tmp_t.image; cPre++; tmp_t = tmp_t.next;  }
     tmp_t = nextPost; cPost = 0;
     while (tmp_t != nullToken) { aPost[ cPost ] = tmp_t.image; cPost++; tmp_t = tmp_t.next;  }
     if ( lastSN != null ) lastSN.postComment = aPost;
     return aPre;
  }


  public final TreeNode[] heirs() {
    if ( zero == null && one == null ) {
      return nullArray;
    } else {
      SyntaxTreeNode result[];
      if ( zero != null ) {
        if ( one != null ) {
          result = new SyntaxTreeNode[ zero.length + one.length ];
          System.arraycopy(zero, 0, result, 0, zero.length);
          System.arraycopy(one, 0, result, zero.length, one.length);
        } else {
          result = new SyntaxTreeNode[ zero.length ];
          System.arraycopy(zero, 0, result, 0, zero.length);
        }
      } else {
        result = new SyntaxTreeNode[ one.length ];
        System.arraycopy(one, 0, result, 0, one.length);
      }
      return result;
    }
  }


  public final String         getFilename() {

    return fileName.toString();

  }

  public final UniqueString   getFN() { return fileName; }


  public final Location       getLocation( ) {

    return new Location( fileName, location[0], location[1], location[2], location[3] );

  }

  public final String         getImage() { return image.toString(); }


  public final UniqueString   getUS() { return image; }


  public final SyntaxTreeNode first() {
    //RuntimeConfiguration.get().getOutStream().println( image);
    if (zero != null) return zero[0]; else return one[0]; 
  }


  private void updateLocation() {
    int lvi = 0;

    location[0] = java.lang.Integer.MAX_VALUE;
    location[1] = java.lang.Integer.MAX_VALUE;
    location[2] = java.lang.Integer.MIN_VALUE;
    location[3] = java.lang.Integer.MIN_VALUE;

    if ( zero != null) {
      for ( lvi = 0; lvi < zero.length; lvi++ ) {
        if ( zero[lvi].location[0] != java.lang.Integer.MAX_VALUE ) {
          location[0] = Math.min ( location[0], zero[lvi].location[0] );
          if ( location[0] == zero[lvi].location[0] )
            location[1] = Math.min ( location[1], zero[lvi].location[1]) ;
          location[2] = Math.max ( location[2], zero[lvi].location[2]);
          if ( location[2] == zero[lvi].location[2] )
            location[3] = Math.max ( location[3], zero[lvi].location[3]);
        }
      }
    }

    if ( one != null) {
      for ( lvi=0; lvi < one.length; lvi++ ) {
        if ( one[lvi].location[0] != java.lang.Integer.MAX_VALUE ) {
          location[0] = Math.min ( location[0], one[lvi].location[0] );
          if ( location[0] == one[lvi].location[0] )
            location[1] = Math.min ( location[1], one[lvi].location[1]) ;
          location[2] = Math.max ( location[2], one[lvi].location[2]);
          if ( location[2] == one[lvi].location[2] )
            location[3] = Math.max ( location[3], one[lvi].location[3]);
        }
      }
    }
  }


  public final tlasany.st.TreeNode[] one() { return one; }


  public final tlasany.st.TreeNode[] zero() { return zero; }


  public final boolean local() { return zero!= null; }


  public void printST(int indentLevel) {

    String      operator = "";
    TreeNode [] heirs    = this.heirs();

    if (image != null && image.toString().equals("N_OperatorDefinition")) {
       if (((SyntaxTreeNode)(heirs()[0])).image.toString().equals("N_IdentLHS")) {
          operator = "*" + ((SyntaxTreeNode)(((SyntaxTreeNode)(heirs()[0])).heirs()[0])).image.toString();
       }
       if (((SyntaxTreeNode)(heirs()[1])).image.toString().equals("N_IdentLHS")) {
          operator = ((SyntaxTreeNode)(((SyntaxTreeNode)(heirs()[1])).heirs()[0])).image.toString();
       }
       if (((SyntaxTreeNode)(heirs()[0])).image.toString().equals("N_InfixLHS")) {
          operator = ((SyntaxTreeNode)(((SyntaxTreeNode)(heirs()[0])).heirs()[1])).image.toString();
       }
    }

    for (int i = 0; i < indentLevel; i++) RuntimeConfiguration.get().getOutStream().print(Strings.blanks[2]);

    RuntimeConfiguration.get().getOutStream().print((image == null ? "(" + SyntaxNodeImage[kind].toString() + ")" : image.toString()) 
                       + "\t" + (operator != "" ? operator + "\t" : "")
         + "  #heirs: " + heirs.length + "\t"
         + "  kind:   " + kind + "\n"
         );

    for (int i=0; i<heirs.length; i++) {
      if (heirs[i] != null)
        ((SyntaxTreeNode)heirs[i]).printST(indentLevel+1);  // Indent 1 more level
      else {
        for (int j = 0; j <= indentLevel; j++) RuntimeConfiguration.get().getOutStream().print(Strings.blanks[2]);
        RuntimeConfiguration.get().getOutStream().println("<null>");
      } // end else
    } // end for
    
  } // end method

}
