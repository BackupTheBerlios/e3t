package de.techjava.tla.ui.editors.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;


/**
 * DOCME 
 *
 * @author Boris Gruschko ( http://gruschko.org, boris at gruschko.org )
 * @version $Id: TLALabelProvider.java,v 1.1 2007/01/29 22:29:23 tlateam Exp $
 */
public class TLALabelProvider
		implements ILabelProvider
{

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	public void dispose()
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
	 */
	public boolean isLabelProperty(Object element, String property)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener)
	{
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element)
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element)
	{
		// TODO Auto-generated method stub
		return null;
	}

}

/*-
 * $Log: TLALabelProvider.java,v $
 * Revision 1.1  2007/01/29 22:29:23  tlateam
 * *** empty log message ***
 *
 * Revision 1.1  2005/08/22 15:43:35  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/20 16:30:26  sza
 * warnings killed
 *
 * Revision 1.1  2004/10/20 11:56:14  bgr
 * first outline added
 *
 */