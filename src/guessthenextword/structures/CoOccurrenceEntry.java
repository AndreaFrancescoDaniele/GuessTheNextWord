package guessthenextword.structures;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class CoOccurrenceEntry {
	
	//==> Fields

	private String wordA;
	private String wordB;
	private int coOccurrences;
	private double similarity;
	private String similarityType;
	
	
	
	//==> Constructors
	
	protected CoOccurrenceEntry(){}
	
	public CoOccurrenceEntry(String wordA, String wordB, int coOccurrences, double similarity, String similarityType){
		this.wordA = wordA;
		this.wordB = wordB;
		this.coOccurrences = coOccurrences;
		this.similarity = similarity;
		this.similarityType = similarityType;
	}//CoOccurrenceEntry
	
	
	
	//==> Methods
	
	public String getFirstWord(){
		return wordA;
	}//getFirstWord
	
	public String getSecondWord(){
		return wordB;
	}//getSecondWord
	
	public int getCoOccurrencesCount(){
		return coOccurrences;
	}//getCoOccurrencesCount
	
	public double getSimilarity(){
		return similarity;
	}//getSimilarity
	
	public String getSimilarityType(){
		return similarityType;
	}//getSimilarityType
	
}//CoOccurrenceEntry
