package guessthenextword.compiler.sources;

import guessthenextword.compiler.ConcurrentSentenceIDProvider;
import guessthenextword.structures.Sentence;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class UkWacDataSource extends AbstractDataSource {

	//==> Fields

	//constant regex
	private static final String openSTag = ".*<s>.*";
	private static final String closeSTag = ".*<\\/s>.*";
	private static final String POStoAvoid = "(``|CD|SENT|\\$|''|\\(|\\)|\\,|:)";

	//regexes and patterns
	private static final String validLineRegex = "(.*)\\t(.*)\\t(.*)";
	private static final Pattern validLinePattern = Pattern.compile(validLineRegex);
	
	//parser data
	private long parsedLines = -1;
	private boolean hasNext = false;

	

	//==> Constructors

	public UkWacDataSource( File[] sourceFiles ){
		super( sourceFiles );
	}//UkWacDataSource

	
	
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
			String line = source.readLine();
			parsedLines++;
			//read the file until a open-tag <s> is found
			while( line != null ){
				if( line.matches(openSTag) ){
					hasNext = true;
					return true;
				}
				//skip the current line
				line = source.readLine();
				//send progress data to the logger
				sendProgressDataToLogger(f.getName(), parsedLines);
				if( line != null ) parsedLines++;
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
		Matcher m;
		List<String> words = new LinkedList<String>();
		File f = sourceFiles.get(selectedSource);
		try{
			String line = source.readLine();
			parsedLines++;
			//read the file line-by-line
			while(line != null){
				//process the current line
				m = validLinePattern.matcher( line );
				if(m.find()){
					//valid line found
					if(!m.group(2).matches(POStoAvoid)){
						String word = m.group(3).replace('/', '-').replaceAll("[^\\w]","").trim().toLowerCase();
						words.add(word);
					}
				}else if(line.matches(closeSTag)){
					//</s> tag found
					//build and return the sentence
					String sentenceID = ConcurrentSentenceIDProvider.getNextSentenceID();
					return new Sentence( words, sentenceID );
				}
				//read next line
				line = source.readLine();
				//send progress data to the logger
				sendProgressDataToLogger(f.getName(), parsedLines);
				if( line != null ) parsedLines++;
			}
		}catch(Exception e){
			return new Sentence( words, null );
		}
		return new Sentence( words, null );
	}//next

	@Override
	public void remove() {
		throw new UnsupportedOperationException(this.getClass().getSimpleName()+": Remove operation not supported!");
	}//remove

}//UkWacDataSource
