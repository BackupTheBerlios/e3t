package de.techjava.tla.ui.launchers;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;

/**
 * Configuration tab group for the TLC launch
 * 
 * @author Boris Gruschko, <a href="http://gruschko.org">http://gruschko.org</a> 
 * @version $Id: TLCLaunchConfigurationTabGroup.java,v 1.1 2007/01/29 22:29:22 tlateam Exp $
 */
public class TLCLaunchConfigurationTabGroup 
	extends AbstractLaunchConfigurationTabGroup 
{

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog, java.lang.String)
     */
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) 
    {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {
                new TLCLaunchMainConfigurationTab(),
				new TLCLaunchSwitchesConfigurationTab(),
                new CommonTab()
        };
        
        setTabs(tabs);
    }

}

/*
 * $Log: TLCLaunchConfigurationTabGroup.java,v $
 * Revision 1.1  2007/01/29 22:29:22  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.4  2004/10/26 12:53:47  sza
 * renaming
 *
 * Revision 1.3  2004/10/25 10:09:10  bgr
 * apply button handling added
 *
 * Revision 1.2  2004/10/13 17:14:31  bgr
 * launcher built
 *
 * Revision 1.1  2004/10/12 16:21:38  sza
 * initial commit
 *
 * Revision 1.2  2004/10/12 09:54:47  sza
 * running copy
 *
 * Revision 1.1  2004/10/11 19:39:43  bgr
 * initial load
 *
 *
 */