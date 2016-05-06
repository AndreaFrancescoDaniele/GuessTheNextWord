package guessthenextword.prophets;

import java.util.List;
import java.util.Map;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public interface Prophet {
	
	public String guess(List<String> words);
	
	public List<String> guess(List<String> words, int maxLength);
	
	public List<String> guess(List<String> words, int maxLength, Map<String,Double> copyResults);
	
	public String getID();

}//Prophet
