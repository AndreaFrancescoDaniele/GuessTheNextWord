package guessthenextword.structures;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Sentence {
	
	//==> Fields
	
	private List<String> words;
	private String sentenceID;
	
	
	
	//==> Constructors
	
	protected Sentence(){}
	
	public Sentence(List<String> words, String sentenceID){
		this.sentenceID = sentenceID;
		this.words = words;
	}//Sentence
	
	
	
	//==> Methods
	
	public String getSentenceID(){
		return sentenceID;
	}//getSentenceID
	
	public int getNumWords(){
		return words.size();
	}//getNumWords
	
	public String getWord(int i){
		if(i < 0){
			throw new IndexOutOfBoundsException("Requested the word number "+i+". 'i' must be >= 0.");
		}
		if(i >= getNumWords()){
			throw new IndexOutOfBoundsException("Requested the word number "+i+" but this sentence only contains "+getNumWords()+" words.");
		}
		return words.get(i);
	}//getWord
	
	public boolean contains(String s){
		for(int i=0; i<getNumWords(); i++){
			if( words.get(i).equals(s) ){
				return true;
			}
		}
		return false;
	}//contains
	
	public String[] toArrayOfDistinctLemmas(){
		//temporary set
		Set<String> tmp = new HashSet<String>();
		int len = getNumWords();
		for(int i=0; i<len; i++){
			tmp.add( words.get(i) );
		}
		//
		String[] result = new String[tmp.size()];
		return tmp.toArray(result);
	}//toArrayOfDistinctLemmas
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for( String word : words ){
			sb.append(word).append(' ');
		}
		return sb.toString().trim();
	}//toString

}//Sentence