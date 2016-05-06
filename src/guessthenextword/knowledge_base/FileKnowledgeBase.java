package guessthenextword.knowledge_base;

import guessthenextword.structures.CoOccurrenceEntry;
import guessthenextword.util.Clock;
import guessthenextword.util.Logger;
import guessthenextword.util.Statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class FileKnowledgeBase implements EditableKnowledgeBase {
	
	//==> Fields
	
	//=> KnowledgeBase
	
	private String path;
	private static final String descriptiveLineRegex = "([\\w\\s]+)\\t([\\w\\s]+)\\t([\\w\\s]+)";
	private static final Pattern descriptiveLinePattern = Pattern.compile(descriptiveLineRegex);
	private static final String validLineRegex = "(\\w+)\\t(\\w+)\\t([\\w\\.\\,]+)";
	private static final Pattern validLinePattern = Pattern.compile(validLineRegex);
	private static boolean isAvailable = false;
	
	//=> EditableKnowledgeBase
	
	private Clock clock;
	private String similarityType;
	private final int entryLimits = 5000;
	private Map<String,Map<String,Container>> tempBuffer = new ConcurrentHashMap<String,Map<String,Container>>();
	private Map<String,Map<String,Container>> fileBuffer = new ConcurrentHashMap<String,Map<String,Container>>();
	private Semaphore bufferSem = new Semaphore(1, true);
	
	
	
	//==> Constructors
	
	protected FileKnowledgeBase(){}
	
	public FileKnowledgeBase(String[] args){
		if( args != null && args.length >= 1 ){
			String path = args[0];
			if( path.length() == 0 ){
				path = new File("").getAbsolutePath();
			}
			path = (path.charAt(path.length()-1) == File.separatorChar)? path : path+File.separator;
			//check if 'path' is valid
			File f = new File(path);
			if( f.exists() && f.isDirectory() ){
				if( FileUtils.listFiles(new File(path), new String[]{"dat"}, true).size() != 0 ){
					isAvailable = true;
				}
				//OK
				this.path = path;
				buildFolderStructure();
			}
		}
		//
		if(path == null ){
			throw new IllegalArgumentException("The specified arguments are not valid");
		}
		//
		//create the clock
		Runnable job = new Runnable() {
			@Override
			public void run() {
				//call the flush function
				FileKnowledgeBase.this.flush();
			}
		};
		//last argument carries the READ_ONLY flag
		if( !args[ args.length-1 ].equals("READ_ONLY") ){
			clock = new Clock(job, true, 4000);
			//start the clock
			clock.start();
		}
	}//FileKnowledgeBase

	
	
	//==> Methods
	
	//=> KnowledgeBase inherited methods
	
	@Override
	public boolean isAvailable() {
		return (isAvailable) || (path != null && new File(path).exists() && (FileUtils.listFiles(new File(path), new String[]{"dat"}, true).size() != 0));
	}//isAvailable
	
	@Override
	public boolean isAvailable(String word) {
		if( word != null && word.length() != 0 ){
			char firstChar = word.charAt(0);
			return new File( path+firstChar+File.separator+word+".dat" ).exists();
		}
		return false;
	}//isAvailable
	
	@Override
	public List<CoOccurrenceEntry> getCoOccurrences(String word) {
		return getCoOccurrences(word, Integer.MAX_VALUE);
	}//getCoOccurrences
	
	@Override
	public List<CoOccurrenceEntry> getCoOccurrences(String word, int resultMaxLength) {
		List<CoOccurrenceEntry> result = new LinkedList<CoOccurrenceEntry>();
		if( isAvailable() ){
			//try to fill the list
			if( word != null && word.length() != 0 ){
				char firstChar = word.charAt(0);
				//
				File wordFile = new File( path+firstChar+File.separator+word+".dat" );
				try {
					String wordA;
					String wordB = null;
					int coOccurrencesCount = 0;
					double similarity = 0;
					//
					wordA = word; //<= wordA
					BufferedReader br = new BufferedReader( new FileReader( wordFile ) );
					//first (descriptive) line
					Matcher m;
					String line = br.readLine();
					if( line != null && line.matches(descriptiveLineRegex) ){
						m = descriptiveLinePattern.matcher(line);
						if( m.find() ){
							similarityType = m.group(3); //<= similarityType
						}
						//fill the list
						int counter = 0;
						line = br.readLine();
						while( line != null && counter < resultMaxLength ){
							m = validLinePattern.matcher(line);
							if( m.find() ){
								wordB = m.group(1); //<= wordB
								try{
									coOccurrencesCount = Integer.parseInt(m.group(2)); //<= coOccurrencesCount
									similarity = Double.parseDouble(m.group(3)); //<= similarity
								}catch(Exception e){
									coOccurrencesCount = 0;
									similarity = 0;
								}
							}
							result.add( new CoOccurrenceEntry(wordA, wordB, coOccurrencesCount, similarity, similarityType) );
							counter++;
							//get the next line
							line = br.readLine();
						}
					}
					//close the reader
					br.close();
					//
				} catch (Exception e) {
					//Logger.log(this.getClass().getSimpleName(), "Error while loading the '"+word+"' coOccurrences list!");
				}
			}
			//
		}else{
			throw new IllegalStateException("The knowledge base path does not contain a valid knowledge base!");
		}
		return result;
	}//getCoOccurrences

	
	//=> EditableKnowledgeBase inherited methods
	
	@Override
	public void setDefaultSimilarityType(String similarityType) {
		this.similarityType = similarityType;
	}//setDefaultSimilarityType

	@Override
	public boolean addNewEntry(String wordA, String wordB, int coOccurrences, double similarity, String similarityType) {
		//
		Map<String,Container> buffer = tempBuffer.get(wordA);
		if( buffer == null ){
			buffer = new HashMap<String,Container>();
			tempBuffer.put(wordA, buffer);
		}
		Container cont = new Container( coOccurrences, similarity );
		buffer.put(wordB, cont);
		//
		Statistics.increaseInt("fileBufferLength");
		return true;
	}//addNewEntry

	@Override
	public boolean removeEntry(String wordA, String wordB) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName()+": 'removeEntry' operation not supported!");
	}//removeEntry
	
	@Override
	public void setFlushable(String word){
		//extract the data from the temporary buffer
		Map<String,Container> base = tempBuffer.remove(word);
		if( base != null ){
			//create the new data structure with a comparator
			ValueComparator vc = new ValueComparator( base );
			Map<String,Container> map = new TreeMap<String,Container>( vc );
			//export the HashMap into the TreeMap
			map.putAll(base);
			//add the TreeMap to the fileBuffer
			fileBuffer.put(word, map);
		}
	}//setFlushable

	@Override
	public void flush(){
		BufferedWriter writer;
		//
		if( fileBuffer.size() > 0 ){
			//take a snapshot of the keys list
			List<String> keys = new LinkedList<String>();
			try{
				bufferSem.acquire();
				//
				keys.addAll( fileBuffer.keySet() );
			}catch(InterruptedException ie){ /* do nothing */ }
			finally{
				//
				bufferSem.release();
			}
			//write out the buffer content
			for( String wordA : keys ){
				Map<String,Container> map = fileBuffer.remove(wordA);
				//
				char firstChar = wordA.charAt(0);
				File wordFile = new File( path+firstChar+File.separator+wordA+".dat" );
				try {
					writer = new BufferedWriter( new FileWriter( wordFile , false ) );
					writer.write( "WORD\tFREQ\t"+similarityType+"\n" );
				} catch (IOException e) { 
					//error occurred => skip word
					Logger.log(this.getClass().getSimpleName(), "Error occurred writing the '"+wordA+"' word data.");
					continue;
				}
				//extract the words
				Set<Entry<String,Container>> wordBset = map.entrySet();
				Iterator<Entry<String, Container>> it = wordBset.iterator();
				int counter = 0;
				while( it.hasNext() && counter < entryLimits ){
					counter++;
					Entry<String, Container> currentNode = it.next();
					//extract the info
					String wordB = currentNode.getKey();
					Container c = currentNode.getValue();
					//write a new line
					if( writer != null ){
						try{
							writer.write( wordB + "\t" + c.coOccurrences + "\t" + c.similarity + "\n" );
						}catch(IOException ioe){ /*do nothing*/ }
					}else{
						//error occurred => skip word
						break;
					}
				}
				//update informations
				Statistics.decreaseInt("fileBufferLength", wordBset.size());
				//flush and close
				try {
					writer.flush();
					writer.close();
				} catch (IOException e) { /* do nothing */ }
			}
			isAvailable = isAvailable();
		}
	}//flush

	@Override
	public void close(){
		if( clock != null ){
			//stop the clock
			clock.interrupt();
		}
	}//close
	
	
	//=> Private/Protected Methods
	
	private void buildFolderStructure(){
		String folder = path;
		new File( path ).mkdir();
		//create words first letter folders
		for(char alphabet = 'a'; alphabet <= 'z'; alphabet++) {
			new File( folder + ((char)alphabet) ).mkdir();
		}
	}//buildFolderStructure

	
	//Inner-Class
	
	private class Container{
		//==> Fields
		public int coOccurrences;
		public double similarity;

		//==> Constructors
		public Container( int coOccurrences, double similarity ) {
			this.coOccurrences = coOccurrences;
			this.similarity = similarity;
		}//Container
	}//Container
	
	private class ValueComparator implements Comparator<String> {
		//==>Fields
	    Map<String,Container> base;
	    
	    //==> Constructors
		@SuppressWarnings("unused")
		protected ValueComparator(){}
	    public ValueComparator(Map<String,Container> base) {
	        this.base = base;
	    }//ValueComparator

	    @Override
	    public int compare(String a, String b) {
	    	Container cA = base.get(a);
	    	Container cB = base.get(b);
	    	if( cA != null && cB != null ){
	    		double simA = cA.similarity;
	    		double simB = cB.similarity;
	    		//
	    		if( simA >= simB ){
	    			return -1;
	    		}else{
	    			return 1;
	    		}
	    	}else{
	    		//error
	    		throw new IllegalStateException("The map does not contain the keys '"+a+"' and/or '"+b+"'.");
	    	}
	    }//compare
	}//ValueComparator

}//FileKnowledgeBase
