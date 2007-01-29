package de.techjava.tla.ui.editors.outline;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import de.techjava.tla.ui.editors.TLAEditor;


/**
 * TLA's outline page
 *
 * @author Boris Gruschko ( Lufthansa Systems Business Solutions GmbH )
 * @version $Id: TLAOutlinePage.java,v 1.1 2007/01/29 22:29:23 tlateam Exp $
 */
public class TLAOutlinePage
		extends ContentOutlinePage
{
	private TLAEditor editor;
	
	public TLAOutlinePage( TLAEditor editor ) {
		this.editor = editor;
	}
	

	/**
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent)
	{
		super.createControl(parent);

		TreeViewer view = getTreeViewer();
		
		view.setContentProvider(new TLAContentProvider());
		view.setLabelProvider( new TLALabelProvider() );
	}
}

/*-
 * $Log: TLAOutlinePage.java,v $
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