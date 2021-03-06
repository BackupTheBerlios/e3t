// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Last modified on Wed Jul 11 00:00:55 PDT 2001 by yuanyu
package util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public final class InternTable implements Serializable {
  private int count;
  private int length;
  private int thresh;
  private UniqueString[] table;

  private static int tokenCnt = 0;      // the token counter 
  public static final int nullTok = 0;

  public InternTable(int size) {
    this.table = new UniqueString[size] ;
    this.count = 0 ;
    this.length = size ;
    this.thresh = this.length / 2;
  }

  private final void grow() {
    UniqueString[] old = this.table;
    this.count = 0;
    this.length = 2 * this.length + 1;
    this.thresh = this.length / 2;
    this.table = new UniqueString[this.length];
    for (int i = 0; i < old.length; i++) {
      UniqueString var = old[i];
      if (var != null) this.put(var);
    }
  }

  private final void put(UniqueString var) {
    int loc = (var.hashCode() & 0x7FFFFFFF) % length ;
    while (true) {
      UniqueString ent = this.table[loc] ;
      if (ent == null) {
	this.table[loc] = var;
	this.count++;
	return;
      }
      loc = (loc + 1) % length;
    }
  }

  /**
   * If there exists a UniqueString object obj such that obj.uid()
   * equals i, then uidToUniqueString(i) returns obj; otherwise,    
   * it returns null.
   */
  public final UniqueString get(int id) {
    for (int i = 0; i < this.table.length; i++) {
      UniqueString var = this.table[i];
      if (var != null && var.getTok() == id) {
	return var;
      }
    }
    return null;
  }
  
  private InternRMI internSource = null;

  public final void setSource(InternRMI source) {
    this.internSource = source;
  }
  
  private final UniqueString create(String str) {
    if (this.internSource == null) {
      return new UniqueString(str, ++tokenCnt);
    }
    try {
      return this.internSource.intern(str);
    }
    catch (Exception e) {
      Assert.fail("Failed to intern " + str + ".");
    }
    return null;  // make compiler happy
  }
  
  public final UniqueString put(String str) {
    synchronized (InternTable.class) {
      if (this.count >= this.thresh) this.grow();
      int loc = (str.hashCode() & 0x7FFFFFFF) % length;
      while (true) {
	UniqueString ent = this.table[loc];
	if (ent == null) {
	  UniqueString var = this.create(str);
	  this.table[loc] = var;
	  this.count++;
	  return var;
	}
	if (ent.toString().equals(str)) {
	  return ent;
	}
	loc = (loc + 1) % length;
      }
    }
  }

  public final void beginChkpt(String filename) throws IOException {
    BufferedDataOutputStream dos =
      new BufferedDataOutputStream(this.chkptName(filename, "tmp"));
    dos.writeInt(tokenCnt);
    for (int i = 0; i < this.table.length; i++) {
      UniqueString var = this.table[i];
      if (var != null) var.write(dos);
    }
    dos.close();
  }

  public final void commitChkpt(String filename) throws IOException {
    File oldChkpt = new File(this.chkptName(filename, "chkpt"));
    File newChkpt = new File(this.chkptName(filename, "tmp"));
    if ((oldChkpt.exists() && !oldChkpt.delete()) ||
	!newChkpt.renameTo(oldChkpt)) {
      throw new IOException("InternTable.commitChkpt: cannot delete " + oldChkpt);
    }
  }

  public final synchronized void recover(String filename) throws IOException {
    BufferedDataInputStream dis = 
      new BufferedDataInputStream(this.chkptName(filename, "chkpt"));
    tokenCnt = dis.readInt();
    try {
      while (!dis.atEOF()) {
	UniqueString var = UniqueString.read(dis);
	this.put(var);
      }
    } catch (EOFException e) {
      Assert.fail("TLC encountered the following error while restarting from a " +
		  "checkpoint;\n the checkpoint file is probably corrupted.\n" +
		  e.getMessage());
    }
    dis.close();
  }

  final private String chkptName(String filename, String ext) {
    return filename + File.separator + "vars." + ext;
  }

}
