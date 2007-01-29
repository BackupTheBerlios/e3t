// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Fri Dec 15 15:24:57 PST 2000 by yuanyu

package tlc.tool;

import java.io.IOException;
import java.rmi.Remote;

public interface FPIntSetRMI extends Remote {

  public void setLeveled(long fp) throws IOException;
  public int setStatus(long fp, int status) throws IOException;
  public int getStatus(long fp) throws IOException;  
  public boolean allLeveled() throws IOException;

  public void exit(boolean cleanup) throws IOException;
  
  public void beginChkpt(String filename) throws IOException;
  public void commitChkpt(String filename) throws IOException;
  public void recover(String filename) throws IOException;

}
