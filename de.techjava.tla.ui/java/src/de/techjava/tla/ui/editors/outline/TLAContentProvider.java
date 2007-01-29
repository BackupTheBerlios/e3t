package de.techjava.tla.ui.editors.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;


/**
 * TLA's outline content provider
 *
 * @author Boris Gruschko ( Lufthansa Systems Business Solutions GmbH )
 * @version $Id: TLAContentProvider.java,v 1.1 2007/01/29 22:29:23 tlateam Exp $
 */
public class TLAContentProvider
		implements ITreeContentProvider
{

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose()
	{
		// TODO Auto-generated method stub
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object parentElement)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	public Object getParent(Object element)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	public boolean hasChildren(Object element)
	{
		// TODO Auto-generated method stub
		return false;
	}

}

/*-
 * $Log: TLAContentProvider.java,v $
 * Revision 1.1  2007/01/29 22:29:23  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:35  szambrovski
 * sf cvs init
 *
 * Revision 1.1  2004/10/20 11:56:14  bgr
 * first outline added
 *
 */