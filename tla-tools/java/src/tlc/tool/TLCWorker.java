// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Thu May 31 13:24:56 PDT 2001 by yuanyu   

package tlc.tool;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.zambrovski.tla.RuntimeConfiguration;

import tlc.TLCGlobals;
import tlc.util.BitVector;
import tlc.util.LongVec;
import util.FP64;
import util.InternRMI;
import util.UniqueString;

public class TLCWorker extends UnicastRemoteObject implements TLCWorkerRMI {

  private DistApp work;
  private FPSetManager fpSetManager;

  public TLCWorker(DistApp work, FPSetManager fpSetManager) throws RemoteException {
    this.work = work;
    this.fpSetManager = fpSetManager;
  }

  public synchronized Object[] getNextStates(TLCState[] states)
  throws WorkerException {
    TLCState state1 = null, state2 = null;
    int fpServerCnt = this.fpSetManager.numOfServers();
    TLCStateVec[] pvv = new TLCStateVec[fpServerCnt];
    TLCStateVec[] nvv = new TLCStateVec[fpServerCnt];
    LongVec[] fpvv = new LongVec[fpServerCnt];
    for (int i = 0; i < fpServerCnt; i++) {
      pvv[i] = new TLCStateVec();
      nvv[i] = new TLCStateVec();
      fpvv[i] = new LongVec();
    }
    try {
      // Compute all of the next states of this block of states.
      for (int i = 0; i < states.length; i++) {
	state1 = states[i];
	TLCState[] nstates = this.work.getNextStates(state1);
	for (int j = 0; j < nstates.length; j++) {
	  long fp = nstates[j].fingerPrint();
	  int fpIndex = (int)((fp & 0x7FFFFFFFFFFFFFFFL) % fpServerCnt);
	  pvv[fpIndex].addElement(state1);
	  nvv[fpIndex].addElement(nstates[j]);
	  fpvv[fpIndex].addElement(fp);
	}
      }

      BitVector[] visited = this.fpSetManager.containsBlock(fpvv);

      // Remove the states that has already been seen, check if the
      // remaining new states are valid and inModel.
      TLCStateVec[] newStates = new TLCStateVec[fpServerCnt];
      LongVec[] newFps = new LongVec[fpServerCnt];
      for (int i = 0; i < fpServerCnt; i++) {
	newStates[i] = new TLCStateVec();
	newFps[i] = new LongVec();
      }

      for (int i = 0; i < fpServerCnt; i++) {
	BitVector.Iter iter = new BitVector.Iter(visited[i]);
	int index;
	while ((index = iter.next()) != -1) {
	  state1 = pvv[i].elementAt(index);
	  state2 = nvv[i].elementAt(index);
	  this.work.checkState(state1, state2);
	  if (this.work.isInModel(state2) &&
	      this.work.isInActions(state1, state2)) {
	    state2.uid = state1.uid;
	    newStates[i].addElement(state2);
	    newFps[i].addElement(fpvv[i].elementAt(index));
	  }
	}
      }
      // Prepare the return value.
      Object[] res = new Object[2];
      res[0] = newStates;
      res[1] = newFps;
      return res;    
    }
    catch (WorkerException e) {
      throw e;
    }
    catch (Throwable e) {
      throw new WorkerException(e.getMessage(), state1, state2, true);
    }
  }
    
  public void exit() throws IOException {
    String hostname = InetAddress.getLocalHost().getHostName();    
    RuntimeConfiguration.get().getOutStream().println(hostname + ", work completed. Thank you!");
    System.exit(0);
  }
  
  public static void main(String args[]) {
    RuntimeConfiguration.get().getOutStream().println("TLC Worker " + TLCGlobals.versionOfTLC);

    String specFile = null;
    String configFile = null;
    String serverName = null;

    // Must have at least two args: a filename and a hostname.
    int index = 0;
    while (index < args.length) {
      if (args[index].equals("-config")) {
	index++;
	if (index < args.length) {
	  configFile = args[index];
	  int len = configFile.length();
	  if (configFile.startsWith(".cfg", len-4)) {
	    configFile = configFile.substring(0, len-4);
	  }
	  index++;
	}
	else {
	  printErrorMsg("Error: configuration file required.");
	  System.exit(0);
	}
      }
      else {
	if (args[index].charAt(0) == '-') {
	  printErrorMsg("Error: unrecognized option: " + args[index]);
	  System.exit(0);
	}
	if (specFile == null) {
	  specFile = args[index++];
	  int len = specFile.length();
	  if (specFile.startsWith(".tla", len-4)) {
	    specFile = specFile.substring(0, len-4);
	  }
	}
	else if (serverName == null) {
	  serverName = args[index++];
	}
	else {
	  printErrorMsg("Error: more than one input files: " + specFile +
			" and " + args[index]);
	  System.exit(0);
	}
      }
    }

    if (specFile == null) {
      printErrorMsg("Error: Missing input TLA+ module.");
      return;
    }
    if (serverName == null) {
      printErrorMsg("Error: Missing hostname of the TLC server to be contacted.");
      return;
    }
    if (configFile == null) configFile = specFile;

    String hostname = "Unknown";
    try {
      hostname = InetAddress.getLocalHost().getHostName();
      String url = "//" + serverName + ":" + TLCServer.Port + "/TLCServer";
      TLCServerRMI server = (TLCServerRMI)Naming.lookup(url);

      long irredPoly = server.getIrredPolyForFP();
      FP64.Init(irredPoly);

      int lastSep = specFile.lastIndexOf(File.separatorChar);
      String specDir = (lastSep == -1) ? "" : specFile.substring(0, lastSep+1);
      specFile = specFile.substring(lastSep+1);
      Object[] appArgs = new Object[5];
      appArgs[0] = specDir;
      appArgs[1] = specFile;
      appArgs[2] = configFile;
      appArgs[3] = server.getCheckDeadlock();
      appArgs[4] = server.getPreprocess();

      String appName = server.getAppName();
      Class appClass = Class.forName(appName);
      Class[] classOfArgs = new Class[appArgs.length];
      for (int i = 0; i < classOfArgs.length; i++) {
	classOfArgs[i] = appArgs[i].getClass();
      }
      Constructor appConstructor = appClass.getDeclaredConstructor(classOfArgs);
      DistApp work = (DistApp)appConstructor.newInstance(appArgs);

      UniqueString.setSource((InternRMI)server);
      
      FPSetManager fpSetManager = server.getFPSetManager();
      TLCWorkerRMI worker = new TLCWorker(work, fpSetManager);
      server.registerWorker(worker, hostname);

      RuntimeConfiguration.get().getOutStream().println("TLC worker at " + hostname + " is ready.") ;
    }
    catch (Throwable e) {
      RuntimeConfiguration.get().getErrStream().println("Error: Failed to start worker at " + hostname +
			 " for server " + serverName + ".\n" + e.getMessage());
    }
  }

  private static void printErrorMsg(String msg) {
    RuntimeConfiguration.get().getErrStream().println(msg);
    RuntimeConfiguration.get().getErrStream().println("Usage: java tlc.tool.TLCWorker [-option] inputfile host");
  }

}
