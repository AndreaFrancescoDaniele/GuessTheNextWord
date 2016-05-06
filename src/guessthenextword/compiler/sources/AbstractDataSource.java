package guessthenextword.compiler.sources;

import guessthenextword.structures.Sentence;
import guessthenextword.util.Formatter;
import guessthenextword.util.Logger;
import guessthenextword.util.Statistics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public abstract class AbstractDataSource implements DataSource{
	
	//==> Fields

	//progress computation data
	private int percentage = 0;
	private long totalLines = 1;
	private long timeMillis = 0;

	//structural data
	protected List<File> sourceFiles;

	//source selection data
	protected int selectedSource = -1;

	//parser data
	protected BufferedReader source;
	
	
	
	//==> Constructors

	protected AbstractDataSource( File[] sourceFiles ){
		this.sourceFiles = new LinkedList<File>();
		// prune the useless files
		for( File f : sourceFiles ){
			if( f != null && f.exists() ){
				this.sourceFiles.add(f);
			}
		}
		//Logger
		String s = (this.sourceFiles.size() > 1)? "s" : "";
		Logger.log(this.getClass().getSimpleName(), this.sourceFiles.size()+" valid file"+s+" found!");
		int i = 1;
		for( File f : this.sourceFiles ){
			Logger.log(this.getClass().getSimpleName(), "| "+i+". "+f.getAbsolutePath());
			i++;
		}
		//
		if(!this.isAvailable()) throw new IllegalArgumentException(this.getClass().getSimpleName()+": The specified source files do not exist.");
	}//AbstractDataSource



	//==> Methods

	//=> DataSource Interface Inherited Methods

	@Override
	public String getSourceName(int index) {
		if( index >= 0 && index < sourceFiles.size() ){
			return sourceFiles.get(index).getName();
		}
		return null;
	}//getSourceName

	@Override
	public int getSourcesNum(){
		return sourceFiles.size();
	}//getSourcesNum

	@Override
	public boolean isAvailable() {
		boolean flag = false;
		for( File f : sourceFiles ){
			flag = flag || f.exists();
		}
		return flag;
	}//isAvailable

	@Override
	public boolean isAvailable(int index) {
		if( index >= 0 && index < sourceFiles.size() ){
			return sourceFiles.get(index).exists();
		}
		return false;
	}//isAvailable

	@Override
	public boolean selectSource(int index){
		if( isAvailable(index) ){
			try{
				//reset the environment
				percentage = 0;
				totalLines = 1;
				File file = sourceFiles.get(index);
				//compute the file size
				LineNumberReader lnr = new LineNumberReader( new FileReader( file ) );
				lnr.skip(Long.MAX_VALUE);
				totalLines = lnr.getLineNumber();
				lnr.close();
				//select the source
				selectedSource = index;
				//create a new reader
				source = new BufferedReader( new FileReader( file ) );
				Logger.log(this.getClass().getSimpleName(), "File "+file.getName()+" opened!");
				//return
				return true;
			}catch(Exception e){
				return false;
			}
		}
		return false;
	}//selectSource

	@Override
	public int getProgress() {
		return percentage;
	}//getProgress


	//=> Iterables Interface Inherited Methods

	@Override
	public Iterator<Sentence> iterator() {
		return this;
	}//iterator
	

	//=> Protected/Private Methods

	protected void sendProgressDataToLogger( String fileName, long currentLine ){
		long newTimeMillis = System.currentTimeMillis();
		int newPercentage = (int) (((double)currentLine/(double)(totalLines-1))*100);
		if( newPercentage != percentage ){
			String remainingTime = Formatter.formatTime( Statistics.getRemainingTime(percentage, newPercentage, timeMillis, newTimeMillis) );
			percentage = newPercentage;
			timeMillis = newTimeMillis;
			Logger.log(this.getClass().getSimpleName(), "File Parsing Status: "+fileName+" ( "+newPercentage+"% - "+remainingTime+" )");
			//
			Statistics.note("fileProgress", percentage);
			Statistics.note("fileRemainingTime", remainingTime);
		}
	}//sendProgressDataToLogger
	
}//AbstractDataSource
