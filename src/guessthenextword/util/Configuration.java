package guessthenextword.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Configuration extends Properties {

	private static final long serialVersionUID = 4065490579861867464L;
	
	//==> Fields
	
	private static final String configurationFile = "config"+File.separator+"config.properties";
	private static Properties properties;
	
	
	
	//==> Methods
	
	public static Properties getConfiguration(){
		if( properties == null ){
			loadConfigurations();
		}
		return properties;
	}//getConfiguration

	private static void loadConfigurations(){
		//load properties
		try{
			properties = new Properties();
			properties.load( new FileInputStream( new File( configurationFile ) ) );
		}catch(IOException e){
			System.err.println("Configuration: Error while loading the configuration file.");
		}
	}//loadConfigurations

}//Configuration
