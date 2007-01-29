// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu Feb  8 23:31:49 PST 2001 by yuanyu   

package tlc.tool;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.value.ValueOutputStream;
import util.Assert;

public class StatePoolWriter extends Thread {

  public StatePoolWriter(int bufSize) {
    this.buf = new TLCState[bufSize];
    this.poolFile = null;
    this.reader = null;
  }

  public StatePoolWriter(int bufSize, StatePoolReader reader) {
    this.buf = new TLCState[bufSize];
    this.poolFile = null;
    this.reader = reader;
  }

  private TLCState[] buf;     
  private File poolFile;           // the file to be written
  private StatePoolReader reader;  // the consumer if not null

  /*
   * This method first completes the preceding write if not started.
   * It then notifies this writer to flush enqBuf to file. In practice,
   * we expect the preceding write to have been completed. 
   */
  public final synchronized TLCState[] doWork(TLCState[] enqBuf, File file)
  throws IOException {
    if (this.poolFile != null) {
      ValueOutputStream vos = new ValueOutputStream(this.poolFile);
      for (int i = 0; i < this.buf.length; i++) {
	this.buf[i].write(vos);
      }
      vos.close();
    }
    TLCState[] res = this.buf;
    this.buf = enqBuf;
    this.poolFile = file;
    this.notify();
    return res;
  }

  /* Spin waiting for the write to complete.  */
  public final void ensureWritten() throws InterruptedException {
    synchronized(this) {
      while (this.poolFile != null) {
	this.wait();
      }
    }
  }

  public final synchronized void beginChkpt(ObjectOutputStream oos)
  throws IOException {
    boolean hasFile = (this.poolFile == null) ? false : true;
    oos.writeBoolean(hasFile);
    if (hasFile) {
      oos.writeObject(this.poolFile);
      for (int i = 0; i < this.buf.length; i++) {
	oos.writeObject(this.buf[i]);
      }
    }
  }

  /* Note this method is not synchronized.  */
  public final void recover(ObjectInputStream ois) throws IOException {    
    boolean hasFile = ois.readBoolean();
    if (hasFile) {
      try {
	this.poolFile = (File)ois.readObject();
	for (int i = 0; i < this.buf.length; i++) {
	  this.buf[i] = (TLCState)ois.readObject();
	}
      }
      catch (ClassNotFoundException e) {
	Assert.fail("TLC encountered the following error while restarting from a " +
		    "checkpoint;\n the checkpoint file is probably corrupted.\n" +
		    e.getMessage());
      }
    }
    else {
      this.poolFile = null;
    }
  }

  /**
   * Write "buf" to "poolFile". The objects in the queue are written
   * using Java's object serialization facilities.
   */
  public void run() {
    try {
      synchronized(this) {
	while (true) {
	  while (this.poolFile == null) {
	    this.wait();
	  }
	  ValueOutputStream vos = new ValueOutputStream(this.poolFile);
	  for (int i = 0; i < this.buf.length; i++) {
	    this.buf[i].write(vos);
	  }
	  vos.close();
	  this.poolFile = null;
	  this.notify();
	  if (this.reader != null) this.reader.wakeup();
	}
      }
    }
    catch (Exception e) {
      // e.printStackTrace();
      RuntimeConfiguration.get().getErrStream().println("Error: when writing the disk (StatePoolWriter.run):\n" +
			 e.getMessage());
      System.exit(1);
    }
  }
  
}
