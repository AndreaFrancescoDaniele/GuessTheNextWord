package guessthenextword.prophets;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import guessthenextword.knowledge_base.KnowledgeBase;
import guessthenextword.structures.CoOccurrenceEntry;
import guessthenextword.util.Configuration;
import guessthenextword.util.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class RewardProphet implements Prophet {

	//==> Fields

	private int itemsPerWord = 30; // Default/Gold: 30 (computed using test results)

	private KnowledgeBase knowledge;

	private static final String defaultWordNetPath = "/usr/local/WordNet-3.0/dict/";
	private WordNetDatabase database;

	private double intersectionWeight = 0.8, similarityWeight = 0.2; //Gold: 0.8 | 0.2
	private double loopReward = 0.85; // Gold: 0.85  ==>  x = x + 85%(x)


	
	//==> Constructors

	protected RewardProphet(){}

	public RewardProphet(KnowledgeBase kb){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		System.setProperty("wordnet.database.dir", Configuration.getConfiguration().getProperty("wordNetPath", defaultWordNetPath));
		//
		this.knowledge = kb;
		database = WordNetDatabase.getFileInstance();
	}//RewardProphet

	public RewardProphet(KnowledgeBase kb, int itemsPerWord){
		if( kb == null ){
			throw new IllegalArgumentException("The knowledge base must be != null.");
		}
		//
		System.setProperty("wordnet.database.dir", Configuration.getConfiguration().getProperty("wordNetPath", defaultWordNetPath));
		//
		this.knowledge = kb;
		this.itemsPerWord = itemsPerWord;
		database = WordNetDatabase.getFileInstance();
	}//RewardProphet



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

	public List<String> guess(List<String> words, int maxLength, Map<String,Double> copyResults) {
		List<String> result = new LinkedList<String>();
		Map<String,Double> rewardMap = new HashMap<String,Double>();
		//
		Map<String,Double> similarityMap = new HashMap<String,Double>();
		Map<String,Integer> intersectionMap = new HashMap<String,Integer>();
		//step1: coOccs list of B words
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
		// |1| base reward
		for( Map.Entry<String,Integer> entry : intersectionMap.entrySet() ){
			String key = entry.getKey();
			int val = entry.getValue();
			//
			addToMap( rewardMap, key, (double)val * intersectionWeight );
		}
		for( Map.Entry<String,Double> entry : similarityMap.entrySet() ){
			String key = entry.getKey();
			Double val = entry.getValue();
			//
			addToMap( rewardMap, key, val * similarityWeight );
		}
		//
		//step2: wordNet lemmatization
		//for each word
		for( String wordB : words ){
			Set<String> wordForms = getWordNetWordForms( wordB );
			//
			wordForms.removeAll(words);
			for( String s : wordForms ){
				// |2| loop1 reward
				if( rewardMap.containsKey(s) ){
					Double val = rewardMap.get(s);
					addToMap( rewardMap, s, val * loopReward );
				}
			}

		}
		//
		//create the new data structure with a comparator
		ValueComparator vc = new ValueComparator( rewardMap );
		Map<String,Double> res = new TreeMap<String,Double>( vc );
		//export the HashMap into the TreeMap
		res.putAll( rewardMap );
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
			copyResults.putAll( rewardMap );
		}
		return result;
	}//guess

	@Override
	public String getID() {
		return "RP";
	}//getID

	@Override
	public String toString(){
		return this.getClass().getSimpleName()+" | itemsPerWord:"+itemsPerWord;
	}//toString


	//=> Private/Protected Methods

	private Set<String> getWordNetWordForms(String word){
		Set<String> ss = new HashSet<String>();
		//
		try{
			Synset[][] synsets = new Synset[4][];
			synsets[0] = database.getSynsets(word, SynsetType.ADJECTIVE);
			synsets[1] = database.getSynsets(word, SynsetType.ADVERB);
			synsets[2] = database.getSynsets(word, SynsetType.NOUN);
			synsets[3] = database.getSynsets(word, SynsetType.VERB);
			//
			for( Synset[] syns : synsets ){
				for( Synset s : syns ){
					String[] wordForms = s.getWordForms();
					//
					for( String w : wordForms ){
						ss.add( w );
					}
				}
			}
		}catch(Exception e){ 
			Logger.log(this.getClass().getSimpleName(), "The WordNet service is not available at the specified path. Please update the configuration file.");
		}
		//
		return ss;
	}//getWordNetWordForms

	private void addToMap(Map<String,Double> map, String key, Double addVal){
		Double val = map.get( key );
		if( val == null ){
			val = 0d;
		}
		val += addVal;
		//update the map
		map.put(key, val);
	}//addToMap


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

}//RewardProphet
