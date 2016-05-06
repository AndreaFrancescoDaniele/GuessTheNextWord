package guessthenextword.compiler;

import guessthenextword.compiler.similarity.Similarity;
import guessthenextword.knowledge_base.EditableKnowledgeBase;
import guessthenextword.util.Statistics;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.LineIterator;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StepFourWorker implements Runnable {

	//==> Fields

	private Map<String,List<String>> data;
	private String fileName;
	private String tempFolder;
	private KnowldgeBaseCompiler kbc;
	private Map<String,Integer> occurMap;
	private EditableKnowledgeBase ekb;
	private Similarity similarityMeasure;
	private Map<String,Integer> partialData;
	private Semaphore partialDataSem;

	private String jobDescription;

	private static final String lineRegEx = "[0-9]{"+ConcurrentSentenceIDProvider.sentenceSuffixLength+"} \\| ([\\w\\s]+)$";
	private static final Pattern p = Pattern.compile(lineRegEx);

	private Map<String,Integer> map = new HashMap<String,Integer>();

	private int percentage = 0;



	//==> Constructors

	protected StepFourWorker(){}

	public StepFourWorker(KnowldgeBaseCompiler kbc, Map<String,List<String>> data, int partNo, int totalParts, String fileName, String tempFolder, 
			Map<String,Integer> occurMap, Map<String,Integer> partialData, Semaphore partialDataSem, EditableKnowledgeBase ekb, Similarity similarityMeasure){
		this.kbc = kbc;
		this.ekb = ekb;
		this.data = data;
		this.fileName = fileName;
		this.tempFolder = tempFolder;
		this.occurMap = occurMap;
		this.similarityMeasure = similarityMeasure;
		this.partialData = partialData;
		this.partialDataSem = partialDataSem;
		//
		jobDescription = "["+String.format("%02d", (partNo+1))+"/"+totalParts+"]"+fileName;
	}//StepFourWorker



	//==> Methods

	@Override
	public void run() {
		if( data != null ){
			String wordA = fileName.replaceFirst("[.][^.]+$", ""); //<= remove the extension
			File step1WordsSentences = new File( tempFolder+"step3"+File.separator );
			//temporary structures
			String prefix;
			int totalSentences = 1, currentSentence = 0;
			File sentenceFile = null;

			//compute the data size
			for( List<String> val : data.values() ){
				totalSentences += val.size();
			}

			//proceed with the file processing
			//for each prefix in the intelliIOmap
			for( Map.Entry<String,List<String>> entry : data.entrySet() ){
				prefix = entry.getKey();
				//compute the sentence path
				String path = step1WordsSentences.getAbsolutePath()+prefix+".dat";
				sentenceFile = new File( path );
				//search the line
				readSentenceFile(wordA, sentenceFile, entry.getValue());
				//increase the counter
				currentSentence += entry.getValue().size();
				//update the progress
				sendProgressDataToLogger(totalSentences, currentSentence);
			}
			//finally, update the partial data
			try{
				partialDataSem.acquire();
				//
				for( Map.Entry<String,Integer> entry : map.entrySet() ){
					String wordB = entry.getKey();
					Integer value = entry.getValue();
					//
					Integer oldValue = partialData.get(wordB);
					if( oldValue == null ){
						oldValue = 0;
					}
					partialData.put(wordB, oldValue+value);
				}
			}catch(InterruptedException ie){}
			finally{
				//
				partialDataSem.release();
			}
			//if it is the last worker associated to this word => flush to KB
			int counter = Statistics.decreaseInt(wordA+"WorkersCounter");
			if( counter <= 0 ){
				//finally, update the knowledge base
				try{
					partialDataSem.acquire();
					//
					for( Entry<String,Integer> entry : partialData.entrySet() ){
						String wordB = entry.getKey();
						int coOccurCount = entry.getValue();
						int wordAcount = occurMap.get(wordA);
						int wordBcount = 0;
						//
						double similarity;
						try{
							wordBcount = occurMap.get(wordB);
							//compute the Similarity
							similarity = similarityMeasure.computeSimilarity(wordAcount, wordBcount, coOccurCount);
						}catch(Exception e){ 
							// exclude the wordB
							similarity = 0;
						}
						//
						ekb.addNewEntry(wordA, wordB, coOccurCount, similarity, null);
					}
				}catch(InterruptedException ie){}
				finally{
					//
					partialDataSem.release();
				}
				//set the word as flushable
				ekb.setFlushable(wordA);
			}
			//
			Statistics.remMapNote("list", jobDescription);
			Statistics.decreaseInt("threadBufferLength");
			kbc.bufferSem.release();
		}
	}//run


	//=> Private/Protected Methods

	private void readSentenceFile(String wordA, File f, List<String> suffixList){
		Matcher m;
		//
		if( f != null && f.exists() ){
			List<Integer> linesList = new LinkedList<Integer>();
			for( String s : suffixList ){
				try{
					Integer i = Integer.parseInt(s);
					linesList.add(i);
				}catch(NumberFormatException nfe){nfe.printStackTrace();}
			}
			//read the wanted lines
			int remainingLines = linesList.size();
			try {
				//open a new file reader
				LineIterator lit = new LineIterator( new FileReader( f ) );
				//search the lines
				for(int lineNumber = 1; (lit.hasNext() && (remainingLines > 0)); lineNumber++ ){
					String line = (String)lit.next();
					if( linesList.contains(lineNumber) ){
						m = p.matcher(line);
						if( m.find() ){
							String subLine = m.group(1);
							if( subLine != null ){
								String[] array = subLine.trim().split("\\s");
								for( int i=0; i<array.length; i++ ){
									//a generic word
									String wordB = array[i];
									if( !wordB.equals(wordA) ){
										//a valid word
										Integer counter = map.get(wordB);
										if( counter == null ){
											map.put(wordB, 1);
										}else{
											map.put(wordB, counter+1);
										}
									}
								}
							}
						}
						//
						remainingLines--;
					}
				}
				//close the reader
				lit.close();
			}catch(Exception e) { /* error occurred => skip file */ }
		}
	}//readSentenceFile

	private void sendProgressDataToLogger( long totalLines, long currentLine ){
		int newPercentage = (int) (((double)currentLine/(double)(totalLines-1))*100);
		if( newPercentage != percentage ){
			percentage = newPercentage;
			//
			Statistics.mapNote("list", jobDescription, percentage);
		}
	}//sendProgressDataToLogger

}//StepFourWorker