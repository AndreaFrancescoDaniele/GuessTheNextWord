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
public class HybridProphet implements Prophet {
	
	//==> Fields
	
	private int itemsPerWord = 75; // Default/Gold: 75 (computed using test results)
	
	private KnowledgeBase knowledge;
	
	
	
	//==> Constructors
	
	protected HybridProphet(){}
	
	public HybridProphet(KnowledgeBase kb){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		this.knowledge = kb;
	}//HybridProphet
	
	public HybridProphet(KnowledgeBase kb, int itemsPerWord){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		this.knowledge = kb;
		this.itemsPerWord = itemsPerWord;
	}//HybridProphet
	
	
	
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
		Map<String,Double> similarityMap = new HashMap<String,Double>();
		Map<String,Integer> intersectionMap = new HashMap<String,Integer>();
		List<String> result = new LinkedList<String>();
		//for each word
		for( String wordB : words ){
			//obtain the coOccurrence list
			List<CoOccurrenceEntry> coOccs = knowledge.getCoOccurrences(wordB, itemsPerWord);
			//get the first 'itemsPerWord' words
			for(int i = 0; i<Math.min(itemsPerWord,coOccs.size()); i++){
				String wordC = coOccs.get(i).getSecondWord();
				if( !words.contains( wordC ) ){
					//similarity
					Double similarity = similarityMap.get(wordC);
					if( similarity == null ){
						similarity = 0d;
					}
					similarity += coOccs.get(i).getSimilarity();
					similarityMap.put(wordC, similarity);
					//intersection
					Integer val = intersectionMap.get( wordC );
					if( val == null ){
						val = 1;
					}else{
						val++;
					}
					intersectionMap.put(wordC, val);
				}
			}
		}
		//sort the map wrt: intersection, then similarity
		CombinedValueComparator cvc = new CombinedValueComparator( intersectionMap, similarityMap );
		Map<String,Integer> res = new TreeMap<String,Integer>( cvc );
		//export the HashMap into the TreeMap
		res.putAll( intersectionMap );
		//compute the size of the resulting list
		int sz = Math.min(maxLength, res.size());
		//fill the resulting list
		int k = 0;
		for( String s : res.keySet() ){
			if( copyResults != null ){
				//copy the results
				copyResults.put( s , new Double(res.get(s)) );
			}else{
				if( k >= sz ){
					break;
				}
			}
			//
			if( k < sz ){
				result.add(s);
			}
			k++;
		}
		return result;
	}//guess

	@Override
	public String getID() {
		return "HP";
	}//getID
	
	@Override
	public String toString(){
		return this.getClass().getSimpleName()+" | itemsPerWord:"+itemsPerWord;
	}//toString
	
	
	//Inner-Class
	
	private class CombinedValueComparator implements Comparator<String> {
		//==>Fields
	    Map<String,Integer> base1;
	    Map<String,Double> base2;
	    
	    //==> Constructors
		@SuppressWarnings("unused")
		protected CombinedValueComparator(){}
	    public CombinedValueComparator(Map<String,Integer> base1, Map<String,Double> base2) {
	        this.base1 = base1;
	        this.base2 = base2;
	    }//IntegerValueComparator

	    @Override
	    public int compare(String a, String b) {
	    	Integer iA = base1.get(a);
	    	Integer iB = base1.get(b);
	    	if( iA != null && iB != null ){
	    		if( iA > iB ){
	    			return -1;
	    		}else if( iA < iB ){
	    			return 1;
	    		}else{
	    			Double dA = base2.get(a);
	    	    	Double dB = base2.get(b);
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
	    		}
	    	}else{
	    		//error
	    		throw new IllegalStateException("The map does not contain the keys '"+a+"' and/or '"+b+"'.");
	    	}
	    }//compare
	}//CombinedValueComparator

}//HybridProphet
