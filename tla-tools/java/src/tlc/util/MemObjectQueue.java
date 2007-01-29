// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon Dec 18 22:56:08 PST 2000 by yuanyu

package tlc.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import util.Assert;

public final class MemObjectQueue {
  private final static int InitialSize = 4096;
  private final static int GrowthFactor = 2;

  /* Fields  */
  private int len;
  private Object[] states;
  private int start = 0;
  private String diskdir;
    
  public MemObjectQueue(String metadir) {
    this.states = new Object[InitialSize];
    this.start = 0;
    this.diskdir = metadir;
  }
    
  public final void enqueue(Object state) {
    if (this.len == this.states.length) {
      // grow the array
      int newLen = Math.max(1, this.len * GrowthFactor);
      Object[] newStates = new Object[newLen];
      int copyLen = this.states.length - this.start;
      System.arraycopy(this.states, this.start, newStates, 0, copyLen);
      System.arraycopy(this.states, 0, newStates, copyLen, this.start);
      this.states = newStates;
      this.start = 0;
    }
    int last = (this.start + this.len) % this.states.length;
    this.states[last] = state;
    this.len++;
  }
    
  public final Object dequeue() {
    if (this.len == 0) return null;
    Object res = this.states[this.start];
    this.states[this.start] = null;
    this.start = (this.start + 1) % this.states.length;
    this.len--;
    return res;
  }

  // Checkpoint.
  public final void beginChkpt() throws IOException {
    String filename = this.diskdir + File.separator + "queue.tmp";
    FileOutputStream fos = new FileOutputStream(filename);
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos));
    oos.writeInt(this.len);
    int index = this.start;
    for (int i = 0; i < this.len; i++) {
      oos.writeObject(this.states[index++]);
      if (index == this.states.length) index = 0;
    }
    oos.close();
    fos.close();
  }

  public final void commitChkpt() throws IOException {
    String oldName = this.diskdir + File.separator + "queue.chkpt";
    File oldChkpt = new File(oldName);
    String newName = this.diskdir + File.separator + "queue.tmp";
    File newChkpt = new File(newName);
    if ((oldChkpt.exists() && !oldChkpt.delete()) ||
	!newChkpt.renameTo(oldChkpt)) {
      throw new IOException("MemStateQueue.commitChkpt: cannot delete " + oldChkpt);
    }
  }
  
  public final void recover() throws IOException {
    String filename = this.diskdir + File.separator + "queue.chkpt";
    FileInputStream fis = new FileInputStream(filename);
    ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fis));
    this.len = ois.readInt();
    try {
      for (int i = 0; i < this.len; i++) {
	this.states[i] = ois.readObject();
      }
    }
    catch (ClassNotFoundException e) {
      ois.close();
      fis.close();
      Assert.fail("TLC encountered the following error while restarting from a " +
		  "checkpoint;\n the checkpoint file is probably corrupted.\n" +
		  e.getMessage());
    }
    ois.close();
    fis.close();
  }

}
