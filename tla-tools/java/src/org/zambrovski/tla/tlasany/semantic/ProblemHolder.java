package org.zambrovski.tla.tlasany.semantic;

import tlasany.st.Location;

/**
 * A wraper for holding a location with
 * the problem message
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: ProblemHolder.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class ProblemHolder 
{
    public final static int ABORT 		= 1;
    public final static int ERROR 		= 2;
    public final static int WARNING 	= 3;
    
    public Location location;
    public String	message;
    public int		type;
    /**
     * Constructs a holder object
     * @param location
     * @param message
     * @param type use ProblemHolder.ABORT, ProblemHolder.ERROR, ProblemHolder.WARNING
     */
    public ProblemHolder(Location location, String message, int type)
    {
        this.location 	= location;
        this.message	= message;
        this.type		= type;
    }
    /**
     * Formats location
     * @return
     */
    public String getFormattedLocation()
    {
        return "from line " + location.beginLine() + " column " + location.beginColumn() + " to line " + location.endLine() + " column "+ location.endColumn();
    }
}

/*
 * $Log: ProblemHolder.java,v $
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