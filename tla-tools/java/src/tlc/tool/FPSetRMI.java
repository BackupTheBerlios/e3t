// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Dec 15 15:24:57 PST 2000 by yuanyu

package tlc.tool;

import java.io.IOException;
import java.rmi.Remote;

import tlc.util.BitVector;
import tlc.util.LongVec;

public interface FPSetRMI extends Remote {

  public boolean put(long fp) throws IOException;
  public boolean contains(long fp) throws IOException;
  public BitVector putBlock(LongVec fpv) throws IOException;
  public BitVector containsBlock(LongVec fpv) throws IOException;
  public long size() throws IOException;
  
  public void exit(boolean cleanup) throws IOException;
  
  public void beginChkpt(String filename) throws IOException;
  public void commitChkpt(String filename) throws IOException;
  public void recover(String filename) throws IOException;

}
