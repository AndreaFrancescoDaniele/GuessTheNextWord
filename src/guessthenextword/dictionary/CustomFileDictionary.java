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
public class CustomFileDictionary implements Dictionary {
	
	//==> Fields
	
	private String file;
	private String regex;
	private int[] validGroups;
	private Set<String> data = null;
	private String dictionaryName;
	private Properties properties = Configuration.getConfiguration();
	
	
	
	//==> Constructors
	
	protected CustomFileDictionary(){};
	
	public CustomFileDictionary( int customFileID ){
		this.dictionaryName = this.getClass().getSimpleName();
		this.file = properties.getProperty("customFile"+customFileID, "");
		this.regex = properties.getProperty("customRegEx"+customFileID, "");
		String vgs = properties.getProperty("customRegExGroups"+customFileID, "");
		String[] array = vgs.split(",");
		int validGroupsCount = 0;
		for( int i = 0; i < array.length; i++ ){
			try{
				Integer.parseInt(array[i]);
				validGroupsCount++;
			}catch(Exception e){
				array[i] = null;
			}
		}
		validGroups = new int[ validGroupsCount ];
		int i = 0;
		for( String s : array ){
			if( s != null ){
				try{
					validGroups[i] = Integer.parseInt(s);
					i++;
				}catch(Exception e){}
			}
		}
	}//CustomFileDictionary
	
	public CustomFileDictionary( String file, String regEx, String groups, String dictionaryName ){
		this.file = file;
		this.regex = regEx;
		String vgs = groups;
		this.dictionaryName = dictionaryName;
		String[] array = vgs.split(",");
		int validGroupsCount = 0;
		for( int i = 0; i < array.length; i++ ){
			try{
				Integer.parseInt(array[i]);
				validGroupsCount++;
			}catch(Exception e){
				array[i] = null;
			}
		}
		validGroups = new int[ validGroupsCount ];
		int i = 0;
		for( String s : array ){
			if( s != null ){
				try{
					validGroups[i] = Integer.parseInt(s);
					i++;
				}catch(Exception e){}
			}
		}
	}//CustomFileDictionary
	
	
	
	//==> Methods
	
	public void loadDictionary(){
		Pattern p = Pattern.compile(regex);
		data = new HashSet<String>();
		//obtain the files to load
		Matcher m;
		BufferedReader br;
		Logger.log(this.getClass().getSimpleName(), "Dictionary loading...");
		int count = 0;
		try {
			br = new BufferedReader( new FileReader( new File( file ) ) );
			String line = br.readLine();
			while( line != null ){
				m = p.matcher(line);
				if( m.find() ){
					//for each valid group in the regex
					for( int i : validGroups ){
						String word = m.group(i);
						if( !data.contains(word) ){
							data.add( word.toLowerCase() );
							count++;
						}
					}
				}
				line = br.readLine();
			}
			br.close();
		} catch (Exception e) { 
			Logger.log(dictionaryName, "Error occurred while loading file: "+file);
		}
		Logger.log(dictionaryName, "Load completed! ("+count+" total words)");
	}//loadDictionary

	@Override
	public Set<String> getDictionary(){
		if( data == null ){
			loadDictionary();
		}
		return data;
	}//getDictionary

}//CustomFileDictionary
