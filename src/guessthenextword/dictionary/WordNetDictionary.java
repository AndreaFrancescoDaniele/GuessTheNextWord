package guessthenextword.dictionary;

import guessthenextword.util.Configuration;
import guessthenextword.util.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class WordNetDictionary implements Dictionary{
	
	//==> Fields
	
	private String wordNetLocation = null;
	private Set<String> data = null;
	
	private String validLineRegEx = "^([\\w]+) (a|r|n|v) \\d \\d (.*)$";
	private Pattern p = Pattern.compile(validLineRegEx);
	
	private Properties properties = Configuration.getConfiguration();

	
	
	//==> Methods
	
	public void init() {
		wordNetLocation = properties.getProperty("wordNetPath");
		wordNetLocation = (wordNetLocation == null)? /*Default*/ "/usr/local/WordNet-3.0/" : wordNetLocation;
		//load
		loadDictionary();
	}//init
	
	public void loadDictionary(){
		if( wordNetLocation == null ){
			init();
		}
		data = new HashSet<String>();
		//obtain the files to load
		String wordNetFiles = properties.getProperty("wordNetFiles");
		wordNetFiles = (wordNetFiles == null)? /*Default*/ "index.adj index.adv index.noun index.verb" : wordNetFiles;
		String[] files = wordNetFiles.split("\\s");
		//for each file
		Matcher m;
		BufferedReader br;
		int total = 0;
		Logger.log(WordNetDictionary.class.getSimpleName(), ": Dictionary loading...");
		for( String file : files ){
			int count = 0;
			try {
				br = new BufferedReader( new FileReader( new File( wordNetLocation+file ) ) );
				String line = br.readLine();
				while( line != null ){
					m = p.matcher(line);
					if( m.find() ){
						String word = m.group(1);
						if( !data.contains(word) ){
							data.add( word );
							count++;
							total++;
						}
					}
					line = br.readLine();
				}
				br.close();
			} catch (Exception e) { 
				System.err.println(WordNetDictionary.class.getSimpleName()+": Error occurred while loading file: "+wordNetLocation+file);
			}
			Logger.log(WordNetDictionary.class.getSimpleName(), "File "+file+" loaded successfully! ("+count+" new words)");
			count = 0;
		}
		Logger.log(WordNetDictionary.class.getSimpleName(), "Load completed! ("+total+" total words) \n");
	}//loadDictionary
	
	@Override
	public Set<String> getDictionary(){
		if( wordNetLocation == null ){
			init();
		}
		if( data == null ){
			loadDictionary();
		}
		return data;
	}//getDictionary

}//WordNetDictionary
