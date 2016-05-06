package guessthenextword.run;

import guessthenextword.util.Clock;
import guessthenextword.util.Logger;
import guessthenextword.util.Statistics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Iterator;

import org.json.JSONObject;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class RemoteStatistics {
	
	//==> Fields
	
	private static int defaultPort = 14000; //<= default port
	private static int defaultRefreshEveryMillis = 1000; //<= default refresh rate
	
	private static int port = defaultPort;
	private static int refreshEveryMillis = defaultRefreshEveryMillis;
	

	
	//==> Methods
	
	public static void main(String[] args) {
		if( args.length == 0 ){
			args = new String[]{"localhost", defaultPort+"", defaultRefreshEveryMillis+""};
		}
		if( args.length != 3 ){
			throw new IllegalArgumentException("Error using the remote statistics launcher. \n Usage: \t  > guessthenextword.run.RemoteStatistics <host_or_ip> <port> <refreshEveryMillis>");
		}
		//
		try{
			port = Integer.parseInt(args[1]);
			if( port < 1 || port > 65535 ){
				//illegal argument => default
				port = defaultPort;
				Logger.log("RemoteStatistics", "Invalid argument! server port: "+port+". The default port will be used: "+defaultPort);
			}
			//
			refreshEveryMillis = Integer.parseInt(args[2]);
			if( refreshEveryMillis < 200 ){
				//illegal argument => default
				refreshEveryMillis = defaultRefreshEveryMillis;
				Logger.log("RemoteStatistics", "Invalid argument! refresh rate: "+refreshEveryMillis+"ms (>= 200ms allowed). The default value will be used: "+defaultRefreshEveryMillis);
			}
		}catch(Exception e){}
		//
		//initialize the Statistics
		Statistics.init();
		Statistics.generateGUI( refreshEveryMillis );
		//
		final String server = args[0];
		final int serverPort = port;
		//
		Runnable job = new Runnable() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				//
				Socket s = null;
				try {
					s = new Socket(server, serverPort);
					
					BufferedReader br = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
					StringBuilder sb = new StringBuilder();
					
					String line = br.readLine();
					while(line != null){
						sb.append(line);
						//
						line = br.readLine();
					}
					//
					br.close();
					s.close();
					
					JSONObject jso = new JSONObject( sb.toString().trim() );
					
					Iterator<String> keys = jso.keys();
					while( keys.hasNext() ){
						String key = keys.next();
						String value = jso.getString(key);
						//
						boolean flag = false;
						try{
							Integer.parseInt(value);
							flag = true;
						}catch(Exception e){}
						//
						if( flag ){
							Statistics.note(key, Integer.parseInt(value));
						}else{
							Statistics.note(key, value);
						}
					}
					
				}catch(Exception e){}
			}
		};
		//
		Clock c = new Clock(job, true, refreshEveryMillis);
		c.start();
	}//main

}//RemoteStatistics
