package guessthenextword.compiler.similarity;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public interface Similarity {

	public double computeSimilarity(int wordAcount, int wordBcount, int coOccurCount);
	
}//Similarity
