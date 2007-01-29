// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
package tlasany.error;

public final class Timing {

  private static long globalTime = 0;
  private static long globalTimeCkPt = 0;

  private static long lexerTime = 0;
  private static long lexerTimeCkPt = 0;

  private static long parserTime = 0;
  private static long parserTimeCkPt = 0;

  private static long semanticTime = 0;
  private static long semanticTimeCkPt = 0;

  public static final void markGT() { globalTimeCkPt = System.currentTimeMillis(); }
  public static final void checkGT() { globalTime += ( System.currentTimeMillis()- globalTimeCkPt ); }
  public static final long reportGT() { return globalTime; }

  public static final void markLT() { lexerTimeCkPt = System.currentTimeMillis(); }
  public static final void checkLT() { lexerTime += ( System.currentTimeMillis()- lexerTimeCkPt ); }
  public static final long reportLT() { return lexerTime; }

  public static final void markPT() { parserTimeCkPt = System.currentTimeMillis(); }
  public static final void checkPT() { parserTime += ( System.currentTimeMillis()- parserTimeCkPt ); }
  public static final long reportPT() { return parserTime; }

  public static final void markST() { semanticTimeCkPt = System.currentTimeMillis(); }
  public static final void checkST() { semanticTime += ( System.currentTimeMillis()- semanticTimeCkPt ); }
  public static final long reportST() { return semanticTime; }

  public static final String reportTime(long delta) { 
    int millis = (int) (delta % 1000);
    long rest = delta / 1000;
    int secs = (int) (rest % 60); rest = rest / 60;
    int mins = (int) (rest % 60); rest = rest / 60;
    int hours = (int) (rest % 24); rest = rest / 24;
    String result = java.lang.Integer.toString( millis ) + " ms";
    if ( secs != 0 || mins != 0 || hours != 0 ) {
      result = java.lang.Integer.toString( secs ) + ", " + result;
      if ( mins != 0 || hours != 0 ) {
        result = java.lang.Integer.toString( mins ) + ":" + result;
        if ( hours != 0 ) {
          result = java.lang.Integer.toString( hours ) + ":" + result;
        }
      }
    }
    return result;
  }
}
