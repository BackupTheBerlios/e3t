package de.techjava.tla.ui.launchers;

import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;

import de.techjava.tla.ui.UIPlugin;

/**
 * 
 * Launcher shortcut
 *
 * @author Boris Gruschko ( http://gruschko.org, boris at gruschko.org )
 * @version $Id: TLCLaunchShortcut.java,v 1.1 2007/01/29 22:29:22 tlateam Exp $
 */
public class TLCLaunchShortcut 
	implements ILaunchShortcut 
{

    /**
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.jface.viewers.ISelection, java.lang.String)
     */
    public void launch(ISelection selection, String mode) 
    {
        System.out.println("Launch pressed");
        // TODO implement
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection stSelection = (IStructuredSelection) selection;
            for(Iterator i = stSelection.iterator();i.hasNext();)
            {
                IFile resource = (IFile)i.next();

                launch(resource.getProject(), resource, mode);
            }
        }
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchShortcut#launch(org.eclipse.ui.IEditorPart, java.lang.String)
     */
    public void launch(IEditorPart editor, String mode) 
    {
        IResource spec = (IResource)editor.getEditorInput().getAdapter( IResource.class );
     
        launch( spec.getProject(), spec, mode );
    }
    
    private void launch( IProject project, IResource spec, String mode )
    {
    	try {
			ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(getLaunchConfigurationType());

			ILaunchConfiguration config = null;
			

			for ( int i = 0; i < configs.length; i++ ) {
				if ( configs[i].getName().equals( spec.getName() ) ) {
					config = configs[i];
					break;
				}
			}
			
			if ( config == null ) {
				config = createConfig(project, spec);
			}
			
			DebugUITools.launch(config, mode);
    	} catch (CoreException e) {
			UIPlugin.logError( e.getMessage(), e );
		}
    }
    
    private ILaunchConfiguration createConfig( IProject project, IResource spec ) 
    	throws CoreException
    {
    	ILaunchConfigurationWorkingCopy configWc = getLaunchConfigurationType().newInstance( null, spec.getName() );
    		
    	TLCLauncherToolkit.setDefaultSwitchesConfiguration(configWc);
    	
    	configWc.setAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_PROJECT,  project.getName());
    	configWc.setAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_FILE,  spec.getFullPath().toString());
    	
    	return configWc.doSave();
    }
    
    private ILaunchConfigurationType getLaunchConfigurationType()
    {
    	return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(
    			ITLCLaunchConfigurationConstants.LAUNCH_CONFIGURATION_TYPE );
    }
}

/*
 * $Log: TLCLaunchShortcut.java,v $
 * Revision 1.1  2007/01/29 22:29:22  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.5  2004/10/27 09:15:25  bgr
 * defaults method extracted into a toolkit class
 *
 * Revision 1.4  2004/10/27 09:11:37  bgr
 * defaults extracted into a new method
 *
 * Revision 1.2  2004/10/26 16:57:21  bgr
 * launch shortcuts added
 *
 * Revision 1.1  2004/10/26 12:53:47  sza
 * renaming
 *
 * Revision 1.1  2004/10/26 12:41:02  sza
 * renamed
 *
 * Revision 1.1  2004/10/13 23:11:59  sza
 * init
 *
 *
 */