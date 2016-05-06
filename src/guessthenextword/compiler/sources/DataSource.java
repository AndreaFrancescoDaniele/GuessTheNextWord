package guessthenextword.compiler.sources;

import guessthenextword.structures.Sentence;

import java.util.Iterator;


/**
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 * 
 */
public interface DataSource extends Iterator<Sentence>, Iterable<Sentence>{
	
	public int getSourcesNum();
	
	public String getSourceName(int index);
	
	public boolean isAvailable();
	
	public boolean isAvailable(int index);
	
	public boolean selectSource(int index);
	
	public int getProgress();
	
}//DataSource
