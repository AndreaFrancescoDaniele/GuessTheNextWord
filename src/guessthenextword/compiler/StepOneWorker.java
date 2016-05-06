package guessthenextword.compiler;

import guessthenextword.dictionary.FullDictionary;
import guessthenextword.structures.Sentence;
import guessthenextword.util.BufferedConcurrentFileWriter;
import guessthenextword.util.Statistics;

import java.io.File;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StepOneWorker implements Runnable {
	
	//==> Fields
	
	private Sentence data;
	private String tempFolder;
	private KnowldgeBaseCompiler kbc;
	private FullDictionary dictionary;
	
	private BufferedConcurrentFileWriter bfw = BufferedConcurrentFileWriter.getInstance();
	
	
	
	//==> Constructors
	
	protected StepOneWorker(){}
	
	public StepOneWorker(KnowldgeBaseCompiler kbc, Sentence data, FullDictionary dict, String tempFolder){
		this.kbc = kbc;
		this.data = data;
		this.tempFolder = tempFolder+"step1"+File.separator;
		this.dictionary = dict;
	}//StepOneWorker

	
	
	//==> Methods
	
	@Override
	public void run() {
		String sentenceFilename;
		String wordFilename;
		//process the sentence
		String idChars = data.getSentenceID().substring(0, ConcurrentSentenceIDProvider.sentencePrefixLength-1);
		char lastIDChar = data.getSentenceID().charAt(ConcurrentSentenceIDProvider.sentencePrefixLength-1);
		StringBuilder sb = new StringBuilder();
		for( int i=0; i<idChars.length(); i++ ){
			sb.append( idChars.charAt(i) ).append( File.separator );
		}
		sentenceFilename = tempFolder+"sentences"+File.separator+sb.toString()+lastIDChar+".dat";
		sb.setLength(0);
		//obtain the words list
		String[] words = data.toArrayOfDistinctLemmas();
		for( String word : words ){
			//for each word
			if( dictionary.contains(word) ){
				char firstLetter = word.charAt(0);
				wordFilename = tempFolder+"words"+File.separator+firstLetter+File.separator+word+".dat";

				String text = data.getSentenceID()+"\n";
				bfw.appendToFile(wordFilename, text);
				
				sb.append(word + " ");
			}
		}
		String idNums = data.getSentenceID().substring(ConcurrentSentenceIDProvider.sentencePrefixLength);
		String text = idNums+" | "+sb.toString().trim() + "\n";
		bfw.appendToFile(sentenceFilename, text);
		Statistics.note("fileBufferLength", BufferedConcurrentFileWriter.getInstance().size());
		//
		Statistics.decreaseInt("threadBufferLength");
		kbc.bufferSem.release();
	}//run

}//StepOneWorker
