package guessthenextword.knowledge_base;

import guessthenextword.structures.CoOccurrenceEntry;

import java.util.List;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public interface KnowledgeBase{
	
	//==> Query Methods
	
	public boolean isAvailable();
	
	public boolean isAvailable( String word );
	
	public List<CoOccurrenceEntry> getCoOccurrences( String word );
	
	public List<CoOccurrenceEntry> getCoOccurrences( String word, int resultMaxLength );
	
	public void close();
	
}//KnowledgeBase
