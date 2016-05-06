package guessthenextword.util;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class BufferedConcurrentFileWriter implements Flushable, Closeable{
	
	//==> Fields
	
	private Clock clock;
	
	private static Semaphore dataSem = new Semaphore(1,true);
	
	private static BufferedConcurrentFileWriter instance;
	private Map<String,StringBuilder> data = new ConcurrentHashMap<String,StringBuilder>();
	
	
	
	//==> Constructors
	
	private BufferedConcurrentFileWriter(){
		//create the clock
		Runnable job = new Runnable() {
			@Override
			public void run() {
				//call the flush function
				BufferedConcurrentFileWriter.getInstance().flush();
			}
		};
		clock = new Clock(job, true, 4000);
		//start the clock
		clock.start();
	}//BufferedConcurrentFileWriter
	
	
	
	//==> Methods
	
	public static BufferedConcurrentFileWriter getInstance(){
		if( instance == null ){
			instance = new BufferedConcurrentFileWriter();
		}
		return instance;
	}//getInstance
	
	public int size(){
		return data.size();
	}//size
	
	public void appendToFile( String file, String text ){
		try{
			dataSem.acquire();
			//
			StringBuilder sb = data.get(file);
			if( sb == null ){
				sb = new StringBuilder();
				data.put(file, sb);
			}
			sb.append(text);
		}catch(InterruptedException ie){ /* do nothing */ }
		finally{
			//
			dataSem.release();
		}
	}//appendToFile
	
	//= Private/Protected Methods
	
	@Override
	public void flush() {
		BufferedWriter writer;
		//
		if( data.size() > 0 ){
			//take a snapshot of the keys list
			List<String> keys = new LinkedList<String>();
			try{
				dataSem.acquire();
				//
				keys.addAll( data.keySet() );
			}catch(InterruptedException ie){ /* do nothing */ }
			finally{
				//
				dataSem.release();
			}
			//write out the buffer content
			for( String filename : keys ){
				StringBuilder text = data.remove(filename);
				if( text != null ){
					try{
						writer = new BufferedWriter( new FileWriter( new File( filename ) , true ) );
						writer.write( text.toString() );
						writer.flush();
						writer.close();
					}catch(IOException ioe){ /*do nothing*/ }
				}
			}
			//update the statistics
			Statistics.note("fileBufferLength", data.size());
		}
	}//flush
	
	@Override
	public void close(){
		clock.interrupt();
	}//close
	
}//BufferedConcurrentFileWriter
