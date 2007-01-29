package de.techjava.tla.ui.views;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Stream to log
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: LogStream.java,v 1.1 2007/01/29 22:25:04 tlateam Exp $
 */
public class LogStream 
	extends PrintStream
{
    private TLAConsole		console;
    private	OutputStream	stream;
    
    /**
     * @param out
     */
    public LogStream( TLAConsole console ) 
    {
        super( new OutputStream() {

            public void write(int b) throws IOException {
                //do nothing
                
            }} );
        
        this.console = console;
    }
    
    

    /**
     * @see java.io.PrintStream#print(boolean)
     */
    public void print(boolean b) {
        console.appendText( "" + b );
    }
    
    /**
     * @see java.io.PrintStream#print(char)
     */
    public void print(char c) {
        console.appendText( "" + c );
    }
    /**
     * @see java.io.PrintStream#print(char[])
     */
    public void print(char[] s) {
        console.appendText( new String( s ) );
    }
    /**
     * @see java.io.PrintStream#print(double)
     */
    public void print(double d) {
        console.appendText( "" + d );;
    }
    /**
     * @see java.io.PrintStream#print(float)
     */
    public void print(float f) {
        console.appendText( "" + f );
    }
    /**
     * @see java.io.PrintStream#print(int)
     */
    public void print(int i) {
        console.appendText( "" + i );;
    }
    /**
     * @see java.io.PrintStream#print(long)
     */
    public void print(long l) {
        console.appendText( "" + l );
    }
    /**
     * @see java.io.PrintStream#print(java.lang.Object)
     */
    public void print(Object obj) {
        console.appendText( obj.toString() );
    }
    /**
     * @see java.io.PrintStream#print(java.lang.String)
     */
    public void print(String s) {
        console.appendText( s );
    }
    /**
     * @see java.io.PrintStream#println()
     */
    public void println() {
        console.appendText( "\n" );
    }
    /**
     * @see java.io.PrintStream#println(boolean)
     */
    public void println(boolean x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(char)
     */
    public void println(char x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(char[])
     */
    public void println(char[] x) {
        console.appendText( new String( x ) + "\n" );
    }
    
    /**
     * @see java.io.PrintStream#println(double)
     */
    public void println(double x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(float)
     */
    public void println(float x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(int)
     */
    public void println(int x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(long)
     */
    public void println(long x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(java.lang.Object)
     */
    public void println(Object x) {
        console.appendText( x + "\n" );
    }
    /**
     * @see java.io.PrintStream#println(java.lang.String)
     */
    public void println(String x) {
        console.appendText( x + "\n" );
    }
}

/*
 * $Log: LogStream.java,v $
 * Revision 1.1  2007/01/29 22:25:04  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:48:31  szambrovski
 * *** empty log message ***
 *
 * Revision 1.1  2004/10/20 15:08:49  sza
 * logging redirected to console
 *
 *
 */