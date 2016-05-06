package guessthenextword.compiler;

import guessthenextword.util.BufferedConcurrentFileWriter;
import guessthenextword.util.Statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class StepThreeWorker implements Runnable {
	
	//==> Fields
	
	private String data;
	private String tempFolder;
	private String step1SentencesFolder;
	private KnowldgeBaseCompiler kbc;
	
	private BufferedConcurrentFileWriter bfw = BufferedConcurrentFileWriter.getInstance();
	
	
	
	//==> Constructors
	
	protected StepThreeWorker(){}
	
	public StepThreeWorker(KnowldgeBaseCompiler kbc, String data, String tempFolder, String step1SentencesFolder){
		this.kbc = kbc;
		this.data = data;
		this.tempFolder = tempFolder+"step3"+File.separator;
		this.step1SentencesFolder = step1SentencesFolder;
	}//StepThreeWorker

	
	
	//==> Methods
	
	@Override
	public void run() {
		if( data != null ){
			File file = new File(step1SentencesFolder+data);
			if( file.exists() ){
				//process the file
				List<String> fileContent = new ArrayList<String>((int) Math.pow(10, ConcurrentSentenceIDProvider.sentenceSuffixLength));
				//read the file
				try{
					BufferedReader br = new BufferedReader( new FileReader( file ) );
					String line = br.readLine();
					while( line != null ){
						fileContent.add(line);
						//get the next line
						line = br.readLine();
					}
					//
					br.close();
				}catch(Exception e){}
				Collections.sort(fileContent);
				//write the sorted file
				for( String s : fileContent ){
					bfw.appendToFile(tempFolder+data, s+"\n");
				}
				//
				Statistics.decreaseInt("threadBufferLength");
				kbc.bufferSem.release();
			}
		}
	}//run

}//StepThreeWorker
