package guessthenextword.util;

import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class Formatter {
	
	//==> Fields
	
	private static final double SECONDS = 1000;
	private static final double MINUTES = 60 * SECONDS;
	private static final double HOURS = 60 * MINUTES;
	private static final double DAYS = 24 * HOURS;
	
	
	
	//==> Methods
	
	public static String formatTime(int timeMillis){
		int days = (int) ((double)(timeMillis) / DAYS);
		timeMillis = (int) (timeMillis-(days*DAYS));
		int hours = (int) ((double)(timeMillis) / HOURS);
		timeMillis = (int) (timeMillis-(hours*HOURS));
		int minutes = (int) ((double)(timeMillis) / MINUTES);
		timeMillis = (int) (timeMillis-(minutes*MINUTES));
		int seconds = (int) ((double)(timeMillis) / SECONDS);
		//
		String sDays = (days > 0)? days+"g" : "";
		String sHours = (hours > 0)? hours+"h" : "";
		String sMinutes = (minutes > 0)? minutes+"m" : "";
		String sSeconds = (seconds > 0)? seconds+"s" : "";
		//
		return sDays+sHours+sMinutes+sSeconds;
	}//formatTime
	
	public static String formatSize(double sizeInBytes, int precision){
		return Formatter.round( (sizeInBytes / (Math.pow(10, 6))), precision ) + " MB";
	}//formatTime
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();
	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}//round

}//Formatter
