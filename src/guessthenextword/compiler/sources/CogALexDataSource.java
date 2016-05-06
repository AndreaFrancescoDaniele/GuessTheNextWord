package guessthenextword.compiler.sources;

import guessthenextword.compiler.ConcurrentSentenceIDProvider;
import guessthenextword.structures.Sentence;

import java.io.File;
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
public class CogALexDataSource extends AbstractDataSource {
	
	//==> Fields
	
	//regex and patterns
	private static final String validLineRegex = "^([\\w]+)[\\s]+~[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)$";
	private static final Pattern validLinePattern = Pattern.compile(validLineRegex);
	private static final int[] validGroups = new int[]{1, 2, 3, 4, 5, 6};
	
	//parser data
	private String currentLine;
	private long parsedLines = -1;
	private boolean hasNext = false;
	
	

	//==> Constructors

	public CogALexDataSource( File[] sourceFiles ){
		super( sourceFiles );
	}//CogALexDataSource
	


	//==> Methods
	
	//=> Iterator Interface Inherited Methods

	@Override
	public boolean hasNext() {
		if( selectedSource == -1 || source == null ){
			throw new IllegalStateException(this.getClass().getSimpleName()+": You must call the 'selectSource' method before calling the 'hasNext'/'next' methods");
		}
		if( hasNext ) return true;
		//
		hasNext = false;
		File f = sourceFiles.get(selectedSource);
		try{
			currentLine = source.readLine();
			parsedLines++;
			//send progress data to the logger
			sendProgressDataToLogger(f.getName(), parsedLines);
			//read the file until a valid currentLine is found
			while( currentLine != null ){
				if( currentLine.matches(validLineRegex) ){
					hasNext = true;
					return true;
				}
				//skip the current currentLine
				currentLine = source.readLine();
			}
		}catch(Exception e){
			return false;
		}
		return false;
	}//hasNext

	@Override
	public Sentence next() {
		if( source == null ){
			throw new IllegalStateException(this.getClass().getSimpleName()+": You must call the 'selectSource' method before calling the 'hasNext'/'next' methods");
		}
		if( !hasNext ){
			throw new IllegalStateException(this.getClass().getSimpleName()+": You must call the 'hasNext' method before calling the 'next' method");
		}
		//
		hasNext = false;
		List<String> words = new LinkedList<String>();
		if( currentLine != null ){
			//process the current currentLine
			Matcher m = validLinePattern.matcher( currentLine );
			if( m.find() ){
				//for each valid group in the regex
				for( int i : validGroups ){
					String word = m.group(i);
					if( word != null ){
						words.add(word);
					}
				}
				//build and return the sentence
				String sentenceID = ConcurrentSentenceIDProvider.getNextSentenceID();
				return new Sentence( words, sentenceID );
			}
		}
		return new Sentence( words, null );
	}//next

	@Override
	public void remove() {
		throw new UnsupportedOperationException(this.getClass().getSimpleName()+": Remove operation not supported!");
	}//remove

}//CogALexDataSource
