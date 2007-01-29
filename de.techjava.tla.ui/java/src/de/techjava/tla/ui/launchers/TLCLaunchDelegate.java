package de.techjava.tla.ui.launchers;

import java.util.Properties;

import org.eclipse.core.internal.resources.Container;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;

import de.techjava.tla.ui.UIPlugin;
import de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration;
import de.techjava.tla.ui.extensions.ITLCModelCheckRuntime;
import de.techjava.tla.ui.util.ITLAProjectConstants;
import de.techjava.tla.ui.util.ProjectUtils;
import de.techjava.tla.ui.util.TLACore;

/**
 * Deledate which handles TLC launches
 * 
 * @author Boris Gruschko, <a href="http://gruschko.org">http://gruschko.org</a> 
 * @version $Id: TLCLaunchDelegate.java,v 1.1 2007/01/29 22:29:22 tlateam Exp $
 */
public class TLCLaunchDelegate 
	implements ILaunchConfigurationDelegate
{

    /**
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void launch(
            ILaunchConfiguration configuration, 
            String mode,
            ILaunch launch, 
            IProgressMonitor monitor) 
    	throws CoreException 
    {
        monitor.beginTask("Launching TLA+ Model Checker", 10);
        
        try 
        {
            monitor.subTask("Setting up the runtime environment");
            monitor.worked(1);
            String launchProjectName 	= configuration.getAttribute(ITLCLaunchConfigurationConstants.ATTR_LAUNCH_PROJECT, "");
            String launchFileName 		= configuration.getAttribute(ITLCLaunchConfigurationConstants.ATTR_LAUNCH_FILE, "");
            String configFileName 		= configuration.getAttribute(ITLCLaunchConfigurationConstants.ATTR_CONFIG_FILE_NAME, "");
            
            IProject project = TLACore.getProjectByName(launchProjectName);
            if (project != null)
            {
            	IEclipsePreferences projectNode = ProjectUtils.getProjectNode(project);
            	
            	if ( projectNode != null ) 
            	{
            		String projectSource = projectNode.get( ITLAProjectConstants.PERSIST_PROJECT_SOURCE_FOLDER, ITLAProjectConstants.DEFAULT_ROOT_FOLDER);
            		String projectConfig = projectNode.get( ITLAProjectConstants.PERSIST_PROJECT_CONFIG_FOLDER, ITLAProjectConstants.DEFAULT_ROOT_FOLDER);
            		String projectWork = projectNode.get( ITLAProjectConstants.PERSIST_PROJECT_WORKING_FOLDER, ITLAProjectConstants.DEFAULT_TLA_PROJECT_LAYOUT_WORKINGDIR);
            		
            		IPath projectPath = project.getLocation().addTrailingSeparator();
                    IPath sourcePath  = projectPath;
                    IPath configPath  = projectPath;
                    IPath workPath    = projectPath;
            		
                    if (!projectSource.equals(ITLAProjectConstants.DEFAULT_ROOT_FOLDER))
                    {
            		    sourcePath = projectPath.append(projectSource).addTrailingSeparator();
                    }
                    if (!projectConfig.equals(ITLAProjectConstants.DEFAULT_ROOT_FOLDER))
                    {
            		    configPath = projectPath.append(projectConfig).addTrailingSeparator();
                    }

                    workPath = projectPath.append(projectWork).addTrailingSeparator();

            		ITLCModelCheckConfiguration modelCheckConfig 	= UIPlugin.getDefault().getExtensionManager().getModelCheckConfiguration();
            		ITLCModelCheckRuntime 		runtime 			= UIPlugin.getDefault().getExtensionManager().getModelCheckerRuntime();

            		
            		Properties switches = new Properties();
            		
            		switches.put( ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK,
								 new Boolean( configuration.getAttribute( ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK, 
								 		ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK_DEFAULT ) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE,ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM, new Boolean(configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM,ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_RECOVER_FROM, configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_RECOVER_FROM,ITLCModelCheckConfiguration.MODEL_RECOVER_FROM_DEFAULT) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE, new Boolean(configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE,ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_DIFF_TRACE, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_DIFF_TRACE,ITLCModelCheckConfiguration.MODEL_DIFF_TRACE_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_TERSE, new Boolean(configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_TERSE,ITLCModelCheckConfiguration.MODEL_TERSE_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_WORKER_COUNT, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_WORKER_COUNT,ITLCModelCheckConfiguration.MODEL_WORKER_COUNT_DEFAULT) ) );
            		switches.put( ITLCModelCheckConfiguration.MODEL_NO_WARNINGS, new Boolean(configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_NO_WARNINGS,ITLCModelCheckConfiguration.MODEL_NO_WARNINGS_DEFAULT) ) );

            		// simulation
            		switches.put( ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE, new Boolean( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE,ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE_DEFAULT ) ));

            		if ( ((Boolean)switches.get(ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE)).booleanValue() ) { 
	            		switches.put( ITLCModelCheckConfiguration.MODEL_RUN_DEPTH, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_RUN_DEPTH,ITLCModelCheckConfiguration.MODEL_RUN_DEPTH_DEFAULT ) ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED, new Boolean( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED,ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED_DEFAULT) ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_WITH_SEED, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_WITH_SEED,ITLCModelCheckConfiguration.MODEL_WITH_SEED_DEFAULT) ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL, new Boolean(configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL,ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL_DEFAULT) ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_WITH_ARIL, new Integer( configuration.getAttribute(ITLCModelCheckConfiguration.MODEL_WITH_ARIL,ITLCModelCheckConfiguration.MODEL_WITH_ARIL_DEFAULT) ) );
            		} else {
	            		switches.put( ITLCModelCheckConfiguration.MODEL_RUN_DEPTH, new Integer( ITLCModelCheckConfiguration.MODEL_RUN_DEPTH_DEFAULT ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED, new Boolean( ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED_DEFAULT) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_WITH_SEED, new Integer( ITLCModelCheckConfiguration.MODEL_WITH_SEED_DEFAULT ) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL, new Boolean(ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL_DEFAULT) );
	            		switches.put( ITLCModelCheckConfiguration.MODEL_WITH_ARIL, new Integer( ITLCModelCheckConfiguration.MODEL_WITH_ARIL_DEFAULT) );
            		}
            		
            		modelCheckConfig.setSwitches(switches);
            		
            		monitor.worked(3);

                    modelCheckConfig.setRootDirectory( sourcePath );
                    modelCheckConfig.setConfigDirectory( configPath );
                    
                    if ("".equals(configFileName))
                    {
                        // no configuration file specified, using default
                        modelCheckConfig.setConfigFileName(null);
                    } else 
                    {
                        // configuration file explicit set using it
                        
                        IResource configFile = ((Container)project.findMember(projectConfig)).findMember(new Path(configFileName).lastSegment());
                        modelCheckConfig.setConfigFileName(configFile);
                        
                    }
                    
                    modelCheckConfig.setWorkingDirectory( workPath );
                    modelCheckConfig.setModuleLibraryPath( UIPlugin.getDefault().getSANYManager().getLibraries() );

                    
                    monitor.subTask("Locating module to check");

                    IResource	resource = ((Container)project.findMember(projectSource)).findMember(new Path(launchFileName).lastSegment());
                    
                    monitor.worked(4);
                    if ( resource != null ) 
                    {
                        monitor.subTask("Starting Model Checker");
                        monitor.worked(5);
                    	runtime.startCheck( new IResource[] { resource } );
                    	monitor.subTask("Finished");
                    	monitor.done();
                    }
            	}
        	}
        } catch ( Exception e ) 
        {
            e.printStackTrace();
        }
    }

}

/*
 * $Log: TLCLaunchDelegate.java,v $
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
 * Revision 1.9  2004/10/25 16:35:59  bgr
 * switches grouped
 *
 * Revision 1.8  2004/10/20 14:01:39  bgr
 * runtime configuration written through to the simulator
 *
 * Revision 1.7  2004/10/15 01:16:53  sza
 * working directory added
 *
 * Revision 1.6  2004/10/14 23:04:36  sza
 * switches set
 *
 * Revision 1.5  2004/10/14 22:42:09  sza
 * config directory set
 *
 * Revision 1.4  2004/10/14 20:52:31  bgr
 * checker running
 *
 * Revision 1.3  2004/10/13 17:14:31  bgr
 * launcher built
 *
 * Revision 1.2  2004/10/12 16:47:23  sza
 * removed compilation probelms
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
