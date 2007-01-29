// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue Aug 22 11:56:52 PDT 2000 by yuanyu

package tlc.value;

public interface ValueEnumeration {
  /* Reset allows repeated use of this enumerator. */
  public void reset();

  /* Return the next element if there is one. Otherwise return null. */
  public Value nextElement();
  
}
