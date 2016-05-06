package guessthenextword.compiler;

import guessthenextword.util.Statistics;

import java.util.concurrent.Semaphore;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class ConcurrentSentenceIDProvider {
	
	//==> Fields
	
	public static final int sentencePrefixLength = 3;
	public static final int sentenceSuffixLength = 4;
	
	public static final String sentenceIDregEx = "[A-Z]{"+sentencePrefixLength+"}[0-9]{"+sentenceSuffixLength+"}";
	
	private static String lastSentenceID = null;
	
	private static final char alphabet[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
		'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
	
	private static Semaphore semaphore = new Semaphore(1, true);
	private static int[] idChar = new int[ sentencePrefixLength ];
	private static int idNum = 0;
	
	
	
	//==> Methods
	
	public static void setLastSentenceID(String sentenceID){
		if( sentenceID != null && sentenceID.matches(sentenceIDregEx) ){
			String prefix = sentenceID.substring(0,sentencePrefixLength);
			String suffix = sentenceID.substring(sentencePrefixLength);
			Integer intSuffix = Integer.parseInt(suffix);
			try{
				semaphore.acquire();
				//
				for( int i=0; i<prefix.length(); i++ ){
					idChar[i] = prefix.charAt(i) - 65;
				}
				idNum = intSuffix;
			}catch(InterruptedException ie){}
			finally{
				//
				semaphore.release();
			}
		}
	}//setLastSentenceID
	
	public static String getLastSentenceID(){
		String result = null;
		try{
			semaphore.acquire();
			//
			if( lastSentenceID == null ){
				//build the base ID
				StringBuilder sb = new StringBuilder(sentencePrefixLength+sentenceSuffixLength);
				for( int i=0; i<sentencePrefixLength; i++){
					sb.append(alphabet[idChar[i]]);
				}
				sb.append( String.format("%0"+sentenceSuffixLength+"d", idNum) );
				result = sb.toString().trim();
			}else{
				result = lastSentenceID;
			}
		}catch(InterruptedException ie){}
		finally{
			//
			semaphore.release();
		}
		return result;
	}//getLastSentenceID
	
	public static String getNextSentenceID(){
		String result = null;
		try{
			semaphore.acquire();
			//
			if( (idNum+1) >= Math.pow(10, sentenceSuffixLength) ){
				idNum = 0;
				//SentenceID Chars
				for( int i=idChar.length-1; i>=0; i-- ){
					if( i==0 && alphabet[idChar[i]] == 'Z' ){
						throw new IndexOutOfBoundsException("Out of precision! The sentenceID has reached its maximum value!");
					}
					if( alphabet[idChar[i]] != 'Z' ){
						for( int j=i+1; j<idChar.length; j++ ){
							idChar[j] = 0;
						}
						idChar[i]++;
						break;
					}
					idChar[i]++;
				}
			}
			//SentenceID Nums
			idNum++;
			//build ID
			StringBuilder sb = new StringBuilder(sentencePrefixLength+sentenceSuffixLength);
			for( int i=0; i<sentencePrefixLength; i++){
				sb.append(alphabet[idChar[i]]);
			}
			sb.append( String.format("%0"+sentenceSuffixLength+"d", idNum) );
			result = sb.toString().trim();
			//
			lastSentenceID = result;
			//
		}catch(InterruptedException e){}
		finally{
			semaphore.release();
		}
		Statistics.note("currentJob", result);
		return result;
	}//getNextSentenceID
	
}//ConcurrentSentenceIDProvider