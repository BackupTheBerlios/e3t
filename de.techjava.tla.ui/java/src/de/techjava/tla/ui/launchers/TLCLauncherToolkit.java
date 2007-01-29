package de.techjava.tla.ui.launchers;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

import de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration;


/**
 * Toolkit for various TLC launch configuration tasks 
 *
 * @author Boris Gruschko ( http://gruschko.org, boris at gruschko.org )
 * @version $Id: TLCLauncherToolkit.java,v 1.1 2007/01/29 22:29:22 tlateam Exp $
 */
public class TLCLauncherToolkit
{

	/**
	 * toolkit method for setting the dafult configuration swiches. This method
	 * is needed for the launch shortcuts
	 * 
	 * @param configuration working copy to be set
	 */
	public static void setDefaultSwitchesConfiguration( ILaunchConfigurationWorkingCopy configuration )
	{
		configuration.setAttribute( 
				ITLCModelCheckConfiguration.MODEL_RUN_DEPTH, ITLCModelCheckConfiguration.MODEL_RUN_DEPTH_DEFAULT);
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK,
				ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK_DEFAULT);
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE,
				ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_RUN_DEPTH,
				ITLCModelCheckConfiguration.MODEL_RUN_DEPTH_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED,
				ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WITH_SEED,
				ITLCModelCheckConfiguration.MODEL_WITH_SEED_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL,
				ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WITH_ARIL,
				ITLCModelCheckConfiguration.MODEL_WITH_ARIL_DEFAULT);
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE,
				ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE_DEFAULT);
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM,
				ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_RECOVER_FROM,
				ITLCModelCheckConfiguration.MODEL_RECOVER_FROM_DEFAULT);
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE,
				ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_DIFF_TRACE,
				ITLCModelCheckConfiguration.MODEL_DIFF_TRACE_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_TERSE,
				ITLCModelCheckConfiguration.MODEL_TERSE_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WORKER_COUNT,
				ITLCModelCheckConfiguration.MODEL_WORKER_COUNT_DEFAULT);
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_NO_WARNINGS,
				ITLCModelCheckConfiguration.MODEL_NO_WARNINGS_DEFAULT);		
	}
	
}

/*-
 * $Log: TLCLauncherToolkit.java,v $
 * Revision 1.1  2007/01/29 22:29:22  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.1  2004/10/27 09:15:25  bgr
 * defaults method extracted into a toolkit class
 *
 */