package guessthenextword.prophets;

import guessthenextword.knowledge_base.KnowledgeBase;
import guessthenextword.structures.CoOccurrenceEntry;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class SimpleSimilarityProphet implements Prophet {
	
	//==> Fields
	
	private int itemsPerWord = 80; // Default/Gold: 80 (computed using test results)
	
	private KnowledgeBase knowledge;
	
	
	
	//==> Constructors
	
	protected SimpleSimilarityProphet(){}
	
	public SimpleSimilarityProphet(KnowledgeBase kb){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		this.knowledge = kb;
	}//SimpleSimilarityProphet
	
	public SimpleSimilarityProphet(KnowledgeBase kb, int itemsPerWord){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		this.knowledge = kb;
		this.itemsPerWord = itemsPerWord;
	}//SimpleSimilarityProphet
	
	
	
	//==> Methods

	@Override
	public String guess(List<String> words) {
		List<String> res = guess(words,1);
		if( res.size() != 0 ){
			return res.get(0);
		}
		return null;
	}//guess
	
	@Override
	public List<String> guess(List<String> words, int maxLength) {
		return guess(words, maxLength, null);
	}//guess

	@Override
	public List<String> guess(List<String> words, int maxLength, Map<String,Double> copyResults) {
		Map<String,Double> map = new HashMap<String,Double>();
		List<String> result = new LinkedList<String>();
		//for each word
		for( String wordB : words ){
			//obtain the coOccurrence list
			List<CoOccurrenceEntry> coOccs = knowledge.getCoOccurrences(wordB, itemsPerWord);
			//get the first 'itemsPerWord' words
			for(int i = 0; i<Math.min(itemsPerWord,coOccs.size()); i++){
				String wordC = coOccs.get(i).getSecondWord();
				if( !words.contains( wordC ) ){
					Double similarity = map.get(wordC);
					if( similarity == null ){
						similarity = 0d;
					}
					similarity += coOccs.get(i).getSimilarity();
					map.put(wordC, similarity);
				}
			}
		}
		//create the new data structure with a comparator
		ValueComparator vc = new ValueComparator( map );
		Map<String,Double> res = new TreeMap<String,Double>( vc );
		//export the HashMap into the TreeMap
		res.putAll( map );
		//compute the size of the resulting list
		int sz = Math.min(maxLength, res.size());
		//fill the resulting list
		int k = 0;
		for( String s : res.keySet() ){
			if( k < sz ){
				result.add(s);
			}else{
				break;
			}
			k++;
		}
		//copy the results
		if( copyResults != null ){
			copyResults.putAll( res );
		}
		return result;
	}//guess

	@Override
	public String getID() {
		return "SSP";
	}//getID

	@Override
	public String toString(){
		return this.getClass().getSimpleName()+" | itemsPerWord:"+itemsPerWord;
	}//toString
	
	
	//Inner-Class
	
	private class ValueComparator implements Comparator<String> {
		//==>Fields
	    Map<String,Double> base;
	    
	    //==> Constructors
		@SuppressWarnings("unused")
		protected ValueComparator(){}
	    public ValueComparator(Map<String,Double> base) {
	        this.base = base;
	    }//ValueComparator

	    @Override
	    public int compare(String a, String b) {
	    	Double dA = base.get(a);
	    	Double dB = base.get(b);
	    	if( dA != null && dB != null ){
	    		if( dA >= dB ){
	    			return -1;
	    		}else{
	    			return 1;
	    		}
	    	}else{
	    		//error
	    		throw new IllegalStateException("The map does not contain the keys '"+a+"' and/or '"+b+"'.");
	    	}
	    }//compare
	}//ValueComparator

}//SimpleSimilarityProphet
