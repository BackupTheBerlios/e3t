package org.zambrovski.tla.tlasany;

import java.io.PrintStream;

import org.zambrovski.tla.RuntimeConfiguration;
import org.zambrovski.tla.tlasany.semantic.ErrorsContainer;

import tlasany.configuration.Configuration;
import tlasany.drivers.InitException;
import tlasany.drivers.SemanticException;
import tlasany.explorer.Explorer;
import tlasany.explorer.ExplorerQuitException;
import tlasany.modanalyzer.ParseUnit;
import tlasany.modanalyzer.SpecObj;
import tlasany.parser.ParseException;
import tlasany.semantic.AbortException;
import tlasany.semantic.BuiltInLevel;
import tlasany.semantic.Context;
import tlasany.semantic.ExternalModuleTable;
import tlasany.semantic.Generator;
import tlasany.semantic.ModuleNode;
import tlasany.semantic.SemanticNode;
import tlasany.st.Location;
import tlasany.st.TreeNode;
import util.UniqueString;

/**
 * TLA+ Syntax Parser
 * @author Simon Zambrovski, <a href="http://simon.zambrovski.org">http://simon.zambrovski.org</a> 
 * @version $Id: TLASyntaxParser.java,v 1.1 2007/01/29 22:35:17 tlateam Exp $
 */
public class TLASyntaxParser 
{

    private static TLASyntaxParser 	instance;
    private RuntimeConfiguration	config;
    /**
     * Private constructor to avoid instantiation
     */
    private TLASyntaxParser()
    {
        config = RuntimeConfiguration.get();
    }
    /**
     * Strart parsing documents
     * @param filenames
     * @return the specification objects of parsed documents
     */
    public SpecObj[] parse(String[] filenames)
    {
        SpecObj[] specifications = new SpecObj[filenames.length];
        for (int i = 0; i < filenames.length; i++) 
        {
            SpecObj spec = new SpecObj(filenames[i]);
            specifications[i] = spec;
            try 
            {
                // Initialize the global environment
                initialize(spec, config.getErrStream());
                  
                // Parsing 
                if (config.doParsing()) 
                {
                    parse(spec, config.getErrStream());   
                }
                // Semantic analysis and level checking
                if (config.doSemanticAnalysis()) 
                {
                    analyseSemantics(spec, config.getErrStream(), config.doLevelChecking());
                }
            } catch (InitException ie)  
            {
                ie.printStackTrace(config.getErrStream());
            } catch (ParseException pe) {
                pe.printStackTrace(config.getErrStream());
            } catch (AbortException ae) {
                ae.printStackTrace(config.getErrStream());
            } catch (SemanticException se) {
                se.printStackTrace(config.getErrStream());
            } catch (Exception e) 
            {
                e.printStackTrace(config.getErrStream());
            }

            // Compile operator usage stats
            if (config.doStats())
            { 
                // frontEndStatistics(spec);
            }

            if (config.doDebugging()) 
            {
              // Run the Semantic Graph Exploration tool
              Explorer explorer = new Explorer(spec.getExternalModuleTable());
              try 
              {
                explorer.main();
              }
              catch (ExplorerQuitException e) { /*do nothing*/ }
            }
        }
        return specifications;
    }
    /**
     * Initialize the environment
     */
    private void initialize(SpecObj spec, PrintStream syserr)
    	throws InitException 
    {
          String fileName   = spec.getFileName();
          ErrorsContainer initErrors = spec.initErrors;
          try {
            // Read & initialize config file for each specification

            // Set Configuration class to (re)start
            Configuration.ReInit();

            // (Re)create an empty Context object
            Context.reInit();

            // (Re)parse tables of builtin operators and synonyms into the global context
            Configuration.load(initErrors);

            // (Re)read & initialize level data for builtin operators
            BuiltInLevel.load();

            // Print any errors from parsing during initialization phase
            if (! initErrors.isSuccess()) 
            {
              syserr.println("*** Errors during initialization of SANY:\n");
              syserr.print( initErrors );

              // indicate fatal error during first phase
              spec.errorLevel = 1;  
              throw new InitException();
            }
          } catch (Exception e) 
          {
            syserr.println("Unexpected exception during SANY initialization " +
      		     fileName + "\n" + e);
            syserr.println("Initialization errors detected before the unexpected exception:\n");
            syserr.print( initErrors );

            // indicate fatal error during first phase
            spec.errorLevel = 1;  
            throw new InitException();
          }
          return;
    }

    /**
     * Parses the specification
     * @param spec
     * @param syserr
     * @throws ParseException
     * @throws AbortException
     */
    private void parse(SpecObj spec, PrintStream syserr) 
    	throws ParseException, AbortException 
    {
      try {
        // Actual parsing method called from inside loadSpec()
        if (!spec.loadSpec(spec.getFileName(), spec.parseErrors)) 
        {
          spec.parseErrors.addError(Location.nullLoc, "Parsing failed; semantic analysis not started");
        }
      
        if (!spec.parseErrors.isSuccess()) 
        {
          if (syserr!= null) syserr.println( spec.parseErrors );

          // indicate fatal error during parsing phase
          spec.errorLevel = 2;
          throw new ParseException(); 
        }
      } catch (ParseException e) 
      {
        throw new ParseException();
      }
      catch (Exception e) {
        syserr.println("\nFatal errors while parsing TLA+ spec in file " + 
  		     spec.getFileName() + "\n"); 
        syserr.print( spec.parseErrors );
        throw new ParseException();
      }
      return;
    }
    /**
     * Perform semantic analysis
     * @param spec
     * @param syserr
     * @param levelCheck
     * @throws SemanticException
     */
    private void analyseSemantics(SpecObj spec, PrintStream syserr, boolean levelCheck) 
    	throws SemanticException 
    {
		String      		moduleStringName;
		TreeNode    		syntaxTreeRoot;
		ExternalModuleTable externalModuleTable = spec.getExternalModuleTable();
		ParseUnit   		parseUnit;
		ModuleNode  		moduleNode = null;
		ErrorsContainer      		semanticErrors = spec.semanticErrors;
		ErrorsContainer      		globalContextErrors = tlasany.semantic.Context.getGlobalContext().getErrors();

		try 
		{
		    SemanticNode.setError(semanticErrors);

			// Go through the semanticAnalysisVector, and generate the
			// semantic graph for each external module in it, adding at each
			// iteration what was generated (i.e. <context, node>) to
			// externalModuleTable.  The semanticAnalysisVector is ordered
			// so that is A depends on B, then B has a lower index in the
			// Vector than A.
		    for (int i = 0; i < spec.semanticAnalysisVector.size(); i++) 
		    {
		        moduleStringName = (String)spec.semanticAnalysisVector.elementAt(i);  

		        // if semantic analysis has not already been done on this module
		        if (externalModuleTable.getContext( UniqueString.uniqueStringOf( moduleStringName)) == null ) 
		        {
		            parseUnit = (ParseUnit)spec.parseUnitContext.get(moduleStringName);

		            // get reference to the syntax tree for the module
		            syntaxTreeRoot = parseUnit.getParseTree();

		            // Generate semantic graph for the entire external module
		            syserr.println("Semantic processing of module " + moduleStringName);

		            // create new Generator object
		            Generator gen = new Generator(externalModuleTable, semanticErrors);

		            // Perform semantic analysis and create semantic graph for one external module here
		            moduleNode = gen.generate(syntaxTreeRoot);    
        
		            // Put the semantic graph and related info for moduleNode into the module table
		            externalModuleTable.put(UniqueString.uniqueStringOf(moduleStringName), gen.getSymbolTable().getExternalContext(),moduleNode);

		            // Level check if semantic analysis succeeded and levelCheck is true.
		            if (moduleNode != null && semanticErrors.isSuccess() && levelCheck ) 
		            {
		                moduleNode.levelCheck();
		            }

					// Indicate in the externalModuleTable that the last module
					// analyzed is the root ModuleNode
					if (i == spec.semanticAnalysisVector.size()-1) 
					{ 
					    externalModuleTable.setRootModule( moduleNode ); 
					}

					// Print error and warning messages for this module
					if (globalContextErrors.getNumMessages() > 0) 
					{
					    syserr.println("Semantic errors in global context:\n");
					    syserr.print( "\n" + globalContextErrors );
					    // indicate fatal error parsing builtin operator tables
					    spec.errorLevel = 3;
					}

					if (semanticErrors.getNumMessages() > 0) 
					{
					    syserr.println("Semantic errors:\n\n" + semanticErrors);
					    // indicate fatal error during semantic analysis or level-checking
					    if ( semanticErrors.getNumAbortsAndErrors() > 0 ) 
					    {
					        spec.errorLevel = 4;
					    } // end if
					} // end if
		        } // end if
		    } // end while
		} catch (AbortException e) 
		{
		    if ( syserr != null) 
		    {
		        syserr.println("Fatal errors in semantic processing of TLA spec " 
		                + spec.getFileName() + "\n" + e.getMessage() 
		                + "\nStack trace for exception:\n"); 
		        e.printStackTrace(syserr);
		    }

			if (globalContextErrors.getNumMessages() > 0) 
			{
			    if (syserr != null) 
			    {
					syserr.println("Semantic errors in global context detected before the unexpected exception:\n");
					syserr.print("\n" + globalContextErrors);
				}
			
			    // indicate fatal error parsing builtin operator tables
			    spec.errorLevel = 3;
			}

			if (semanticErrors.getNumMessages() > 0) 
			{
				if ( syserr != null ) 
				{
				    syserr.println("Semantic errors detected before the unexpected exception:\n");
				    syserr.print("\n" + semanticErrors);
				}

				// indicate fatal error during semantic analysis or level-checking
				if ( semanticErrors.getNumAbortsAndErrors() > 0 ) 
				{ 
				    spec.errorLevel = 4;
				}
			}
			throw new SemanticException();
		}
		return;
    }
 
    /**
     * Retrieves a working copy of parser
     * @return parser instances
     */
    public static TLASyntaxParser getInstance()
    {
        if (instance == null) 
        {
            instance = new TLASyntaxParser();
        }
        return instance;
    }
}

/*
 * $Log: TLASyntaxParser.java,v $
 * Revision 1.1  2007/01/29 22:35:17  tlateam
 * Start version (Original TLA Eclipse plugin)
 *
 * Revision 1.1  2005/08/22 15:01:56  szambrovski
 * sf cvs init
 *
 * Revision 1.2  2004/10/14 20:56:59  bgr
 * configuration moved
 *
 * Revision 1.1  2004/10/12 09:55:54  sza
 * imports, changes to Runners for path resolution
 *
 *
 */