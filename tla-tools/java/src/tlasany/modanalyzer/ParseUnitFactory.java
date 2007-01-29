package tlasany.modanalyzer;

import org.zambrovski.tla.tlasany.modanalyser.NamedInputStreamHelper;

/**
 * Factory for parse units
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: ParseUnitFactory.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class ParseUnitFactory 
{
    public static ParseUnit createParseUnit(String resourceName, SpecObj spec)
    	throws ParseUnitCreationException
    {
        // find a file derived from the name and create a
        
        NamedInputStream nis = NamedInputStreamHelper.getIStream(resourceName);

        if ( nis != null ) 
        {
            // if a non-null NamedInputStream exists, create ParseUnit
            // from "nis", but don't parse it yet
            return new ParseUnit(spec, nis); 
        } else {
            throw new ParseUnitCreationException();
        }
    }
}

/*
 * $Log: ParseUnitFactory.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:52  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
 *
 * Revision 1.1  2004/10/06 21:57:21  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 01:03:26  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:58:09  sza
 * initial commit
 *
 * Revision 1.1  2004/10/06 00:54:39  sza
 * init
 *
 *
 */