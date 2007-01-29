// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon Dec 18 22:56:08 PST 2000 by yuanyu

package tlc.tool;

import java.io.File;
import java.io.IOException;

import tlc.value.ValueInputStream;
import tlc.value.ValueOutputStream;

public final class MemStateQueue extends StateQueue {
  private final static int InitialSize = 4096;
  private final static int GrowthFactor = 2;

  /* Fields  */
  private TLCState[] states;
  private int start = 0;
  private String diskdir;
    
  public MemStateQueue(String metadir) {
    this.states = new TLCState[InitialSize];
    this.start = 0;
    this.diskdir = metadir;
  }
    
  final void enqueueInner(TLCState state) {
    if (this.len == this.states.length) {
      // grow the array
      int newLen = Math.max(1, this.len * GrowthFactor);
      TLCState[] newStates = new TLCState[newLen];
      int copyLen = this.states.length - this.start;
      System.arraycopy(this.states, this.start, newStates, 0, copyLen);
      System.arraycopy(this.states, 0, newStates, copyLen, this.start);
      this.states = newStates;
      this.start = 0;
    }
    int last = (this.start + this.len) % this.states.length;
    this.states[last] = state;
  }
    
  final TLCState dequeueInner() {
    TLCState res = this.states[this.start];
    this.states[this.start] = null;
    this.start = (this.start + 1) % this.states.length;
    return res;
  }

  // Checkpoint.
  public final void beginChkpt() throws IOException {
    String filename = this.diskdir + File.separator + "queue.tmp";
    ValueOutputStream vos = new ValueOutputStream(filename);
    vos.writeInt(this.len);
    int index = this.start;
    for (int i = 0; i < this.len; i++) {
      this.states[index++].write(vos);
      if (index == this.states.length) index = 0;
    }
    vos.close();
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
    ValueInputStream vis = new ValueInputStream(filename);
    this.len = vis.readInt();
    for (int i = 0; i < this.len; i++) {
      this.states[i] = TLCState.Empty.createEmpty();
      this.states[i].read(vis);
    }
    vis.close();
  }

}
