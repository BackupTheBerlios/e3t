// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Tue May 15 11:35:52 PDT 2001 by yuanyu

package tlc.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.util.Vect;
import util.Assert;

public class TLCConfig {

  private static Hashtable ConfigTbl = null;
  private static int nextChar;

  private static void parse() throws IOException {
    ConfigTbl = new Hashtable();
    File cfgFile = new File("TLC.cfg");
    if (!cfgFile.exists()) return;
    Reader rd = new BufferedReader(new FileReader(cfgFile));

    nextChar = rd.read();
    String nt = nextToken(rd);
    while (true) {
      if (nt == null) return;    
      String key = nt;
      checkString(key);
      nt = nextToken(rd);
      if (nt == null || !nt.equals("=")) {
	Assert.fail("TLC configuration must be a sequence of \"key=val\".");
      }
      String val = nextToken(rd);
      if (val == null) {
	Assert.fail("TLC configuration must be a sequence of \"key=val\".");
      }
      checkString(val);
      nt = nextToken(rd);
      if (nt != null && nt.equals(",")) {
	Vect elems = new Vect();
	elems.addElement(val);
	do {
	  val = nextToken(rd);
	  checkString(val);
	  elems.addElement(val);
	  nt = nextToken(rd);
	}
	while (nt != null && nt.equals(","));
	
	String[] strs = new String[elems.size()];
	for (int i = 0; i < strs.length; i++) {
	  strs[i] = (String)elems.elementAt(i);
	}
	ConfigTbl.put(key, strs);
      }
      else {
	ConfigTbl.put(key, val);
      }
    }
  }

  private static void checkString(String token) {
    if (token.equals("=") || token.equals(",")) {
      Assert.fail("The strings in TLC configuration cannot be \"=\" or \",\"");
    }
  }
  
  private static boolean isWhiteSpace(int ch) {
    return (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r');
  }

  private static boolean endToken(int ch) {
    return (isWhiteSpace(ch) || ch == '=' || ch == ',');
  }
    
  private static void skipWhiteSpace(Reader rd) throws IOException {
    while (isWhiteSpace(nextChar)) {
      nextChar = rd.read();
    }
  }
      
  private static String nextToken(Reader rd) throws IOException {
    String res;
    skipWhiteSpace(rd);    
    if (nextChar == -1) {
      return null;
    }
    else if (nextChar == '=') {
      res = "=";
      nextChar = rd.read();
    }
    else if (nextChar == ',') {
      res = ",";
      nextChar = rd.read();
    }
    else {
      StringBuffer token = new StringBuffer();
      while (!endToken(nextChar)) {
	token.append((char)nextChar);
	nextChar = rd.read();
      }
      res = token.toString();
    }
    return res;
  }
  
  public static synchronized Object getVal(String key)
  throws IOException {
    if (ConfigTbl == null) parse();
    return ConfigTbl.get(key);
  }

  public static synchronized String getString(String key)
  throws IOException {
    if (ConfigTbl == null) parse();
    return (String)ConfigTbl.get(key);    
  }
  
  public static synchronized String[] getStringArray(String key)
  throws IOException {
    if (ConfigTbl == null) parse();
    Object val = ConfigTbl.get(key);
    if (val instanceof String) {
      String[] res = new String[1];
      res[0] = (String)val;
      return res;
    }
    return (String[])val;
  }

  public static void main(String[] args) throws Exception {
    String[] servers = TLCConfig.getStringArray("fp_servers");
    for (int i = 0; i < servers.length; i++) {
      RuntimeConfiguration.get().getOutStream().println(servers[i]);
    }
  }
  
}
