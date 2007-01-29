package de.techjava.tla.ui.workingsets;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.dialogs.ResourceSorter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import de.techjava.tla.ui.UIPlugin;
import de.techjava.tla.ui.widgets.provider.TLAFolderProvider;

/**
 * Workingset Page
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: TLAWorkingSet.java,v 1.1 2007/01/29 22:29:23 tlateam Exp $
 */
public class TLAWorkingSet 
	extends WizardPage 
	implements IWorkingSetPage 
{
    private static final String PAGENAME			= "de.techjava.tla.ui.workingsets.TLAWorkingSetPage";
    private static final String TITLE				= "TLA+ Working sets";
    private static final String DESCRIPTION			= "Enter the working set name and select TLA+ source folders";
    private static final String NAME_MESSAGE 		= "Enter the name of working set";
    private static final String TREE_MESSAGE		= "Select TLA+ Source folders";

    private static final int 	TREE_WIDGET_WIDTH_HINT = 50;
    private static final int 	TREE_WIDGET_HEIGHT_HINT = 200;

    
    private IWorkingSet 		workingSet;
    
    private Text 				nameText;
    private CheckboxTreeViewer 	viewer;
    
    private boolean initialCheck = false; // set to true if selection is set in setSelection
    
    /**
     * @param pageName
     */

    public TLAWorkingSet()
    {
        super(PAGENAME, TITLE, null );
    }
    /**
     * @see org.eclipse.ui.dialogs.IWorkingSetPage#finish()
     */
    /**
     * Implements IWorkingSetPage.
     * 
     * @see org.eclipse.ui.dialogs.IWorkingSetPage#finish()
     */
    public void finish() 
    {
        ArrayList resources = new ArrayList(10);
        findCheckedResources(resources, (IContainer) viewer.getInput());
        if (workingSet == null) 
        {
            IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
            workingSet = workingSetManager.createWorkingSet( 
                    getWorkingSetName(), 
                    (IAdaptable[]) resources.toArray(new IAdaptable[resources.size()])
            );
        } else 
        {
            workingSet.setName(getWorkingSetName());
            workingSet.setElements((IAdaptable[]) resources.toArray(new IAdaptable[resources.size()]));
        }
    }
    /**
     * Finds selected resources
     * @param checkedResources
     * @param container
     */
    private void findCheckedResources(List checkedResources, IContainer container) 
    {
        IResource[] resources = null;
        try 
        {
            resources = container.members();
        } catch (CoreException ex) {
            UIPlugin.logError("Error finding selected resources", ex);
        }
        for (int i = 0; i < resources.length; i++) 
        {
            if (viewer.getGrayed(resources[i])) 
            {
                // partly selected
                if (resources[i].isAccessible()) 
                {
                    findCheckedResources(checkedResources, (IContainer) resources[i]);
                }
                else
                {
                    addWorkingSetElements(checkedResources, (IContainer) resources[i]);
                }
            } else if (viewer.getChecked(resources[i])) 
            {
                // all selected
                checkedResources.add(resources[i]);
            }
        }
    }
    
    /**
     * Adds working set elements contained in the given container to the list
     * of checked resources.
     * 
     * @param collectedResources list of collected resources
     * @param container container to collect working set elements for
     */
    private void addWorkingSetElements( List collectedResources, IContainer container) 
    {
        IAdaptable[] elements = workingSet.getElements();
        IPath containerPath = container.getFullPath();

        for (int i = 0; i < elements.length; i++) 
        {
            IResource resource = null;

            if (elements[i] instanceof IResource) 
            {
                resource = (IResource) elements[i];
            }
            else
            {
                resource = (IResource) elements[i].getAdapter(IResource.class);
            }
            if (resource != null) 
            {
                IPath resourcePath = resource.getFullPath();
                if (containerPath.isPrefixOf(resourcePath)) 
                {
                    collectedResources.add(elements[i]);
                }
            }
        }
    }


    /**
     * @see org.eclipse.ui.dialogs.IWorkingSetPage#getSelection()
     */
    public IWorkingSet getSelection() 
    {
        return this.workingSet;
    }
    
    /**
     * Returns the name entered in the working set name field.
     * 
     * @return the name entered in the working set name field.
     */
    private String getWorkingSetName() 
    {
        return nameText.getText();
    }

    /**
     * @see org.eclipse.ui.dialogs.IWorkingSetPage#setSelection(org.eclipse.ui.IWorkingSet)
     */
    public void setSelection(IWorkingSet workingSet) 
    {
        if (workingSet == null) 
        {
            throw new IllegalArgumentException("Working set must not be null");
        }
        this.workingSet = workingSet;
        if (getShell() != null && nameText != null) 
        {
            initialCheck = true;
            initializeCheckedState();
            nameText.setText(workingSet.getName());
        }
    }

    /**
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl(Composite parent) 
    {
// control creation
        Composite 	composite 		= new Composite(parent, SWT.NULL);
        Label 		nameLabel 		= new Label(composite, SWT.WRAP);
        			nameText 		= new Text(composite, SWT.SINGLE | SWT.BORDER);
        Label	    treeLabel 		= new Label(composite, SWT.WRAP);
        			viewer 			= new CheckboxTreeViewer(composite);
        			
// layout         			
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
        
        nameLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));
        nameText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
        treeLabel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER));

        GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
        data.heightHint = TREE_WIDGET_HEIGHT_HINT;
        data.widthHint 	= TREE_WIDGET_WIDTH_HINT;
        viewer.getControl().setLayoutData(data);


        setPageComplete(false);

// listener and data binding
        nameText.addModifyListener(new ModifyListener() 
                {
		            public void modifyText(ModifyEvent e) {
		                validateInput();
		            }
                }
        );
        viewer.setUseHashlookup(true);
        viewer.setContentProvider(new TLAFolderProvider(true, false, false));
        
        viewer.setLabelProvider(new DecoratingLabelProvider(
                new WorkbenchLabelProvider(), IDEWorkbenchPlugin.getDefault()
                        .getWorkbench().getDecoratorManager()
                        .getLabelDecorator()));
        viewer.setInput(IDEWorkbenchPlugin.getPluginWorkspace().getRoot());
        viewer.setSorter(new ResourceSorter(ResourceSorter.NAME));
        viewer.addCheckStateListener(new ICheckStateListener() 
                {
		            public void checkStateChanged(CheckStateChangedEvent event) 
		            {
		                handleCheckStateChange(event);
		            }
                }
        );

        viewer.addTreeListener(new ITreeViewerListener() 
                {
		            public void treeCollapsed(TreeExpansionEvent event) 
		            {
		            }
		
		            public void treeExpanded(TreeExpansionEvent event) 
		            {
		                final Object element = event.getElement();
		                if (viewer.getGrayed(element) == false)
		                {
		                    BusyIndicator.showWhile(getShell().getDisplay(),
		                            new Runnable() {
		                                public void run() 
		                                {
		                                    setSubtreeChecked((IContainer) element, viewer.getChecked(element), false);
		                                }
		                            }
		                    );
		                }
		            }
                }
        );
        nameLabel.setText(NAME_MESSAGE);
        treeLabel.setText(TREE_MESSAGE);

        
        initializeCheckedState();
        if (this.workingSet != null) 
        {
            nameText.setText(workingSet.getName());
        }

        nameText.setFocus();
        
        setControl(composite);
    }

    /**
     * Called when the checked state of a tree items changes.
     * 
     * @param event the checked state change event.
     */
    private void handleCheckStateChange(final CheckStateChangedEvent event) 
    {
        BusyIndicator.showWhile(
                getShell().getDisplay(), 
                new Runnable() 
                {
		            public void run() {
		                IResource resource = (IResource) event.getElement();
		                boolean state = event.getChecked();
		
		                viewer.setGrayed(resource, false);
		                if (resource instanceof IContainer) 
		                {
		                    setSubtreeChecked((IContainer) resource, state, true);
		                }
		                updateParentState(resource);
		                validateInput();
		            }
                }
        );
    }
    
    /**
     * Validates the working set name and the checked state of the 
     * resource tree.
     */
    private void validateInput() 
    {
        String errorMessage = null; 
        String newText = nameText.getText();

        if (newText.equals(newText.trim()) == false) {
            errorMessage = "Don't use leading or trailing whitespaces in the name of working set";
        } else if (initialCheck) 
        {
            initialCheck = false;
            return;
        }
        if (newText.equals("")) 
        { 
            errorMessage = "Working set name must be not empty"; 
        }
        if (errorMessage == null
                && (workingSet == null || newText.equals(workingSet.getName()) == false)) 
        {
            IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
            for (int i = 0; i < workingSets.length; i++) 
            {
                if (newText.equals(workingSets[i].getName())) 
                {
                    errorMessage = "Working set with given name already exists";
                }
            }
        }
        if (errorMessage == null && viewer.getCheckedElements().length == 0) 
        {
            errorMessage = "At least one resource must be selected";
        }
        setMessage(errorMessage, WizardPage.ERROR);
        setPageComplete(errorMessage == null);
    }
    
    
    /**
     * Sets the checked state of tree items based on the initial 
     * working set, if any.
     */
    private void initializeCheckedState() 
    {
        if (workingSet == null) 
        {
            return;
        }
        
        BusyIndicator.showWhile(getShell().getDisplay(), 
                new Runnable() 
                {
	            public void run() {
	                IAdaptable[] items = workingSet.getElements();
	                viewer.setCheckedElements(items);
	                for (int i = 0; i < items.length; i++) 
	                {
	                    IAdaptable item = items[i];
	                    IContainer container = null;
	                    IResource resource = null;
	
	                    if (item instanceof IContainer) 
	                    {
	                        container = (IContainer) item;
	                    } else {
	                        container = (IContainer) item.getAdapter(IContainer.class);
	                    }
	                    if (container != null) 
	                    {
	                        setSubtreeChecked(container, true, true);
	                    }
	                    if (item instanceof IResource) 
	                    {
	                        resource = (IResource) item;
	                    } else {
	                        resource = (IResource) item.getAdapter(IResource.class);
	                    }
	                    if (resource != null && resource.isAccessible() == false) 
	                    {
	                        IProject project = resource.getProject();
	                        if (viewer.getChecked(project) == false) 
	                        {
	                            viewer.setGrayChecked(project, true);
	                        }
	                    } else {
	                        updateParentState(resource);
	                    }
	                }
	            }
            }
        );
    }
    
    /**
     * Sets the checked state of the container's members.
     * 
     * @param container the container whose children should be checked/unchecked
     * @param state true=check all members in the container. false=uncheck all 
     * 	members in the container.
     * @param checkExpandedState true=recurse into sub-containers and set the 
     * 	checked state. false=only set checked state of members of this container
     */
    private void setSubtreeChecked(
            IContainer container, 
            boolean state,
            boolean checkExpandedState) 
    {
        // checked state is set lazily on expand, don't set it if container is collapsed
        
        if (container.isAccessible() == false
                || (viewer.getExpandedState(container) == false && state && checkExpandedState)) 
        {
            return;
        }
        IResource[] members = null;
        try {
            members = container.members();
        } catch (CoreException ex) 
        {
            UIPlugin.logError("Error updaing workingset page", ex);
        }
        for (int i = members.length - 1; i >= 0; i--) 
        {
            IResource element = members[i];
            boolean elementGrayChecked = (viewer.getGrayed(element) || viewer.getChecked(element));

            if (state) 
            {
                viewer.setChecked(element, true);
                viewer.setGrayed(element, false);
            } else {
                viewer.setGrayChecked(element, false);
            }
            // unchecked state only needs to be set when the container is 
            // checked or grayed
            if (element instanceof IContainer && (state || elementGrayChecked)) 
            {
                setSubtreeChecked((IContainer) element, state, true);
            }
        }
    }
    
    /**
     * Check and gray the resource parent if all resources of the 
     * parent are checked.
     * 
     * @param child the resource whose parent checked state should 
     * 	be set.
     */
    private void updateParentState(IResource child) {
        if (child == null || child.getParent() == null)
            return;

        IContainer parent = child.getParent();
        boolean childChecked = false;
        IResource[] members = null;
        try {
            members = parent.members();
        } catch (CoreException ex) 
        {
            UIPlugin.logError("Error updating working set parent state", ex);
        }
        for (int i = members.length - 1; i >= 0; i--) 
        {
            if (viewer.getChecked(members[i]) || viewer.getGrayed(members[i])) 
            {
                childChecked = true;
                break;
            }
        }
        viewer.setGrayChecked(parent, childChecked);
        updateParentState(parent);
    }

}

/*
 * $Log: TLAWorkingSet.java,v $
 * Revision 1.1  2007/01/29 22:29:23  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:36  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/25 13:52:36  sza
 * fixed
 *
 * Revision 1.1  2004/10/13 23:12:53  sza
 * working set
 *
 *
 */