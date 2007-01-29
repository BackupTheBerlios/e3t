// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Wed Jun  2 00:10:22 PDT 1999 by yuanyu

package tlc.value;

import tlc.tool.EvalException;

public interface Applicable {
  
  public Value apply(Value[] args, int control) throws EvalException;
  public Value apply(Value arg, int control) throws EvalException;
  public Value getDomain() throws EvalException;
  public Value select(Value arg) throws EvalException;
  
}
