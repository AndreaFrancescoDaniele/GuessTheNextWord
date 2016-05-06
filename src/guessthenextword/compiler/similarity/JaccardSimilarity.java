package guessthenextword.compiler.similarity;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class JaccardSimilarity implements Similarity {
	
	//==> Fields
	
	private static Similarity instance;
	
	
	
	//==> Constructors
	
	private JaccardSimilarity(){};
	
	
	
	//==> Methods
	
	public static Similarity getInstance() {
		if( instance == null ){
			instance = new JaccardSimilarity();
		}
		return instance;
	}//getInstance

	@Override
	public double computeSimilarity(int wordAcount, int wordBcount, int coOccurCount) {
		//compute and return the Jaccard Similarity
		return (double)coOccurCount / ( (double)wordAcount + (double)wordBcount - (double)coOccurCount );
	}//computeSimilarity

}//JaccardSimilarity
