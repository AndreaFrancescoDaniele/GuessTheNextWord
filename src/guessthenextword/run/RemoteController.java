package guessthenextword.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class RemoteController {

	public static void main(String[] args) {
		if( args.length < 2 || args.length > 3){
			showErrorAndClose();
		}
		//
		int port = 14001; // <= default port
		//
		String server = args[0];
		String command = "";
		if( args.length == 2 ){
			// use default port
			command = args[1];
		}else if( args.length == 3 ){
			try{
				port = Integer.parseInt( args[1] );
			}catch(Exception e){}
			command = args[2];
		}else{
			showErrorAndClose();
		}
		//
		try {
			//
			Socket s = new Socket(server, port);
			s.setSoTimeout(4000);
			BufferedWriter bw = new BufferedWriter( new OutputStreamWriter( s.getOutputStream() ) );
			//send command
			bw.write(command);
			bw.flush();
			System.out.println("Message sent: "+command);
			s.shutdownOutput();
			//
			BufferedReader br = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
			//wait a response for the next 4 seconds
			String response = br.readLine();
			System.out.println("Response: "+response);
			//close
			s.shutdownInput();
			s.close();
			//
		} catch (Exception e) {
			System.out.println("Error occurred connecting to "+server+":"+port+"\nReason:\n \t"+e.toString()+"\n");
			//
			System.exit(-1);
		}
		//
	}//main

	
	private static void showErrorAndClose(){
		System.out.println();
		String txt = 
		  "Command syntax error.\n"
		+ " * ==================================================== \n"
		+ " * Usage: \n"
		+ " *  \n"
		+ " * \t send a 'commandString' to a running GuessTheNextWork compiler server identified by 'host' \n"
		+ " * \t or 'IP', and listening on 'serverPort' TCP port.\n"
		+ " *  \n"
		+ " * \t   > guessthenextword.run.RemoteController <serverHost_or_IP> [<serverPort>] <commandString> \n"
		+ " * \n"
		+ " * \n"
		+ " The program will now exit! \n";
		System.out.println(txt);
		System.exit(-1);
	}//showErrorAndClose
	
}//RemoteController
