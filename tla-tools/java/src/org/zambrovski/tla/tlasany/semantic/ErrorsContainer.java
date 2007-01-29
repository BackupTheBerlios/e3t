package org.zambrovski.tla.tlasany.semantic;

import java.util.Enumeration;
import java.util.Hashtable;

import tlasany.semantic.AbortException;
import tlasany.st.Location;

/**
 * Represent errors
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: ErrorsContainer.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class ErrorsContainer 
{
    private static final int DEFAULT_MAX_COUNT_ABORT 	= 100;
    private static final int DEFAULT_MAX_COUNT_ERROR 	= 100;
    private static final int DEFAULT_MAX_COUNT_WARNING 	= 100;
    
    Hashtable aborts		=	new Hashtable(DEFAULT_MAX_COUNT_ABORT, 0.75f);
    Hashtable errors		=	new Hashtable(DEFAULT_MAX_COUNT_ERROR, 0.75f);
    Hashtable warnings		=	new Hashtable(DEFAULT_MAX_COUNT_WARNING, 0.75f);
    
    /**
     * Adds an abort
     * @param location
     * @param message
     */
    public void addAbort(Location location, String message, boolean throwAbortException)
    	throws AbortException
    {
        if (location == null) location = Location.nullLoc;
        String locationKey = getLocationKey(location);
        if(!errors.contains(locationKey))
        {
            errors.put(locationKey, new ProblemHolder(location, message, ProblemHolder.ERROR));
        }
        
        if (throwAbortException)
        {
            throw new AbortException();
        }
    }

    /**
     * Adds abort
     * @param location
     * @param message
     * @throws AbortException
     */
    public void addAbort(Location location, String message)
    	throws AbortException
    {
        addAbort(location, message, true);
    }

    /**
     * Adds abort
     * @param message
     * @param throwAbortException
     * @throws AbortException
     */
    public void addAbort(String message, boolean throwAbortException)
		throws AbortException
	{
	    addAbort(Location.nullLoc, message, throwAbortException);
	}

    /**
     * 
     * @param location
     * @param message
     * @throws AbortException
     */
    public void addAbort(String message)
		throws AbortException
	{
	    addAbort(Location.nullLoc, message, true);
	}
    
    /**
     * Adds an error
     * @param location
     * @param message
     */
    public void addError(Location location, String message)
    {
        if (location == null) location = Location.nullLoc;
        String locationKey = getLocationKey(location);
        if(!errors.contains(locationKey))
        {
            errors.put(locationKey, new ProblemHolder(location, message, ProblemHolder.ABORT));
        }
    }
    /**
     * Adds a warning
     * @param location
     * @param message
     */
    public void addWarning(Location location, String message)
    {
        if (location == null) location = Location.nullLoc;
        String locationKey = getLocationKey(location);
        if(!errors.contains(locationKey))
        {
            errors.put(locationKey, new ProblemHolder(location, message, ProblemHolder.WARNING));
        }
        
    }
    /**
     * Retrieves a hash key from location object
     * @param location
     * @return
     */
    private String getLocationKey(Location location)
    {
        
        return location.source() + location.beginLine() + location.beginColumn() + location.endLine() + location.endColumn();
    }
    
    /**
     * Retrieves error enumeration
     * @return all errors
     */
    public Enumeration getAborts()
    {
        return aborts.elements();
    }
    
    /**
     * Retrieves error enumeration
     * @return all errors
     */
    public Enumeration getErrors()
    {
        return errors.elements();
    }
    
    /**
     * Retrieves error enumeration
     * @return all errors
     */
    public Enumeration getWarnings()
    {
        return warnings.elements();
    }
    /**
     * Retrieves status about aborts
     * @return true if no aborts, false otherwise
     */
    public boolean hasAborts()
    {
        return !aborts.isEmpty();
    }

    /**
     * Retrieves status about errors
     * @return true if no errors, false otherwise
     */
    public boolean hasErrors()
    {
        return !errors.isEmpty();
    }

    /**
     * Retrieves status about warnings
     * @return true if no warnings, false otherwise
     */
    public boolean hasWarnings()
    {
        return !warnings.isEmpty();
    }
    
    //	maintain the comaptibility to tlasany.semantic.Errors 
    
    public final boolean isSuccess()             { return !hasAborts() && !hasErrors(); }

    public final boolean isFailure()             { return isSuccess(); }

    public final int     getNumErrors()          { return errors.size(); }

    public final int     getNumAbortsAndErrors() { return aborts.size() + getNumErrors(); }

    public final int     getNumMessages()        { return warnings.size() + getNumAbortsAndErrors(); }

    /**
     * @see java.lang.Object#toString()
     */
    public final String  toString()  
    { 
      StringBuffer ret = new StringBuffer("");

      ret.append((aborts.size() > 0) ? "*** Abort messages: " + aborts.size() + "\n\n" : "");
      for (Enumeration abortEnum = getAborts(); abortEnum.hasMoreElements();)   
      {
          ProblemHolder holder = (ProblemHolder)abortEnum.nextElement();
          ret.append(holder.location.toString() + "\n\n" + holder.message + "\n\n\n");
      }

      ret.append((errors.size() > 0) ? "*** Errors: " + errors.size() + "\n\n" : "");
      for (Enumeration errorsEnum = getErrors(); errorsEnum.hasMoreElements();)   
      {
          ProblemHolder holder = (ProblemHolder)errorsEnum.nextElement();
          ret.append(holder.location.toString() + "\n\n" + holder.message + "\n\n\n");
      }

      ret.append((warnings.size() > 0) ? "*** Warnings: " + warnings.size() + "\n\n" : "");
      for (Enumeration warningsEnum = getWarnings(); warningsEnum.hasMoreElements();)   
      {
          ProblemHolder holder = (ProblemHolder)warningsEnum.nextElement();
          ret.append(holder.location.toString() + "\n\n" + holder.message + "\n\n\n");
      }

      return ret.toString();
    }
    
}

/*
 * $Log: ErrorsContainer.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:57  szambrovski
 * sf cvs init
 *
 * Revision 1.1  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
 *
 *
 */