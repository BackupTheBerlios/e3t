package de.techjava.tla.ui.launchers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import de.techjava.tla.ui.UIPlugin;
import de.techjava.tla.ui.util.ITLAProjectConstants;
import de.techjava.tla.ui.widgets.IFileSelectionListener;
import de.techjava.tla.ui.widgets.IProjectSelectionListener;
import de.techjava.tla.ui.widgets.ProjectSelectionWidget;
import de.techjava.tla.ui.widgets.TLAFileSelectionWidget;

/**
 * Tab for the TLC setting
 * 
 * @author Boris Gruschko, <a href="http://gruschko.org">http://gruschko.org</a> 
 * @version $Id: TLCLaunchMainConfigurationTab.java,v 1.1 2007/01/29 22:29:22 tlateam Exp $
 */
public class TLCLaunchMainConfigurationTab 
	extends AbstractLaunchConfigurationTab 
	implements ITLCLaunchUIConstants
{
    private ProjectSelectionWidget	project;
    private TLAFileSelectionWidget	mainFile;
    private TLAFileSelectionWidget  configFile;
    private Button					useDefaultConfigFileButton;

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) 
    {
        Composite control = new Composite( parent, SWT.FILL );
        GridLayout controlLayout = new GridLayout(1, true);
        
        control.setLayout( controlLayout );
        
        project						=	new ProjectSelectionWidget( control );
        mainFile					=	new TLAFileSelectionWidget( control, ITLAProjectConstants.TLA_FILE_EXTENSION, "Main file", "Select TLA+ main file" );
        useDefaultConfigFileButton 	= 	new Button( control, SWT.CHECK );
        configFile					=	new TLAFileSelectionWidget( control, ITLAProjectConstants.CFG_FILE_EXTENSION, "Config file", "Select TLA+ config file" );
        
        
        useDefaultConfigFileButton.setText("Use default configuration file");
        useDefaultConfigFileButton.setSelection(true);

        project.addProjectSelectionListener(mainFile);
        project.addProjectSelectionListener(configFile);

        useDefaultConfigFileButton.addSelectionListener(new SelectionListener()
                {
            		/**
            		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
            		 */
                    public void widgetSelected(SelectionEvent e) 
                    {
                        setDirty(true);
                        configFile.setEnabled( !useDefaultConfigFileButton.getSelection() );
                        updateLaunchConfigurationDialog();
                    }
                    /**
                     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
                     */
                    public void widgetDefaultSelected(SelectionEvent e)
                    {
                        widgetSelected(e);
                    }
            
                }
        );        
        project.addProjectSelectionListener( 
        		new IProjectSelectionListener()
				{

					public void projectSelected(IProject project)
					{
					    setDirty(true);
						updateLaunchConfigurationDialog();
					}
        			
				}
        );
        
        mainFile.addFileSelectionListener( 
                
                new IFileSelectionListener() {

					public void fileSelected(IFile file)
					{
					    setDirty(true);
						updateLaunchConfigurationDialog();
					}
				} 
        );
        configFile.addFileSelectionListener( 
                
                new IFileSelectionListener() {

					public void fileSelected(IFile file)
					{
					    setDirty(true);
					    
						updateLaunchConfigurationDialog();
					}
				} 
        );
        
        configFile.setEnabled( !useDefaultConfigFileButton.getSelection() );
        setControl(control);
        
    }


    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) 
    {
    	// no defaults here
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom(ILaunchConfiguration configuration) 
    {
        try 
        {
            project.setProject(configuration.getAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_PROJECT, ITLCLaunchConfigurationConstants.DEFAULT_LAUNCH_PROJECT));
            mainFile.setMainFile(configuration.getAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_FILE, ITLCLaunchConfigurationConstants.DEFAULT_LAUNCH_FILE));
            String configFileName = configuration.getAttribute( ITLCLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, "");
            configFile.setMainFile(configFileName);
            if ("".equals(configFileName)) 
            {
                useDefaultConfigFileButton.setSelection(true);
            } else {
                useDefaultConfigFileButton.setSelection(false);
            }
            
            configFile.setEnabled( !useDefaultConfigFileButton.getSelection() );
            
        } catch (CoreException ce)
        {
            ce.printStackTrace();
        }
    }
    
    public boolean isValid(ILaunchConfiguration launchConfig) 
    {
        return true;
    }
    
    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply(ILaunchConfigurationWorkingCopy configuration) 
    {
    	if (project.getProject() != null 
    	        && mainFile.getMainFile() != null
    	) 
        {
            configuration.setAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_PROJECT, project.getProject().getName());
            configuration.setAttribute( ITLCLaunchConfigurationConstants.ATTR_LAUNCH_FILE, mainFile.getMainFile());
            String configFileName = "";
            if (!useDefaultConfigFileButton.getSelection()) 
            {
                configFileName = configFile.getMainFile();
            }
            configuration.setAttribute( ITLCLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, configFileName);
        }
    	
    }
    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() 
    {
        return TAB_MAIN_NAME;
    }
    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    public Image getImage() 
    {
        return UIPlugin.getDefault().getImageDescriptor(TAB_MAIN_ICON_NAME).createImage();
    }
}

/*
 * $Log: TLCLaunchMainConfigurationTab.java,v $
 * Revision 1.1  2007/01/29 22:29:22  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/26 14:24:22  sza
 * configuration file added
 *
 * Revision 1.1  2004/10/26 12:53:47  sza
 * renaming
 *
 * Revision 1.6  2004/10/25 16:49:20  sza
 * config file selection
 *
 * Revision 1.5  2004/10/25 16:36:52  sza
 * config file selection
 *
 * Revision 1.4  2004/10/25 11:22:23  bgr
 * file type extensions added
 *
 * Revision 1.3  2004/10/25 10:58:05  bgr
 * only TLA files can be selected
 *
 * Revision 1.2  2004/10/25 10:18:40  sza
 * icons added
 *
 * Revision 1.1  2004/10/25 10:09:10  bgr
 * apply button handling added
 *
 * Revision 1.4  2004/10/14 23:04:20  bgr
 * number format checking added
 *
 * Revision 1.3  2004/10/13 17:14:31  bgr
 * launcher built
 *
 * Revision 1.2  2004/10/13 11:13:24  bgr
 * layout fixed
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