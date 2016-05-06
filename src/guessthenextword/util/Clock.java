package guessthenextword.util;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Clock extends Thread{
	
	//==> Fields
	
	private int sleep;
	private Runnable job;
	private boolean cyclic = false;
	
	private boolean lastRun = true;
	private boolean isInterrupted = false;
	
	
	
	//==> Constructors
	
	protected Clock(){}
	
	public Clock( Runnable job, int sleep ){
		this.job = job;
		this.sleep = sleep;
	}//Clock
	
	public Clock( Runnable job , boolean cyclic, int sleep ){
		this.job = job;
		this.sleep = sleep;
		this.cyclic = cyclic;
	}//Clock
	
	
	
	//==> Methods
	
	@Override
	public void interrupt(){
		lastRun = true;
		isInterrupted = true;
		super.interrupt();
	}//interrupt
	
	@Override
	public void run(){
		boolean firstRun = true;
		if( job != null ){
			while( firstRun || lastRun || (cyclic && !isInterrupted) ){
				firstRun = false;
				lastRun = false;
				//
				job.run();
				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) { isInterrupted = true; }
				//
			}//while
		}
	}//run
	
}//Clock
