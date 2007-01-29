package de.techjava.tla.ui.launchers;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import de.techjava.tla.ui.UIPlugin;
import de.techjava.tla.ui.extensions.ITLCModelCheckConfiguration;

/**
 * configures the switches needed for a TLC Launch
 * 
 * @author Boris Gruschko ( Lufthansa Systems Business Solutions GmbH )
 * @version $Id: TLCLaunchSwitchesConfigurationTab.java,v 1.2 2004/10/14
 *          23:04:20 bgr Exp $
 */
public class TLCLaunchSwitchesConfigurationTab
		extends AbstractLaunchConfigurationTab
		implements ITLCLaunchUIConstants, ITriggerPuller
{

	private Button					modelCheckDeadlockButton;
	private Button					runInSimulateModeButton;
	private Text						runDepthText;
	private Button					useSeedButton;
	private Text						seedText;
	private Button					useArilButton;
	private Text						arilText;
	private Text						coverageText;
	private Button					useRecoverFrom;
	private Text						recoverText;
	private Button					useDiffTrace;
	private Text						diffTraceText;
	private Button					terseButton;
	private Text						workerCountText;
	private Button					noWarningButton;

	private List					triggers	=	new LinkedList();
	
	private ModifyListener	modifyListener	= new ModifyListener()
	{
		
		public void modifyText(ModifyEvent e)
		{
			setDirty(true);
			getLaunchConfigurationDialog()
			.updateMessage();
			getLaunchConfigurationDialog()
			.updateButtons();
		}
		
	};

	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		Composite control = new Composite(parent, SWT.FILL);

		GridLayout controlLayout = new GridLayout(2, false);

		control.setLayout(controlLayout);

		modelCheckDeadlockButton = createCheckButton(control,
				"Don't check deadlock");
		new Label(control, SWT.SINGLE);
		modelCheckDeadlockButton.addSelectionListener( new _CheckBoxUpdateListener() );

		(new Label(control, SWT.SINGLE)).setText("Coverage");
		coverageText = new Text(control, SWT.BORDER | SWT.SINGLE);
		coverageText.addModifyListener(modifyListener);

		useRecoverFrom = createCheckButton(control, "Recover from");
		recoverText = new Text(control, SWT.BORDER | SWT.SINGLE);
		recoverText.addModifyListener(modifyListener);
		attachButtonToField(useRecoverFrom, recoverText);

		useDiffTrace = createCheckButton(control, "Diff trace");
		diffTraceText = new Text(control, SWT.BORDER | SWT.SINGLE);
		diffTraceText.addModifyListener(modifyListener);
		attachButtonToField(useDiffTrace, diffTraceText);

		terseButton = createCheckButton(control, "Terse");
		new Label(control, SWT.SINGLE);
		terseButton.addSelectionListener( new _CheckBoxUpdateListener() );

		(new Label(control, SWT.SINGLE)).setText("Worker count");
		workerCountText = new Text(control, SWT.BORDER | SWT.SINGLE);
		workerCountText.addModifyListener(modifyListener);

		noWarningButton = createCheckButton(control, "No warning");
		new Label(control, SWT.SINGLE);
		noWarningButton.addSelectionListener( new _CheckBoxUpdateListener() );

		// simulation
		createSeparator(control, 2);
		
		GridData	simulationGroupData	=	new GridData();
		
		simulationGroupData.horizontalSpan = 2;
		
		runInSimulateModeButton = createCheckButton(control, "Run in simulate mode");
		new Label(control, SWT.SINGLE);
		runInSimulateModeButton.addSelectionListener( new _CheckBoxUpdateListener() );
		
		(new Label(control, SWT.SINGLE)).setText("Run depth");
		runDepthText = new Text(control, SWT.BORDER | SWT.SINGLE);
		runDepthText.addModifyListener(modifyListener);
		attachButtonToField(runInSimulateModeButton, runDepthText);
		
		useSeedButton = createCheckButton(control, "Seed");
		seedText = new Text(control, SWT.BORDER | SWT.SINGLE);
		seedText.addModifyListener(modifyListener);

		new DoubleStateListener( runInSimulateModeButton, useSeedButton, seedText, this );
		
		useArilButton = createCheckButton(control, "Aril");
		arilText = new Text(control, SWT.BORDER | SWT.SINGLE);
		arilText.addModifyListener(modifyListener);

		new DoubleStateListener( runInSimulateModeButton, useArilButton, arilText, this );
		
		fireTriggers();
		
		setControl(control);
	}
	
	
	private void attachButtonToField(final Button button, final Text field)
	{
		button.addSelectionListener(new _SelectionListener(button, field));
		button.setSelection(false);
		field.setEnabled(false);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		TLCLauncherToolkit.setDefaultSwitchesConfiguration(configuration);
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try {
			modelCheckDeadlockButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK,
					ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK_DEFAULT));
			runInSimulateModeButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE,
					ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE_DEFAULT));
			;
			runDepthText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_RUN_DEPTH,
					ITLCModelCheckConfiguration.MODEL_RUN_DEPTH_DEFAULT));
			useSeedButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED,
					ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED_DEFAULT));
			seedText.setEnabled(useSeedButton.getSelection());
			seedText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_WITH_SEED,
					ITLCModelCheckConfiguration.MODEL_WITH_SEED_DEFAULT));
			useArilButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL,
					ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL_DEFAULT));
			arilText.setEnabled(useArilButton.getSelection());
			arilText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_WITH_ARIL,
					ITLCModelCheckConfiguration.MODEL_WITH_ARIL_DEFAULT));
			coverageText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE,
					ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE_DEFAULT));
			useRecoverFrom.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM,
					ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM_DEFAULT));
			recoverText.setEnabled(useRecoverFrom.getSelection());
			recoverText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_RECOVER_FROM,
					ITLCModelCheckConfiguration.MODEL_RECOVER_FROM_DEFAULT));
			useDiffTrace.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE,
					ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE_DEFAULT));
			diffTraceText.setEnabled(useDiffTrace.getSelection());
			diffTraceText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_DIFF_TRACE,
					ITLCModelCheckConfiguration.MODEL_DIFF_TRACE_DEFAULT));
			terseButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_TERSE,
					ITLCModelCheckConfiguration.MODEL_TERSE_DEFAULT));
			workerCountText.setText(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_WORKER_COUNT,
					ITLCModelCheckConfiguration.MODEL_WORKER_COUNT_DEFAULT));
			noWarningButton.setSelection(configuration.getAttribute(
					ITLCModelCheckConfiguration.MODEL_NO_WARNINGS,
					ITLCModelCheckConfiguration.MODEL_NO_WARNINGS_DEFAULT));
			
			fireTriggers();
		}
		catch (CoreException exc) {
			// do nothing
		}
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_CHECK_DEADLOCK,
				modelCheckDeadlockButton.getSelection());
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_RUN_IN_SIMULATE_MODE,
				runInSimulateModeButton.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_RUN_DEPTH,
				runDepthText.getText());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED,
				useSeedButton.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WITH_SEED,
				seedText.getText());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL,
				useArilButton.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WITH_ARIL,
				arilText.getText());
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE, coverageText
						.getText());
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_USE_RECOVER_FROM, useRecoverFrom
						.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_RECOVER_FROM,
				recoverText.getText());
		configuration.setAttribute(
				ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE, useDiffTrace
						.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_DIFF_TRACE,
				diffTraceText.getText());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_TERSE,
				terseButton.getSelection());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_WORKER_COUNT,
				workerCountText.getText());
		configuration.setAttribute(ITLCModelCheckConfiguration.MODEL_NO_WARNINGS,
				noWarningButton.getSelection());

	}
	
	
	public void addTrigger( IValueTrigger trigger )
	{
		this.triggers.add( trigger );
	}
	
	private void fireTriggers()
	{
		for ( Iterator i = triggers.iterator(); i.hasNext(); )
			((IValueTrigger)i.next()).trigger();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName()
	{
		return TAB_ARGUMENTS_NAME;
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage()
	{
		return UIPlugin.getDefault().getImageDescriptor(TAB_ARGUMENTS_ICON_NAME).createImage();
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration cfg)
	{
		boolean isValid;
		try {
			isValid = setError(isNumber(cfg.getAttribute(
					ITLCModelCheckConfiguration.MODEL_RUN_DEPTH, "")),
					"Run depth value must be numerical")
					&& setError(
							(!cfg.getAttribute(
									ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED,
									ITLCModelCheckConfiguration.MODEL_USE_WITH_SEED_DEFAULT) || isNumber(cfg
									.getAttribute(ITLCModelCheckConfiguration.MODEL_WITH_SEED, ""))),
							"Seed value must be numerical")
					&& setError(
							(!cfg.getAttribute(
									ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL,
									ITLCModelCheckConfiguration.MODEL_USE_WITH_ARIL_DEFAULT) || isNumber(cfg
									.getAttribute(ITLCModelCheckConfiguration.MODEL_WITH_ARIL, ""))),
							"Aril value must be numerical")
					&& setError(isNumber(cfg.getAttribute(
							ITLCModelCheckConfiguration.MODEL_PRINT_COVERAGE, "")),
							"Coverage value must be numerical")
					&& setError(
							(!cfg.getAttribute(
									ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE,
									ITLCModelCheckConfiguration.MODEL_USE_DIFF_TRACE_DEFAULT) || isNumber(cfg
									.getAttribute(ITLCModelCheckConfiguration.MODEL_DIFF_TRACE,
											""))), "Diff trace value must be numerical")
					&& setError(isNumber(cfg.getAttribute(
							ITLCModelCheckConfiguration.MODEL_WORKER_COUNT, "")),
							"Worker count value must be numerical");
		}
		catch (CoreException e) {
			return false;
		}

		if ( isValid )
			setErrorMessage(null);

		getLaunchConfigurationDialog().updateMessage();

		return isValid && super.isValid(cfg);
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#canSave()
	 */
	public boolean canSave()
	{
		return super.canSave();
	}

	private boolean setError(boolean setNotSet, String message)
	{
		if ( !setNotSet )
			setErrorMessage(message);

		return setNotSet;
	}

	private boolean isNumber(String str)
	{
		if ( str == null || str.length() == 0 )
			return false;

		char[] chars = str.toCharArray();

		for ( int i = 0; i < chars.length; i++ ) {
			if ( !Character.isDigit(chars[i]) )
				return false;
		}

		return true;
	}

	private final class _SelectionListener
			implements SelectionListener
	{

		private final Button	button;
		private final Text		field;

		private _SelectionListener(Button button, Text field)
		{
			super();
			this.button = button;
			this.field = field;
		}

		public void widgetSelected(SelectionEvent e)
		{
			field.setEnabled( button.getSelection());

			setDirty(true);

			getLaunchConfigurationDialog().updateMessage();
			getLaunchConfigurationDialog().updateButtons();
		}

		public void widgetDefaultSelected(SelectionEvent e)
		{
			widgetSelected(e);
		}
	}
	
	private class _CheckBoxUpdateListener
		implements SelectionListener
	{

		/**
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e)
		{
			setDirty(true);

			getLaunchConfigurationDialog().updateMessage();
			getLaunchConfigurationDialog().updateButtons();
		}

		/**
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e)
		{
			widgetSelected(e);
		}
		
	}
	
	private class DoubleStateListener
		implements SelectionListener, IValueTrigger
	{
		private Button	s1;
		private Button	s2;
		
		private	Text	sink		=	null;
		
		public DoubleStateListener( 
				Button s1, Button s2, Text sink, ITriggerPuller puller )
		{
			this.s1	=	s1;
			this.s2	=	s2;
			
			this.sink	=	sink;
			
			s1.addSelectionListener( this );
			s2.addSelectionListener( this );
		
			puller.addTrigger( this );
			
			computeStates();
		}
		
		/**
		 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetSelected(SelectionEvent e)
		{
			computeStates();
		}
		
		private void computeStates()
		{
			s2.setEnabled( s1.getSelection() );
			
			sink.setEnabled( s1.getSelection() && s2.getSelection() );
			
			TLCLaunchSwitchesConfigurationTab.this.updateLaunchConfigurationDialog();
		}

		/**
		 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
		 */
		public void widgetDefaultSelected(SelectionEvent e)
		{
			widgetSelected(e);
		}

		/**
		 * @see IValueTrigger#trigger()
		 */
		public void trigger()
		{
			computeStates();
		}
		
	}
	
}

/*
 * $Log: TLCLaunchSwitchesConfigurationTab.java,v $
 * Revision 1.1  2007/01/29 22:29:22  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:33  szambrovski
 * sf cvs init
 *
 * Revision 1.10  2004/10/27 09:15:25  bgr
 * defaults method extracted into a toolkit class
 *
 * Revision 1.9  2004/10/27 09:11:45  bgr
 * defaults extracted into a new method
 *
 * Revision 1.8  2004/10/26 12:54:47  sza
 * renaming
 *
 * Revision 1.7  2004/10/25 16:35:59  bgr
 * switches grouped
 *
 * Revision 1.6  2004/10/25 13:51:48  bgr
 * switches grouped
 *
 * Revision 1.5  2004/10/25 11:04:23  bgr
 * recover from bug fixed
 *
 * Revision 1.4  2004/10/25 10:19:35  sza
 * icons added
 *
 * Revision 1.3  2004/10/25 10:09:10  bgr
 * apply button handling added
 * Revision 1.2 2004/10/14
 * 23:04:20 bgr number format checking added
 * 
 * Revision 1.1 2004/10/13 17:14:31 bgr launcher built
 *  
 */