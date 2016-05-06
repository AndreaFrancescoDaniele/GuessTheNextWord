package guessthenextword.run;

import guessthenextword.compiler.KnowldgeBaseCompiler;
import guessthenextword.knowledge_base.KnowledgeBase;
import guessthenextword.knowledge_base.KnowledgeBases;
import guessthenextword.prophets.Prophet;
import guessthenextword.util.Formatter;
import guessthenextword.util.ImageWindow;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * 
 * @author Dott. Andrea Francesco Daniele
 * 		   1618594 - Università di Roma - La Sapienza
 *
 */
public class ProphetsComparisonDemo {
	
	//==> Fields
	
	private static KnowledgeBase knowledgeBase;
	private static Map<String,List<String>> testset;

	private static double bestAccuracy = Double.MIN_VALUE;
	private static int bestWordsToGuess;
	private static Prophet bestProphet;

	private static int wordsToGuess;
	

	
	//==> Methods

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		//dataset filePath
		String datasetFilePath = "datasets/training-set.txt";

		//open the default knowledge base
		knowledgeBase = KnowledgeBases.getDefaultKnowledgeBase();

		//
		//STEP1: Compile
		
		if( !knowledgeBase.isAvailable() ){
			KnowldgeBaseCompiler kbc = new KnowldgeBaseCompiler("Jaccard");
			kbc.addSource("UkWac", "/home/andrea/NLP/workspace/GuessTheNextWord/datasets/UkWac/UkWac/UKWAC-*.xml");
			kbc.buildKB();
		}

		
		//
		//STEP2: Ask

		testset = readTestSet(datasetFilePath);
		
		
		int[] resultSizeList = new int[]{1, 50};
		int[] itemsPerWordList = new int[]{ 15, 20, 25, 30, 35, 40, 45 };

		//define the number of charts and initialize them
		List<XYSeries>[] seriesLists = new List[resultSizeList.length];
		for( int i=0; i<seriesLists.length; i++ ){
			seriesLists[i] = new LinkedList<XYSeries>();
		}

		//Guess
		
		runSimulation("SimpleSimilarityProphet", resultSizeList, itemsPerWordList, seriesLists );
		
		runSimulation("SimpleIntersectionProphet", resultSizeList, itemsPerWordList, seriesLists );
		
		runSimulation("HybridProphet", resultSizeList, itemsPerWordList, seriesLists );
		
		runSimulation("RewardProphet", resultSizeList, itemsPerWordList, seriesLists );


		//plot the charts
		for( int i=0; i<seriesLists.length; i++ ){
			String s = (resultSizeList[i] > 1)? "s" : "";
			plotNewChart( i, resultSizeList[i]+" Word"+s+" Guessed", seriesLists[i] );
		}

		//close the knowledge base
		knowledgeBase.close();

		//print statistics
		if( bestProphet != null ){
			System.out.println( "BEST: \n ["+bestProphet.toString()+" , resultSize:"+bestWordsToGuess+"] - " + Formatter.round( bestAccuracy, 2 ) + "%" );
		}
	}//main
	
	
	//=> Private/Protected Methods
	
	private static double runProphet( int resultSize, KnowledgeBase knowledgeBase, Map<String,List<String>> testset, Prophet p ){
		List<Integer> distance = new LinkedList<Integer>();
		//
		int total = 1, correct = 0;
		for( Map.Entry<String,List<String>> entry : testset.entrySet() ){
			String wordA = entry.getKey();
			List<String> wordsB = entry.getValue();
			//process
			List<String> guessedWords = p.guess(wordsB, resultSize);
			if( guessedWords.contains(wordA) ){
				correct++;
				distance.add( guessedWords.indexOf(wordA)+1 );
			}
			total++;
		}
		double accuracy = ((double)correct/(double)total)*100;
		if( total == 1 ) accuracy = 0;
		if( accuracy > bestAccuracy ){
			bestAccuracy = accuracy;
			bestWordsToGuess = wordsToGuess;
			bestProphet = p;
		}
		//calculate average
		double sum = 0;
		for (int i=0; i< distance.size(); i++) {
			sum += distance.get(i);
		}
		double avg = sum / ((double)distance.size() );
		//
		System.out.println( "["+p.toString()+"] - " + Formatter.round( accuracy, 2 ) + "% | distAvg:" + Formatter.round( avg, 1 ) );
		return accuracy;
	}//runProphet
	
	private static void runSimulation( String prophetName, int[] resultSizeList, int[] itemsPerWordList, List<XYSeries>[] seriesList ){
		//verify if the requested Prophet exists
		boolean exception = false;
		String prophetID = "";
		try{
			Class<?> c = Class.forName("guessthenextword.prophets."+prophetName);
			Class<? extends Prophet> m = c.asSubclass(Prophet.class);

			Constructor<? extends Prophet> constr = m.getConstructor( new Class[]{KnowledgeBase.class, int.class} );
			Prophet p = constr.newInstance(new Object[] { knowledgeBase, 0 });
			
			prophetID = p.getID();
		}catch(Exception e){
			exception = true;
			e.printStackTrace();
		}
		//
		if( exception ){
			return;
		}
		// run the requested Prophet
		for( int i = 0; i<resultSizeList.length; i++ ){
			wordsToGuess = resultSizeList[i];
			System.out.println("Guessing ("+wordsToGuess+") word(s)");
			XYSeries xys = new XYSeries( prophetID );
			//
			for( int j=0; j<itemsPerWordList.length; j++ ){
				int itemsPerWord = itemsPerWordList[j];
				//
				Prophet p = null;
				try{
					Class<?> c = Class.forName("guessthenextword.prophets."+prophetName);
					Class<? extends Prophet> m = c.asSubclass(Prophet.class);

					Constructor<? extends Prophet> constr = m.getConstructor( new Class[]{KnowledgeBase.class, int.class} );
					p = constr.newInstance(new Object[] { knowledgeBase, itemsPerWord });
				}catch(Exception e){}
				//
				if( p != null ){
					double accuracy = runProphet(wordsToGuess, knowledgeBase, testset, p);
					xys.add(itemsPerWord, accuracy);
				}else{
					return;
				}
			}
			//
			System.out.println();
			seriesList[i].add(xys);
		}
		//
		System.out.println();
	}//runSimulation
	
	private static Map<String,List<String>> readTestSet( String filePath ){
		Map<String,List<String>> result = new LinkedHashMap<String,List<String>>();
		//constants
		final String regEx = "^([\\w]+)[\\s]+~[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)[\\s]+([\\w]+)$";
		final Pattern p = Pattern.compile(regEx);
		final int wordAgroup = 1;
		final int[] wordBgroups = new int[]{2,3,4,5,6};
		//create the file
		File file = new File(filePath);
		//parse the file
		BufferedReader br = null;
		try{
			br = new BufferedReader( new FileReader(file) );
			Matcher m;
			String line = br.readLine();
			while( line != null ){
				m = p.matcher(line);
				if( m.find() ){
					String wordA = m.group(wordAgroup).toLowerCase();
					List<String> wordBlist = new LinkedList<String>();
					for( int g : wordBgroups ){
						String wordB = m.group(g);
						if( wordB != null ){
							wordBlist.add( wordB.toLowerCase() );
						}
					}
					//
					result.put( wordA, wordBlist );
				}
				//
				line = br.readLine();
			}
			//close the reader
			br.close();
		}catch(Exception e){ 
			e.printStackTrace();
			System.exit(-1); 
		}
		return result;
	}//readTestSet
	
	private static void plotNewChart(int id, String name, List<XYSeries> series){
		ImageWindow window = new ImageWindow(id);
		window.setVisible(true);
		window.setTitle(name);
		
		int width = window.getContentPane().getWidth();
		int height = window.getContentPane().getHeight();
		
		XYDataset dataset = new XYSeriesCollection();
		double Xmin = Double.MAX_VALUE;
		double Xmax = Double.MIN_VALUE;
		double Ymin = Double.MAX_VALUE;
		double Ymax = Double.MIN_VALUE;
		for( XYSeries xys : series ){
			((XYSeriesCollection)dataset).addSeries(xys);
			//
			for( Object obj : xys.getItems() ){
				XYDataItem item = (XYDataItem) obj;
				//
				double Xval = item.getXValue();
				if( Xval > Xmax ){
					Xmax = Xval;
				}
				if( Xval < Xmin ){
					Xmin = Xval;
				}
				//
				double Yval = item.getYValue();
				if( Yval > Ymax ){
					Ymax = Yval;
				}
				if( Yval < Ymin ){
					Ymin = Yval;
				}
			}
		}
		
		JFreeChart jchart = ChartFactory.createXYLineChart(name, "Items per Word", "Accuracy (%)", dataset);
		
		XYPlot plot = ((XYPlot)jchart.getPlot());
		plot.getDomainAxis().setRange(Xmin-2, Xmax+2);
		plot.getRangeAxis().setRange(0, Ymax+10);
		
		plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray);
        plot.setRangeGridlinePaint(Color.gray);
		
		XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseShapesVisible(true);
        renderer.setBaseShapesFilled(true);
		
        BufferedImage bi = jchart.createBufferedImage(width, height);
        
        window.setImage(bi);
        window.repaint();
	}//plotNewChart

}//ProphetsComparisonDemo
