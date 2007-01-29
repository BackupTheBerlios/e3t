// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu Dec  7 20:32:45 PST 2000 by yuanyu

package tlc.tool;

public class EvalException extends RuntimeException {
  public static int ERROR = 0;
  public static int ASSERT = 1;

  private int errno;

  public EvalException(String msg) {
    super(msg);
    this.errno = ERROR;
  }

  public EvalException(int errno, String msg) {
    super(msg);
    this.errno = errno;
  }

  public final int getErrno() { return this.errno; }

  public final EvalException addMessage(String msg) {
    return new EvalException(this.errno, this.getMessage() + "\n" + msg);
  }

}
