package guessthenextword.compiler;

import guessthenextword.util.BufferedConcurrentFileWriter;
import guessthenextword.util.Statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Map;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StepTwoWorker implements Runnable {
	
	//==> Fields
	
	private File data;
	private String tempFolder;
	private KnowldgeBaseCompiler kbc;
	private Map<String,Integer> wordOccur;
	
	private BufferedConcurrentFileWriter bfw = BufferedConcurrentFileWriter.getInstance();
	
	
	
	//==> Constructors
	
	protected StepTwoWorker(){}
	
	public StepTwoWorker(KnowldgeBaseCompiler kbc, File data, String tempFolder, Map<String,Integer> wordOccur){
		this.kbc = kbc;
		this.data = data;
		this.tempFolder = tempFolder+"step2"+File.separator;
		this.wordOccur = wordOccur;
	}//StepTwoWorker

	
	
	//==> Methods
	
	@Override
	public void run() {
		if( data != null ){
			//process the file
			String fileName = data.getName();
			char firstChar = fileName.charAt(0);
			String wordFilename = tempFolder+firstChar+".dat";
			//
			int length = 0;
			LineNumberReader lnr = null;
			try{
				//compute the file length
				lnr = new LineNumberReader( new FileReader(data) );
				lnr.skip(Long.MAX_VALUE);
				length = lnr.getLineNumber();
			}catch (FileNotFoundException e) { 
				// File Not Found => Skip
				return;
			}catch (IOException e) { /* do nothing */ }
			finally{
				try {
					lnr.close();
				} catch (IOException e) { /* do nothing */ }
			}
			//
			String word = fileName.replaceFirst("[.][^.]+$", ""); //<= remove the extension
			String text = word+" | "+length+"\n";
			bfw.appendToFile(wordFilename, text);
			wordOccur.put(word, length);
			//
			Statistics.decreaseInt("threadBufferLength");
			kbc.bufferSem.release();
		}
	}//run

}//StepTwoWorker
