// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.drivers;


public class FrontEndException extends Exception {

  Exception ex;

  public FrontEndException(Exception e) { ex = e; }

}
