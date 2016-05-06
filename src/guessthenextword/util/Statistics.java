package guessthenextword.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONStringer;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Statistics {

	//==> Fields

	private static boolean initialized = false;

	private static Map<String,Object> blackboard;

	private static Semaphore blackboardSem = new Semaphore(1, true);

	private static StatisticsGUI gui = new StatisticsGUI();
	private static StatisticsTableGUI egui;
	private static Clock guiClock;
	
	private static StatisticsServer server;


	
	//==> Methods

	public static void init( int serverPort ){
		init();
		//
		if( server == null ){
			server = new StatisticsServer( serverPort );
			server.start();
			Logger.log("Statistics", "Server listening on port "+serverPort+".");
		}
	}//init
	
	public static void init(){
		if( !initialized ){
			//initialize the blackboard
			blackboard = new ConcurrentHashMap<String,Object>();
			//set the exception handler
			Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
				public void uncaughtException(Thread t, Throwable e) {
					// do nothing!
				}
			});
			//set as initialized
			initialized = true;
		}
	}//init

	public static void note(String var, Object value){
		blackboard.put(var, value);
	}//note

	public static Object note(String var){
		Object val = blackboard.get(var);
		return (val == null)? "undefined" : val;
	}//note

	public static void remNote(String var){
		blackboard.remove(var);
	}//note

	public static Integer intNote(String var){
		return intNote(var, 0);
	}//intNote

	public static Integer intNote(String var, int defaultValue){
		Integer val = defaultValue;
		Object o = blackboard.get(var);
		if( o != null && o instanceof Integer ){
			val = ((Integer)o);
		}
		return val;
	}//intNote

	public static int increaseInt(String var){
		return increaseInt(var,1);
	}//increaseInt

	public static int decreaseInt(String var){
		return decreaseInt(var, 1);
	}//decreaseInt

	public static int increaseInt(String var, int value){
		int res = value;
		try{
			blackboardSem.acquire();
			//
			Object o = blackboard.get(var);
			if( o == null ){
				o = 0;
			}
			if( o instanceof Integer ){
				res = ((Integer)o)+value;
				blackboard.put(var, res);
			}
		}catch(InterruptedException ie){ /* do nothing */ }
		finally{
			//
			blackboardSem.release();
		}
		return res;
	}//increaseInt

	public static int decreaseInt(String var, int value){
		int res = -value;
		try{
			blackboardSem.acquire();
			//
			Object o = blackboard.get(var);
			if( o == null ){
				o = 0;
			}
			if( o instanceof Integer ){
				res = ((Integer)o)-value;
				blackboard.put(var, res);
			}
		}catch(InterruptedException ie){ /* do nothing */ }
		finally{
			//
			blackboardSem.release();
		}
		return res;
	}//decreaseInt

	@SuppressWarnings("unchecked")
	public static void mapNote(String var, Object key, Object value){
		Map<Object,Object> map = null;
		Object o = blackboard.get(var);
		if( o != null ){
			if( o instanceof Map ){
				try{
					map = ((Map<Object,Object>)o);
				}catch(Exception e){
					return;
				}
			}else{
				return;
			}
		}
		if( map == null ){
			map = new ConcurrentHashMap<Object,Object>();
			blackboard.put(var, map);
		}
		map.put(key, value);
	}//mapNote

	@SuppressWarnings("unchecked")
	public static Map<Object,Object> mapNote(String var){
		Map<Object,Object> map = null;
		Object o = blackboard.get(var);
		if( o != null && o instanceof Map ){
			try{
				map = ((Map<Object,Object>)o);
			}catch(Exception e){
				map = new ConcurrentHashMap<Object,Object>();
			}
		}else{
			map = new ConcurrentHashMap<Object,Object>();
		}
		return map;
	}//mapNote

	@SuppressWarnings("unchecked")
	public static void listNote(String var, Object value){
		Queue<Object> list = null;
		Object o = blackboard.get(var);
		if( o != null ){
			if( o instanceof List ){
				try{
					list = ((Queue<Object>)o);
				}catch(Exception e){
					return;
				}
			}else{
				return;
			}
		}
		if( list == null ){
			list = new ConcurrentLinkedQueue<Object>();
			blackboard.put(var, list);
		}
		list.add(value);
	}//listNote

	@SuppressWarnings("unchecked")
	public static Queue<Object> listNote(String var){
		Queue<Object> list = null;
		Object o = blackboard.get(var);
		if( o != null && o instanceof Queue ){
			try{
				list = ((Queue<Object>)o);
			}catch(Exception e){
				list = new ConcurrentLinkedQueue<Object>();
			}
		}else{
			list = new ConcurrentLinkedQueue<Object>();
		}
		return list;
	}//listNote

	@SuppressWarnings("unchecked")
	public static void remMapNote(String var, Object key){
		Object o = blackboard.get(var);
		if( o != null ){
			if( o instanceof Map ){
				((Map<Object,Object>)o).remove(key);
			}
		}
	}//listNote

	public static void tic(String var){
		note(var, System.currentTimeMillis());
	}//tic

	public static void toc(String var){
		Object val = blackboard.get(var);
		//
		if( val != null && val instanceof Long){
			Long time = (Long)val;
			long now = System.currentTimeMillis();
			//
			Logger.log("Statistics", "Elapsed Time ["+var+"]: "+Formatter.formatTime( (int)Math.abs(now-time) ) );
		}
	}//toc

	public static void generateGUI(int refreshEveryMillis){
		gui.setVisible(true);
		//
		Runnable job = new Runnable() {
			@Override
			public void run() {
				gui.threadBufferProgressBar.setMaximum( Statistics.intNote("maxThreadBufferLength") );
				int fileBufferLength = Statistics.intNote("fileBufferLength");
				if( fileBufferLength > gui.fileBufferProgressBar.getMaximum() ){
					gui.fileBufferProgressBar.setMaximum(fileBufferLength);
				}
				gui.threadBufferProgressBar.setValue( Statistics.intNote("threadBufferLength") );
				gui.threadBufferField.setText( Statistics.intNote("threadBufferLength")+"" );
				gui.fileBufferProgressBar.setValue( fileBufferLength );
				gui.fileBufferField.setText( fileBufferLength+"" );
				gui.currentStepField.setText( Statistics.note("currentStep").toString() );
				gui.currentJobField.setText( Statistics.note("currentJob").toString() );
				gui.fileNameField.setText( Statistics.note("currentFile").toString() );
				int fileProgress = Statistics.intNote("fileProgress");
				gui.percentageProgressBar.setValue( fileProgress );
				gui.percentageField.setText( fileProgress+"%" );
				gui.etaField.setText( Statistics.note("fileRemainingTime").toString() );
				//
				if( egui != null ){
					//fill table
					egui.clearTable();
					Map<Object,Object> map = Statistics.mapNote("list");
					for( Map.Entry<Object,Object> o : map.entrySet() ){
						try{
							String file = (String) o.getKey();
							int prog = Integer.parseInt(o.getValue().toString());
							//
							egui.addRow(file, prog);
						}catch(Exception e){
							e.printStackTrace();
							continue;
						}
					}
					egui.refresh();
					//optimization informations
					String excludedFiles = Statistics.intNote("excludedFilesCount").toString();
					String excludedSize = Formatter.formatSize(Statistics.intNote("excludedFilesSize"),2);
					egui.stopWordsExclField.setText(excludedFiles+" files ( ~ "+excludedSize+")");
					Integer intelliOriginalIOcount = Statistics.intNote("intelliOriginalIOcount", 1);
					Integer intelliRealIOcount = Statistics.intNote("intelliRealIOcount", 1);
					//
					int savedIOcount = intelliOriginalIOcount - intelliRealIOcount;
					int savedPercent = (int) ( (((double)savedIOcount)/((double)intelliOriginalIOcount)) * 100 );
					egui.intelliIOField.setText(savedPercent+"% I/O operations saved!");
				}
			}
		};
		if( guiClock != null ){
			guiClock.interrupt();
		}
		guiClock = new Clock( job, true, refreshEveryMillis );
		guiClock.start();
	}//generateGUI

	public static void openExtraGUI(){
		egui = new StatisticsTableGUI(gui);
		egui.setVisible(true);
	}//openExtraGUI

	public static void close(){
		if( guiClock != null && !guiClock.isInterrupted() ){
			guiClock.interrupt();
		}
		if( gui != null ){
			gui.dispose();
		}
		if( egui != null ){
			egui.dispose();
		}
		if( server != null ){
			try{
				server.close();
				server.interrupt();
			}catch(Exception e){}
		}
	}//close

	//= Utility methods

	public static int getRemainingTime(int perc1, int perc2, long timeMillis1, long timeMillis2){
		int valueDiff = Math.abs(perc2-perc1);
		long timeDiff = Math.abs(timeMillis2-timeMillis1);
		double timePerPoint = ((double)timeDiff) / ((double)valueDiff);
		//
		return (int) (((double)(100-perc2)) * timePerPoint);
	}//getRemainingTime



	//==> Inner Class

	private static class StatisticsServer extends Thread{

		//==> Fields

		private int port;
		ServerSocket ss = null;


		//==> Constructors

		public StatisticsServer( int port ){
			this.port = port;
		}//StatisticsServer


		//==> Methods

		@Override
		public void run() {
			int failCounter = 0;
			//
			while( failCounter < 10 && !this.isInterrupted() ){
				try {
					//
					ss = new ServerSocket( port );
					while( true ){
						Socket s = ss.accept();
						//new client connected
						new Thread( new Worker(s) ).start();
					}
					//
				} catch (IOException e) {
					//increase the failure counter
					failCounter++;
				}finally{
					if( ss != null ){
						try {
							ss.close();
						} catch (IOException e) {}
					}
				}
				//
			}
		}//run
		
		public void close(){
			try {
				ss.close();
			} catch (IOException e) {}
		}//close
		
		
		//=> Inner Class
		
		private static class Worker implements Runnable{
			private Socket s;
			private static final String[] keys = new String[]{
				"maxThreadBufferLength",
				"fileBufferLength",
				"threadBufferLength",
				"currentStep",
				"currentJob",
				"currentFile",
				"fileProgress",
				"fileRemainingTime",
				"excludedFilesCount",
				"excludedFilesSize",
				"intelliOriginalIOcount",
				"intelliRealIOcount",
			};
			
			public Worker(Socket s){
				this.s = s;
			}//Worker
			
			@Override
			public void run(){
				try{
					BufferedWriter w = new BufferedWriter( new OutputStreamWriter( s.getOutputStream() ) );
					
					JSONStringer jss = new JSONStringer();
					try {
						jss.object();
						for( String key : keys ){
							jss
								.key( key )
								.value( Statistics.note( key ).toString() );
						}
						jss.endObject();
					} catch (JSONException e) {e.printStackTrace();}
					
					String json = jss.toString();
					w.write(json);
					
					w.flush();
					w.close();
					//
					s.close();
				}catch(Exception e){ }
			}//run
		}//Worker

	}//StatisticsServer

}//Statistics
