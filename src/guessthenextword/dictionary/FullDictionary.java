package guessthenextword.dictionary;

import guessthenextword.util.Logger;

import java.util.Set;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class FullDictionary {
	
	//==> Fields
	
	private Set<String> data;
	
	
	
	//==> Methods
	
	public int size(){
		return (data == null)? 0 : data.size();
	}//size
	
	public void addDictionary( Set<String> dict ){
		if( dict != null ){
			int count = 0;
			if( data == null ){
				data = dict;
				count = dict.size();
			}else{
				for( String word : dict ){
					if( !data.contains(word) ){
						data.add( word );
						count++;
					}
				}
			}
			Logger.log(this.getClass().getSimpleName(), "Added new dictionary! ("+count+" new elements added)");
		}else{
			System.err.println(this.getClass().getSimpleName()+": Error occurred adding a new dictionary!");
		}
	}//addDictionary
	
	public boolean contains( String word ){
		return data.contains( word );
	}//contains

}//FullDictionary
