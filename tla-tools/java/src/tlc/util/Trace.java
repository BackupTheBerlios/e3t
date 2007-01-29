// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlc.util ;

import java.io.PrintWriter;

import org.zambrovski.tla.RuntimeConfiguration;

/** Class implementing various print routines used for tracing programs.
 **/
public class Trace {
    // Whitespace string used for indenting tracing output.
    public static final String INDENT_STRING = "  " ;

    /** Counter for tracking amount of indentation to use. <p>
     */
    private static int indent = 0 ;

    private static /*@ non_null */ PrintWriter out ;
    private static /*@ non_null */ PrintWriter err ;

    static {
	out = new PrintWriter(RuntimeConfiguration.get().getErrStream(), true) ;
	err = new PrintWriter(RuntimeConfiguration.get().getErrStream(), true) ;
    }

    /** Prints given string to output stream, without a newline at the end.
     */
    public static void output(/*@ non_null */ String text) {
	out.print(text) ;
    }

    /** Prints given string to output stream, and atinewline at end.
     */
    public static void outputln(/*@ non_null */ String text) {
	out.println(text) ;
    }

    /** Prints given string to output stream, with indentation, but
     **   without a newline at the end.
     */
    public static void trace(/*@ non_null */ String text) {
	for (int i = indent ; i > 0 ; i--) {
	    err.print(INDENT_STRING) ;
	}
	err.print(text) ;
    }

    /** Prints given string to output stream, with both indentation,
     **   and a newline at the end.
     */
    public static void traceln(/*@ non_null */ String text) {
	for (int i = indent ; i > 0 ; i--) {
	    err.print(INDENT_STRING) ;
	}
	err.println(text) ;
	err.flush();
    }

    /** The following method writes a string to the error stream, without
     **    any indentation.
     */
    public static void errput(/*@ non_null */ String text) {
	err.print(text) ;
    }

    /** The following two methods are used for tracing procedure calls.
     **   The intent is that traceIn be used on entry to a procedure, and
     **   that traceOut be used on exit.  The indentation is increased on
     **   invoking traceIn, and decreased on invoking traceOut.
     */
    public static void traceIn(/*@ non_null */ String text) {
	++indent ;
	traceln(text) ;
    }

    public static void traceOut(String text) {
	if (text != null) { traceln(text) ; }
	--indent ;
    }
}
