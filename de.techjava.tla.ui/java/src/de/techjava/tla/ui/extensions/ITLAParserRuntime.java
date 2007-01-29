package de.techjava.tla.ui.extensions;

import org.eclipse.core.resources.IProject;

/**
 * Basic interface for TLA+ Parser Runtime Extension
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: ITLAParserRuntime.java,v 1.1 2007/01/29 22:29:23 tlateam Exp $
 */
public interface ITLAParserRuntime 
{
    /**
     * Initialize the parsering of TLA files
     * 
     * @param resourceNames Array containing resource names relative to source root
     * @param project		Project the compilation is being executed upon
     * 
     * @return array containing the results in order corresponding to input files 
     */
    public ITLAParserResult[] parse(String[] resourceNames, IProject project );
    /**
     * Flushes the parser cache
     */
    public void flush();
}

/*
 * $Log: ITLAParserRuntime.java,v $
 * Revision 1.1  2007/01/29 22:29:23  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/20 17:57:39  bgr
 * incremental build functionality started
 *
 * Revision 1.1  2004/10/12 16:21:38  sza
 * initial commit
 *
 *
 */