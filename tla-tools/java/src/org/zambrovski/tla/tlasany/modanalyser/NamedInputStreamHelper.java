package org.zambrovski.tla.tlasany.modanalyser;

import java.io.File;
import java.io.FileNotFoundException;

import org.zambrovski.tla.RuntimeConfiguration;


import tlasany.modanalyzer.NamedInputStream;


/**
 * File util toolkit
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: NamedInputStreamHelper.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class NamedInputStreamHelper 
{
    public final static String TLA_FILE_EXTENTION = ".tla";
    public final static String FILE_SEPARATOR     = System.getProperty("file.separator");
    
    /**
     * Retrieves NamedInputStream for a give resource name
     * @param resourceName a name relative to project root or to library path
     * @return a NamedInputStream
     */
    public static NamedInputStream getIStream(String resourceName)
    {

//      Strip off the newline
        resourceName = ( resourceName.indexOf( '\n' ) >= 0 ) ? resourceName.substring( 0, resourceName.indexOf( '\n' ) ): resourceName;
//		Ensure we have a path with extention
        resourceName = ( resourceName.endsWith(TLA_FILE_EXTENTION) ) ? resourceName : resourceName + TLA_FILE_EXTENTION;
        
        String sourceModuleName = resourceName.substring(0, resourceName.length() - TLA_FILE_EXTENTION.length());
        
        File sourceFile;
        String sourceFileName =  RuntimeConfiguration.get().getRootDirectoryName() + resourceName;
        
        // try to find in specified directory
        sourceFile = new File(sourceFileName);
        if (!sourceFile.exists())
        {
	        String[] libraryPaths = RuntimeConfiguration.get().getLibraries();
	        for (int libraryIndex = 0; libraryIndex < libraryPaths.length; libraryIndex++) 
	        {
	            sourceFileName = libraryPaths[libraryIndex] + resourceName;
	            sourceFile = new File( sourceFileName );
	            if (sourceFile.exists()) 
	            {
	                break;
	            }
	        }
        }
        
        if (sourceFileName != null && sourceFile != null && sourceFile.exists())
        {
            try 
            {
            NamedInputStream nis = new NamedInputStream( 
                    sourceFileName, 
					sourceModuleName, 
					sourceFile );
            return nis;
            } catch (FileNotFoundException e) {
                RuntimeConfiguration.get().logError(e);
                return null;
            }
        } else {
            return null;
        }
    }
}

/*
 * $Log: NamedInputStreamHelper.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:57  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/14 20:56:59  bgr
 * configuration moved
 *
 * Revision 1.1  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
 *
 * Revision 1.1  2004/10/06 21:57:23  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 01:03:27  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:58:10  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:54:41  sza
 * init
 *
 *
 */