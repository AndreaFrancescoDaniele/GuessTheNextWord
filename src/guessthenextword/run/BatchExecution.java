package guessthenextword.run;

import guessthenextword.knowledge_base.KnowledgeBase;
import guessthenextword.knowledge_base.KnowledgeBases;
import guessthenextword.prophets.Prophet;
import guessthenextword.prophets.RewardProphet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class BatchExecution {
	
	//==> Fields
	
	private static int percentage = 0;

	
	
	//==> Methods
	
	public static void main(String[] args) {
		if( args.length != 2 ){
			showErrorAndClose();
		}
		// get the arguments
		String sourceFilePath = args[0];
		String destinationFilePath = args[1];
		// if the source file exists
		File sourceFile = new File( sourceFilePath );
		if( !sourceFile.exists() ){
			System.out.println("\n Error: the provided source file does not exist!");
			System.out.println(" The program will now exit! \n");
			System.exit(-1);
		}
		// read the source file
		File destinationFile = new File( destinationFilePath );
		List<List<String>> source = readFile( sourceFile );
		// get the knowledge base and build a prophet
		KnowledgeBase knowledgeBase = KnowledgeBases.getDefaultKnowledgeBase();
		Prophet p = new RewardProphet( knowledgeBase );
		// create the resulting list
		List<String> result = new LinkedList<String>();
		// guess the next word
		int totalLines = source.size()+1, currentLine = 0;
		for( List<String> words : source ){
			String guessedWord = p.guess(words);
			result.add( guessedWord );
			// update the process progress
			currentLine++;
			showProgressData(totalLines, currentLine);
		}
		// write the results
		try{
			BufferedWriter bw = new BufferedWriter( new FileWriter(destinationFile) );
			// for each guessed word
			for( String word : result ){
				bw.write( word + "\n" );
			}
			// flush and close the writer
			bw.flush();
			bw.close();
		}catch(Exception e){
			System.out.println("\n Error while writing the destination file: "+destinationFile.getAbsolutePath());
			System.out.println(" The program will now exit! \n");
			System.exit(-1);
		}
		//
		System.out.println("\n Batch execution completed successfully!");
		System.out.println("  Results: "+destinationFile.getAbsolutePath()+" \n \n");
	}//main

	private static List<List<String>> readFile( File sourceFile ){
		List<List<String>> result = new LinkedList<List<String>>();
		try{
			BufferedReader br = new BufferedReader( new FileReader(sourceFile) );
			String line = br.readLine();
			//
			while( line != null ){
				List<String> lineContent = new LinkedList<String>();
				String[] lineContentArray = line.split("\\s+");
				if( lineContentArray.length > 1 ){
					for( String s : lineContentArray ){
						lineContent.add( s.trim().toLowerCase() );
					}
					result.add( lineContent );
				}
				//get the next line
				line = br.readLine();
			}
			//
			br.close();
		}catch(Exception e){
			System.out.println("\n Error occurred while reading the file: "+sourceFile.getAbsolutePath());
			System.out.println(" The program will now exit! \n");
			System.exit(-1);
		}
		//
		return result;
	}//readFile
	
	private static void showProgressData( long totalLines, long currentLine ){
		int newPercentage = (int) (((double)currentLine/(double)(totalLines-1))*100);
		if( newPercentage != percentage ){
			percentage = newPercentage;
			//
			System.out.println(" Progress: "+ percentage+"%" );
		}
	}//sendProgressDataToLogger
	
	private static void showErrorAndClose(){
		System.out.println();
		String txt = 
		  "Command syntax error.\n"
		+ " * ==================================================== \n"
		+ " * Usage: \n"
		+ " *  \n"
		+ " * \t process a file in which each line contains a list of words (space-separated each other). \n"
		+ " * \t the result will be a file with the same number of lines in which each line contains (1) guessed word\n"
		+ " * \t corresponding to the list of words at the same position in the source file. \n"
		+ " *  \n"
		+ " * \t   > guessthenextword.run.BatchExecution <sourceFilePath> <destinationFilePath> \n"
		+ " * \n"
		+ " * \n"
		+ " The program will now exit! \n";
		System.out.println(txt);
		System.exit(-1);
	}//showErrorAndClose

}//BatchExecution
