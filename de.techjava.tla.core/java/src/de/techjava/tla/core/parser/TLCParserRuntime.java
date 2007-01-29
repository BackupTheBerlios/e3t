package de.techjava.tla.core.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.zambrovski.tla.RuntimeConfiguration;
import org.zambrovski.tla.tlasany.TLASyntaxParser;
import org.zambrovski.tla.tlasany.semantic.ErrorsContainer;
import org.zambrovski.tla.tlasany.semantic.ProblemHolder;

import tlasany.modanalyzer.SpecObj;
import tlasany.st.Location;
import de.techjava.tla.ui.extensions.ILocation;
import de.techjava.tla.ui.extensions.IProblemContainer;
import de.techjava.tla.ui.extensions.IProblemHolder;
import de.techjava.tla.ui.extensions.ITLAParserConfiguration;
import de.techjava.tla.ui.extensions.ITLAParserResult;
import de.techjava.tla.ui.extensions.ITLAParserRuntime;
import de.techjava.tla.ui.views.ConsoleLogger;
import de.techjava.tla.ui.views.LogStream;


/**
 * Parser extension 
 *
 * @author Boris Gruschko ( Lufthansa Systems Business Solutions GmbH )
 * @version $Id: TLCParserRuntime.java,v 1.1 2007/01/29 22:25:04 tlateam Exp $
 */
public class TLCParserRuntime
		implements ITLAParserRuntime, ITLAParserConfiguration
{
	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserRuntime#parse(java.lang.String[])
	 */
	public ITLAParserResult[] parse(String[] resourceNames, IProject project )
	{
	    //ConsoleLogger.getLogger().log("Start parsing");
	    RuntimeConfiguration.get().setOutStream(new LogStream(ConsoleLogger.getLogger().getParserConsole()));
	    RuntimeConfiguration.get().setErrorStream(new LogStream(ConsoleLogger.getLogger().getParserConsole()));
		
	    SpecObj[] specs = TLASyntaxParser.getInstance().parse(resourceNames);
		
		ITLAParserResult[] ret = new ITLAParserResult[specs.length];
		
		for ( int i = 0; i < specs.length; i++ )
		{
			_TLAParserResult res = new _TLAParserResult();

			rewriteErrorsContainer( specs[i].getInitErrors(), (_ProblemContainer)res.initErrors );
			rewriteErrorsContainer( specs[i].getParseErrors(), (_ProblemContainer)res.parseErrors );
			rewriteErrorsContainer( specs[i].getSemanticErrors(), (_ProblemContainer)res.semanticErrors );
		
			ret[i] = res;
		}
		//ConsoleLogger.getLogger().log("Parse finished");		
		return ret;
	}
	
	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserRuntime#flush()
	 */
	public void flush()
	{
		//TODO: add flush implementation here
	}
	
	private void rewriteErrorsContainer( 
			ErrorsContainer container, _ProblemContainer rewritten )
	{
		if ( container.hasWarnings() ) {
			for ( Enumeration e = container.getWarnings(); e.hasMoreElements(); )
			{
				_ProblemHolder holder = new _ProblemHolder();
				
				rewriteProblemHolder( (ProblemHolder)e.nextElement(), holder );
				
				rewritten.warnings.add(holder);
			}
		}
		
		if ( container.hasErrors() ) {
			for ( Enumeration e = container.getErrors(); e.hasMoreElements(); )
			{
				_ProblemHolder holder = new _ProblemHolder();
				
				rewriteProblemHolder( (ProblemHolder)e.nextElement(), holder );
				
				rewritten.errors.add(holder);
			}
		}

		if ( container.hasAborts() ) {
			for ( Enumeration e = container.getAborts(); e.hasMoreElements(); )
			{
				_ProblemHolder holder = new _ProblemHolder();
				
				rewriteProblemHolder( (ProblemHolder)e.nextElement(), holder );
				
				rewritten.aborts.add(holder);
			}
		}

	}
	
	private void rewriteProblemHolder( ProblemHolder holder, _ProblemHolder rewrite )
	{
		rewrite.type 	= holder.type;
		rewrite.message	= holder.message;
		
		_Location rLoc	= new _Location();
		
		rewriteLocation( holder.location, rLoc );
		
		rewrite.location = rLoc;
	}
	
	private void rewriteLocation( Location location, _Location rewrite )
	{
		rewrite.beginColumn 	= location.beginColumn();
		rewrite.endColumn		= location.endColumn();
		rewrite.beginLine		= location.beginLine();
		rewrite.endLine			= location.endLine();
		rewrite.isNullLocation	= location.equals( Location.nullLoc );
		rewrite.source			= location.source();
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#getLibraryPath()
	 */
	public IPath[] getLibraryPath()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#getRootDirectory()
	 */
	public IPath getRootDirectory()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#getSwitches()
	 */
	public Map getSwitches()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#setLibraryPath(org.eclipse.core.runtime.IPath[])
	 */
	public void setLibraryPath(IPath[] paths)
	{
		String[] rewrittenPathes = new String[paths.length];
		
		for ( int i = 0; i < paths.length; i++ )
			rewrittenPathes[i] = paths[i].toOSString();
		
		RuntimeConfiguration.get().clearLibraryPath();
		RuntimeConfiguration.get().addLibraryPath(rewrittenPathes);
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#setRootDirectory(org.eclipse.core.runtime.IPath)
	 */
	public void setRootDirectory(IPath path)
	{
		RuntimeConfiguration.get().setRootDirectoryName( path.toOSString() );
	}

	/**
	 * @see de.techjava.tla.ui.extensions.ITLAParserConfiguration#setSwitches(java.util.Map)
	 */
	public void setSwitches(Map properties)
	{
		Properties	rewrittenProps = new Properties();
		
		rewrittenProps.put( 
				RuntimeConfiguration.SWITCH_DEBUGGING, 
				properties.get( ITLAParserConfiguration.PARSER_BE_VERBOSE ));
		rewrittenProps.put( 
				RuntimeConfiguration.SWITCH_LEVELCHECK, 
				properties.get( ITLAParserConfiguration.PARSER_LEVEL_CHECK ));
		rewrittenProps.put( 
				RuntimeConfiguration.SWITCH_SEMANTICS, 
				properties.get( ITLAParserConfiguration.PARSER_CHECK_SEMANTIC ) );
		rewrittenProps.put( 
				RuntimeConfiguration.SWITCH_STATS, 
				properties.get( ITLAParserConfiguration.PARSER_GENERATE_STATS ) );
	
		RuntimeConfiguration.get().setSwitches(rewrittenProps);
	}

	private class _TLAParserResult
		implements ITLAParserResult
	{
		private IProblemContainer initErrors		=	new _ProblemContainer();
		private IProblemContainer parseErrors		=	new _ProblemContainer();
		private	IProblemContainer semanticErrors	=	new _ProblemContainer();

		/**
		 * @see de.techjava.tla.ui.extensions.ITLAParserResult#getInitErrors()
		 */
		public IProblemContainer getInitErrors()
		{
			return initErrors;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ITLAParserResult#getParseErrors()
		 */
		public IProblemContainer getParseErrors()
		{
			return parseErrors;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ITLAParserResult#getSemanticErrors()
		 */
		public IProblemContainer getSemanticErrors()
		{
			return semanticErrors;
		}
		
	}
	
	private class _ProblemContainer
		implements IProblemContainer
	{
		private Collection	warnings	= new LinkedList();
		private Collection	errors		= new LinkedList();
		private	Collection	aborts		= new LinkedList();
		
		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#getWarnings()
		 */
		public Enumeration getWarnings()
		{
			return Collections.enumeration(warnings);
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#getErrors()
		 */
		public Enumeration getErrors()
		{
			return Collections.enumeration(errors);
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#getAborts()
		 */
		public Enumeration getAborts()
		{
			return Collections.enumeration(aborts);
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#hasAborts()
		 */
		public boolean hasAborts()
		{
			return aborts.size() > 0;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#hasErrors()
		 */
		public boolean hasErrors()
		{
			return errors.size() > 0;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemContainer#hasWarnings()
		 */
		public boolean hasWarnings()
		{
			return warnings.size() > 0;
		}
		
	}
	
	private class _ProblemHolder
		implements IProblemHolder
	{
		private int 	  type;
		private ILocation location;
		private String	  message;
		/**
		 * @see de.techjava.tla.ui.extensions.IProblemHolder#getLocation()
		 */
		public ILocation getLocation()
		{
			return location;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemHolder#getType()
		 */
		public int getType()
		{
			return type;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.IProblemHolder#getMessage()
		 */
		public String getMessage()
		{
			return message;
		}
		
	}
	
	private class _Location
		implements ILocation
	{
		private boolean isNullLocation;
		private int		beginLine;
		private int		endLine;
		private int 	beginColumn;
		private int		endColumn;
		private String	source;
		
		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#isNullLocation()
		 */
		public boolean isNullLocation()
		{
			return isNullLocation;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#beginLine()
		 */
		public int beginLine()
		{
			return beginLine;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#beginColumn()
		 */
		public int beginColumn()
		{
			return beginColumn;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#endLine()
		 */
		public int endLine()
		{
			return endLine;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#endColumn()
		 */
		public int endColumn()
		{
			return endColumn;
		}

		/**
		 * @see de.techjava.tla.ui.extensions.ILocation#source()
		 */
		public String source()
		{
			return source;
		}
		
	}
}

/*
 * $Log: TLCParserRuntime.java,v $
 * Revision 1.1  2007/01/29 22:25:04  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:48:31  szambrovski
 * *** empty log message ***
 *
 * Revision 1.7  2004/10/23 16:39:58  sza
 * imports
 *
 * Revision 1.6  2004/10/23 16:39:46  sza
 * fix
 *
 * Revision 1.5  2004/10/20 17:57:36  bgr
 * incremental build functionality started
 *
 * Revision 1.4  2004/10/20 15:08:49  sza
 * logging redirected to console
 *
 * Revision 1.3  2004/10/20 11:50:22  sza
 * logging added
 *
 * Revision 1.2  2004/10/14 20:56:58  bgr
 * configuration moved
 *
 * Revision 1.1  2004/10/13 10:52:33  bgr
 * parser renamed
 *
 * Revision 1.2  2004/10/13 09:45:17  bgr
 * parser objects rewrite added
 *
 * Revision 1.1  2004/10/13 08:41:45  bgr
 * parser extension added
 *
 */