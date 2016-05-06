package guessthenextword.knowledge_base;

import java.io.Flushable;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public interface EditableKnowledgeBase extends KnowledgeBase, Flushable {

	//==> Compile Methods
	
	public void setDefaultSimilarityType(String similarityType );
	
	public boolean addNewEntry(String wordA, String wordB, int coOccurrences, double similarity, String similarityType);
	
	public boolean removeEntry(String wordA, String wordB);
	
	public void setFlushable(String wordA);
	
}//EditableKnowledgeBase
