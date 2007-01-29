// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon Aug 20 10:53:55 PDT 2001 by yuanyu

package tlc.value;


public abstract class UserObj {

  /* Returns negative, 0, positive for less than, equal, greater than. */
  public abstract int compareTo(Value val);

  /* True iff val is a member of this object. */
  public abstract boolean member(Value val);

  public abstract boolean isFinite();
  
  /* The String representation.    */
  public abstract StringBuffer toString(StringBuffer sb, int offset);

  public final String toString() {
    StringBuffer sb = new StringBuffer();
    sb = this.toString(sb, 0);
    return sb.toString();
  }
  
}
