// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
package tlc.util;

/** An <code>IdThread</code> is a <code>Thread</code> with an
    integer identifier. */

public class IdThread extends Thread {
    private final int id;
    
    /** Create a new thread with ID <code>id</code>. */
    public IdThread(int id) {
        this.id = id;
    }
   
    /** Return this thread's ID. */
    public final int getID() {
        return this.id;
    }

    /** Return the Id of the calling thread. This method
        will result in a <TT>ClassCastException</TT> if
        the calling thread is not of type <TT>IdThread</TT>. */
    public static int GetId() {
        return ((IdThread)Thread.currentThread()).id;
    }

    /** If the calling thread is of type <TT>IdThread</TT>,
        return its ID. Otherwise, return <TT>otherId</TT>. */
    public static int GetId(int otherId) {
        Thread th = Thread.currentThread();
        return (th instanceof IdThread) ? ((IdThread)th).id : otherId;
    }
}
