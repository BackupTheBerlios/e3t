package de.techjava.tla.ui.views;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import de.techjava.tla.ui.UIPlugin;
import de.techjava.tla.ui.util.ITLAConsoleConstants;

/**
 * TLA Console
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: TLAConsole.java,v 1.1 2007/01/29 22:25:04 tlateam Exp $
 */
public class TLAConsole
	extends IOConsole
{
    public final static int TYPE_CHECKER 	= 1;
    public final static int TYPE_PARSER 	= 2;
    
    
    public static final String TLA_CONSOLE = "de.techjava.tla.core.TLAConsole_";
    private IOConsoleOutputStream stream;
    /**
     * @param name
     * @param imageDescriptor
     */
    public TLAConsole(String name, ImageDescriptor imageDescriptor, int type) 
    {
        super(name, TLA_CONSOLE + type, imageDescriptor, true);
        this.stream = newOutputStream();
        
        
        if (type == TYPE_CHECKER) 
        {
            Color color = UIPlugin.getDefault().getColorManager().getColor(ITLAConsoleConstants.CONSOLE_CHECKER_COLOR);
            this.stream.setColor(color);
            this.stream.setFontStyle(SWT.BOLD);
        } else if (type == TYPE_PARSER)
        {
            Color color = UIPlugin.getDefault().getColorManager().getColor(ITLAConsoleConstants.CONSOLE_PARSER_COLOR);
            this.stream.setColor(color);
            this.stream.setFontStyle(SWT.BOLD);
        }
        
    }
    
    /**
     * Writes to console
     * @param text
     */
    public void appendText(String data)
    {
        String text =  ( (data.endsWith("\n")) ? data : data + "\n");
        
        try 
        {
            this.activate();
            this.stream.write(text);
        } catch (IOException e) 
        {
            // e.printStackTrace();
        }
    }
}

/*
 * $Log: TLAConsole.java,v $
 * Revision 1.1  2007/01/29 22:25:04  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:48:31  szambrovski
 * *** empty log message ***
 *
 * Revision 1.3  2004/10/23 16:39:37  sza
 * console fix
 *
 * Revision 1.2  2004/10/23 16:09:39  sza
 * new console supported
 *
 * Revision 1.1  2004/10/20 13:59:12  sza
 * console init
 *
 *
 */