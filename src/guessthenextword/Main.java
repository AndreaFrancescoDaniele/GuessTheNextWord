package guessthenextword;

import guessthenextword.compiler.KnowldgeBaseCompiler;
import guessthenextword.knowledge_base.KnowledgeBase;
import guessthenextword.knowledge_base.KnowledgeBases;
import guessthenextword.prophets.Prophet;
import guessthenextword.prophets.RewardProphet;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Main {

	public static void main(String[] args) {
		
		/*
		 * Two different commands: 1.'compile', 2.'ask' 
		 *  
		 * 1.'compile':
		 *       compile the Knowledge Base using the provided data sources with the '-source' and '-path' tags.
		 * 
		 *         > guessthenextword.Main --compile [-m|similarityMeasure:<similarityModule> -server:<TCPport> -gui] -s|source:<sourceType> -p|path:<regexPath> 
		 * 
		 *       Options:
		 *         --compile                    :         select the 'compile' command.   
		 *         -m -similarityMeasure        :         (*optional) select a specific similarity measure module.
		 *                                                  Default: Jaccard 
		 *         -server                      :         (*optional) launch a log server listening on the '<TCPport' if specified 
		 *                                                  or on the default port 14000.
		 *                                                  Default: NO 
		 *         -gui                         :         (*optional) launch a local window showing the process progress.
		 *                                                  Default: YES 
		 *         -s -source                   :         it specifies a data source type and it is always coupled with a -path tag
		 *                                                  ahead which indicates the sorce path. Ex: UkWac,CogALex,ecc.. 
		 *         -p -path                     :         it specifies a data path and it is always coupled with a -source tag
		 *                                                  behind which indicates the sorce type. 
		 * 
		 *       Examples: 
		 *               > guessthenextword.Main --compile -server:14000 -gui -similarityMeasure:Jaccard -source:UkWac -path:/etc/ukwac/UKWAC-*.xml 
		 *          OR 
		 *               > guessthenextword.Main --compile -m:Jaccard -s:UkWac -p:/etc/ukwac/UKWAC-*.xml 
		 * 
		 * 
		 * 2.'ask':
		 *       query the guessed word you are looking for, providing a list of related words (case-insensitive). 
		 * 
		 *         > guessthenextword.Main --ask [-r|resultSize:<wordsToGuess>] <space-separated-words-list> 
		 * 
		 *       Options:
		 *         --ask                        :         select the 'ask' command.   
		 *         -r -resultSize               :         (*optional) request a list of 'wordsToGuess' guessed words.
		 *                                                  Default: 1 
		 * 
		 *       Examples: 
		 *               > guessthenextword.Main --ask -r:10 gin drink scotch bottle soda 
		 *          OR 
		 *               > guessthenextword.Main --ask gin drink scotch bottle soda 
		 * 
		 */

		
		if( args.length == 0 ){
			showErrorAndClose();
		}
		
		// constants
		final String compileRegEx = "[\\s]*-(s|source):([^\\s]+)[\\s]*-(p|path):([^\\s]+)[\\s]*";
		final String similarityMeasureRegEx = ".*-(m|similarityMeasure):([^\\s]+)[\\s]*.*";
		final String serverModeRegEx = ".*-server(:)?([0-9]+)?[\\s]*.*";
		final String guiModeRegEx = ".*-gui.*";
		final String resultSizeRegEx = "[\\s]*-(r|resultSize):([0-9]+)[\\s]*";

		boolean compileCommand = false;
		boolean askCommand = false;
		
		if( args[0].equals("--compile") ){
			compileCommand = true;
		}else if( args[0].equals("--ask") ){
			askCommand = true;
		}else{
			showErrorAndClose();
		}
		
		if( compileCommand ){
			// 1. 'compile' command
			// concatenate the arguments strings
			StringBuilder sb = new StringBuilder();
			for( int i = 1; i < args.length; i++ ) sb.append(args[i]+' ');
			String commandArgs = sb.toString();
			// read the similarity measure
			String similarityMeasure = null; //<= null = invoke default
			Pattern p = Pattern.compile(similarityMeasureRegEx);
			Matcher m = p.matcher(commandArgs);
			if( m.find() ){
				similarityMeasure = m.group(2);
			}
			// read the server mode flag
			Integer port = -1; //<= -1 = classicMode
			p = Pattern.compile(serverModeRegEx);
			m = p.matcher(commandArgs);
			if( m.find() ){
				if( m.group(1) != null && m.group(2) != null ){
					try{
						port = Integer.parseInt( m.group(2) );
					}catch(Exception e){}
				}else{
					port = 0; //<= 0 = default port
				}
			}
			// read the server mode flag
			boolean gui = (port == -1); //<= default = GUI only in standalone mode
			if( commandArgs.matches(guiModeRegEx) ){
				gui = true;
			}
			// parse the arguments
			p = Pattern.compile(compileRegEx);
			m = p.matcher(commandArgs);
			// create a list of Strings couples: <sourceType, sourcePath>
			List<String[]> sources = new LinkedList<String[]>();
			while( m.find() ){
				String[] array = new String[2];
				array[0] = m.group(2);
				array[1] = m.group(4);
				sources.add( array );
			}
			// sources size constraint
			if( sources.size() == 0 ){
				showErrorAndClose();
			}
			//==> Command Execution
			KnowldgeBaseCompiler kbc = new KnowldgeBaseCompiler(similarityMeasure, port, gui);
			for( String[] source : sources ){
				kbc.addSource( source[0], source[1] );
			}
			kbc.buildKB();
			//<== =================
		}else
		if( askCommand ){
			// 2. 'ask' command
			List<String> words = new LinkedList<String>();
			int i = 1, resultSize = 1;
			if( args.length > 1){
				Pattern p = Pattern.compile(resultSizeRegEx);
				Matcher m = p.matcher( args[1] );
				if( m.find() ){
					try{
						resultSize = Integer.parseInt( m.group(2) );
						i = 2;
					}catch(Exception e){}
				}
			}
			for( ; i < args.length; i++ ){
				words.add( args[i].toLowerCase() );
			}
			if( words.size() == 0 ){
				showErrorAndClose();
			}
			//==> Command Execution
			//load the KnowledgeBase
			KnowledgeBase knowledgeBase = KnowledgeBases.getDefaultKnowledgeBase();
			Prophet p = new RewardProphet(knowledgeBase);
			//=== guess the next word ===
			List<String> guessedWords = p.guess(words, resultSize);
			//print results
			System.out.println(guessedWords);
			//<== =================
			//close the knowledge base
			knowledgeBase.close();
		}else{
			showErrorAndClose();
		}
	}//main
	
	
	private static void showErrorAndClose(){
		System.out.println();
		String txt = 
		  "Command syntax error.\n"
		+ " * ==================================================== \n"
		+ " * Usage: \n"
		+ " *  \n"
		+ " * Two different commands: 1.'compile', 2.'ask' \n"
		+ " *  \n"
		+ " * 1.'compile':\n"
		+ " * \t compile the Knowledge Base using the provided data sources with the '-source' and '-path' tags.\n"
		+ " * \n"
		+ " * \t   > guessthenextword.Main --compile [-m|similarityMeasure:<similarityModule> -server:<TCPport> -gui] -s|source:<sourceType> -p|path:<regexPath> \n"
		+ " * \n"
		+ " * \t Options:\n"
		+ " * \t   --compile \t\t\t:\t  select the 'compile' command.   \n"
		+ " * \t   -m -similarityMeasure\t:\t  (*optional) select a specific similarity measure module.\n"
		+ " * \t              \t\t\t\t    Default: Jaccard \n"
		+ " * \t   -server \t\t\t:\t  (*optional) launch a log server listening on the '<TCPport' if specified \n"
		+ " * \t              \t\t\t\t    or on the default port 14000.\n"
		+ " * \t              \t\t\t\t    Default: NO \n"
		+ " * \t   -gui \t\t\t:\t  (*optional) launch a local window showing the process progress.\n"
		+ " * \t              \t\t\t\t    Default: YES \n"
		+ " * \t   -s -source \t\t\t:\t  it specifies a data source type and it is always coupled with a -path tag\n"
		+ " * \t              \t\t\t\t    ahead which indicates the sorce path. Ex: UkWac,CogALex,ecc.. \n"
		+ " * \t   -p -path \t\t\t:\t  it specifies a data path and it is always coupled with a -source tag\n"
		+ " * \t              \t\t\t\t    behind which indicates the sorce type. \n"
		+ " * \n"
		+ " * \t Examples: \n"
		+ " * \t\t > guessthenextword.Main --compile -server:14000 -gui -similarityMeasure:Jaccard -source:UkWac -path:/etc/ukwac/UKWAC-*.xml \n"
		+ " * \t    OR \n"
		+ " * \t\t > guessthenextword.Main --compile -m:Jaccard -s:UkWac -p:/etc/ukwac/UKWAC-*.xml \n"
		+ " * \n"
		+ " * \n"
		+ " * 2.'ask':\n"
		+ " * \t query the guessed word you are looking for, providing a list of related words (case-insensitive). \n"
		+ " * \n"
		+ " * \t   > guessthenextword.Main --ask [-r|resultSize:<wordsToGuess>] <space-separated-words-list> \n"
		+ " * \n"
		+ " * \t Options:\n"
		+ " * \t   --ask \t\t\t:\t  select the 'ask' command.   \n"
		+ " * \t   -r -resultSize\t\t:\t  (*optional) request a list of 'wordsToGuess' guessed words.\n"
		+ " * \t              \t\t\t\t    Default: 1 \n"
		+ " * \n"
		+ " * \t Examples: \n"
		+ " * \t\t > guessthenextword.Main --ask -r:10 gin drink scotch bottle soda \n"
		+ " * \t    OR \n"
		+ " * \t\t > guessthenextword.Main --ask gin drink scotch bottle soda \n"
		+ " * \n"
		+ " * \n"
		+ " The program will now exit! \n";
		System.out.println(txt);
		System.exit(-1);
	}//showErrorAndClose

}//Main
