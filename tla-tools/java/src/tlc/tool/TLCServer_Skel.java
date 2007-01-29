// Skeleton class generated by rmic, do not edit.
// Contents subject to change without notice.

package tlc.tool;

public final class TLCServer_Skel
    implements java.rmi.server.Skeleton
{
    private static final java.rmi.server.Operation[] operations = {
	new java.rmi.server.Operation("java.lang.String getAppName()"),
	new java.rmi.server.Operation("java.lang.Boolean getCheckDeadlock()"),
	new java.rmi.server.Operation("tlc.tool.FPSetManager getFPSetManager()"),
	new java.rmi.server.Operation("long getIrredPolyForFP()"),
	new java.rmi.server.Operation("java.lang.Boolean getPreprocess()"),
	new java.rmi.server.Operation("util.UniqueString intern(java.lang.String)"),
	new java.rmi.server.Operation("void registerWorker(tlc.tool.TLCWorkerRMI, java.lang.String)")
    };
    
    private static final long interfaceHash = 5137994958997970526L;
    
    public java.rmi.server.Operation[] getOperations() {
	return (java.rmi.server.Operation[]) operations.clone();
    }
    
    public void dispatch(java.rmi.Remote obj, java.rmi.server.RemoteCall call, int opnum, long hash)
	throws java.lang.Exception
    {
	if (opnum < 0) {
	    if (hash == -3423945746120918602L) {
		opnum = 0;
	    } else if (hash == -930174832494166104L) {
		opnum = 1;
	    } else if (hash == -4181204942007244269L) {
		opnum = 2;
	    } else if (hash == -6640314000531774756L) {
		opnum = 3;
	    } else if (hash == 3870059516661020728L) {
		opnum = 4;
	    } else if (hash == 4045716862337859024L) {
		opnum = 5;
	    } else if (hash == -5097816240577720572L) {
		opnum = 6;
	    } else {
		throw new java.rmi.UnmarshalException("invalid method hash");
	    }
	} else {
	    if (hash != interfaceHash)
		throw new java.rmi.server.SkeletonMismatchException("interface hash mismatch");
	}
	
	tlc.tool.TLCServer server = (tlc.tool.TLCServer) obj;
	switch (opnum) {
	case 0: // getAppName()
	{
	    call.releaseInputStream();
	    java.lang.String $result = server.getAppName();
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeObject($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 1: // getCheckDeadlock()
	{
	    call.releaseInputStream();
	    java.lang.Boolean $result = server.getCheckDeadlock();
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeObject($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 2: // getFPSetManager()
	{
	    call.releaseInputStream();
	    tlc.tool.FPSetManager $result = server.getFPSetManager();
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeObject($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 3: // getIrredPolyForFP()
	{
	    call.releaseInputStream();
	    long $result = server.getIrredPolyForFP();
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeLong($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 4: // getPreprocess()
	{
	    call.releaseInputStream();
	    java.lang.Boolean $result = server.getPreprocess();
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeObject($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 5: // intern(String)
	{
	    java.lang.String $param_String_1;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$param_String_1 = (java.lang.String) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
	    } finally {
		call.releaseInputStream();
	    }
	    util.UniqueString $result = server.intern($param_String_1);
	    try {
		java.io.ObjectOutput out = call.getResultStream(true);
		out.writeObject($result);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	case 6: // registerWorker(TLCWorkerRMI, String)
	{
	    tlc.tool.TLCWorkerRMI $param_TLCWorkerRMI_1;
	    java.lang.String $param_String_2;
	    try {
		java.io.ObjectInput in = call.getInputStream();
		$param_TLCWorkerRMI_1 = (tlc.tool.TLCWorkerRMI) in.readObject();
		$param_String_2 = (java.lang.String) in.readObject();
	    } catch (java.io.IOException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
	    } catch (java.lang.ClassNotFoundException e) {
		throw new java.rmi.UnmarshalException("error unmarshalling arguments", e);
	    } finally {
		call.releaseInputStream();
	    }
	    server.registerWorker($param_TLCWorkerRMI_1, $param_String_2);
	    try {
		call.getResultStream(true);
	    } catch (java.io.IOException e) {
		throw new java.rmi.MarshalException("error marshalling return", e);
	    }
	    break;
	}
	    
	default:
	    throw new java.rmi.UnmarshalException("invalid method number");
	}
    }
}
