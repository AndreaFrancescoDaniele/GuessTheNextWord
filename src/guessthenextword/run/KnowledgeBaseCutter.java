package guessthenextword.run;

import guessthenextword.util.Formatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.LineIterator;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class KnowledgeBaseCutter {
	
	public static void main(String[] args) {
		if( args.length < 3 ){
			System.out.println("Command syntax error.\n Usage:\n\t > guessthenextword.run.KnowledgeBaseCutter <newKBfilesLines> <originalKBpath> <destinationKBpath>");
			System.exit(-1);
		}

		int newSize = -1;
		try{
			newSize = Integer.parseInt( args[0] );
		}catch(Exception e){
			System.out.println("Error: the new knowledge base size provided is not a number. \n");
			System.out.println(" Usage:\n\t > guessthenextword.run.KnowledgeBaseCutter <newKBfilesLines> <originalKBpath> <destinationKBpath>");
			System.exit(-1);
		}
		
		File originalKBfolder = new File( args[1] );
		String cutKBfolder = args[2];
		
		//create the folder structure
		builFolderStructure( cutKBfolder );
		//create a specific fileNameFilter object
		FilenameFilter wordsFolderFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.matches("[a-z]")) && (new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
			}
		};
		List<String> wordsFolders = Arrays.asList( originalKBfolder.list(wordsFolderFilter) );
		Collections.sort(wordsFolders);
		//create a specific fileNameFilter object
		FilenameFilter wordsFilesFilter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return (name.matches("(\\w+).dat")) && !(new File(dir.getAbsolutePath()+File.separator+name).isDirectory());
			}
		};
		//compute the number of files we have to process
		long totalWords = 0;
		for( String folder : wordsFolders ){
			File wordsFolder = new File( originalKBfolder.getAbsolutePath()+File.separator+folder );
			totalWords += wordsFolder.list(wordsFilesFilter).length;
		}
		//process the data
		long currentWord = 0;
		//for each '[a-z]' folder
		for( String folder : wordsFolders ){
			//
			File wordsFolder = new File( originalKBfolder.getAbsolutePath()+File.separator+folder );
			List<String> words = Arrays.asList( wordsFolder.list(wordsFilesFilter) );
			//for each '<word>.dat' file
			for( String word : words ){
				//
				currentWord++;
				//
				File file = new File( originalKBfolder.getAbsolutePath()+File.separator+folder+File.separator+word );
				
				int remainingLines = newSize+1;
				try {
					//open a new file reader
					LineIterator lit = new LineIterator( new FileReader( file ) );
					//open a new file writer
					BufferedWriter bw = new BufferedWriter( new FileWriter( new File(cutKBfolder+File.separator+folder+File.separator+word) ) );
					//search the lines
					while( (lit.hasNext() && (remainingLines > 0)) ){
						//
						String line = (String)lit.next() + ((remainingLines == 1)? "" : "\n");
						bw.write( line );
						remainingLines--;
						//
					}
					//close the reader
					lit.close();
					//close the writer
					bw.flush();
					bw.close();
				}catch(Exception e) { 
					e.printStackTrace();
				}
				//
				System.out.println( Formatter.round( (( (double)currentWord / (double)totalWords )*100) , 1 ) +"%" );
			}
		}
	}//main
	
	public static void builFolderStructure( String baseFolder ){
		//create 'baseFolder' folder
		new File( baseFolder ).mkdirs();
		//create words first letter folders
		baseFolder = baseFolder + File.separator;
		for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) {
			new File( baseFolder + ((char)alphabet) ).mkdir();
		}
	}//builFolderStructure
	
}//KnowledgeBaseCutter
